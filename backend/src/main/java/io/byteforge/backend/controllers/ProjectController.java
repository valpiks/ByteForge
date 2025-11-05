package io.byteforge.backend.controllers;

import io.byteforge.backend.model.custom.CustomUserDetails;
import io.byteforge.backend.model.dto.ProjectDto;
import io.byteforge.backend.service.ProjectExportService;
import io.byteforge.backend.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectExportService projectExportService;

    @GetMapping("/project")
    public ResponseEntity<List<ProjectDto.Response>> getProjects(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Get projects for user with id: {}", userDetails.getId());
        return projectService.getProjects(userDetails.getId());
    }

    @PostMapping("/project")
    public ResponseEntity<ProjectDto.Response> createProject(@Valid @RequestBody ProjectDto.Create projectData, @AuthenticationPrincipal CustomUserDetails userDetails) throws BadRequestException {
        log.info("Create project for user with id: {}", userDetails.getId());
        return projectService.createProject(projectData, userDetails.getId());
    }

    @GetMapping("/project/{id}")
    public ResponseEntity<ProjectDto.Response> getProject(@PathVariable("id") Long projectId, @AuthenticationPrincipal CustomUserDetails userDetails) throws BadRequestException {
        log.info("Get project with id: {} for user with id: {}", projectId, userDetails.getId());
        return projectService.getProject(projectId, userDetails.getId());
    }

    @PutMapping("/project/{id}")
    public ResponseEntity<ProjectDto.Response> editProject(@PathVariable("id") Long projectId, @Valid @RequestBody ProjectDto.Create projectData, @AuthenticationPrincipal CustomUserDetails userDetails) throws BadRequestException {
        log.info("Edit project with id: {} for user with id: {}", projectId, userDetails.getId());
        return projectService.editProject(projectData, projectId, userDetails.getId());
    }

    @DeleteMapping("/project/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("id") Long projectId) throws BadRequestException {
        log.info("Delete project with id: {}", projectId);
        return projectService.deleteProject(projectId);
    }

    @GetMapping("/project/{id}/contributors")
    public ResponseEntity<List<ProjectDto.ProjectUsers>> getContributors(@PathVariable("id") Long projectId) throws BadRequestException {
        log.info("Get contributors for project with id: {}", projectId);
        return projectService.getProjectUsers(projectId);
    }

    @PutMapping("/project/{id}/contributors/{contId}")
    public void changeContributorRole(@PathVariable("id") Long projectId, @PathVariable("contId") Long contributorId, @RequestBody ProjectDto.ChangeRole data) {
        log.info("Change contributor for project with id: {}", projectId);
        projectService.changeContributorRole(contributorId, data.getRole());
    }

    @DeleteMapping("/project/{id}/contributors/{contId}")
    public void removeContributor(@PathVariable("id") Long projectId, @PathVariable("contId") Long contributorId) {
        log.info("Remove user from project with id: {}", projectId);
        projectService.removeUserFromSharedProject(contributorId);
    }

    @GetMapping("/project/{id}/join-link")
    public ResponseEntity<ProjectDto.InviteToken> createJoinLink(@PathVariable("id") Long projectId) throws BadRequestException {
        log.info("Create join link for project with id: {}", projectId);
        return projectService.createJoinLink(projectId);
    }

    @PostMapping("/project/join-link")
    public ResponseEntity<ProjectDto.JoinResponse> joinProjectByToken(@RequestBody @Valid ProjectDto.InviteToken body, @AuthenticationPrincipal CustomUserDetails userDetails) throws BadRequestException {
        log.info("Join user to project with token: {}", body.getToken());
        return projectService.joinByToken(body.getToken(), body.getRole(), userDetails.getId());
    }

    @PostMapping("/project/{id}/export")
    public ResponseEntity<ProjectDto.ExportResponse> startExport(@PathVariable("id") Long projectId, @RequestBody ProjectDto.ExportRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Start export for project with id: {}", projectId);
        return ResponseEntity.ok(projectExportService.startExport(projectId, request, userDetails.getId()));
    }

    @GetMapping("/project/{id}/export/{exportId}/status")
    public ResponseEntity<ProjectDto.ExportResponse> getExportStatus(@PathVariable("id") Long projectId, @PathVariable String exportId) {
        log.info("Get export status for project with id: {} and exportId: {}", projectId, exportId);
        return ResponseEntity.ok(projectExportService.getExportStatus(exportId));
    }

    @GetMapping("/project/{id}/export/{exportId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadExport(@PathVariable("id") Long projectId, @PathVariable String exportId) {
        log.info("Download export for project with id: {} and exportId: {}", projectId, exportId);
        return projectExportService.downloadExport(exportId);
    }
}
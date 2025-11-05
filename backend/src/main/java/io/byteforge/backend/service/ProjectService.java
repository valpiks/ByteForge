package io.byteforge.backend.service;

import io.byteforge.backend.exceptions.UserNotFoundException;
import io.byteforge.backend.model.dto.ProjectDto;
import io.byteforge.backend.model.entity.Project;
import io.byteforge.backend.model.entity.SharedProject;
import io.byteforge.backend.model.entity.User;
import io.byteforge.backend.repository.ProjectRepository;
import io.byteforge.backend.repository.SharedProjectRepository;
import io.byteforge.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final SharedProjectRepository sharedProjectRepository;

    public ResponseEntity<ProjectDto.Response> createProject(ProjectDto.Create projectData, Long userId) throws BadRequestException {
        User existUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        Project newProject = new Project();
        newProject.setTitle(projectData.getTitle());
        newProject.setDescription(projectData.getDescription());
        newProject.setOwner(existUser);

        return getResponseResponseEntity(userId, newProject);
    }

    public ResponseEntity<List<ProjectDto.Response>> getProjects(Long userId) {
        List<Object[]> results = projectRepository.findProjectsWithFileCountRaw(userId);

        List<ProjectDto.Response> response = results.stream()
                .map(result
                        -> ProjectDto.Response
                        .toDto((Project) result[0], (Long) result[1]))
                .toList();

        return ResponseEntity
                .status(HttpServletResponse.SC_OK).body(response);
    }

    public ResponseEntity<ProjectDto.Response> getProject(Long projectId, Long userId) throws BadRequestException {
        Project result = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found"));

        return ResponseEntity
                .status(HttpServletResponse.SC_OK).body(ProjectDto.Response
                        .toDto(result, null));
    }

    public ResponseEntity<ProjectDto.Response> editProject(ProjectDto.Create projectData, Long projectId, Long userId) throws BadRequestException {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found"));

        existingProject.setTitle(projectData.getTitle());
        existingProject.setDescription(projectData.getDescription());
        projectRepository.save(existingProject);

        return getResponseResponseEntity(userId, existingProject);
    }

    private ResponseEntity<ProjectDto.Response> getResponseResponseEntity(Long userId, Project existingProject) throws BadRequestException {
        Project savedProject = projectRepository.save(existingProject);

        Project project = projectRepository.findProjectByIdAndOwner(savedProject.getId(), userId)
                .orElseThrow(() -> new BadRequestException("Project not found after update"));

        Long fileCount = projectRepository.countFilesByProjectId(savedProject.getId());

        ProjectDto.Response responseDto = ProjectDto.Response.toDto(project, fileCount);
        return ResponseEntity.status(HttpServletResponse.SC_ACCEPTED).body(responseDto);
    }

    public ResponseEntity<Void> deleteProject(Long projectId) throws BadRequestException {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found"));

        projectRepository.delete(existingProject);

        return ResponseEntity.status(HttpServletResponse.SC_OK).build();
    }

    public ResponseEntity<List<ProjectDto.ProjectUsers>> getProjectUsers(Long projectId) throws BadRequestException {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not exists"));

        List<ProjectDto.ProjectUsers> users = new ArrayList<>();

        users.add(ProjectDto.ProjectUsers
                .toOwner(userRepository.findById(existingProject.getOwner().getId())));

        sharedProjectRepository.findSharedProjectByProject(existingProject)
                .forEach(sharedProject ->
                        users.add(ProjectDto.ProjectUsers.toUser(sharedProject)));

        return ResponseEntity.status(HttpServletResponse.SC_OK).body(users);
    }

    public void changeContributorRole(Long userId, String role) {
        SharedProject existingSharedProject = sharedProjectRepository.findSharedProjectByUser_Id(userId);
        existingSharedProject.setAccessLevel(role);
        sharedProjectRepository.save(existingSharedProject);
    }

    public void removeUserFromSharedProject(Long userId) {
        SharedProject existingSharedProject = sharedProjectRepository.findSharedProjectByUser_Id(userId);
        sharedProjectRepository.delete(existingSharedProject);
    }

    public ResponseEntity<ProjectDto.InviteToken> createJoinLink(Long projectId) throws BadRequestException {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found"));

        if (!existingProject.getInviteToken().isEmpty()) {
            return ResponseEntity.status(HttpServletResponse.SC_CREATED).body(ProjectDto.InviteToken.toDto(existingProject.getInviteToken()));
        }

        String token = UUID.randomUUID().toString();

        existingProject.setInviteToken(token);
        projectRepository.save(existingProject);

        return ResponseEntity.status(HttpServletResponse.SC_CREATED).body(ProjectDto.InviteToken.toDto(token));
    }

    public ResponseEntity<ProjectDto.JoinResponse> joinByToken(String token, String role, Long userId) throws BadRequestException {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));


        Project existingProject = projectRepository.findProjectsByInviteToken(token)
                .orElseThrow(() -> new BadRequestException("Project not found"));

        if (userId.equals(existingProject.getOwner().getId())) {
            throw new BadRequestException("You already own this project");
        }

        SharedProject newJoin = new SharedProject();
        newJoin.setProject(existingProject);
        newJoin.setUser(existingUser);
        newJoin.setAccessLevel(role);

        sharedProjectRepository.save(newJoin);

        return ResponseEntity.status(HttpServletResponse.SC_CREATED)
                .body(ProjectDto.JoinResponse.toDto(existingProject.getId()));
    }
}

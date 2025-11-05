package io.byteforge.backend.controllers;

import io.byteforge.backend.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/project/{projectId}")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileService fileService;

    @GetMapping("/files")
    public ResponseEntity<?> getFiles(@PathVariable Long projectId) throws BadRequestException {
        log.info("Get files for project with id: {}", projectId);
        return fileService.getFiles(projectId);
    }
}
package io.byteforge.backend.service;

import io.byteforge.backend.model.custom.FileType;
import io.byteforge.backend.model.dto.FileDto;
import io.byteforge.backend.model.entity.Project;
import io.byteforge.backend.model.entity.ProjectFile;
import io.byteforge.backend.repository.ProjectFileRepository;
import io.byteforge.backend.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;

    public ResponseEntity<?> getFiles(Long projectId) throws BadRequestException {
        if (!projectRepository.existsById(projectId)) {
            throw new BadRequestException("Project not exists");
        }

        List<ProjectFile> projectFiles = projectFileRepository.findByProject_Id(projectId);

        List<FileDto.Response> response = projectFiles.stream()
                .map(FileDto.Response::toDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    public FileDto.Response createFile(Long projectId, String fileName, String filePath, String type, Long parentId) throws BadRequestException {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not exists"));

        ProjectFile newFile = new ProjectFile();
        newFile.setName(fileName);
        newFile.setPath(filePath);
        newFile.setType(FileType.valueOf(type));
        newFile.setProject(existingProject);

        if (parentId != 0) {
            ProjectFile parent = projectFileRepository.findById(parentId)
                    .orElseThrow(() -> new BadRequestException("Parent folder not found"));
            newFile.setParent(parent);
        }

        if (projectFileRepository.existsProjectFileByProject_IdAndPathAndNameAndType(projectId, filePath, fileName, FileType.valueOf(type))) {
            throw new BadRequestException("File/folder with this path already exists");
        }

        return FileDto.Response.toDto(projectFileRepository.save(newFile));
    }

    public ResponseEntity<FileDto.Response> updateFile(Long fileId, String fileData) throws BadRequestException {
        ProjectFile existingFile = projectFileRepository.findById(fileId)
                .orElseThrow(() -> new BadRequestException("File not exists"));

        existingFile.setContent(fileData);

        return ResponseEntity.ok(FileDto.Response.toDto(projectFileRepository.save(existingFile)));
    }

    public void renameFile(Long fileId, String name) throws BadRequestException {
        ProjectFile existingFile = projectFileRepository.findById(fileId)
                .orElseThrow(() -> new BadRequestException("File not exists"));

        existingFile.setName(name);
        projectFileRepository.save(existingFile);
    }

    @Transactional
    public void deleteFile(Long fileId) throws BadRequestException {
        ProjectFile existingFile = projectFileRepository.findById(fileId)
                .orElseThrow(() -> new BadRequestException("File not exists"));

        existingFile.softDelete();

        projectFileRepository.save(existingFile);

        ResponseEntity.noContent().build();
    }
}
package io.byteforge.backend.repository;

import io.byteforge.backend.model.custom.FileType;
import io.byteforge.backend.model.entity.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

    List<ProjectFile> findByProject_Id(Long projectId);

    boolean existsProjectFileByProject_IdAndPathAndNameAndType(Long projectId, String path, String name, FileType type);

    @Query("SELECT pf FROM ProjectFile pf LEFT JOIN FETCH pf.children WHERE pf.project.id = :projectId AND pf.deleted = false")
    List<ProjectFile> findAllByProjectIdWithChildren(@Param("projectId") Long projectId);
}

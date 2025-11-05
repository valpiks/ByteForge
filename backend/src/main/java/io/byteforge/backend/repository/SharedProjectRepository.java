package io.byteforge.backend.repository;

import io.byteforge.backend.model.entity.Project;
import io.byteforge.backend.model.entity.SharedProject;
import io.byteforge.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface SharedProjectRepository extends JpaRepository<SharedProject, Long> {

    List<SharedProject> findSharedProjectByProject(Project project);

    SharedProject findSharedProjectByProject_Id(Long projectId);

    SharedProject findSharedProjectByUser_Id(Long userId);

    Long user(User user);
}

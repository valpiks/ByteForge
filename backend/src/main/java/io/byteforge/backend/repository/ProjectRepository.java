package io.byteforge.backend.repository;

import io.byteforge.backend.model.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findProjectsByInviteToken(String inviteToken);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.owner WHERE p.id = :projectId AND p.owner.id = :userId")
    Optional<Project> findProjectByIdAndOwner(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query("SELECT COUNT(pf) FROM ProjectFile pf WHERE pf.project.id = :projectId")
    Long countFilesByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT p, COUNT(pf) " +
            "FROM Project p " +
            "LEFT JOIN p.files pf " +
            "LEFT JOIN p.sharedWithUsers su " +
            "WHERE p.owner.id = :userId OR su.user.id = :userId " +
            "GROUP BY p")
    List<Object[]> findProjectsWithFileCountRaw(@Param("userId") Long userId);
}

package io.byteforge.backend.model.entity;

import io.byteforge.backend.model.custom.FileType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "project_files")
@SQLRestriction("deleted = false")
public class ProjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "current_version")
    private Integer currentVersion = 1;

    @Column(name = "last_version_date")
    private LocalDateTime lastVersionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ProjectFile parent;

    @OneToMany(mappedBy = "parent")
    private List<ProjectFile> children = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastVersionDate = LocalDateTime.now();
        deleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (!deleted) {
            lastVersionDate = LocalDateTime.now();
        }
    }

    public void softDelete() {
        this.deleted = true;
        if (this.children != null) {
            this.children.forEach(ProjectFile::softDelete);
        }
    }

    public void restore() {
        this.deleted = false;
        if (this.children != null) {
            this.children.forEach(ProjectFile::restore);
        }
    }
}
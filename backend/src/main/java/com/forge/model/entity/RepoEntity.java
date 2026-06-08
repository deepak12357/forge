package com.forge.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "repo")
@Getter
@Setter
public class RepoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "git_url", nullable = false, length = 1000)
    private String gitUrl;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "class_count", nullable = false)
    private Integer classCount = 0;

    @Column(name = "method_count", nullable = false)
    private Integer methodCount = 0;

    @Column(name = "parse_failure_count", nullable = false)
    private Integer parseFailureCount = 0;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "repo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassNodeEntity> classNodes;
}

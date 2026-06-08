package com.forge.model.entity;

import jakarta.persistence.*;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "class_node")
@Getter
@Setter
public class ClassNodeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // FK → repo
  @ManyToOne(optional = false)
  @JoinColumn(name = "repo_id", nullable = false)
  private RepoEntity repo;

  @Column(name = "package_name", length = 500)
  private String packageName;

  @Column(name = "class_name", nullable = false, length = 255)
  private String className;

  @Column(name = "fully_qualified_name", nullable = false, length = 1000)
  private String fullyQualifiedName;

  @Column(name = "file_path", nullable = false, length = 2000)
  private String filePath;

  @Column(name = "start_line")
  private Integer startLine;

  @Column(name = "end_line")
  private Integer endLine;

  // 1 Class → many Methods
  @OneToMany(mappedBy = "classNode", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MethodNodeEntity> methodNodes;
}

package com.forge.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "edge")
@Getter
@Setter
public class EdgeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "repo_id", nullable = false)
  private RepoEntity repo;

  @ManyToOne
  @JoinColumn(name = "from_method_node_id")
  private MethodNodeEntity fromMethodNode;

  @ManyToOne
  @JoinColumn(name = "to_method_node_id")
  private MethodNodeEntity toMethodNode;

  @Column(name = "type", nullable = false, length = 50)
  private String type;

  // Optional metadata (e.g. qualified name, line info) stored as text/json
  @Column(name = "metadata", columnDefinition = "text")
  private String metadata;
}

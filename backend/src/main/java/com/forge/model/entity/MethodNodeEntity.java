package com.forge.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "method_node")
@Getter
@Setter
public class MethodNodeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // FK → class_node
  @ManyToOne(optional = false)
  @JoinColumn(name = "class_node_id", nullable = false)
  private ClassNodeEntity classNode;

  @Column(name = "method_name", nullable = false, length = 255)
  private String methodName;

  @Column(name = "signature", nullable = false, length = 1000)
  private String signature;

  @Column(name = "return_type", length = 500)
  private String returnType;

  @Column(name = "modifiers", length = 500)
  private String modifiers;

  @Column(name = "start_line")
  private Integer startLine;

  @Column(name = "end_line")
  private Integer endLine;

  @Column(name = "parameter_types", length = 2000)
  private String parameterTypes;
}

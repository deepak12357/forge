package com.forge.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTOs for the map API response. Uses Lombok to reduce boilerplate. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapResponse {
  private List<ClassDto> classes;
  private List<MethodDto> methods;
  private List<EdgeDto> edges;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ClassDto {
    private Long id;
    private String packageName;
    private String className;
    private String fullyQualifiedName;
    private String filePath;
    private Integer startLine;
    private Integer endLine;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MethodDto {
    private Long id;
    private Long classId;
    private String methodName;
    private String signature;
    private String returnType;
    private String modifiers;
    private Integer startLine;
    private Integer endLine;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EdgeDto {
    private Long id;
    private Long fromMethodId;
    private Long toMethodId;
    private String type;
    private String metadata;
  }
}

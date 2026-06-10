package com.forge.api;

import com.forge.model.entity.ClassNodeEntity;
import com.forge.model.entity.MethodNodeEntity;
import com.forge.model.entity.EdgeEntity;
import com.forge.repo.ClassNodeRepository;
import com.forge.repo.MethodNodeRepository;
import com.forge.repo.EdgeRepository;
import com.forge.repo.RepoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.forge.model.dto.MapResponse;
import com.forge.model.dto.MapResponse.ClassDto;
import com.forge.model.dto.MapResponse.MethodDto;
import com.forge.model.dto.MapResponse.EdgeDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/repos")
public class MapController {
  private final RepoRepository repoRepository;
  private final ClassNodeRepository classNodeRepository;
  private final MethodNodeRepository methodNodeRepository;
  private final EdgeRepository edgeRepository;

  public MapController(
      RepoRepository repoRepository,
      ClassNodeRepository classNodeRepository,
      MethodNodeRepository methodNodeRepository,
      EdgeRepository edgeRepository) {
    this.repoRepository = repoRepository;
    this.classNodeRepository = classNodeRepository;
    this.methodNodeRepository = methodNodeRepository;
    this.edgeRepository = edgeRepository;
  }

  @GetMapping("/{id}/map")
  public ResponseEntity<MapResponse> getMap(@PathVariable("id") Long repoId) {
    Optional<?> repoOpt = repoRepository.findById(repoId);
    if (repoOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    List<ClassNodeEntity> classes = classNodeRepository.findByRepoId(repoId);
    List<MethodNodeEntity> methods = new ArrayList<>();
    if (!classes.isEmpty()) {
      methods = methodNodeRepository.findByClassNodeIn(classes);
    }

    List<EdgeEntity> edges = edgeRepository.findByRepoId(repoId);

    // Build DTOs
    List<ClassDto> classDtos = classes.stream()
        .map(c -> new ClassDto(c.getId(), c.getPackageName(), c.getClassName(), c.getFullyQualifiedName(), c.getFilePath(), c.getStartLine(), c.getEndLine()))
        .collect(Collectors.toList());

    List<MethodDto> methodDtos = methods.stream()
        .map(m -> new MethodDto(m.getId(), m.getClassNode().getId(), m.getMethodName(), m.getSignature(), m.getReturnType(), m.getModifiers(), m.getStartLine(), m.getEndLine()))
        .collect(Collectors.toList());

    List<EdgeDto> edgeDtos = edges.stream().map(e -> {
      Long fromId = e.getFromMethodNode() == null ? null : e.getFromMethodNode().getId();
      Long toId = e.getToMethodNode() == null ? null : e.getToMethodNode().getId();
      return new EdgeDto(e.getId(), fromId, toId, e.getType(), e.getMetadata());
    }).collect(Collectors.toList());

    MapResponse resp = new MapResponse(classDtos, methodDtos, edgeDtos);

    return ResponseEntity.ok(resp);
  }

  // DTOs
  public static class MapResponse {
    public List<ClassDto> classes;
    public List<MethodDto> methods;
    public List<EdgeDto> edges;

    public MapResponse(List<ClassDto> classes, List<MethodDto> methods, List<EdgeDto> edges) {
      this.classes = classes;
      this.methods = methods;
      this.edges = edges;
    }
  }

  public static class ClassDto {
    public Long id;
    public String packageName;
    public String className;
    public String fullyQualifiedName;
    public String filePath;
    public Integer startLine;
    public Integer endLine;

    public ClassDto(Long id, String packageName, String className, String fullyQualifiedName, String filePath, Integer startLine, Integer endLine) {
      this.id = id;
      this.packageName = packageName;
      this.className = className;
      this.fullyQualifiedName = fullyQualifiedName;
      this.filePath = filePath;
      this.startLine = startLine;
      this.endLine = endLine;
    }
  }

  public static class MethodDto {
    public Long id;
    public Long classId;
    public String methodName;
    public String signature;
    public String returnType;
    public String modifiers;
    public Integer startLine;
    public Integer endLine;

    public MethodDto(Long id, Long classId, String methodName, String signature, String returnType, String modifiers, Integer startLine, Integer endLine) {
      this.id = id;
      this.classId = classId;
      this.methodName = methodName;
      this.signature = signature;
      this.returnType = returnType;
      this.modifiers = modifiers;
      this.startLine = startLine;
      this.endLine = endLine;
    }
  }

  public static class EdgeDto {
    public Long id;
    public Long fromMethodId;
    public Long toMethodId;
    public String type;
    public String metadata;

    public EdgeDto(Long id, Long fromMethodId, Long toMethodId, String type, String metadata) {
      this.id = id;
      this.fromMethodId = fromMethodId;
      this.toMethodId = toMethodId;
      this.type = type;
      this.metadata = metadata;
    }
  }
}


package com.forge.service;

import com.forge.model.entity.ClassNodeEntity;
import com.forge.model.entity.EdgeEntity;
import com.forge.model.entity.MethodNodeEntity;
import com.forge.model.entity.RepoEntity;
import com.forge.repo.ClassNodeRepository;
import com.forge.repo.EdgeRepository;
import com.forge.repo.MethodNodeRepository;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CallGraphService {
  private static final Logger log = LoggerFactory.getLogger(CallGraphService.class);

  private final ClassNodeRepository classNodeRepository;
  private final MethodNodeRepository methodNodeRepository;
  private final EdgeRepository edgeRepository;

  public CallGraphService(
      ClassNodeRepository classNodeRepository,
      MethodNodeRepository methodNodeRepository,
      EdgeRepository edgeRepository) {
    this.classNodeRepository = classNodeRepository;
    this.methodNodeRepository = methodNodeRepository;
    this.edgeRepository = edgeRepository;
  }

  /**
   * Build call graph for a cloned repository directory and persist edges. Best-effort resolution:
   * resolves when symbol solver can resolve declarations inside the source tree. Unresolved calls
   * are skipped but counted in logs.
   */
  public void buildCallGraph(File repoDir, RepoEntity repo) {
    try {
      // Prepare type solver using reflection + source
      CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
      combinedTypeSolver.add(new ReflectionTypeSolver());
      combinedTypeSolver.add(new JavaParserTypeSolver(repoDir));

      JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);

      ParserConfiguration config = new ParserConfiguration();
      config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
      config.setSymbolResolver(symbolSolver);

      StaticJavaParser.setConfiguration(config);

      // Load persisted classes & methods for this repo into maps to avoid DB lookups per edge
      List<ClassNodeEntity> classes = classNodeRepository.findByRepoId(repo.getId());
      List<MethodNodeEntity> methods = methodNodeRepository.findByClassNodeIn(classes);

      // Key: fullyQualifiedClassName#methodName#parameterTypes -> MethodNodeEntity
      // This allows precise matching of overloaded methods
      Map<String, MethodNodeEntity> methodMap = new HashMap<>();
      for (MethodNodeEntity m : methods) {
        String classFqn = m.getClassNode().getFullyQualifiedName();
        String paramTypes = m.getParameterTypes() != null ? m.getParameterTypes() : "";
        String key = classFqn + "#" + m.getMethodName() + "#" + paramTypes;
        methodMap.put(key, m);
      }
      log.debug("Loaded {} methods into call-graph lookup map", methodMap.size());

      // Track created edges to avoid duplicates
      Set<String> created = new HashSet<>();

      // Walk java files
      List<File> javaFiles = walkJavaFiles(repoDir);
      java.util.concurrent.atomic.AtomicInteger resolvedCount =
          new java.util.concurrent.atomic.AtomicInteger(0);
      java.util.concurrent.atomic.AtomicInteger unresolvedCount =
          new java.util.concurrent.atomic.AtomicInteger(0);

      for (File f : javaFiles) {
        try {
          CompilationUnit cu = StaticJavaParser.parse(f);

          // For each method declaration, find method calls
          cu.findAll(MethodDeclaration.class)
              .forEach(
                  md -> {
                    String classFqn =
                        md.findAncestor(
                                com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                            .flatMap(c -> c.getFullyQualifiedName())
                            .orElse(null);

                    if (classFqn == null) return;

                    // Extract caller method's parameter types
                    String callerParamTypes =
                        md.getParameters().isEmpty()
                            ? ""
                            : md.getParameters().stream()
                                .map(p -> p.getTypeAsString())
                                .collect(java.util.stream.Collectors.joining(","));

                    String callerKey =
                        classFqn + "#" + md.getNameAsString() + "#" + callerParamTypes;
                    MethodNodeEntity callerTemp = methodMap.get(callerKey);
                    if (callerTemp == null) {
                      // caller may be anonymous or not persisted; try without param types
                      // (fallback)
                      callerTemp =
                          methodMap.values().stream()
                              .filter(
                                  m ->
                                      m.getClassNode().getFullyQualifiedName().equals(classFqn)
                                          && m.getMethodName().equals(md.getNameAsString()))
                              .findFirst()
                              .orElse(null);

                      if (callerTemp == null) {
                        // caller not found; skip
                        return;
                      }
                    }
                    final MethodNodeEntity caller = callerTemp;

                    md.findAll(MethodCallExpr.class)
                        .forEach(
                            call -> {
                              try {
                                ResolvedMethodDeclaration resolved = call.resolve();
                                final String declClass =
                                    resolved.declaringType().getQualifiedName();
                                final String declName = resolved.getName();

                                // Extract parameter types from the resolved method for precise
                                // overload matching
                                String paramTypes =
                                    resolved.getNumberOfParams() == 0
                                        ? ""
                                        : java.util.stream.IntStream.range(
                                                0, resolved.getNumberOfParams())
                                            .mapToObj(i -> resolved.getParam(i).describeType())
                                            .collect(java.util.stream.Collectors.joining(","));

                                // Try exact match first
                                String calleeKey = declClass + "#" + declName + "#" + paramTypes;
                                MethodNodeEntity calleeFound = methodMap.get(calleeKey);

                                // If no exact match, try without parameter types (allows for format
                                // mismatches)
                                if (calleeFound == null && !paramTypes.isEmpty()) {
                                  // Look for any method with same class and name (best effort)
                                  calleeFound =
                                      methodMap.values().stream()
                                          .filter(
                                              m ->
                                                  m.getClassNode()
                                                          .getFullyQualifiedName()
                                                          .equals(declClass)
                                                      && m.getMethodName().equals(declName))
                                          .findFirst()
                                          .orElse(null);
                                }

                                final MethodNodeEntity callee = calleeFound;
                                if (callee != null) {
                                  String edgeKey = caller.getId() + "->" + callee.getId() + ":CALL";
                                  if (!created.contains(edgeKey)) {
                                    EdgeEntity edge = new EdgeEntity();
                                    edge.setRepo(repo);
                                    edge.setFromMethodNode(caller);
                                    edge.setToMethodNode(callee);
                                    edge.setType("CALL");
                                    edge.setMetadata(resolved.getQualifiedSignature());
                                    edgeRepository.save(edge);
                                    created.add(edgeKey);
                                  }
                                  resolvedCount.incrementAndGet();
                                } else {
                                  unresolvedCount.incrementAndGet();
                                  if (log.isDebugEnabled()) {
                                    log.debug(
                                        "Could not resolve call: {} (from {}.{})",
                                        calleeKey,
                                        classFqn,
                                        md.getNameAsString());
                                  }
                                }
                              } catch (Exception e) {
                                // symbol resolution failed for this call
                                unresolvedCount.incrementAndGet();
                                if (log.isDebugEnabled()) {
                                  log.debug(
                                      "Symbol resolution failed for call in {}.{}: {}",
                                      classFqn,
                                      md.getNameAsString(),
                                      e.getMessage());
                                }
                              }
                            });
                  });
        } catch (Exception e) {
          log.warn("Failed to parse file for call graph: {}", f.getAbsolutePath(), e);
        }
      }

      int totalCalls = resolvedCount.get() + unresolvedCount.get();
      log.info(
          "Call graph built for repo {} — analyzed {} files, found {} method calls: {} resolved, {} unresolved",
          repo.getId(),
          javaFiles.size(),
          totalCalls,
          resolvedCount.get(),
          unresolvedCount.get());

    } catch (Exception e) {
      log.error("Error building call graph for repo {}: {}", repo.getId(), e.getMessage(), e);
    }
  }

  private List<File> walkJavaFiles(File rootDir) {
    List<File> javaFiles = new java.util.ArrayList<>();
    scanDirectory(rootDir, javaFiles);
    return javaFiles;
  }

  private void scanDirectory(File file, List<File> javaFiles) {
    if (file == null) return;
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          scanDirectory(child, javaFiles);
        }
      }
    } else {
      if (file.getName().endsWith(".java")) {
        javaFiles.add(file);
      }
    }
  }
}

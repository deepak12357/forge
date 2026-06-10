package com.forge.service;

import com.forge.model.entity.ClassNodeEntity;
import com.forge.model.entity.MethodNodeEntity;
import com.forge.model.entity.RepoEntity;
import com.forge.repo.ClassNodeRepository;
import com.forge.repo.MethodNodeRepository;
import com.forge.repo.RepoRepository;
import com.forge.service.AstVisitor.ClassMetadata;
import com.forge.service.AstVisitor.MethodMetadata;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;
import com.forge.service.CallGraphService;

@Service
public class RepoIngestionService {
  private final RepoRepository repoRepository;
  private final ClassNodeRepository classNodeRepository;
  private final MethodNodeRepository methodNodeRepository;
  private final CallGraphService callGraphService;
  private static final String BASE_DIR = "forge-workspace";
  private final JavaParser javaParser;

  public RepoIngestionService(
      RepoRepository repoRepository,
      ClassNodeRepository classNodeRepository,
      MethodNodeRepository methodNodeRepository,
      CallGraphService callGraphService) {
    this.repoRepository = repoRepository;
    this.classNodeRepository = classNodeRepository;
    this.methodNodeRepository = methodNodeRepository;
    this.callGraphService = callGraphService;

    ParserConfiguration config =
        new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
    this.javaParser = new JavaParser(config);
  }

  public void ingest(String gitUrl) {

    // STEP 1: create entity object
    RepoEntity repo = new RepoEntity();

    repo.setGitUrl(gitUrl);
    repo.setStatus("INITIATED");

    repo.setClassCount(0);
    repo.setMethodCount(0);
    repo.setParseFailureCount(0);

    repo.setCreatedAt(OffsetDateTime.now());
    repo.setUpdatedAt(OffsetDateTime.now());

    // STEP 2: save to DB
    repoRepository.save(repo);

    File repoDir = null;

    try {
      // STEP 3: clone repository
      repoDir = cloneRepository(gitUrl, repo.getId());

      repo.setStatus("CLONED");
      repo.setUpdatedAt(OffsetDateTime.now());
      repoRepository.save(repo);

      System.out.println("Repo cloned at: " + repoDir.getAbsolutePath());

      // STEP 4: update to PARSING status
      repo.setStatus("PARSING");
      repo.setUpdatedAt(OffsetDateTime.now());
      repoRepository.save(repo);

      // STEP 5: discover and parse java files
      List<File> javaFiles = findJavaFiles(repoDir);

      System.out.println("Java files found: " + javaFiles.size());

      // Track counts for this ingestion
      AtomicInteger classCount = new AtomicInteger(0);
      AtomicInteger methodCount = new AtomicInteger(0);
      AtomicInteger parseFailureCount = new AtomicInteger(0);

      for (File file : javaFiles) {
        try {
          ParseResult<CompilationUnit> parseResult = javaParser.parse(file);
          if (parseResult.isSuccessful()) {
            CompilationUnit cu = parseResult.getResult().get();
            String packageName =
                cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse(null);

            // Use visitor to extract metadata
            AstVisitor visitor = new AstVisitor();
            visitor.visit(cu, null);

            // Persist classes and methods
            for (ClassMetadata classMetadata : visitor.getClasses()) {
              ClassNodeEntity classNode = new ClassNodeEntity();
              classNode.setRepo(repo);
              classNode.setPackageName(packageName);
              classNode.setClassName(classMetadata.getClassName());
              classNode.setFullyQualifiedName(
                  packageName == null
                      ? classMetadata.getClassName()
                      : packageName + "." + classMetadata.getClassName());
              classNode.setFilePath(file.getAbsolutePath());
              classNode.setStartLine(classMetadata.getStartLine());
              classNode.setEndLine(classMetadata.getEndLine());

              ClassNodeEntity savedClass = classNodeRepository.save(classNode);
              classCount.incrementAndGet();

              // Persist methods for this class
              for (MethodMetadata methodMetadata : classMetadata.getMethods()) {
                MethodNodeEntity methodNode = new MethodNodeEntity();
                methodNode.setClassNode(savedClass);
                methodNode.setMethodName(methodMetadata.getMethodName());
                methodNode.setSignature(methodMetadata.getSignature());
                methodNode.setReturnType(methodMetadata.getReturnType());
                methodNode.setStartLine(methodMetadata.getStartLine());
                methodNode.setEndLine(methodMetadata.getEndLine());
                methodNode.setModifiers(methodMetadata.getModifiers());

                methodNodeRepository.save(methodNode);
                methodCount.incrementAndGet();
              }
            }
          } else {
            System.out.println("FAILED FILE (unparseable): " + file.getName());
            parseFailureCount.incrementAndGet();
          }
        } catch (Exception e) {
          System.out.println("EXCEPTION PARSING FILE: " + file.getName());
          e.printStackTrace();
          parseFailureCount.incrementAndGet();
        }
      }

      // STEP 6: build call graph (best-effort) and update repository with counts and mark as PARSED
      try {
        callGraphService.buildCallGraph(repoDir, repo);
      } catch (Exception e) {
        System.err.println("Call graph build failed: " + e.getMessage());
        e.printStackTrace();
      }

      repo.setClassCount(classCount.get());
      repo.setMethodCount(methodCount.get());
      repo.setParseFailureCount(parseFailureCount.get());
      repo.setStatus("PARSED");
      repo.setUpdatedAt(OffsetDateTime.now());
      repoRepository.save(repo);

      System.out.println(
          "Repo parsing completed. Classes: "
              + classCount.get()
              + ", Methods: "
              + methodCount.get()
              + ", Parse failures: "
              + parseFailureCount.get());
      System.out.println("Saved repo with id: " + repo.getId());

    } catch (Exception e) {
      System.out.println("INGESTION FAILED: " + e.getMessage());
      e.printStackTrace();

      repo.setStatus("FAILED_PARSE");
      repo.setUpdatedAt(OffsetDateTime.now());
      repoRepository.save(repo);

      throw new RuntimeException("Failed to ingest repository: " + gitUrl, e);

    } finally {
      // STEP 7: cleanup cloned repository
      if (repoDir != null && repoDir.exists()) {
        try {
          deleteDir(repoDir);
          System.out.println("Cleaned up repo directory: " + repoDir.getAbsolutePath());
        } catch (Exception e) {
          System.err.println("Failed to cleanup repo directory: " + e.getMessage());
          e.printStackTrace();
        }
      }
    }
  }

  private void ensureWorkspaceExists() {
    java.io.File dir = new java.io.File(BASE_DIR);
    if (!dir.exists()) {
      dir.mkdirs();
    }
  }

  private File cloneRepository(String gitUrl, Long repoId) throws Exception {

    ensureWorkspaceExists();

    String folderName = "repo_" + repoId;
    File targetDir = new File(BASE_DIR + "/" + folderName);

    // If already exists, delete later (we keep simple for now)
    if (targetDir.exists()) {
      deleteDir(targetDir);
    }

    Git.cloneRepository().setURI(gitUrl).setDirectory(targetDir).call();

    return targetDir;
  }

  private void deleteDir(File file) {
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        deleteDir(child);
      }
    }
    file.delete();
  }

  private List<File> findJavaFiles(File rootDir) {

    List<File> javaFiles = new ArrayList<>();

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

package com.forge.service;

import com.forge.model.entity.ClassNodeEntity;
import com.forge.model.entity.MethodNodeEntity;
import com.forge.model.entity.RepoEntity;
import com.forge.repo.ClassNodeRepository;
import com.forge.repo.MethodNodeRepository;
import com.forge.repo.RepoRepository;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

@Service
public class RepoIngestionService {
  private final RepoRepository repoRepository;
  private final ClassNodeRepository classNodeRepository;
  private final MethodNodeRepository methodNodeRepository;
  private static final String BASE_DIR = "forge-workspace";
  private final JavaParser javaParser;

  public RepoIngestionService(
      RepoRepository repoRepository,
      ClassNodeRepository classNodeRepository,
      MethodNodeRepository methodNodeRepository) {
    this.repoRepository = repoRepository;
    this.classNodeRepository = classNodeRepository;
    this.methodNodeRepository = methodNodeRepository;

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

    try {
      File repoDir = cloneRepository(gitUrl, repo.getId());

      repo.setStatus("CLONED");
      repo.setUpdatedAt(OffsetDateTime.now());
      repoRepository.save(repo);

      System.out.println("Repo cloned at: " + repoDir.getAbsolutePath());

      // STEP 3: discover java files
      List<File> javaFiles = findJavaFiles(repoDir);

      System.out.println("Java files found: " + javaFiles.size());
      for (File file : javaFiles) {
        parseAndSave(file, repo);
      }
    } catch (Exception e) {
      repo.setStatus("FAILED_CLONE");
      repoRepository.save(repo);
      throw new RuntimeException(e);
    }

    System.out.println("Saved repo with id: " + repo.getId());
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

  private void parseAndSave(File file, RepoEntity repo) {
    try {
      ParseResult<CompilationUnit> result = javaParser.parse(file);

      if (result.getResult().isEmpty()) {
        System.out.println("FAILED FILE (unparseable): " + file.getName());
        return;
      }

      CompilationUnit cu = result.getResult().get();

      String packageName =
          cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse(null);

      cu.findAll(ClassOrInterfaceDeclaration.class)
          .forEach(
              cls -> {

                // CREATE CLASS ENTITY
                ClassNodeEntity classNode = new ClassNodeEntity();
                classNode.setRepo(repo);
                classNode.setPackageName(packageName);
                classNode.setClassName(cls.getNameAsString());
                classNode.setFullyQualifiedName(
                    packageName == null
                        ? cls.getNameAsString()
                        : packageName + "." + cls.getNameAsString());
                classNode.setFilePath(file.getAbsolutePath());

                ClassNodeEntity savedClass = classNodeRepository.save(classNode);

                // METHODS
                cls.getMethods()
                    .forEach(
                        method -> {
                          MethodNodeEntity methodNode = new MethodNodeEntity();
                          methodNode.setClassNode(savedClass);
                          methodNode.setMethodName(method.getNameAsString());
                          methodNode.setSignature(method.getDeclarationAsString());
                          methodNode.setReturnType(method.getTypeAsString());

                          methodNodeRepository.save(methodNode);
                        });
              });

    } catch (Exception e) {
      System.out.println("FAILED FILE: " + file.getName());
      e.printStackTrace();
    }
  }
}

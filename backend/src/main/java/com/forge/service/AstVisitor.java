package com.forge.service;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * AST Visitor for extracting class and method metadata from a parsed Java file. Traverses the AST
 * and collects structured information about classes and methods.
 */
public class AstVisitor extends VoidVisitorAdapter<Void> {

  @Getter private final List<ClassMetadata> classes = new ArrayList<>();

  @Override
  public void visit(ClassOrInterfaceDeclaration classDecl, Void arg) {
    // Extract class metadata
    ClassMetadata classMetadata = new ClassMetadata();
    classMetadata.setClassName(classDecl.getNameAsString());
    classMetadata.setStartLine(classDecl.getBegin().map(pos -> pos.line).orElse(null));
    classMetadata.setEndLine(classDecl.getEnd().map(pos -> pos.line).orElse(null));

    // Capture modifiers (public, private, abstract, final, etc.)
    String modifiers =
        classDecl.getModifiers().isEmpty()
            ? "package-private"
            : classDecl.getModifiers().stream()
                .map(Object::toString)
                .reduce((a, b) -> a + " " + b)
                .orElse("package-private");
    classMetadata.setModifiers(modifiers);

    // Extract method metadata from this class
    List<MethodMetadata> methods = new ArrayList<>();
    classDecl
        .getMethods()
        .forEach(
            methodDecl -> {
              MethodMetadata methodMetadata = new MethodMetadata();
              methodMetadata.setMethodName(methodDecl.getNameAsString());
              methodMetadata.setSignature(methodDecl.getDeclarationAsString());
              methodMetadata.setReturnType(methodDecl.getTypeAsString());
              methodMetadata.setStartLine(methodDecl.getBegin().map(pos -> pos.line).orElse(null));
              methodMetadata.setEndLine(methodDecl.getEnd().map(pos -> pos.line).orElse(null));

              // Capture parameter types: comma-separated list of type names
              String paramTypes =
                  methodDecl.getParameters().isEmpty()
                      ? ""
                      : methodDecl.getParameters().stream()
                          .map(p -> p.getTypeAsString())
                          .reduce((a, b) -> a + "," + b)
                          .orElse("");
              methodMetadata.setParameterTypes(paramTypes);

              // Capture method modifiers
              String methodModifiers =
                  methodDecl.getModifiers().isEmpty()
                      ? "package-private"
                      : methodDecl.getModifiers().stream()
                          .map(Object::toString)
                          .reduce((a, b) -> a + " " + b)
                          .orElse("package-private");
              methodMetadata.setModifiers(methodModifiers);

              methods.add(methodMetadata);
            });
    classMetadata.setMethods(methods);

    classes.add(classMetadata);

    // Continue visiting nested classes
    super.visit(classDecl, arg);
  }

  /** Data class for class metadata extracted from AST. */
  @Getter
  public static class ClassMetadata {
    private String className;
    private Integer startLine;
    private Integer endLine;
    private String modifiers;
    private List<MethodMetadata> methods;

    public void setClassName(String className) {
      this.className = className;
    }

    public void setStartLine(Integer startLine) {
      this.startLine = startLine;
    }

    public void setEndLine(Integer endLine) {
      this.endLine = endLine;
    }

    public void setModifiers(String modifiers) {
      this.modifiers = modifiers;
    }

    public void setMethods(List<MethodMetadata> methods) {
      this.methods = methods;
    }
  }

  /** Data class for method metadata extracted from AST. */
  @Getter
  public static class MethodMetadata {
    private String methodName;
    private String signature;
    private String returnType;
    private Integer startLine;
    private Integer endLine;
    private String modifiers;
    private String parameterTypes;

    public void setMethodName(String methodName) {
      this.methodName = methodName;
    }

    public void setSignature(String signature) {
      this.signature = signature;
    }

    public void setReturnType(String returnType) {
      this.returnType = returnType;
    }

    public void setStartLine(Integer startLine) {
      this.startLine = startLine;
    }

    public void setEndLine(Integer endLine) {
      this.endLine = endLine;
    }

    public void setModifiers(String modifiers) {
      this.modifiers = modifiers;
    }

    public void setParameterTypes(String parameterTypes) {
      this.parameterTypes = parameterTypes;
    }
  }
}

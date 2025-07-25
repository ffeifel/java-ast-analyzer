package com.example.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import lombok.extern.java.Log;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * TreeWalker analyzes Java source files to extract structural information.
 * <p>
 * This class uses JavaParser to parse Java source files and extract key elements such as:
 * <ul>
 *   <li>Package declaration</li>
 *   <li>Class name</li>
 *   <li>Method signatures</li>
 *   <li>Constructor signatures</li>
 *   <li>Import statements</li>
 * </ul>
 * <p>
 * The extracted information is returned as a structured map for further processing.
 */

@Log
public class TreeWalker {

    public static final String PACKAGE = "package";
    public static final String CLASS_NAME = "className";
    public static final String METHODS = "methods";
    public static final String CONSTRUCTORS = "constructors";
    public static final String IMPORTS = "imports";
    public static final String EXTENDS = "extends";
    public static final String IMPLEMENTS = "implements";
    public static final String METHOD_SIGNATURES = "methodSignatures";

    private final Path path;
    private final JavaParser parser;

    /**
     * Constructs a TreeWalker for analyzing a Java source file.
     *
     * @param path The file path to the Java source file to be analyzed
     */
    public TreeWalker(String path) {
        parser = new JavaParser();
        this.path = Path.of(path).toAbsolutePath().normalize();
    }

    /**
     * Analyzes the Java source file and extracts structural information.
     * <p>
     * This method parses the Java file specified during construction and extracts
     * information about its structure, including package name, class name, methods,
     * constructors, and imports.
     *
     * @return A map containing the extracted information with the following possible keys:
     * <ul>
     *   <li>{@link #PACKAGE} - List containing the package name</li>
     *   <li>{@link #CLASS_NAME} - List containing the class name</li>
     *   <li>{@link #METHODS} - List of method signatures</li>
     *   <li>{@link #CONSTRUCTORS} - List of constructor signatures</li>
     *   <li>{@link #IMPORTS} - List of import statements</li>
     *   <li>"error" - List containing error message if parsing fails</li>
     * </ul>
     */
    public Map<String, List<String>> analyzeJavaFile() {
        final Map<String, List<String>> result = new HashMap<>();
        final List<String> methods = new ArrayList<>();
        final List<String> methodSignatures = new ArrayList<>();
        final List<String> constructors = new ArrayList<>();
        final List<String> imports = new ArrayList<>();

        try {
            final CompilationUnit cu = parser.parse(path).getResult().orElseThrow();

            // Get package name
            cu.getPackageDeclaration().ifPresent(pkg ->
                    result.put(PACKAGE, List.of(pkg.getNameAsString())));

            // Get imports
            cu.getImports().forEach(importDecl -> {
                if (importDecl.isStatic()) {
                    imports.add("static " + importDecl.getNameAsString());
                } else {
                    imports.add(importDecl.getNameAsString());
                }
            });

            // Get class information including inheritance
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
            if (!classes.isEmpty()) {
                // Get the first class found
                ClassOrInterfaceDeclaration firstClass = classes.getFirst();
                result.put(CLASS_NAME, List.of(firstClass.getNameAsString()));

                // Get extends information
                firstClass.getExtendedTypes().forEach(extendedType ->
                        result.put(EXTENDS, List.of(extendedType.getNameAsString())));

                // Get implements information
                if (!firstClass.getImplementedTypes().isEmpty()) {
                    List<String> interfaces = firstClass.getImplementedTypes().stream()
                            .map(NodeWithSimpleName::getNameAsString)
                            .collect(Collectors.toList());
                    result.put(IMPLEMENTS, interfaces);
                }
            }

            // Get method signatures with return types
            cu.findAll(MethodDeclaration.class).forEach(method -> {
                // Create method signature with parameter names for METHODS
                String returnType = method.getTypeAsString();
                String methodName = method.getNameAsString();
                String params = method.getParameters().stream()
                        .map(param -> param.getTypeAsString() + " " + param.getNameAsString())
                        .collect(Collectors.joining(", "));

                String methodSignature = returnType + " " + methodName + "(" + params + ")";
                methods.add(methodSignature);

                // Create detailed method signature for METHOD_SIGNATURES (same as above for now)
                methodSignatures.add(methodSignature);
            });

            // Get constructor signatures
            cu.findAll(ConstructorDeclaration.class).forEach(constructor -> {
                String constructorName = constructor.getNameAsString();
                String params = constructor.getParameters().stream()
                        .map(param -> param.getTypeAsString() + " " + param.getNameAsString())
                        .collect(Collectors.joining(", "));

                String signature = constructorName + "(" + params + ")";
                constructors.add(signature);
            });

            if (!imports.isEmpty()) {
                result.put(IMPORTS, imports);
            }
            if (!methods.isEmpty()) {
                result.put(METHODS, methods);
            }
            if (!methodSignatures.isEmpty()) {
                result.put(METHOD_SIGNATURES, methodSignatures);
            }
            if (!constructors.isEmpty()) {
                result.put(CONSTRUCTORS, constructors);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Error parsing Java file: " + path, e);
            result.put("error", List.of("Failed to parse file: " + e.getMessage()));
        }

        return result;
    }

}

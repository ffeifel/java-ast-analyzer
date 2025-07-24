package com.example.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.extern.java.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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
        final List<String> constructors = new ArrayList<>();
        final List<String> imports = new ArrayList<>();

        try {
            final CompilationUnit cu = parser.parse(path).getResult().orElseThrow();

            // Get package name
            cu.getPackageDeclaration().ifPresent(pkg ->
                    result.put(PACKAGE, List.of(pkg.getNameAsString())));

            // Get imports
            cu.getImports().forEach(importDecl ->
                    imports.add(importDecl.getNameAsString()));

            // Get class name
            cu.getTypes().forEach(type ->
                    result.put(CLASS_NAME, List.of(type.getNameAsString())));

            // Get method signatures
            cu.findAll(MethodDeclaration.class).forEach(method ->
                    methods.add(method.getDeclarationAsString(false, false, false)));

            // Get constructor signatures
            cu.findAll(ConstructorDeclaration.class).forEach(constructor ->
                    constructors.add(constructor.getDeclarationAsString(false, false, false)));

            if (!imports.isEmpty()) {
                result.put(IMPORTS, imports);
            }
            if (!methods.isEmpty()) {
                result.put(METHODS, methods);
            }
            if (!constructors.isEmpty()) {
                result.put(CONSTRUCTORS, constructors);
            }

        } catch (IOException e) {
            log.log(Level.WARNING, "Error parsing Java file: " + path, e);
            result.put("error", List.of("Failed to parse file: " + e.getMessage()));
        }

        return result;
    }

}

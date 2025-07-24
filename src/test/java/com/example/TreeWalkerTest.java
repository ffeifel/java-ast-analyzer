package com.example;

import com.example.analyzer.TreeWalker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TreeWalker Tests")
class TreeWalkerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should extract package name from Java file")
    void shouldExtractPackageName() throws IOException {
        // Given
        String javaCode = "package com.example.test;\n\npublic class TestClass { }";
        Path javaFile = createTempJavaFile("TestClass.java", javaCode);
        TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.PACKAGE), "Result should contain package information");
        assertEquals("com.example.test", result.get(TreeWalker.PACKAGE).getFirst(),
                "Package name should be correctly extracted");
    }

    @Test
    @DisplayName("Should extract class name from Java file")
    void shouldExtractClassName() throws IOException {
        // Given
        String javaCode = "package com.example.test;\n\npublic class ExtractedClass { }";
        Path javaFile = createTempJavaFile("ExtractedClass.java", javaCode);
        TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.CLASS_NAME), "Result should contain class name information");
        assertEquals("ExtractedClass", result.get(TreeWalker.CLASS_NAME).getFirst(),
                "Class name should be correctly extracted");
    }

    @Test
    @DisplayName("Should extract method signatures from Java file")
    void shouldExtractMethodSignatures() throws IOException {
        // Given
        String javaCode =
                "package com.example.test;\n\n" +
                        "public class MethodClass {\n" +
                        "    public void testMethod() {}\n" +
                        "    private int calculateValue(String input, int factor) { return 0; }\n" +
                        "    protected static String formatData(Object data) { return \"\"; }\n" +
                        "}";
        Path javaFile = createTempJavaFile("MethodClass.java", javaCode);
        TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.METHODS), "Result should contain methods information");
        List<String> methods = result.get(TreeWalker.METHODS);
        assertEquals(3, methods.size(), "Should extract all three methods");
        assertTrue(methods.contains("void testMethod()"), "Should extract simple method signature");
        assertTrue(methods.contains("int calculateValue(String input, int factor)"),
                "Should extract method with parameters and return type");
        assertTrue(methods.contains("static String formatData(Object data)"),
                "Should extract static method with parameter and return type");
    }

    @Test
    @DisplayName("Should extract constructor signatures from Java file")
    void shouldExtractConstructorSignatures() throws IOException {
        // Given
        String javaCode =
                "package com.example.test;\n\n" +
                        "public class ConstructorClass {\n" +
                        "    public ConstructorClass() {}\n" +
                        "    private ConstructorClass(String name) {}\n" +
                        "    protected ConstructorClass(int id, String description) {}\n" +
                        "}";
        Path javaFile = createTempJavaFile("ConstructorClass.java", javaCode);
        TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.CONSTRUCTORS), "Result should contain constructors information");
        List<String> constructors = result.get(TreeWalker.CONSTRUCTORS);
        assertEquals(3, constructors.size(), "Should extract all three constructors");
        assertTrue(constructors.contains("ConstructorClass()"), "Should extract default constructor");
        assertTrue(constructors.contains("ConstructorClass(String name)"),
                "Should extract constructor with single parameter");
        assertTrue(constructors.contains("ConstructorClass(int id, String description)"),
                "Should extract constructor with multiple parameters");
    }

    @Test
    @DisplayName("Should extract import statements from Java file")
    void shouldExtractImportStatements() throws IOException {
        // Given
        String javaCode =
                "package com.example.test;\n\n" +
                        "import java.util.List;\n" +
                        "import java.util.Map;\n" +
                        "import com.example.SomeClass;\n" +
                        "import static java.util.Collections.emptyList;\n\n" +
                        "public class ImportClass { }";
        Path javaFile = createTempJavaFile("ImportClass.java", javaCode);
        TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.IMPORTS), "Result should contain imports information");
        List<String> imports = result.get(TreeWalker.IMPORTS);
        assertEquals(4, imports.size(), "Should extract all four imports");
        assertTrue(imports.contains("java.util.List"), "Should extract standard import");
        assertTrue(imports.contains("java.util.Map"), "Should extract standard import");
        assertTrue(imports.contains("com.example.SomeClass"), "Should extract custom class import");
        assertTrue(imports.contains("static java.util.Collections.emptyList"), "Should extract static import");
    }

    @Test
    @DisplayName("Should handle Java file without package declaration")
    void shouldHandleFileWithoutPackage() throws IOException {
        // Given
        String javaCode = "public class NoPackageClass { }";
        Path javaFile = createTempJavaFile("NoPackageClass.java", javaCode);
        TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertFalse(result.containsKey(TreeWalker.PACKAGE), "Result should not contain package information");
        assertTrue(result.containsKey(TreeWalker.CLASS_NAME), "Result should contain class name information");
        assertEquals("NoPackageClass", result.get(TreeWalker.CLASS_NAME).getFirst(),
                "Class name should be correctly extracted");
    }

    @Test
    @DisplayName("Should handle Java file with multiple classes")
    void shouldHandleFileWithMultipleClasses() throws IOException {
        // Given
        String javaCode =
                "package com.example.test;\n\n" +
                        "public class MainClass {\n" +
                        "    private class InnerClass {}\n" +
                        "}\n\n" +
                        "class SecondaryClass {}";
        Path javaFile = createTempJavaFile("MainClass.java", javaCode);
        TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.CLASS_NAME), "Result should contain class name information");
        // Note: JavaParser typically returns the primary class name for getTypes()
        assertEquals("MainClass", result.get(TreeWalker.CLASS_NAME).getFirst(),
                "Primary class name should be correctly extracted");
    }

    @Test
    @DisplayName("Should handle invalid Java file and return error")
    void shouldHandleInvalidJavaFile() throws IOException {
        // Given
        String invalidCode = "This is not valid Java code";
        Path invalidFile = createTempJavaFile("Invalid.java", invalidCode);
        TreeWalker treeWalker = new TreeWalker(invalidFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey("error"), "Result should contain error information");
        assertFalse(result.isEmpty(), "Result should not be empty even with parsing error");
    }

    @Test
    @DisplayName("Should handle non-existent file and return error")
    void shouldHandleNonExistentFile() {
        // Given
        String nonExistentPath = tempDir.resolve("NonExistent.java").toString();
        TreeWalker treeWalker = new TreeWalker(nonExistentPath);

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey("error"), "Result should contain error information");
        assertTrue(result.get("error").getFirst().contains("Failed to parse file"),
                "Error message should indicate parsing failure");
    }

    @Test
    @DisplayName("Should handle empty Java file")
    void shouldHandleEmptyJavaFile() throws IOException {
        // Given
        String emptyCode = "";
        Path emptyFile = createTempJavaFile("Empty.java", emptyCode);
        TreeWalker treeWalker = new TreeWalker(emptyFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey("error") || result.isEmpty(),
                "Result should either contain error or be empty for empty file");
    }

    @Test
    @DisplayName("Should handle complex Java file with all features")
    void shouldHandleComplexJavaFile() throws IOException {
        // Given
        String complexCode =
                "package com.example.complex;\n\n" +
                        "import java.util.*;\n" +
                        "import java.io.IOException;\n" +
                        "import static java.lang.Math.*;\n\n" +
                        "public class ComplexClass {\n" +
                        "    private final String name;\n" +
                        "    private int count;\n\n" +
                        "    public ComplexClass() {\n" +
                        "        this(\"default\");\n" +
                        "    }\n\n" +
                        "    public ComplexClass(String name) {\n" +
                        "        this.name = name;\n" +
                        "        this.count = 0;\n" +
                        "    }\n\n" +
                        "    public void incrementCount() {\n" +
                        "        count++;\n" +
                        "    }\n\n" +
                        "    public int getCount() {\n" +
                        "        return count;\n" +
                        "    }\n\n" +
                        "    public String getName() {\n" +
                        "        return name;\n" +
                        "    }\n\n" +
                        "    public double calculateValue(double input) {\n" +
                        "        return sqrt(input) * PI;\n" +
                        "    }\n" +
                        "}";
        Path complexFile = createTempJavaFile("ComplexClass.java", complexCode);
        TreeWalker treeWalker = new TreeWalker(complexFile.toString());

        // When
        Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.PACKAGE), "Result should contain package information");
        assertEquals("com.example.complex", result.get(TreeWalker.PACKAGE).getFirst(),
                "Package name should be correctly extracted");

        assertTrue(result.containsKey(TreeWalker.CLASS_NAME), "Result should contain class name information");
        assertEquals("ComplexClass", result.get(TreeWalker.CLASS_NAME).getFirst(),
                "Class name should be correctly extracted");

        assertTrue(result.containsKey(TreeWalker.IMPORTS), "Result should contain imports information");
        assertEquals(3, result.get(TreeWalker.IMPORTS).size(), "Should extract all three imports");

        assertTrue(result.containsKey(TreeWalker.METHODS), "Result should contain methods information");
        assertEquals(4, result.get(TreeWalker.METHODS).size(), "Should extract all four methods");

        assertTrue(result.containsKey(TreeWalker.CONSTRUCTORS), "Result should contain constructors information");
        assertEquals(2, result.get(TreeWalker.CONSTRUCTORS).size(), "Should extract both constructors");
    }

    /**
     * Helper method to create a temporary Java file with the given content
     */
    private Path createTempJavaFile(String fileName, String content) throws IOException {
        Path filePath = tempDir.resolve(fileName);
        Files.writeString(filePath, content);
        return filePath;
    }
}

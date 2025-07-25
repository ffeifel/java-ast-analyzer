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
        final String javaCode = "package com.example.test;\n\npublic class TestClass { }";
        final Path javaFile = createTempJavaFile("TestClass.java", javaCode);
        final TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.PACKAGE), "Result should contain package information");
        assertEquals("com.example.test", result.get(TreeWalker.PACKAGE).getFirst(),
                "Package name should be correctly extracted");
    }

    @Test
    @DisplayName("Should extract class name from Java file")
    void shouldExtractClassName() throws IOException {
        // Given
        final String javaCode = "package com.example.test;\n\npublic class ExtractedClass { }";
        final Path javaFile = createTempJavaFile("ExtractedClass.java", javaCode);
        final TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.CLASS_NAME), "Result should contain class name information");
        assertEquals("ExtractedClass", result.get(TreeWalker.CLASS_NAME).getFirst(),
                "Class name should be correctly extracted");
    }

    @Test
    @DisplayName("Should extract method signatures from Java file")
    void shouldExtractMethodSignatures() throws IOException {
        // Given
        final String javaCode =
                """
                        package com.example.test;
                        
                        public class MethodClass {
                            public void testMethod() {}
                            private int calculateValue(String input, int factor) { return 0; }
                            protected static String formatData(Object data) { return ""; }
                        }""";
        final Path javaFile = createTempJavaFile("MethodClass.java", javaCode);
        final TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.METHODS), "Result should contain methods information");
        final List<String> methods = result.get(TreeWalker.METHODS);
        assertEquals(3, methods.size(), "Should extract all three methods");
        assertTrue(methods.contains("void testMethod()"), "Should extract simple method signature");
        assertTrue(methods.contains("int calculateValue(String input, int factor)"),
                "Should extract method with parameters and return type");
        assertTrue(methods.contains("String formatData(Object data)"),
                "Should extract static method with parameter and return type");
    }

    @Test
    @DisplayName("Should extract constructor signatures from Java file")
    void shouldExtractConstructorSignatures() throws IOException {
        // Given
        final String javaCode =
                """
                        package com.example.test;
                        
                        public class ConstructorClass {
                            public ConstructorClass() {}
                            private ConstructorClass(String name) {}
                            protected ConstructorClass(int id, String description) {}
                        }""";
        final Path javaFile = createTempJavaFile("ConstructorClass.java", javaCode);
        final TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.CONSTRUCTORS), "Result should contain constructors information");
        final List<String> constructors = result.get(TreeWalker.CONSTRUCTORS);
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
        final String javaCode =
                """
                        package com.example.test;
                        
                        import java.util.List;
                        import java.util.Map;
                        import com.example.SomeClass;
                        import static java.util.Collections.emptyList;
                        
                        public class ImportClass { }""";
        final Path javaFile = createTempJavaFile("ImportClass.java", javaCode);
        final TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey(TreeWalker.IMPORTS), "Result should contain imports information");
        final List<String> imports = result.get(TreeWalker.IMPORTS);
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
        final String javaCode = "public class NoPackageClass { }";
        final Path javaFile = createTempJavaFile("NoPackageClass.java", javaCode);
        final TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

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
        final String javaCode =
                """
                        package com.example.test;
                        
                        public class MainClass {
                            private class InnerClass {}
                        }
                        
                        class SecondaryClass {}""";
        final Path javaFile = createTempJavaFile("MainClass.java", javaCode);
        final TreeWalker treeWalker = new TreeWalker(javaFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

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
        final String invalidCode = "This is not valid Java code { invalid syntax !!!";
        final Path invalidFile = createTempJavaFile("Invalid.java", invalidCode);
        final TreeWalker treeWalker = new TreeWalker(invalidFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        // JavaParser might return an empty result or throw an exception
        // Either way, we should get an error or an empty result
        if (result.containsKey("error")) {
            assertFalse(result.get("error").isEmpty(), "Error list should not be empty");
            assertTrue(result.get("error").getFirst().contains("Failed to parse file"),
                    "Error message should indicate parsing failure");
        } else {
            // If no error key, the result should be mostly empty (no meaningful content extracted)
            assertFalse(result.containsKey(TreeWalker.CLASS_NAME),
                    "Should not extract class name from invalid Java");
            assertFalse(result.containsKey(TreeWalker.METHODS),
                    "Should not extract methods from invalid Java");
        }
    }

    @Test
    @DisplayName("Should handle non-existent file and return error")
    void shouldHandleNonExistentFile() {
        // Given
        final String nonExistentPath = tempDir.resolve("NonExistent.java").toString();
        final TreeWalker treeWalker = new TreeWalker(nonExistentPath);

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey("error"), "Result should contain error information");
        assertTrue(result.get("error").getFirst().contains("Failed to parse file"),
                "Error message should indicate parsing failure");
    }

    @Test
    @DisplayName("Should handle empty Java file")
    void shouldHandleEmptyJavaFile() throws IOException {
        // Given
        final String emptyCode = "";
        final Path emptyFile = createTempJavaFile("Empty.java", emptyCode);
        final TreeWalker treeWalker = new TreeWalker(emptyFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

        // Then
        assertTrue(result.containsKey("error") || result.isEmpty(),
                "Result should either contain error or be empty for empty file");
    }

    @Test
    @DisplayName("Should handle complex Java file with all features")
    void shouldHandleComplexJavaFile() throws IOException {
        // Given
        final String complexCode =
                """
                        package com.example.complex;
                        
                        import java.util.*;
                        import java.io.IOException;
                        import static java.lang.Math.*;
                        
                        public class ComplexClass {
                            private final String name;
                            private int count;
                        
                            public ComplexClass() {
                                this("default");
                            }
                        
                            public ComplexClass(String name) {
                                this.name = name;
                                this.count = 0;
                            }
                        
                            public void incrementCount() {
                                count++;
                            }
                        
                            public int getCount() {
                                return count;
                            }
                        
                            public String getName() {
                                return name;
                            }
                        
                            public double calculateValue(double input) {
                                return sqrt(input) * PI;
                            }
                        }""";
        final Path complexFile = createTempJavaFile("ComplexClass.java", complexCode);
        final TreeWalker treeWalker = new TreeWalker(complexFile.toString());

        // When
        final Map<String, List<String>> result = treeWalker.analyzeJavaFile();

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
    private Path createTempJavaFile(final String fileName, final String content) throws IOException {
        final Path filePath = tempDir.resolve(fileName);
        Files.writeString(filePath, content);
        return filePath;
    }
}

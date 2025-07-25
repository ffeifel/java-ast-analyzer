package com.example.tokenizer;

import com.example.tokenizer.entity.CodeElement;
import com.example.tokenizer.entity.JavaFileAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CodeElementParser Tests")
class CodeElementParserTest {

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("parseFromJson Tests")
    class ParseFromJsonTests {

        @Test
        @DisplayName("Should parse valid JSON file with complete JavaFileAnalysis data")
        void shouldParseValidJsonFileWithCompleteData() throws IOException {
            // Given
            final JavaFileAnalysis analysis = createCompleteJavaFileAnalysis();
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());

            final CodeElement element = result.getFirst();
            assertEquals("TestClass", element.getClassName());
            assertEquals("com.example.test", element.getPackageName());
            assertEquals(Arrays.asList("java.util.List", "java.io.IOException"), element.getImports());
            assertEquals("BaseClass", element.getExtendsClass());
            assertEquals(Arrays.asList("Interface1", "Interface2"), element.getImplementsInterfaces());
            assertEquals(4, element.getMethods().size());
        }

        @Test
        @DisplayName("Should parse JSON file with minimal data")
        void shouldParseJsonFileWithMinimalData() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("MinimalClass"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("minimal.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());

            final CodeElement element = result.getFirst();
            assertEquals("MinimalClass", element.getClassName());
            assertNull(element.getPackageName());
            assertTrue(element.getImports() == null || element.getImports().isEmpty());
            assertNull(element.getExtendsClass());
            assertTrue(element.getImplementsInterfaces() == null || element.getImplementsInterfaces().isEmpty());
            assertTrue(element.getMethods() == null || element.getMethods().isEmpty());
        }

        @Test
        @DisplayName("Should parse empty JSON array")
        void shouldParseEmptyJsonArray() throws IOException {
            // Given
            final String jsonContent = "[]";
            final Path jsonFile = tempDir.resolve("empty.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should throw IOException for non-existent file")
        void shouldThrowIOExceptionForNonExistentFile() {
            // Given
            final String nonExistentFile = tempDir.resolve("non-existent.json").toString();

            // When & Then
            assertThrows(IOException.class, () -> CodeElementParser.parseFromJson(nonExistentFile));
        }

        @Test
        @DisplayName("Should throw IOException for invalid JSON")
        void shouldThrowIOExceptionForInvalidJson() throws IOException {
            // Given
            final String invalidJson = "{ invalid json content }";
            final Path jsonFile = tempDir.resolve("invalid.json");
            Files.write(jsonFile, invalidJson.getBytes());

            // When & Then
            assertThrows(IOException.class, () -> CodeElementParser.parseFromJson(jsonFile.toString()));
        }
    }

    @Nested
    @DisplayName("Method Signature Parsing Tests")
    class MethodSignatureParsingTests {

        @Test
        @DisplayName("Should parse method signature with return type and parameters")
        void shouldParseMethodSignatureWithReturnTypeAndParameters() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("TestClass"));
            analysis.setMethodSignatures(Collections.singletonList("String getName(int id, boolean active)"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("method-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals(1, element.getMethods().size());

            final CodeElement.Method method = element.getMethods().getFirst();
            assertEquals("getName", method.getName());
            assertEquals("String", method.getReturnType());
            assertEquals(Arrays.asList("int id", "boolean active"), method.getParameters());
        }

        @Test
        @DisplayName("Should parse method signature with void return type")
        void shouldParseMethodSignatureWithVoidReturnType() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("TestClass"));
            analysis.setMethodSignatures(Collections.singletonList("void setName(String name)"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("void-method-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            final CodeElement.Method method = element.getMethods().getFirst();
            assertEquals("setName", method.getName());
            assertEquals("void", method.getReturnType());
            assertEquals(Collections.singletonList("String name"), method.getParameters());
        }

        @Test
        @DisplayName("Should parse method signature with no parameters")
        void shouldParseMethodSignatureWithNoParameters() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("TestClass"));
            analysis.setMethodSignatures(Collections.singletonList("int getCount()"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("no-params-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            final CodeElement.Method method = element.getMethods().getFirst();
            assertEquals("getCount", method.getName());
            assertEquals("int", method.getReturnType());
            assertTrue(method.getParameters().isEmpty());
        }

        @Test
        @DisplayName("Should parse constructor signature")
        void shouldParseConstructorSignature() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("TestClass"));
            analysis.setConstructors(Collections.singletonList("TestClass(String name, int value)"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("constructor-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals(1, element.getMethods().size());

            final CodeElement.Method constructor = element.getMethods().getFirst();
            assertEquals("TestClass", constructor.getName());
        }

        @Test
        @DisplayName("Should fallback to simple method names when methodSignatures is null")
        void shouldFallbackToSimpleMethodNamesWhenMethodSignaturesIsNull() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("TestClass"));
            analysis.setMethods(Arrays.asList("void doSomething(String param)", "int calculate()"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("fallback-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals(2, element.getMethods().size());
            assertEquals("doSomething", element.getMethods().get(0).getName());
            assertEquals("calculate", element.getMethods().get(1).getName());
        }

        @Test
        @DisplayName("Should handle method signature without parentheses")
        void shouldHandleMethodSignatureWithoutParentheses() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("TestClass"));
            analysis.setMethodSignatures(Collections.singletonList("simpleMethod"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("no-parens-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            final CodeElement.Method method = element.getMethods().getFirst();
            assertEquals("simpleMethod", method.getName());
        }
    }

    @Nested
    @DisplayName("Data Conversion Tests")
    class DataConversionTests {

        @Test
        @DisplayName("Should handle multiple class names and take first one")
        void shouldHandleMultipleClassNamesAndTakeFirstOne() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Arrays.asList("FirstClass", "SecondClass", "ThirdClass"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("multiple-classes-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals("FirstClass", element.getClassName());
        }

        @Test
        @DisplayName("Should handle multiple package names and take first one")
        void shouldHandleMultiplePackageNamesAndTakeFirstOne() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("TestClass"));
            analysis.setPackageName(Arrays.asList("com.example.first", "com.example.second"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("multiple-packages-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals("com.example.first", element.getPackageName());
        }

        @Test
        @DisplayName("Should handle multiple extends classes and take first one")
        void shouldHandleMultipleExtendsClassesAndTakeFirstOne() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("TestClass"));
            analysis.setExtendsClasses(Arrays.asList("FirstBase", "SecondBase"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("multiple-extends-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals("FirstBase", element.getExtendsClass());
        }

        @Test
        @DisplayName("Should handle null and empty collections gracefully")
        void shouldHandleNullAndEmptyCollectionsGracefully() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.emptyList());
            analysis.setPackageName(null);
            analysis.setImports(Collections.emptyList());
            analysis.setExtendsClasses(Collections.emptyList());
            analysis.setImplementsInterfaces(null);
            analysis.setMethodSignatures(null);
            analysis.setMethods(null);
            analysis.setConstructors(null);
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("null-empty-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());

            final CodeElement element = result.getFirst();
            assertNull(element.getClassName());
            assertNull(element.getPackageName());
            assertNull(element.getExtendsClass());
            assertTrue(element.getMethods() == null || element.getMethods().isEmpty());
        }
    }

    private JavaFileAnalysis createCompleteJavaFileAnalysis() {
        final JavaFileAnalysis analysis = new JavaFileAnalysis();
        analysis.setClassName(Arrays.asList("TestClass", "AnotherClass"));
        analysis.setPackageName(Arrays.asList("com.example.test", "com.example.other"));
        analysis.setImports(Arrays.asList("java.util.List", "java.io.IOException"));
        analysis.setExtendsClasses(Arrays.asList("BaseClass", "OtherBase"));
        analysis.setImplementsInterfaces(Arrays.asList("Interface1", "Interface2"));
        analysis.setMethodSignatures(Arrays.asList(
                "String getName(int id, boolean active)",
                "void setName(String name)",
                "List<String> getItems()"
        ));
        analysis.setConstructors(List.of("TestClass(String name)"));
        return analysis;
    }

    @Nested
    @DisplayName("Edge Cases and Complex Scenarios")
    class EdgeCasesAndComplexScenariosTests {

        @Test
        @DisplayName("Should handle complex method signatures with generics")
        void shouldHandleComplexMethodSignaturesWithGenerics() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("GenericClass"));
            analysis.setMethodSignatures(Arrays.asList(
                    "List<String> getStringList(Map<String, Integer> map)",
                    "Optional<User> findUser(String id)",
                    "void processData(List<Map<String, Object>> data)"
            ));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("generics-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals(3, element.getMethods().size());

            final CodeElement.Method method1 = element.getMethods().getFirst();
            assertEquals("getStringList", method1.getName());
            assertEquals("List<String>", method1.getReturnType());
            final var expectedParams = new ArrayList<>();
            expectedParams.add("Map<String");
            expectedParams.add("Integer> map");
            assertEquals(expectedParams, method1.getParameters());

            final CodeElement.Method method2 = element.getMethods().get(1);
            assertEquals("findUser", method2.getName());
            assertEquals("Optional<User>", method2.getReturnType());

            final CodeElement.Method method3 = element.getMethods().get(2);
            assertEquals("processData", method3.getName());
            assertEquals("void", method3.getReturnType());
        }

        @Test
        @DisplayName("Should handle method signatures with access modifiers")
        void shouldHandleMethodSignaturesWithAccessModifiers() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("ModifierClass"));
            analysis.setMethodSignatures(Arrays.asList(
                    "public String getPublicMethod()",
                    "private void setPrivateMethod(String value)",
                    "protected static int getProtectedStaticMethod()",
                    "final synchronized boolean getFinalSyncMethod()"
            ));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("modifiers-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals(4, element.getMethods().size());

            // The parser should extract method names correctly despite modifiers
            assertEquals("getPublicMethod", element.getMethods().get(0).getName());
            assertEquals("setPrivateMethod", element.getMethods().get(1).getName());
            assertEquals("getProtectedStaticMethod", element.getMethods().get(2).getName());
            assertEquals("getFinalSyncMethod", element.getMethods().get(3).getName());
        }

        @Test
        @DisplayName("Should handle method signatures with array parameters")
        void shouldHandleMethodSignaturesWithArrayParameters() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("ArrayClass"));
            analysis.setMethodSignatures(Arrays.asList(
                    "String[] getStringArray(int[] indices)",
                    "void processMatrix(int[][] matrix)",
                    "Object[] convertArray(String... varargs)"
            ));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("arrays-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals(3, element.getMethods().size());

            final CodeElement.Method method1 = element.getMethods().getFirst();
            assertEquals("getStringArray", method1.getName());
            assertEquals("String[]", method1.getReturnType());
            assertEquals(Collections.singletonList("int[] indices"), method1.getParameters());

            final CodeElement.Method method2 = element.getMethods().get(1);
            assertEquals("processMatrix", method2.getName());
            assertEquals("void", method2.getReturnType());
            assertEquals(Collections.singletonList("int[][] matrix"), method2.getParameters());
        }

        @Test
        @DisplayName("Should handle malformed method signatures gracefully")
        void shouldHandleMalformedMethodSignaturesGracefully() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("MalformedClass"));
            analysis.setMethodSignatures(Arrays.asList(
                    "malformedMethod(",
                    "anotherMalformed)",
                    "",
                    "   ",
                    "normalMethod(String param)"
            ));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("malformed-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals(5, element.getMethods().size());

            // Should handle malformed signatures without throwing exceptions
            assertNotNull(element.getMethods().get(0).getName());
            assertNotNull(element.getMethods().get(1).getName());
            assertNotNull(element.getMethods().get(2).getName());
            assertNotNull(element.getMethods().get(3).getName());
            assertEquals("normalMethod", element.getMethods().get(4).getName());
        }

        @Test
        @DisplayName("Should combine methods, methodSignatures, and constructors correctly")
        void shouldCombineMethodsMethodSignaturesAndConstructorsCorrectly() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("CombinedClass"));
            analysis.setMethodSignatures(List.of("String getSignatureMethod()"));
            analysis.setMethods(List.of("void getSimpleMethod()"));
            analysis.setConstructors(List.of("CombinedClass(String param)"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("combined-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals(2, element.getMethods().size()); // methodSignatures takes precedence over methods, plus
            // constructor

            // Should have the method from methodSignatures (not from methods since methodSignatures is not null)
            assertEquals("getSignatureMethod", element.getMethods().get(0).getName());
            assertEquals("CombinedClass", element.getMethods().get(1).getName()); // Constructor
        }

        @Test
        @DisplayName("Should handle multiple JavaFileAnalysis objects in single JSON")
        void shouldHandleMultipleJavaFileAnalysisObjectsInSingleJson() throws IOException {
            // Given
            final JavaFileAnalysis analysis1 = new JavaFileAnalysis();
            analysis1.setClassName(Collections.singletonList("FirstClass"));
            analysis1.setPackageName(Collections.singletonList("com.example.first"));

            final JavaFileAnalysis analysis2 = new JavaFileAnalysis();
            analysis2.setClassName(Collections.singletonList("SecondClass"));
            analysis2.setPackageName(Collections.singletonList("com.example.second"));

            final List<JavaFileAnalysis> analysisList = Arrays.asList(analysis1, analysis2);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("multiple-analysis-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            assertEquals(2, result.size());

            final CodeElement element1 = result.getFirst();
            assertEquals("FirstClass", element1.getClassName());
            assertEquals("com.example.first", element1.getPackageName());

            final CodeElement element2 = result.get(1);
            assertEquals("SecondClass", element2.getClassName());
            assertEquals("com.example.second", element2.getPackageName());
        }

        @Test
        @DisplayName("Should handle method signature with only method name")
        void shouldHandleMethodSignatureWithOnlyMethodName() throws IOException {
            // Given
            final JavaFileAnalysis analysis = new JavaFileAnalysis();
            analysis.setClassName(Collections.singletonList("SimpleClass"));
            analysis.setMethodSignatures(Collections.singletonList("simpleMethod()"));
            final List<JavaFileAnalysis> analysisList = Collections.singletonList(analysis);
            final String jsonContent = objectMapper.writeValueAsString(analysisList);
            final Path jsonFile = tempDir.resolve("simple-method-test.json");
            Files.write(jsonFile, jsonContent.getBytes());

            // When
            final List<CodeElement> result = CodeElementParser.parseFromJson(jsonFile.toString());

            // Then
            final CodeElement element = result.getFirst();
            assertEquals(1, element.getMethods().size());

            final CodeElement.Method method = element.getMethods().getFirst();
            assertEquals("simpleMethod", method.getName());
            assertEquals("void", method.getReturnType()); // Default return type
            assertTrue(method.getParameters().isEmpty());
        }
    }
}

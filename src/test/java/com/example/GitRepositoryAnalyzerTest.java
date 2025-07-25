package com.example;

import com.example.analyzer.GitRepositoryAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("GitRepositoryAnalyzer Tests")
@ExtendWith(MockitoExtension.class)
class GitRepositoryAnalyzerTest {

    private GitRepositoryAnalyzer analyzer;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        analyzer = new GitRepositoryAnalyzer();
    }

    @Test
    @DisplayName("Should skip processing when directory is not a Git repository")
    void shouldSkipProcessingWhenNotGitRepo() throws IOException {
        // Given
        final Path nonGitDir = tempDir.resolve("non-git-project");
        Files.createDirectories(nonGitDir);

        // Create a test file
        final Path testFile = nonGitDir.resolve("Test.java");
        Files.writeString(testFile, "public class Test {}");

        // When
        analyzer.parseGitProject(nonGitDir.toString());

        // Then
        // We can't directly assert the internal state, but we can verify no files were created
        final File structureFile = new File(System.getProperty("user.dir"),
                nonGitDir.getFileName().toString() + "_structure.json");
        final File analysisFile = new File(System.getProperty("user.dir"),
                nonGitDir.getFileName().toString() + "_java_analysis.json");

        assertFalse(structureFile.exists(), "Structure file should not be created for non-git repo");
        assertFalse(analysisFile.exists(), "Analysis file should not be created for non-git repo");
    }

    @Test
    @DisplayName("Should process Git repository and generate JSON files")
    void shouldProcessGitRepositoryAndGenerateJsonFiles() throws IOException {
        // Given
        // Create a mock Git repository structure
        final Path gitRepoDir = tempDir.resolve("git-project");
        Files.createDirectories(gitRepoDir);

        // Create .git directory to make it a valid Git repo
        final Path gitDir = gitRepoDir.resolve(".git");
        Files.createDirectories(gitDir);

        // Create a source directory with Java files
        final Path srcDir = gitRepoDir.resolve("src");
        final Path mainDir = srcDir.resolve("main");
        final Path javaDir = mainDir.resolve("java");
        Files.createDirectories(javaDir);

        // Create a Java file
        final Path javaFile = javaDir.resolve("TestClass.java");
        final String javaContent = "package com.test;\n\npublic class TestClass {\n    public void testMethod() {}\n}";
        Files.writeString(javaFile, javaContent);


        // When
        analyzer.parseGitProject(gitRepoDir.toString());

        // Then
        final File structureFile = new File(System.getProperty("user.dir"),
                gitRepoDir.getFileName().toString() + "_structure.json");
        final File analysisFile = new File(System.getProperty("user.dir"),
                gitRepoDir.getFileName().toString() + "_java_analysis.json");

        assertTrue(structureFile.exists(), "Structure file should be created");
        assertTrue(analysisFile.exists(), "Analysis file should be created");

        // Clean up
        structureFile.delete();
        analysisFile.delete();
    }

    @Test
    @DisplayName("Should skip .git and target directories during analysis")
    void shouldSkipGitAndTargetDirectories() throws IOException {
        // Given
        // Create a mock Git repository with .git and target directories
        final Path gitRepoDir = tempDir.resolve("skip-test-repo");
        Files.createDirectories(gitRepoDir);

        // Create .git directory
        final Path gitDir = gitRepoDir.resolve(".git");
        Files.createDirectories(gitDir);
        final Path gitFile = gitDir.resolve("config");
        Files.writeString(gitFile, "# Git config");

        // Create target directory
        final Path targetDir = gitRepoDir.resolve("target");
        Files.createDirectories(targetDir);
        final Path targetFile = targetDir.resolve("TestClass.class");
        Files.writeString(targetFile, "compiled bytecode");

        // Create a source file
        final Path srcFile = gitRepoDir.resolve("TestClass.java");
        final String javaContent = "public class TestClass {}";
        Files.writeString(srcFile, javaContent);

        // Use a spy to verify internal method calls
        final GitRepositoryAnalyzer spyAnalyzer = spy(analyzer);

        // When

        spyAnalyzer.parseGitProject(gitRepoDir.toString());

        // Then
        // Verify that buildSimpleTree returns null for .git and target directories
        verify(spyAnalyzer, never()).buildSimpleTree(eq(gitDir));
        verify(spyAnalyzer, never()).buildSimpleTree(eq(targetDir));

        // Clean up
        final File structureFile = new File(System.getProperty("user.dir"),
                gitRepoDir.getFileName().toString() + "_structure.json");
        final File analysisFile = new File(System.getProperty("user.dir"),
                gitRepoDir.getFileName().toString() + "_java_analysis.json");

        if (structureFile.exists()) structureFile.delete();
        if (analysisFile.exists()) analysisFile.delete();

    }

    @Test
    @DisplayName("Should handle errors during Java file analysis")
    void shouldHandleErrorsDuringJavaFileAnalysis() throws IOException {
        // Given
        // Create a mock Git repository with a problematic Java file
        final Path gitRepoDir = tempDir.resolve("error-test-repo");
        Files.createDirectories(gitRepoDir);

        // Create .git directory
        final Path gitDir = gitRepoDir.resolve(".git");
        Files.createDirectories(gitDir);

        // Create a Java file that will cause analysis error
        final Path javaFile = gitRepoDir.resolve("ErrorClass.java");
        final String invalidJavaContent = "This is not valid Java code";
        Files.writeString(javaFile, invalidJavaContent);

        // When
        analyzer.parseGitProject(gitRepoDir.toString());

        // Then
        final File analysisFile = new File(System.getProperty("user.dir"),
                gitRepoDir.getFileName().toString() + "_java_analysis.json");

        assertTrue(analysisFile.exists(), "Analysis file should be created even with errors");

        // Read the file to verify it contains error information
        final String content = Files.readString(analysisFile.toPath());
        assertTrue(content.contains("error"), "Analysis file should contain error information");

        // Clean up
        final boolean ignored = analysisFile.delete();
    }

    @Test
    @DisplayName("Should correctly relativize file paths")
    void shouldCorrectlyRelativizeFilePaths() throws IOException {
        // Given
        // Create a mock Git repository with nested structure
        final Path gitRepoDir = tempDir.resolve("path-test-repo");
        Files.createDirectories(gitRepoDir);

        // Create .git directory
        final Path gitDir = gitRepoDir.resolve(".git");
        Files.createDirectories(gitDir);

        // Create nested directories with a Java file
        final Path nestedDir = gitRepoDir.resolve("src/main/java/com/example");
        Files.createDirectories(nestedDir);
        final Path javaFile = nestedDir.resolve("TestClass.java");
        Files.writeString(javaFile, "package com.example; public class TestClass {}");

        // When
        analyzer.parseGitProject(gitRepoDir.toString());

        // Then
        final File analysisFile = new File(System.getProperty("user.dir"),
                gitRepoDir.getFileName().toString() + "_java_analysis.json");

        assertTrue(analysisFile.exists(), "Analysis file should be created");

        // Read the file to verify it contains the correct relative path
        final String content = Files.readString(analysisFile.toPath());
        assertTrue(content.contains("relativePath"), "Analysis file should contain relativePath");
        assertTrue(content.contains("src/main/java/com/example/TestClass.java"),
                "Analysis file should contain the correct relative path");

        // Clean up
        final boolean ignored = analysisFile.delete();
    }
}

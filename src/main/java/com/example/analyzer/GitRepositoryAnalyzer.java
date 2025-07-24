package com.example.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Analyzes Git repositories and exports their structure and Java source code analysis to JSON files.
 * <p>
 * This class performs two main functions:
 * <ol>
 *   <li>Creates a JSON representation of the Git repository's directory structure</li>
 *   <li>Analyzes all Java files in the repository and exports detailed information about each file</li>
 * </ol>
 * <p>
 * The analysis includes:
 * <ul>
 *   <li>Repository structure (directories and files)</li>
 *   <li>For Java files: package names, class names, methods, constructors, and imports</li>
 * </ul>
 * <p>
 * The output is written to two JSON files in the current working directory:
 * <ul>
 *   <li>{repoName}_structure.json - Contains the repository directory structure</li>
 *   <li>{repoName}_java_analysis.json - Contains detailed analysis of all Java files</li>
 * </ul>
 * <p>
 * Note: This class skips the .git and target directories during analysis.
 *
 * @see TreeWalker For the Java source code analysis implementation
 */

@Log
public class GitRepositoryAnalyzer {

    private static final String GIT_DIR = ".git";
    private static final String TARGET_DIR = "target";
    private static final String USER_DIR = "user.dir";
    private static final String JAVA_FILE_EXTENSION = ".java";
    private static final String FILES = "files";
    private static final String FILE_PATH = "filePath";
    private static final String FILE_NAME = "fileName";
    private static final String RELATIVE_PATH = "relativePath";

    private final List<Path> javaFilePaths = new ArrayList<>();
    
    // Cache for file analysis results
    private final Map<String, CachedAnalysis> analysisCache = new ConcurrentHashMap<>();
    
    private static class CachedAnalysis {
        final Map<String, List<String>> analysis;
        final FileTime lastModified;
        
        CachedAnalysis(Map<String, List<String>> analysis, FileTime lastModified) {
            this.analysis = analysis;
            this.lastModified = lastModified;
        }
    }

    /**
     * Parses a Git repository and generates JSON files containing its structure and Java file analysis.
     * <p>
     * This method:
     * <ol>
     *   <li>Verifies the provided path is a Git repository</li>
     *   <li>Builds a representation of the repository's directory structure</li>
     *   <li>Collects all Java files for detailed analysis</li>
     *   <li>Exports the repository structure to a JSON file</li>
     *   <li>Analyzes all Java files and exports the analysis to a separate JSON file</li>
     * </ol>
     *
     * @param gitProjectPath The path to the Git repository to analyze
     * @throws RuntimeException If there is an error writing the JSON output files
     */
    public String parseGitProject(final String gitProjectPath) {
        final Path root = Paths.get(gitProjectPath).toAbsolutePath().normalize();

        if (!isGitRepo(root)) {
            log.log(Level.INFO, "Not a valid Git repository: " + root);
            return null;
        }

        // Get repository name from the path
        final String repoName = root.getFileName().toString();

        // Determine output directory, use current directory by default
        final String outputDir = System.getProperty(USER_DIR);

        // Clear any previous java file paths
        javaFilePaths.clear();

        // Build the repository structure
        final Map<String, Object> structure = buildSimpleTree(root);

        // Generate the repository structure JSON
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            // Create output files with repo name in the filename
            final File structureFile = new File(outputDir, repoName + "_structure.json");

            mapper.writeValueAsString(structure);
            mapper.writeValue(structureFile, structure);
            log.log(Level.INFO, "Project structure written to " + structureFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Now process all Java files and create a separate json
        return generateJavaFilesAnalysis(root, repoName, outputDir);
    }

    /**
     * Recursively builds a map representing the directory structure of the repository.
     * <p>
     * This method traverses the directory tree starting from the given path and:
     * <ul>
     *   <li>Skips .git and target directories</li>
     *   <li>For directories, recursively processes their contents</li>
     *   <li>For files, adds them to a list under the "files" key</li>
     *   <li>Collects paths to all Java files for later analysis</li>
     * </ul>
     *
     * @param path The directory path to process
     * @return A map representing the directory structure, or null for files and skipped directories
     */
    public Map<String, Object> buildSimpleTree(final Path path) {
        final String fileName = path.getFileName().toString();

        // Skip .git directory
        if (fileName.equals(GIT_DIR) || fileName.equals(TARGET_DIR)) {
            return null;
        }

        if (!Files.isDirectory(path)) {
            // For files, just return the filename as a string
            return null;
        }

        // For directories, create a map of contents
        final Map<String, Object> result = new HashMap<>();
        final List<String> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                final String entryName = entry.getFileName().toString();

                // Skip .git and target directories
                if (entryName.equals(GIT_DIR) || entryName.equals(TARGET_DIR)) {
                    continue;
                }

                if (Files.isDirectory(entry)) {
                    final Map<String, Object> subDir = buildSimpleTree(entry);
                    if (subDir != null && !subDir.isEmpty()) {
                        result.put(entryName, subDir);
                    }
                } else {
                    // Add files to the files list
                    files.add(entryName);

                    // Collect Java files for later analysis
                    if (entryName.endsWith(JAVA_FILE_EXTENSION)) {
                        javaFilePaths.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Error reading directory: " + path, e);
        }

        // Only add the files list if it's not empty
        if (!files.isEmpty()) {
            result.put(FILES, files);
        }

        return result;
    }

    /**
     * Analyzes all Java files found in the repository and generates a JSON file with the analysis.
     * <p>
     * For each Java file, this method:
     * <ul>
     *   <li>Uses TreeWalker to analyze the Java source code</li>
     *   <li>Creates an entry with file path, name, and relative path from repository root</li>
     *   <li>Adds the analysis results (package, class name, methods, etc.)</li>
     *   <li>Handles errors by adding an error entry with details</li>
     * </ul>
     *
     * @param root      The root path of the Git repository
     * @param repoName  The name of the repository (used for the output filename)
     * @param outputDir The directory where the output JSON file will be written
     */
    private String generateJavaFilesAnalysis(final Path root, final String repoName, final String outputDir) {
        final List<Map<String, Object>> javaFilesAnalysis = new ArrayList<>();

        for (Path javaFilePath : javaFilePaths) {

            try {
                final Map<String, Object> fileEntry = getStringObjectMap(root, javaFilePath);
                javaFilesAnalysis.add(fileEntry);
            } catch (Exception e) {
                log.log(Level.WARNING, "Error analyzing Java file: " + javaFilePath, e);

                // Add error entry with normalized path
                final Path normalizedPath = javaFilePath.normalize();
                final Map<String, Object> errorEntry = new HashMap<>();
                errorEntry.put(FILE_PATH, normalizedPath.toString());
                errorEntry.put(FILE_NAME, normalizedPath.getFileName().toString());
                errorEntry.put("error", "Failed to analyze: " + e.getMessage());
                javaFilesAnalysis.add(errorEntry);
            }
        }

        // Write the Java files analysis to a separate json file
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            final File analysisFile = new File(outputDir, repoName + "_java_analysis.json");
            mapper.writeValue(analysisFile, javaFilesAnalysis);
            log.log(Level.INFO, "Java files analysis written to " + analysisFile.getAbsolutePath());
            return analysisFile.getAbsolutePath();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error writing Java files analysis", e);
            return null;
        }
    }

    /**
     * Creates a map containing analysis information for a single Java file.
     * Now includes caching to avoid re-analyzing unchanged files.
     * <p>
     * This method:
     * <ul>
     *   <li>Checks cache first based on file modification time</li>
     *   <li>Uses TreeWalker to analyze the Java source code if not cached</li>
     *   <li>Adds file path, name, and relative path from repository root</li>
     *   <li>Incorporates the analysis results from TreeWalker</li>
     * </ul>
     *
     * @param root         The root path of the Git repository
     * @param javaFilePath The path to the Java file to analyze
     * @return A map containing the file information and analysis results
     */
    private Map<String, Object> getStringObjectMap(final Path root, final Path javaFilePath) {
        final Path normalizedPath = javaFilePath.normalize();
        final String pathKey = normalizedPath.toString();

        Map<String, List<String>> analysis;
        
        try {
            FileTime currentModified = Files.getLastModifiedTime(normalizedPath);
            CachedAnalysis cached = analysisCache.get(pathKey);
            
            // Check if we have a valid cached result
            if (cached != null && cached.lastModified.equals(currentModified)) {
                log.log(Level.FINE, "Using cached analysis for: " + pathKey);
                analysis = cached.analysis;
            } else {
                // Analyze the file and cache the result
                log.log(Level.FINE, "Analyzing file: " + pathKey);
                final TreeWalker walker = new TreeWalker(pathKey);
                analysis = walker.analyzeJavaFile();
                
                // Cache the result
                analysisCache.put(pathKey, new CachedAnalysis(analysis, currentModified));
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Error getting file modification time for: " + pathKey, e);
            // Fallback to direct analysis without caching
            final TreeWalker walker = new TreeWalker(pathKey);
            analysis = walker.analyzeJavaFile();
        }

        final Map<String, Object> fileEntry = new HashMap<>();
        fileEntry.put(FILE_PATH, pathKey);
        fileEntry.put(FILE_NAME, normalizedPath.getFileName().toString());

        // Add relative path from the git repo root
        try {
            final Path relativePath = root.relativize(normalizedPath);
            fileEntry.put(RELATIVE_PATH, relativePath.toString());
        } catch (IllegalArgumentException e) {
            // If paths can't be relativized
            fileEntry.put(RELATIVE_PATH, pathKey);
        }

        fileEntry.putAll(analysis);
        return fileEntry;
    }

    /**
     * Checks if the given path is within a Git repository.
     * <p>
     * This method looks for a .git directory in the given path or any of its parent directories.
     *
     * @param root The path to check
     * @return true if the path is within a Git repository, false otherwise
     */
    private boolean isGitRepo(final Path root) {
        Path current = root;
        while (current != null) {
            if (Files.isDirectory(current.resolve(GIT_DIR))) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}

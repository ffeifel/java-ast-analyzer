package com.example;

import com.example.analyzer.CodeSearcher;
import com.example.analyzer.GitRepositoryAnalyzer;
import com.example.analyzer.PromptAnalyzer;
import com.example.tokenizer.CodeElementParser;
import com.example.tokenizer.CodeTokenizer;
import com.example.tokenizer.entity.TokenizedCodeElement;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

@Log
public class Main {

    public static void main(String[] args) {
        String gitRepoPath;
        String prompt = null;

        if (args.length > 0) {
            gitRepoPath = args[0];
        } else {
            gitRepoPath = ".";
            log.log(Level.INFO, "No path provided. Using default: " + gitRepoPath);
        }

        if (args.length > 1) {
            prompt = args[1];
        }

        GitRepositoryAnalyzer gitProjectToJson = new GitRepositoryAnalyzer();
        final String outputDir = gitProjectToJson.parseGitProject(gitRepoPath);

        try {
            final var codeElements = CodeElementParser.parseFromJson(outputDir);

            final var tokenizedElements = codeElements.stream()
                    .map(codeElement -> new TokenizedCodeElement(codeElement, new CodeTokenizer()))
                    .toList();

            log.log(Level.INFO, "Analyzed " + tokenizedElements.size() + " code elements");

            // If a prompt is provided, perform search and generate context
            if (prompt != null && !prompt.trim().isEmpty()) {
                performSearch(prompt, tokenizedElements);
            } else {
                log.log(Level.INFO, "No prompt provided. Repository analysis complete.");
                log.log(Level.INFO, "Usage: java -jar app.jar <repo-path> \"<search-prompt>\"");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void performSearch(String prompt, List<TokenizedCodeElement> tokenizedElements) {
        log.log(Level.INFO, "Performing search for prompt: " + prompt);

        PromptAnalyzer promptAnalyzer = new PromptAnalyzer();
        var promptTokens = promptAnalyzer.analyzePrompt(prompt);

        CodeSearcher searcher = new CodeSearcher();
        var searchResults = searcher.search(promptTokens, tokenizedElements, 10);

        ContextGenerator contextGenerator = new ContextGenerator();
        String context = contextGenerator.generateContext(prompt, searchResults);

        // Output the context
        System.out.println("\n" + "=".repeat(80));
        System.out.println("GENERATED CONTEXT FOR LLM:");
        System.out.println("=".repeat(80));
        System.out.println(context);
        System.out.println("=".repeat(80));
    }

}

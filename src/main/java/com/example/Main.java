package com.example;

import com.example.tokenizer.CodeElementParser;
import com.example.tokenizer.CodeTokenizer;
import com.example.tokenizer.TokenizedCodeElement;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.logging.Level;

@Log
public class Main {

    public static void main(String[] args) {
        String gitRepoPath;

        if (args.length > 0) {
            gitRepoPath = args[0];
        } else {
            // Default path if no argument is provided
            gitRepoPath = ".";
            log.log(Level.INFO, "No path provided. Using default: " + gitRepoPath);
        }

        GitRepositoryAnalyzer gitProjectToJson = new GitRepositoryAnalyzer();
        final String outputDir = gitProjectToJson.parseGitProject(gitRepoPath);

        try {
            final var codeElements = CodeElementParser.parseFromJson(outputDir);

            final var tokenizedElements = codeElements.stream()
                    .map(codeElement -> new TokenizedCodeElement(codeElement, new CodeTokenizer()))
                    .toList();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}

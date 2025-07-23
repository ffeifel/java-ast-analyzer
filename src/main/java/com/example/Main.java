package com.example;

import lombok.extern.java.Log;

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
        gitProjectToJson.parseGitProject(gitRepoPath);
    }

}

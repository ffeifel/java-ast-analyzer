package com.example.analyzer;

import com.example.tokenizer.CodeTokenizer;
import lombok.extern.java.Log;

import java.util.Set;
import java.util.logging.Level;

@Log
public class PromptAnalyzer {

    private final CodeTokenizer tokenizer;

    public PromptAnalyzer() {
        this.tokenizer = new CodeTokenizer();
    }

    /**
     * Analyzes a user prompt and extracts relevant tokens for code search
     *
     * @param prompt the user's search prompt
     * @return set of tokens extracted from the prompt
     */
    public Set<String> analyzePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            log.log(Level.WARNING, "Empty or null prompt provided");
            return Set.of();
        }

        log.log(Level.INFO, "Analyzing prompt: " + prompt);

        // Tokenize the prompt using the same tokenizer as code elements
        Set<String> tokens = tokenizer.tokenize(prompt);

        log.info("Tokens extracted.");
        log.log(Level.FINE, "Extracted tokens: " + tokens);

        return tokens;
    }
}

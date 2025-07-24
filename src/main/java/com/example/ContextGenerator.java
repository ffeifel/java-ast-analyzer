package com.example;

import com.example.analyzer.CodeSearcher.ScoredCodeElement;
import com.example.tokenizer.CodeElement;
import com.example.tokenizer.TokenizedCodeElement;
import lombok.extern.java.Log;

import java.util.List;
import java.util.logging.Level;

@Log
public class ContextGenerator {

    /**
     * Generates LLM context from search results
     *
     * @param prompt        the original user prompt
     * @param searchResults list of scored code elements
     * @return formatted context string for LLM
     */
    public String generateContext(String prompt, List<ScoredCodeElement> searchResults) {
        if (searchResults.isEmpty()) {
            log.log(Level.INFO, "No search results to generate context from");
            return "No relevant code elements found for the given prompt.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Relevant code for: \"").append(prompt).append("\"\n\n");

        for (int i = 0; i < searchResults.size(); i++) {
            ScoredCodeElement scored = searchResults.get(i);
            TokenizedCodeElement tokenized = scored.codeElement();
            CodeElement original = tokenized.getOriginalCodeElement();

            // Class header with file path
            context.append(String.format("%d. %s (%.2f)\n", 
                i + 1, 
                original.getClassName() != null ? original.getClassName() : "Unknown",
                scored.score()));
            
            context.append(String.format("   File: %s/%s.java\n",
                original.getPackageName() != null ? original.getPackageName().replace(".", "/") : "",
                original.getClassName() != null ? original.getClassName() : "Unknown"));

            if (original.getMethods() != null && !original.getMethods().isEmpty()) {
                context.append("   Methods: ");
                context.append(original.getMethods().stream()
                        .limit(6) // Reduced to 6 for better readability
                        .map(method -> {
                            String params = "";
                            if (method.getParameters() != null && !method.getParameters().isEmpty()) {
                                params = method.getParameters().size() == 1 ? "1 param" : method.getParameters().size() + " params";
                            }
                            return method.getName() + "(" + params + ")";
                        })
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));
                context.append("\n");
            }

            if (original.getImports() != null && !original.getImports().isEmpty()) {
                context.append("   Uses: ");
                context.append(String.join(", ", original.getImports().stream()
                        .filter(imp -> !imp.startsWith("java.lang")) // Filter out basic Java imports
                        .map(imp -> imp.substring(imp.lastIndexOf('.') + 1)) // Just class names
                        .limit(4)
                        .toList()));
                context.append("\n");
            }
            context.append("\n");
        }

        String result = context.toString();
        log.log(Level.INFO, "Generated context with " + searchResults.size() + " code elements");

        return result;
    }
}

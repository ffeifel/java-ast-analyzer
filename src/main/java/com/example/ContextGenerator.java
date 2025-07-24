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

            context.append(String.format("%d. %s (%.2f) - %s\n", 
                i + 1, 
                original.getClassName() != null ? original.getClassName() : "Unknown",
                scored.score(),
                original.getPackageName() != null ? original.getPackageName() : ""));

            if (original.getMethods() != null && !original.getMethods().isEmpty()) {
                context.append("   Methods: ");
                context.append(original.getMethods().stream()
                        .limit(8) // Limit to avoid clutter
                        .map(method -> method.getName() + 
                            (method.getParameters() != null && !method.getParameters().isEmpty() 
                                ? "(" + method.getParameters().size() + " params)" 
                                : "()"))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));
                context.append("\n");
            }

            if (original.getImports() != null && !original.getImports().isEmpty()) {
                context.append("   Key imports: ");
                context.append(String.join(", ", original.getImports().stream()
                        .limit(3) // Reduced from 5 to 3
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

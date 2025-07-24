package com.example;

import com.example.CodeSearcher.ScoredCodeElement;
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
     * @param prompt the original user prompt
     * @param searchResults list of scored code elements
     * @return formatted context string for LLM
     */
    public String generateContext(String prompt, List<ScoredCodeElement> searchResults) {
        if (searchResults.isEmpty()) {
            log.log(Level.INFO, "No search results to generate context from");
            return "No relevant code elements found for the given prompt.";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("Based on your prompt: \"").append(prompt).append("\"\n\n");
        context.append("Here are the most relevant code elements from the repository:\n\n");
        
        for (int i = 0; i < searchResults.size(); i++) {
            ScoredCodeElement scored = searchResults.get(i);
            TokenizedCodeElement tokenized = scored.getCodeElement();
            CodeElement original = tokenized.getOriginalCodeElement();
            
            context.append(String.format("=== Result %d (Relevance: %.2f) ===\n", i + 1, scored.getScore()));
            
            if (original.getClassName() != null && !original.getClassName().isEmpty()) {
                context.append("Class: ").append(original.getClassName()).append("\n");
            }
            
            if (original.getPackageName() != null && !original.getPackageName().isEmpty()) {
                context.append("Package: ").append(original.getPackageName()).append("\n");
            }
            
            if (original.getMethods() != null && !original.getMethods().isEmpty()) {
                context.append("Methods:\n");
                for (CodeElement.Method method : original.getMethods()) {
                    context.append("  - ").append(method.getName());
                    if (method.getParameters() != null && !method.getParameters().isEmpty()) {
                        context.append("(").append(String.join(", ", method.getParameters())).append(")");
                    }
                    context.append("\n");
                }
            }
            
            if (original.getImports() != null && !original.getImports().isEmpty()) {
                context.append("Key Imports: ");
                context.append(String.join(", ", original.getImports().stream()
                        .limit(5) // Limit to first 5 imports to avoid clutter
                        .toList()));
                context.append("\n");
            }
            
            context.append("\n");
        }
        
        context.append("Use this context to provide more accurate and relevant responses about the codebase.");
        
        String result = context.toString();
        log.log(Level.INFO, "Generated context with " + searchResults.size() + " code elements");
        
        return result;
    }
}

package com.example;

import com.example.tokenizer.TokenizedCodeElement;
import lombok.extern.java.Log;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
public class CodeSearcher {
    
    /**
     * Searches for code elements that match the given prompt tokens
     * 
     * @param promptTokens tokens extracted from the user prompt
     * @param codeElements tokenized code elements to search through
     * @param maxResults maximum number of results to return
     * @return list of matching code elements sorted by relevance score
     */
    public List<ScoredCodeElement> search(Set<String> promptTokens, 
                                        List<TokenizedCodeElement> codeElements, 
                                        int maxResults) {
        
        if (promptTokens.isEmpty()) {
            log.log(Level.WARNING, "No prompt tokens provided for search");
            return List.of();
        }
        
        log.log(Level.INFO, "Searching " + codeElements.size() + " code elements for tokens: " + promptTokens);
        
        List<ScoredCodeElement> scoredElements = codeElements.stream()
                .map(element -> new ScoredCodeElement(element, calculateRelevanceScore(promptTokens, element)))
                .filter(scored -> scored.getScore() > 0.15) // Only include elements with meaningful relevance
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore())) // Sort by score descending
                .limit(maxResults)
                .collect(Collectors.toList());
        
        log.log(Level.INFO, "Found " + scoredElements.size() + " matching elements");
        
        return scoredElements;
    }
    
    /**
     * Calculates relevance score between prompt tokens and a code element
     * 
     * @param promptTokens tokens from the user prompt
     * @param codeElement tokenized code element
     * @return relevance score (higher = more relevant)
     */
    private double calculateRelevanceScore(Set<String> promptTokens, TokenizedCodeElement codeElement) {
        double score = 0.0;
        
        // Weight different token types differently
        score += calculateTokenOverlap(promptTokens, codeElement.getClassTokens()) * 3.0; // Class names are very important
        score += calculateTokenOverlap(promptTokens, codeElement.getMethodTokens()) * 2.0; // Method names are important
        score += calculateTokenOverlap(promptTokens, codeElement.getPackageTokens()) * 1.0; // Package names are somewhat important
        score += calculateTokenOverlap(promptTokens, codeElement.getImportTokens()) * 0.5; // Imports are less important
        
        return score;
    }
    
    /**
     * Calculates the overlap ratio between two token sets
     * 
     * @param promptTokens tokens from prompt
     * @param codeTokens tokens from code element
     * @return overlap ratio (0.0 to 1.0)
     */
    private double calculateTokenOverlap(Set<String> promptTokens, Set<String> codeTokens) {
        if (promptTokens.isEmpty() || codeTokens.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(promptTokens);
        intersection.retainAll(codeTokens);
        
        // Return the ratio of matching tokens to total unique tokens
        Set<String> union = new HashSet<>(promptTokens);
        union.addAll(codeTokens);
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * Container class for a code element with its relevance score
     */
    public static class ScoredCodeElement {
        private final TokenizedCodeElement codeElement;
        private final double score;
        
        public ScoredCodeElement(TokenizedCodeElement codeElement, double score) {
            this.codeElement = codeElement;
            this.score = score;
        }
        
        public TokenizedCodeElement getCodeElement() {
            return codeElement;
        }
        
        public double getScore() {
            return score;
        }
    }
}

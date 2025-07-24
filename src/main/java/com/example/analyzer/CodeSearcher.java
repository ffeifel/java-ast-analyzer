package com.example.analyzer;

import com.example.tokenizer.entity.TokenizedCodeElement;
import lombok.extern.java.Log;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
public class CodeSearcher {
    
    private final InvertedIndex invertedIndex = new InvertedIndex();
    private boolean indexBuilt = false;

    /**
     * Searches for code elements that match the given prompt tokens
     *
     * @param promptTokens tokens extracted from the user prompt
     * @param codeElements tokenized code elements to search through
     * @param maxResults   maximum number of results to return
     * @return list of matching code elements sorted by relevance score
     */
    public List<ScoredCodeElement> search(Set<String> promptTokens, List<TokenizedCodeElement> codeElements, int maxResults) {

        if (promptTokens.isEmpty()) {
            log.log(Level.WARNING, "No prompt tokens provided for search");
            return List.of();
        }

        // Build index if not already built
        if (!indexBuilt) {
            invertedIndex.buildIndex(codeElements);
            indexBuilt = true;
        }

        log.log(Level.FINE, "Searching " + codeElements.size() + " code elements for tokens: " + promptTokens);
        log.info("Searching " + codeElements.size() + " code elements for tokens");

        // Use inverted index to get candidates
        Set<TokenizedCodeElement> candidates = invertedIndex.getCandidates(promptTokens);
        log.log(Level.INFO, "Inverted index reduced search space from " + codeElements.size() + " to " + candidates.size() + " candidates");

        // Use priority queue for early termination
        PriorityQueue<ScoredCodeElement> topResults = new PriorityQueue<>(
            maxResults + 1, 
            Comparator.comparingDouble(ScoredCodeElement::score)
        );

        double minScoreThreshold = 0.15;
        
        for (TokenizedCodeElement element : candidates) {
            double score = calculateRelevanceScore(promptTokens, element);
            
            if (score > minScoreThreshold) {
                topResults.offer(new ScoredCodeElement(element, score));
                
                // Early termination: keep only top maxResults
                if (topResults.size() > maxResults) {
                    topResults.poll(); // Remove lowest score
                    // Update threshold to the new minimum
                    if (!topResults.isEmpty()) {
                        minScoreThreshold = Math.max(minScoreThreshold, topResults.peek().score());
                    }
                }
            }
        }

        // Convert to list and sort in descending order
        List<ScoredCodeElement> scoredElements = new ArrayList<>(topResults);
        scoredElements.sort((a, b) -> Double.compare(b.score(), a.score()));

        log.log(Level.INFO, "Found " + scoredElements.size() + " matching elements");

        return scoredElements;
    }

    /**
     * Calculates relevance score between prompt tokens and a code element
     * Now includes TF-IDF weighting for better relevance
     *
     * @param promptTokens tokens from the user prompt
     * @param codeElement  tokenized code element
     * @return relevance score (higher = more relevant)
     */
    private double calculateRelevanceScore(Set<String> promptTokens, TokenizedCodeElement codeElement) {
        double score = 0.0;

        // Weight different token types differently with TF-IDF
        score += calculateTfIdfScore(promptTokens, codeElement.getClassTokens()) * 3.0;
        score += calculateTfIdfScore(promptTokens, codeElement.getMethodTokens()) * 2.0;
        score += calculateTfIdfScore(promptTokens, codeElement.getPackageTokens()) * 1.0;
        score += calculateTfIdfScore(promptTokens, codeElement.getImportTokens()) * 0.5;

        return score;
    }
    
    /**
     * Calculates TF-IDF weighted score for token overlap
     */
    private double calculateTfIdfScore(Set<String> promptTokens, Set<String> codeTokens) {
        if (promptTokens.isEmpty() || codeTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(promptTokens);
        intersection.retainAll(codeTokens);
        
        if (intersection.isEmpty()) {
            return 0.0;
        }

        double tfIdfScore = 0.0;
        for (String token : intersection) {
            // Term frequency in this context is binary (present or not)
            double tf = 1.0;
            // Get inverse document frequency from index
            double idf = invertedIndex.getTokenIDF(token);
            tfIdfScore += tf * idf;
        }

        // Normalize by the size of the union to get a ratio
        Set<String> union = new HashSet<>(promptTokens);
        union.addAll(codeTokens);
        
        return tfIdfScore / union.size();
    }

    /**
     * Calculates the overlap ratio between two token sets
     *
     * @param promptTokens tokens from prompt
     * @param codeTokens   tokens from code element
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
    public record ScoredCodeElement(TokenizedCodeElement codeElement, double score) {
    }
}

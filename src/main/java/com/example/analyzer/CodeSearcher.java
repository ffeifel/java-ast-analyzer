package com.example.analyzer;

import com.example.tokenizer.TokenizedCodeElement;
import lombok.extern.java.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
public class CodeSearcher {

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

        log.log(Level.FINE, "Searching " + codeElements.size() + " code elements for tokens: " + promptTokens);
        log.info("Searching " + codeElements.size() + " code elements for tokens");

        List<ScoredCodeElement> scoredElements = codeElements.stream()
                .map(element -> new ScoredCodeElement(element, calculateRelevanceScore(promptTokens, element)))
                .filter(scored -> scored.score() > 0.15)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(maxResults)
                .collect(Collectors.toList());

        log.log(Level.INFO, "Found " + scoredElements.size() + " matching elements");

        return scoredElements;
    }

    /**
     * Calculates relevance score between prompt tokens and a code element
     *
     * @param promptTokens tokens from the user prompt
     * @param codeElement  tokenized code element
     * @return relevance score (higher = more relevant)
     */
    private double calculateRelevanceScore(Set<String> promptTokens, TokenizedCodeElement codeElement) {
        double score = 0.0;

        // Weight different token types differently
        score += calculateTokenOverlap(promptTokens, codeElement.getClassTokens()) * 3.0;
        score += calculateTokenOverlap(promptTokens, codeElement.getMethodTokens()) * 2.0;
        score += calculateTokenOverlap(promptTokens, codeElement.getPackageTokens());
        score += calculateTokenOverlap(promptTokens, codeElement.getImportTokens()) * 0.5;

        return score;
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

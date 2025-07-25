package com.example.analyzer;

import com.example.tokenizer.entity.TokenizedCodeElement;
import lombok.extern.java.Log;

import java.util.*;
import java.util.logging.Level;

@Log
public class CodeSearcher {

    private final InvertedIndex invertedIndex = new InvertedIndex();
    private boolean indexBuilt = false;

    /**
     * Searches for code elements that match the given prompt tokens using vector space model
     *
     * @param promptTokens tokens extracted from the user prompt
     * @param codeElements tokenized code elements to search through
     * @param maxResults   maximum number of results to return
     * @return list of matching code elements sorted by relevance score
     */
    public List<ScoredCodeElement> search(final Set<String> promptTokens,
                                          final List<TokenizedCodeElement> codeElements, final int maxResults) {

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

        // Build query vector
        final Map<String, Double> queryVector = invertedIndex.buildQueryVector(promptTokens);
        final double queryNorm = calculateVectorNorm(queryVector);

        if (queryNorm == 0.0) {
            log.log(Level.WARNING, "Query vector has zero norm - no matching tokens in vocabulary");
            return List.of();
        }

        // Use inverted index to get candidates
        final Set<TokenizedCodeElement> candidates = invertedIndex.getCandidates(promptTokens);
        log.log(Level.INFO,
                "Inverted index reduced search space from " + codeElements.size() + " to " + candidates.size() + " " +
                        "candidates");

        // Use priority queue for early termination
        final PriorityQueue<ScoredCodeElement> topResults = new PriorityQueue<>(
                maxResults + 1,
                Comparator.comparingDouble(ScoredCodeElement::score)
        );

        double minScoreThreshold = 0.01; // Lower threshold for cosine similarity

        for (final TokenizedCodeElement element : candidates) {
            final double score = calculateCosineSimilarity(queryVector, queryNorm, element);

            if (score > minScoreThreshold) {
                topResults.offer(new ScoredCodeElement(element, score));

                // Early termination: keep only top maxResults
                if (topResults.size() > maxResults) {
                    topResults.poll(); // Remove the lowest score
                    // Update threshold to the new minimum
                    if (!topResults.isEmpty()) {
                        minScoreThreshold = Math.max(minScoreThreshold, topResults.peek().score());
                    }
                }
            }
        }

        // Convert to list and sort in descending order
        final List<ScoredCodeElement> scoredElements = new ArrayList<>(topResults);
        scoredElements.sort((a, b) -> Double.compare(b.score(), a.score()));

        log.log(Level.INFO, "Found " + scoredElements.size() + " matching elements");

        return scoredElements;
    }

    /**
     * Calculates cosine similarity between query vector and document vector
     *
     * @param queryVector the TF-IDF vector of the query
     * @param queryNorm   the norm of the query vector
     * @param document    the document to compare against
     * @return cosine similarity score (0.0 to 1.0)
     */
    private double calculateCosineSimilarity(final Map<String, Double> queryVector, final double queryNorm,
                                             final TokenizedCodeElement document) {
        final Map<String, Double> docVector = invertedIndex.getDocumentVector(document);
        final double docNorm = invertedIndex.getDocumentNorm(document);

        if (docNorm == 0.0 || queryNorm == 0.0) {
            return 0.0;
        }

        // Calculate dot product
        double dotProduct = 0.0;
        for (final Map.Entry<String, Double> entry : queryVector.entrySet()) {
            final String token = entry.getKey();
            final double queryWeight = entry.getValue();
            final double docWeight = docVector.getOrDefault(token, 0.0);
            dotProduct += queryWeight * docWeight;
        }

        // Cosine similarity = dot product / (norm1 * norm2)
        return dotProduct / (queryNorm * docNorm);
    }

    /**
     * Calculates the norm (magnitude) of a vector
     */
    private double calculateVectorNorm(final Map<String, Double> vector) {
        return Math.sqrt(vector.values().stream()
                .mapToDouble(value -> value * value)
                .sum());
    }

    /**
     * Container class for a code element with its relevance score
     */
    public record ScoredCodeElement(TokenizedCodeElement codeElement, double score) {
    }
}

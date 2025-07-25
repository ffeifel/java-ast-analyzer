package com.example.analyzer;

import com.example.tokenizer.entity.TokenizedCodeElement;
import lombok.extern.java.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Log
public class InvertedIndex {

    private final Map<String, Set<TokenizedCodeElement>> tokenToElements = new ConcurrentHashMap<>();
    private final Map<String, Double> tokenIDF = new ConcurrentHashMap<>();
    private final Map<TokenizedCodeElement, Map<String, Double>> documentVectors = new ConcurrentHashMap<>();
    private final Map<TokenizedCodeElement, Double> documentNorms = new ConcurrentHashMap<>();
    private Set<String> vocabulary = new HashSet<>();
    private boolean isBuilt = false;

    /**
     * Builds the inverted index and TF-IDF vectors from a list of tokenized code elements
     */
    public void buildIndex(final List<TokenizedCodeElement> codeElements) {
        log.log(Level.INFO, "Building inverted index for " + codeElements.size() + " code elements");

        tokenToElements.clear();
        tokenIDF.clear();
        documentVectors.clear();
        documentNorms.clear();
        vocabulary.clear();

        // Step 1: Build inverted index and collect vocabulary
        for (final TokenizedCodeElement element : codeElements) {
            indexTokens(element.getClassTokens(), element);
            indexTokens(element.getMethodTokens(), element);
            indexTokens(element.getPackageTokens(), element);
            indexTokens(element.getImportTokens(), element);
        }

        vocabulary = new HashSet<>(tokenToElements.keySet());

        // Step 2: Calculate IDF for each token
        final int totalDocuments = codeElements.size();
        for (final Map.Entry<String, Set<TokenizedCodeElement>> entry : tokenToElements.entrySet()) {
            final String token = entry.getKey();
            final int documentFreq = entry.getValue().size();
            // Add smoothing to avoid division by zero and log(0)
            final double idf = Math.log((double) (totalDocuments + 1) / (documentFreq + 1)) + 1.0;
            tokenIDF.put(token, idf);
        }

        // Step 3: Build TF-IDF vectors for each document
        for (final TokenizedCodeElement element : codeElements) {
            final Map<String, Double> vector = buildDocumentVector(element);
            documentVectors.put(element, vector);

            // Calculate and store document norm for cosine similarity
            final double norm = calculateVectorNorm(vector);
            documentNorms.put(element, norm);
        }

        isBuilt = true;
        log.log(Level.INFO, "Vector space model built with " + vocabulary.size() + " unique tokens");
    }

    private void indexTokens(final Set<String> tokens, final TokenizedCodeElement element) {
        for (final String token : tokens) {
            tokenToElements.computeIfAbsent(token, k -> new HashSet<>()).add(element);
        }
    }

    /**
     * Builds a TF-IDF vector for a document
     */
    private Map<String, Double> buildDocumentVector(final TokenizedCodeElement element) {
        final Map<String, Double> vector = new HashMap<>();

        // Count term frequencies with different weights for different token types
        final Map<String, Double> termFreqs = new HashMap<>();

        // Weight different token types
        addTokensWithWeight(termFreqs, element.getClassTokens(), 3.0);
        addTokensWithWeight(termFreqs, element.getMethodTokens(), 2.0);
        addTokensWithWeight(termFreqs, element.getPackageTokens(), 1.0);
        addTokensWithWeight(termFreqs, element.getImportTokens(), 0.5);

        // Calculate total terms for TF normalization
        final double totalTerms = termFreqs.values().stream().mapToDouble(Double::doubleValue).sum();

        // Build TF-IDF vector
        for (final Map.Entry<String, Double> entry : termFreqs.entrySet()) {
            final String token = entry.getKey();
            final double tf = entry.getValue() / totalTerms; // Normalized term frequency
            final double idf = tokenIDF.getOrDefault(token, 0.0);
            final double tfIdf = tf * idf;

            if (tfIdf > 0) {
                vector.put(token, tfIdf);
            }
        }

        return vector;
    }

    private void addTokensWithWeight(final Map<String, Double> termFreqs, final Set<String> tokens,
                                     final double weight) {
        for (final String token : tokens) {
            termFreqs.merge(token, weight, Double::sum);
        }
    }

    private double calculateVectorNorm(final Map<String, Double> vector) {
        return Math.sqrt(vector.values().stream()
                .mapToDouble(value -> value * value)
                .sum());
    }

    /**
     * Gets candidate elements that contain at least one of the prompt tokens
     */
    public Set<TokenizedCodeElement> getCandidates(final Set<String> promptTokens) {
        if (!isBuilt) {
            throw new IllegalStateException("Index must be built before searching");
        }

        final Set<TokenizedCodeElement> candidates = new HashSet<>();
        for (final String token : promptTokens) {
            final Set<TokenizedCodeElement> elements = tokenToElements.get(token);
            if (elements != null) {
                candidates.addAll(elements);
            }
        }

        return candidates;
    }

    /**
     * Gets the TF-IDF vector for a document
     */
    public Map<String, Double> getDocumentVector(final TokenizedCodeElement element) {
        return documentVectors.getOrDefault(element, new HashMap<>());
    }

    /**
     * Gets the norm of a document vector
     */
    public double getDocumentNorm(final TokenizedCodeElement element) {
        return documentNorms.getOrDefault(element, 0.0);
    }

    /**
     * Gets the inverse document frequency for a token
     */
    public double getTokenIDF(final String token) {
        return tokenIDF.getOrDefault(token, 0.0);
    }

    /**
     * Builds a query vector from prompt tokens
     */
    public Map<String, Double> buildQueryVector(final Set<String> promptTokens) {
        if (!isBuilt) {
            throw new IllegalStateException("Index must be built before creating query vector");
        }

        final Map<String, Double> queryVector = new HashMap<>();

        // Simple TF for query (could be enhanced with query term weighting)
        for (final String token : promptTokens) {
            if (vocabulary.contains(token)) {
                final double tf = 1.0; // Binary or could count occurrences
                final double idf = getTokenIDF(token);
                final double tfIdf = tf * idf;

                if (tfIdf > 0) {
                    queryVector.put(token, tfIdf);
                }
            }
        }

        return queryVector;
    }
}

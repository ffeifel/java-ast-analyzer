package com.example.analyzer;

import com.example.tokenizer.entity.TokenizedCodeElement;
import lombok.extern.java.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Log
public class InvertedIndex {
    
    private final Map<String, Set<TokenizedCodeElement>> tokenToElements = new ConcurrentHashMap<>();
    private final Map<String, Double> tokenFrequency = new ConcurrentHashMap<>();
    private boolean isBuilt = false;
    
    /**
     * Builds the inverted index from a list of tokenized code elements
     */
    public void buildIndex(List<TokenizedCodeElement> codeElements) {
        log.log(Level.INFO, "Building inverted index for " + codeElements.size() + " code elements");
        
        tokenToElements.clear();
        tokenFrequency.clear();
        
        for (TokenizedCodeElement element : codeElements) {
            indexTokens(element.getClassTokens(), element);
            indexTokens(element.getMethodTokens(), element);
            indexTokens(element.getPackageTokens(), element);
            indexTokens(element.getImportTokens(), element);
        }
        
        // Calculate inverse document frequency for each token
        int totalElements = codeElements.size();
        for (Map.Entry<String, Set<TokenizedCodeElement>> entry : tokenToElements.entrySet()) {
            String token = entry.getKey();
            int documentFreq = entry.getValue().size();
            double idf = Math.log((double) totalElements / documentFreq);
            tokenFrequency.put(token, idf);
        }
        
        isBuilt = true;
        log.log(Level.INFO, "Inverted index built with " + tokenToElements.size() + " unique tokens");
    }
    
    private void indexTokens(Set<String> tokens, TokenizedCodeElement element) {
        for (String token : tokens) {
            tokenToElements.computeIfAbsent(token, k -> new HashSet<>()).add(element);
        }
    }
    
    /**
     * Gets candidate elements that contain at least one of the prompt tokens
     */
    public Set<TokenizedCodeElement> getCandidates(Set<String> promptTokens) {
        if (!isBuilt) {
            throw new IllegalStateException("Index must be built before searching");
        }
        
        Set<TokenizedCodeElement> candidates = new HashSet<>();
        for (String token : promptTokens) {
            Set<TokenizedCodeElement> elements = tokenToElements.get(token);
            if (elements != null) {
                candidates.addAll(elements);
            }
        }
        
        return candidates;
    }
    
    /**
     * Gets the inverse document frequency for a token
     */
    public double getTokenIDF(String token) {
        return tokenFrequency.getOrDefault(token, 0.0);
    }
    
    /**
     * Gets all tokens that have elements associated with them
     */
    public Set<String> getAllTokens() {
        return tokenToElements.keySet();
    }
}

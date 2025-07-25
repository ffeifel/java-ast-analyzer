package com.example.tokenizer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CodeTokenizer {

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[._|\\-\\s$@]+");
    private static final Pattern CAMEL_CASE_PATTERN1 = Pattern.compile("([a-z])([A-Z])");
    private static final Pattern CAMEL_CASE_PATTERN2 = Pattern.compile("([A-Z+])([A-Z][a-z])");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([a-zA-Z])([0-9])|([0-9])([a-zA-Z])");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9]+");

    // Cache for tokenization results
    private final ConcurrentHashMap<String, Set<String>> tokenCache = new ConcurrentHashMap<>();

    // Pre-compiled regex results cache
    private final ConcurrentHashMap<String, String[]> separatorCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> camelCaseCache = new ConcurrentHashMap<>();

    public Set<String> tokenize(final String text) {
        if (text == null || text.isEmpty()) {
            return new HashSet<>();
        }

        // Check cache first
        final Set<String> cached = tokenCache.get(text);
        if (cached != null) {
            return new HashSet<>(cached); // Return copy to avoid modification
        }

        final Set<String> tokens = new HashSet<>();

        addTokensFromSeparators(text, tokens);
        addTokensFromCamelCase(text, tokens);
        addTokensFromNumericSeparation(text, tokens);
        addTokensFromAcronyms(text, tokens);
        addSubstrings(text, tokens);

        // Cache the result
        tokenCache.put(text, new HashSet<>(tokens));
        return tokens;
    }

    private void addTokensFromSeparators(final String text, final Set<String> tokens) {
        final String[] parts = separatorCache.computeIfAbsent(text, SEPARATOR_PATTERN::split);
        for (final String part : parts) {
            // Clean special characters from each part
            final String cleanPart = SPECIAL_CHARS_PATTERN.matcher(part).replaceAll("");
            // Only add if it's purely alphabetic and longer than 1 character
            if (cleanPart.length() > 1 && cleanPart.matches("[a-zA-Z]+")) {
                tokens.add(cleanPart.toLowerCase());
            }
        }
    }

    private void addTokensFromCamelCase(final String text, final Set<String> tokens) {
        final String processed = camelCaseCache.computeIfAbsent(text, t -> {
            final String temp = CAMEL_CASE_PATTERN1.matcher(t).replaceAll("$1 $2");
            return CAMEL_CASE_PATTERN2.matcher(temp).replaceAll("$1 $2");
        });

        final String[] parts = WHITESPACE_PATTERN.split(processed);
        for (final String part : parts) {
            // Clean special characters from each part
            final String cleanPart = SPECIAL_CHARS_PATTERN.matcher(part).replaceAll("");
            if (cleanPart.length() > 1) {
                tokens.add(cleanPart.toLowerCase());
            }
        }
    }

    private void addTokensFromNumericSeparation(final String text, final Set<String> tokens) {
        final String processed = NUMERIC_PATTERN.matcher(text).replaceAll("$1$3 $2$4");
        final String[] parts = WHITESPACE_PATTERN.split(processed);
        for (final String part : parts) {
            if (part.length() > 1 && !part.matches("\\d+")) {
                tokens.add(part.toLowerCase());
            }
        }
    }

    private void addTokensFromAcronyms(final String text, final Set<String> tokens) {
        final StringBuilder currentAcronym = new StringBuilder();
        for (final char c : text.toCharArray()) {
            if (Character.isUpperCase(c)) {
                currentAcronym.append(c);
            } else {
                if (currentAcronym.length() > 1) {
                    tokens.add(currentAcronym.toString().toLowerCase());
                }
                currentAcronym.setLength(0);
            }
        }
        if (currentAcronym.length() > 1) {
            tokens.add(currentAcronym.toString().toLowerCase());
        }
    }

    private void addSubstrings(final String text, final Set<String> tokens) {
        final String cleanText = SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");
        if (cleanText.length() < 3) return;

        for (int len = 3; len <= Math.min(6, cleanText.length()); len++) {
            for (int i = 0; i <= cleanText.length() - len; i++) {
                final String substring = cleanText.substring(i, i + len).toLowerCase();
                if (substring.matches("[a-z]+")) {
                    tokens.add(substring);
                }
            }
        }
    }

}

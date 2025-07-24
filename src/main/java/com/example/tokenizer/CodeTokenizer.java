package com.example.tokenizer;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class CodeTokenizer {

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[._|\\-\\s]+");
    private static final Pattern CAMEL_CASE_PATTERN1 = Pattern.compile("([a-z])([A-Z])");
    private static final Pattern CAMEL_CASE_PATTERN2 = Pattern.compile("([A-Z+])([A-Z][a-z])");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("([a-zA-Z])([0-9])|([0-9])([a-zA-Z])");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9]+");

    public Set<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> tokens = new HashSet<>();

        addTokensFromSeparators(text, tokens);
        addTokensFromCamelCase(text, tokens);
        addTokensFromNumericSeparation(text, tokens);
        addTokensFromAcronyms(text, tokens);
        addSubstrings(text, tokens);

        return tokens;
    }

    private void addTokensFromSeparators(String text, Set<String> tokens) {
        String[] parts = SEPARATOR_PATTERN.split(text);
        for (String part : parts) {
            if (part.length() > 1) {
                tokens.add(part.toLowerCase());
            }
        }
    }

    private void addTokensFromCamelCase(String text, Set<String> tokens) {
        String processed = CAMEL_CASE_PATTERN1.matcher(text).replaceAll("$1 $2");
        processed = CAMEL_CASE_PATTERN2.matcher(processed).replaceAll("$1 $2");

        String[] parts = WHITESPACE_PATTERN.split(processed);
        for (String part : parts) {
            if (part.length() > 1) {
                tokens.add(part.toLowerCase());
            }
        }
    }

    private void addTokensFromNumericSeparation(String text, Set<String> tokens) {
        String processed = NUMERIC_PATTERN.matcher(text).replaceAll("$1$3 $2$4");
        String[] parts = WHITESPACE_PATTERN.split(processed);
        for (String part : parts) {
            if (part.length() > 1 && !part.matches("\\d+")) {
                tokens.add(part.toLowerCase());
            }
        }
    }

    private void addTokensFromAcronyms(String text, Set<String> tokens) {
        StringBuilder currentAcronym = new StringBuilder();
        for (char c : text.toCharArray()) {
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

    private void addSubstrings(String text, Set<String> tokens) {
        String cleanText = SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");
        if (cleanText.length() < 3) return;

        for (int len = 3; len <= Math.min(6, cleanText.length()); len++) {
            for (int i = 0; i <= cleanText.length() - len; i++) {
                String substring = cleanText.substring(i, i + len).toLowerCase();
                if (substring.matches("[a-z]+")) {
                    tokens.add(substring);
                }
            }
        }
    }

}
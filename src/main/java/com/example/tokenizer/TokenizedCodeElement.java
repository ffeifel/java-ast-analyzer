package com.example.tokenizer;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TokenizedCodeElement {
    private final CodeElement originalCodeElement;
    private Set<String> classTokens;
    private Set<String> methodTokens;
    private Set<String> packageTokens;
    private Set<String> importTokens;
    private Set<String> allTokens;

    public TokenizedCodeElement(CodeElement codeElement, CodeTokenizer tokenizer) {
        this.originalCodeElement = codeElement;

        this.classTokens = tokenizer.tokenize(codeElement.getClassName());
        this.methodTokens = codeElement.getMethods().stream()
                .flatMap(method -> tokenizer.tokenize(method.getName()).stream())
                .collect(Collectors.toSet());
        this.packageTokens = tokenizer.tokenize(codeElement.getPackageName());
        this.importTokens = codeElement.getImports().stream()
                .flatMap(importStmt -> tokenizer.tokenize(getLastPartOfImport(importStmt)).stream())
                .collect(Collectors.toSet());

        this.allTokens = new HashSet<>();
        this.allTokens.addAll(classTokens);
        this.allTokens.addAll(methodTokens);
        this.allTokens.addAll(packageTokens);
        this.allTokens.addAll(importTokens);
    }

    private String getLastPartOfImport(String importStmt) {
        String[] parts = importStmt.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : importStmt;
    }

    // Getter methods
    public CodeElement getOriginalCodeElement() {
        return originalCodeElement;
    }

    public Set<String> getClassTokens() {
        return classTokens;
    }

    public Set<String> getMethodTokens() {
        return methodTokens;
    }

    public Set<String> getPackageTokens() {
        return packageTokens;
    }

    public Set<String> getImportTokens() {
        return importTokens;
    }

    public Set<String> getAllTokens() {
        return allTokens;
    }
}

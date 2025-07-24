package com.example.tokenizer.entity;

import com.example.tokenizer.CodeTokenizer;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class TokenizedCodeElement {
    private final CodeElement originalCodeElement;
    private final Set<String> classTokens;
    private final Set<String> methodTokens;
    private final Set<String> packageTokens;
    private final Set<String> importTokens;
    private final Set<String> allTokens;

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

}

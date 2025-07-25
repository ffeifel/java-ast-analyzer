package com.example.tokenizer;

import com.example.tokenizer.entity.CodeElement;
import com.example.tokenizer.entity.JavaFileAnalysis;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to parse Java files analysis JSON into CodeElement objects
 */
public class CodeElementParser {

    /**
     * Parses a JSON file containing Java files analysis into a list of CodeElement objects
     *
     * @param jsonFilePath the path to the JSON file
     * @return a list of CodeElement objects
     * @throws IOException if there's an error reading or parsing the file
     */
    public static List<CodeElement> parseFromJson(final String jsonFilePath) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final List<JavaFileAnalysis> dtoList = objectMapper.readValue(
                new File(jsonFilePath),
                new TypeReference<>() {
                }
        );

        return dtoList.stream()
                .map(CodeElementParser::convertToCodeElement)
                .collect(Collectors.toList());
    }

    /**
     * Converts a JavaFileAnalysisDTO to a CodeElement
     *
     * @param dto the DTO to convert
     * @return a CodeElement object
     */
    private static CodeElement convertToCodeElement(final JavaFileAnalysis dto) {
        final CodeElement codeElement = new CodeElement();

        // Set class name (take the first one if it's a list)
        if (dto.getClassName() != null && !dto.getClassName().isEmpty()) {
            codeElement.setClassName(dto.getClassName().getFirst());
        }

        // Set package name (take the first one if it's a list)
        if (dto.getPackageName() != null && !dto.getPackageName().isEmpty()) {
            codeElement.setPackageName(dto.getPackageName().getFirst());
        }

        // Set imports
        if (dto.getImports() != null) {
            codeElement.setImports(dto.getImports());
        }

        // Set class relationships
        if (dto.getExtendsClasses() != null && !dto.getExtendsClasses().isEmpty()) {
            codeElement.setExtendsClass(dto.getExtendsClasses().getFirst());
        }

        if (dto.getImplementsInterfaces() != null) {
            codeElement.setImplementsInterfaces(dto.getImplementsInterfaces());
        }

        // Set methods
        final List<CodeElement.Method> methods = new ArrayList<>();

        // Add regular methods with detailed signatures if available
        if (dto.getMethodSignatures() != null) {
            for (final String methodSignature : dto.getMethodSignatures()) {
                methods.add(parseMethodSignature(methodSignature));
            }
        } else if (dto.getMethods() != null) {
            // Fallback to simple method names
            for (final String methodSignature : dto.getMethods()) {
                methods.add(new CodeElement.Method(extractMethodName(methodSignature)));
            }
        }

        // Add constructors as methods too
        if (dto.getConstructors() != null) {
            for (final String constructorSignature : dto.getConstructors()) {
                methods.add(new CodeElement.Method(extractMethodName(constructorSignature)));
            }
        }

        codeElement.setMethods(methods);

        return codeElement;
    }

    /**
     * Parses a detailed method signature into a Method object
     *
     * @param methodSignature the method signature like "String getName(int id, boolean active)"
     * @return a Method object with return type, name, and parameters
     */
    private static CodeElement.Method parseMethodSignature(final String methodSignature) {
        // Parse signature like "String getName(int id, boolean active)"
        final int openParenIndex = methodSignature.indexOf('(');
        final int closeParenIndex = methodSignature.lastIndexOf(')');

        if (openParenIndex == -1) {
            return new CodeElement.Method(methodSignature); // Fallback
        }

        final String beforeParams = methodSignature.substring(0, openParenIndex).trim();
        String paramsPart = "";
        if (closeParenIndex > openParenIndex) {
            paramsPart = methodSignature.substring(openParenIndex + 1, closeParenIndex).trim();
        }

        // Extract return type and method name
        final String[] parts = beforeParams.split("\\s+");
        final String methodName;
        String returnType = "void";

        if (parts.length >= 2) {
            returnType = parts[parts.length - 2];
        }
        methodName = parts[parts.length - 1];

        // Parse parameters
        final List<String> parameters = new ArrayList<>();
        if (!paramsPart.isEmpty()) {
            final String[] paramArray = paramsPart.split(",");
            for (final String param : paramArray) {
                parameters.add(param.trim());
            }
        }

        return new CodeElement.Method(methodName, returnType, parameters);
    }

    /**
     * Extracts the method name from a method signature
     *
     * @param methodSignature the method signature
     * @return the method name
     */
    private static String extractMethodName(final String methodSignature) {
        // Extract method name from signature like "void parseGitProject(String)"
        final int openParenIndex = methodSignature.indexOf('(');
        if (openParenIndex == -1) {
            return methodSignature; // No parameters
        }

        final String beforeParams = methodSignature.substring(0, openParenIndex).trim();
        final int lastSpaceIndex = beforeParams.lastIndexOf(' ');

        if (lastSpaceIndex == -1) {
            return beforeParams; // No return type (probably a constructor)
        }

        return beforeParams.substring(lastSpaceIndex + 1);
    }

}

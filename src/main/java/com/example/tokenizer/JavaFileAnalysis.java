package com.example.tokenizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO class that matches the structure of the JSON file
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JavaFileAnalysis {
    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("package")
    private List<String> packageName;

    @JsonProperty("imports")
    private List<String> imports;

    @JsonProperty("relativePath")
    private String relativePath;

    @JsonProperty("methods")
    private List<String> methods;

    @JsonProperty("constructors")
    private List<String> constructors;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("className")
    private List<String> className;

    @JsonProperty("extends")
    private List<String> extendsClasses;

    @JsonProperty("implements")
    private List<String> implementsInterfaces;

    @JsonProperty("methodSignatures")
    private List<String> methodSignatures;

    // Getter methods for new fields
    public List<String> getExtendsClasses() {
        return extendsClasses;
    }

    public List<String> getImplementsInterfaces() {
        return implementsInterfaces;
    }

    public List<String> getMethodSignatures() {
        return methodSignatures;
    }
}

package com.example.tokenizer.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a code element with its structural information such as
 * class name, methods, package name, and imports.
 */
@Setter
@Getter
public class CodeElement {

    private String className;
    private List<Method> methods;
    private String packageName;
    private List<String> imports;
    private String extendsClass;
    private List<String> implementsInterfaces;

    public CodeElement(final String className, final List<Method> methods, final String packageName,
                       final List<String> imports) {
        this.className = className;
        this.methods = methods;
        this.packageName = packageName;
        this.imports = imports;
    }

    public CodeElement() {
        this.methods = new ArrayList<>();
        this.imports = new ArrayList<>();
        this.implementsInterfaces = new ArrayList<>();
    }

    @Setter
    @Getter
    public static class Method {
        private String name;
        private String returnType;
        private List<String> parameters;

        public Method(final String name) {
            this.name = name;
            this.parameters = new ArrayList<>();
        }

        public Method(final String name, final String returnType, final List<String> parameters) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = parameters;
        }
    }

}

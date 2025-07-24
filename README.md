# Code Search Tool

A Java-based tool for analyzing Git repositories and intelligent code search.

## Overview

This tool analyzes Java projects in Git repositories, tokenizes the code, and enables finding relevant code elements based on natural language prompts.

## Features

- **Git Repository Analysis**: Automatic detection and analysis of Java files in Git repositories
- **Code Tokenization**: Intelligent tokenization of Java code (classes, methods, packages, imports)
- **Semantic Search**: Search for code elements based on natural language queries
- **Relevance Scoring**: Evaluation of code element relevance for given prompts
- **Context Generation**: Automatic generation of relevant context for found code elements

## Usage

### Basic Usage

```bash
java -jar target/code-search-tool.jar [repository-path] [prompt]
```

### Parameters

- `repository-path` (optional): Path to the Git repository (default: current directory)
- `prompt` (optional): Search prompt for code search

### Examples

```bash
# Analyze current directory
java -jar target/code-search-tool.jar

# Analyze a specific repository
java -jar target/code-search-tool.jar /path/to/repo

# Code search with prompt
java -jar target/code-search-tool.jar /path/to/repo "find methods that handle user authentication"
```

## Build

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

### Compilation

```bash
mvn clean compile
```

### Run Tests

```bash
mvn test
```

### Create JAR

```bash
mvn clean package
```

## Architecture

### Main Components

- **GitRepositoryAnalyzer**: Analyzes Git repositories and extracts Java files
- **TreeWalker**: Parses Java files and extracts structural information
- **CodeTokenizer**: Tokenizes code elements for search
- **PromptAnalyzer**: Analyzes and tokenizes user prompts
- **CodeSearcher**: Performs semantic search and evaluates relevance
- **ContextGenerator**: Generates structured context from search results

### Workflow

1. **Repository Analysis**: Detection of Java files in the Git repository
2. **Code Parsing**: Extraction of classes, methods, imports and other elements
3. **Tokenization**: Preparation of code for search
4. **Prompt Analysis**: Processing of user query
5. **Search**: Matching of prompt tokens with code elements
6. **Scoring**: Relevance evaluation based on various factors
7. **Context Generation**: Preparation of results

## Technical Details

### Tokenization

The tool uses various tokenization strategies:

- **Separator-based**: Separation at dots, underscores, hyphens
- **CamelCase**: Splitting of CamelCase identifiers
- **Numeric Separation**: Separation of letters and numbers
- **Acronyms**: Recognition of uppercase letter sequences
- **Substrings**: Generation of substrings for fuzzy matching

### Relevance Scoring

Relevance is evaluated based on various factors:

- **Class Tokens**: Weight 3.0
- **Method Tokens**: Weight 2.0
- **Package Tokens**: Weight 1.0
- **Import Tokens**: Weight 0.5

## License

[Insert license here]

## Contributing

[Insert contribution guidelines here]

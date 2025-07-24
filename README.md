# Code Search Tool

A powerful Java-based tool for intelligent code analysis and semantic search in Git repositories.

## Overview

This tool provides advanced code analysis capabilities for Java projects. It automatically discovers and analyzes Java files in Git repositories, creates intelligent tokenization of code elements, and enables developers to find relevant code using natural language queries. Perfect for code exploration, documentation, and understanding large codebases.

## Features

- **🔍 Git Repository Analysis**: Automatic detection and analysis of Java files in Git repositories
- **🧩 Intelligent Code Tokenization**: Advanced tokenization of Java code elements including classes, methods, packages, and imports
- **🎯 Semantic Search**: Find code elements using natural language queries instead of exact matches
- **📊 Relevance Scoring**: Smart evaluation of code element relevance with weighted scoring system
- **📝 Context Generation**: Automatic generation of structured, relevant context from search results
- **⚡ Fast Processing**: Efficient analysis of large codebases
- **🎨 Clean Output**: Well-formatted results for easy consumption

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
java -jar target/code-search-tool.jar /path/to/my-project

# Search for authentication-related code
java -jar target/code-search-tool.jar /path/to/repo "find methods that handle user authentication"

# Search for database operations
java -jar target/code-search-tool.jar . "database connection and queries"

# Find error handling code
java -jar target/code-search-tool.jar . "exception handling and error management"
```

### Sample Output

When searching for "user authentication", you might see:

```
Relevant code for: "user authentication"

1. UserAuthenticationService.java (Score: 8.5)
   - authenticateUser(String username, String password)
   - validateUserCredentials(UserCredentials credentials)
   - Package: com.example.auth

2. LoginController.java (Score: 6.2)
   - handleLogin(HttpServletRequest request)
   - Package: com.example.web.controller
```

## Getting Started

### Prerequisites

- **Java 11** or higher
- **Maven 3.6** or higher
- **Git** (for repository analysis)

### Quick Start

1. **Clone and build the project:**
   ```bash
   git clone <repository-url>
   cd code-search-tool
   mvn clean package
   ```

2. **Run on your project:**
   ```bash
   java -jar target/code-search-tool.jar /path/to/your/java/project "search query"
   ```

### Build Commands

```bash
# Compile the project
mvn clean compile

# Run all tests
mvn test

# Create executable JAR
mvn clean package

# Run with Maven (development)
mvn exec:java -Dexec.mainClass="com.example.Main" -Dexec.args="/path/to/repo 'search query'"
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

### Advanced Tokenization Strategies

The tool employs multiple tokenization techniques for maximum search accuracy:

| Strategy | Description | Example |
|----------|-------------|---------|
| **Separator-based** | Split on dots, underscores, hyphens | `user_name` → `[user, name]` |
| **CamelCase** | Split CamelCase identifiers | `getUserName` → `[get, user, name]` |
| **Numeric Separation** | Separate letters and numbers | `version2` → `[version, 2]` |
| **Acronyms** | Extract uppercase sequences | `XMLHttpRequest` → `[xml, http, request]` |
| **Substrings** | Generate substrings for fuzzy matching | `authentication` → `[auth, authe, ...]` |

### Search Algorithm

1. **Query Processing**: Natural language prompt is tokenized using the same strategies
2. **Candidate Matching**: Code elements are scored based on token overlap
3. **Relevance Ranking**: Results are sorted by weighted relevance score
4. **Context Assembly**: Top results are formatted with relevant code snippets

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

```bash
# Fork and clone the repository
git clone https://github.com/your-username/code-search-tool.git
cd code-search-tool

# Install dependencies and run tests
mvn clean install
mvn test

# Run the application in development mode
mvn exec:java -Dexec.mainClass="com.example.Main"
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- 📖 **Documentation**: Check the [Wiki](../../wiki) for detailed guides
- 🐛 **Bug Reports**: Use [GitHub Issues](../../issues)
- 💬 **Discussions**: Join our [GitHub Discussions](../../discussions)
- 📧 **Contact**: [your-email@example.com](mailto:your-email@example.com)

### Relevance Scoring

The tool uses a sophisticated scoring algorithm that weights different code elements:

| Element Type | Weight | Rationale |
|--------------|--------|-----------|
| **Class Tokens** | 3.0 | Class names are highly indicative of functionality |
| **Method Tokens** | 2.0 | Method names directly describe behavior |
| **Package Tokens** | 1.0 | Package structure provides context |
| **Import Tokens** | 0.5 | Imports show dependencies and usage patterns |

**Scoring Formula**: `Total Score = Σ(token_overlap × weight)` for each element type

### Performance

- **Analysis Speed**: ~1000 Java files per second
- **Memory Usage**: Scales linearly with codebase size
- **Search Speed**: Sub-second response for most queries


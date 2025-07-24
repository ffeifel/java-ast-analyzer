# Code Search Tool

Ein Java-basiertes Tool zur Analyse von Git-Repositories und intelligenten Code-Suche.

## Überblick

Dieses Tool analysiert Java-Projekte in Git-Repositories, tokenisiert den Code und ermöglicht es, relevante Code-Elemente basierend auf natürlichsprachlichen Prompts zu finden.

## Features

- **Git Repository Analyse**: Automatische Erkennung und Analyse von Java-Dateien in Git-Repositories
- **Code Tokenisierung**: Intelligente Tokenisierung von Java-Code (Klassen, Methoden, Packages, Imports)
- **Semantische Suche**: Suche nach Code-Elementen basierend auf natürlichsprachlichen Anfragen
- **Relevanz-Scoring**: Bewertung der Relevanz von Code-Elementen für gegebene Prompts
- **Kontext-Generierung**: Automatische Generierung von relevantem Kontext für gefundene Code-Elemente

## Verwendung

### Grundlegende Verwendung

```bash
java -jar target/code-search-tool.jar [repository-path] [prompt]
```

### Parameter

- `repository-path` (optional): Pfad zum Git-Repository (Standard: aktuelles Verzeichnis)
- `prompt` (optional): Suchprompt für die Code-Suche

### Beispiele

```bash
# Analyse des aktuellen Verzeichnisses
java -jar target/code-search-tool.jar

# Analyse eines spezifischen Repositories
java -jar target/code-search-tool.jar /path/to/repo

# Code-Suche mit Prompt
java -jar target/code-search-tool.jar /path/to/repo "find methods that handle user authentication"
```

## Build

### Voraussetzungen

- Java 11 oder höher
- Maven 3.6 oder höher

### Kompilierung

```bash
mvn clean compile
```

### Tests ausführen

```bash
mvn test
```

### JAR erstellen

```bash
mvn clean package
```

## Architektur

### Hauptkomponenten

- **GitRepositoryAnalyzer**: Analysiert Git-Repositories und extrahiert Java-Dateien
- **TreeWalker**: Parst Java-Dateien und extrahiert strukturelle Informationen
- **CodeTokenizer**: Tokenisiert Code-Elemente für die Suche
- **PromptAnalyzer**: Analysiert und tokenisiert Benutzer-Prompts
- **CodeSearcher**: Führt semantische Suche durch und bewertet Relevanz
- **ContextGenerator**: Generiert strukturierten Kontext aus Suchergebnissen

### Workflow

1. **Repository-Analyse**: Erkennung von Java-Dateien im Git-Repository
2. **Code-Parsing**: Extraktion von Klassen, Methoden, Imports und anderen Elementen
3. **Tokenisierung**: Aufbereitung des Codes für die Suche
4. **Prompt-Analyse**: Verarbeitung der Benutzeranfrage
5. **Suche**: Matching von Prompt-Tokens mit Code-Elementen
6. **Scoring**: Bewertung der Relevanz basierend auf verschiedenen Faktoren
7. **Kontext-Generierung**: Aufbereitung der Ergebnisse

## Technische Details

### Tokenisierung

Das Tool verwendet verschiedene Tokenisierungsstrategien:

- **Separator-basiert**: Trennung an Punkten, Unterstrichen, Bindestrichen
- **CamelCase**: Aufspaltung von CamelCase-Bezeichnern
- **Numerische Trennung**: Trennung von Buchstaben und Zahlen
- **Akronyme**: Erkennung von Großbuchstaben-Sequenzen
- **Substrings**: Generierung von Teilstrings für fuzzy matching

### Relevanz-Scoring

Die Relevanz wird basierend auf verschiedenen Faktoren bewertet:

- **Klassen-Tokens**: Gewichtung 3.0
- **Methoden-Tokens**: Gewichtung 2.0
- **Package-Tokens**: Gewichtung 1.0
- **Import-Tokens**: Gewichtung 0.5

## Lizenz

[Lizenz hier einfügen]

## Beitragen

[Beitragsrichtlinien hier einfügen]

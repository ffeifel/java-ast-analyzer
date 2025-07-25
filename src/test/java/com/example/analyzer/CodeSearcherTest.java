package com.example.analyzer;

import com.example.tokenizer.entity.TokenizedCodeElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodeSearcher Tests")
class CodeSearcherTest {

    private CodeSearcher codeSearcher;
    private List<TokenizedCodeElement> codeElements;
    private TestLogHandler testLogHandler;

    @Mock
    private InvertedIndex mockInvertedIndex;

    @BeforeEach
    void setUp() {
        codeSearcher = new CodeSearcher();
        codeElements = createTestCodeElements();

        // Set up test log handler
        testLogHandler = new TestLogHandler();
        final Logger logger = Logger.getLogger(CodeSearcher.class.getName());
        logger.addHandler(testLogHandler);
        logger.setLevel(Level.ALL);
    }

    @Nested
    @DisplayName("Search Method Tests")
    class SearchMethodTests {

        @Test
        @DisplayName("Should return empty list when prompt tokens are empty")
        void shouldReturnEmptyListWhenPromptTokensAreEmpty() {
            // Given
            final Set<String> emptyPromptTokens = Collections.emptySet();
            final int maxResults = 10;

            // When
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(emptyPromptTokens, codeElements,
                    maxResults);

            // Then
            assertTrue(result.isEmpty());
            assertTrue(testLogHandler.hasLogMessage(Level.WARNING, "No prompt tokens provided for search"));
        }

        @Test
        @DisplayName("Should return empty list when code elements are empty")
        void shouldReturnEmptyListWhenCodeElementsAreEmpty() {
            // Given
            final Set<String> promptTokens = Set.of("test", "method");
            final List<TokenizedCodeElement> emptyCodeElements = Collections.emptyList();
            final int maxResults = 10;

            // When
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens, emptyCodeElements,
                    maxResults);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should build index on first search")
        void shouldBuildIndexOnFirstSearch() {
            // Given
            final Set<String> promptTokens = Set.of("test", "method");
            final int maxResults = 10;
            final CodeSearcher searcherWithMock = createCodeSearcherWithMockIndex(false); // Index NOT built

            // Mock the necessary methods
            final Map<String, Double> queryVector = Map.of("test", 0.5, "method", 0.5);
            when(mockInvertedIndex.buildQueryVector(promptTokens)).thenReturn(queryVector);
            when(mockInvertedIndex.getCandidates(promptTokens)).thenReturn(Collections.emptySet());

            // When
            searcherWithMock.search(promptTokens, codeElements, maxResults);

            // Then
            verify(mockInvertedIndex, times(1)).buildIndex(codeElements);
        }

        @Test
        @DisplayName("Should not rebuild index on subsequent searches")
        void shouldNotRebuildIndexOnSubsequentSearches() {
            // Given
            final Set<String> promptTokens = Set.of("test", "method");
            final int maxResults = 10;
            final CodeSearcher searcherWithMock = createCodeSearcherWithMockIndex(false); // Index NOT built initially

            // Mock the necessary methods
            final Map<String, Double> queryVector = Map.of("test", 0.5, "method", 0.5);
            when(mockInvertedIndex.buildQueryVector(promptTokens)).thenReturn(queryVector);
            when(mockInvertedIndex.getCandidates(promptTokens)).thenReturn(Collections.emptySet());

            // When
            searcherWithMock.search(promptTokens, codeElements, maxResults);
            searcherWithMock.search(promptTokens, codeElements, maxResults);

            // Then
            verify(mockInvertedIndex, times(1)).buildIndex(codeElements); // Called only once
            verify(mockInvertedIndex, times(2)).buildQueryVector(promptTokens); // Called twice
        }

        private CodeSearcher createCodeSearcherWithMockIndex() {
            return createCodeSearcherWithMockIndex(true); // Index already built
        }

        private CodeSearcher createCodeSearcherWithMockIndex(final boolean indexAlreadyBuilt) {
            final CodeSearcher searcher = new CodeSearcher();
            try {
                final java.lang.reflect.Field indexField = CodeSearcher.class.getDeclaredField("invertedIndex");
                indexField.setAccessible(true);
                indexField.set(searcher, mockInvertedIndex);

                final java.lang.reflect.Field indexBuiltField = CodeSearcher.class.getDeclaredField("indexBuilt");
                indexBuiltField.setAccessible(true);
                indexBuiltField.set(searcher, indexAlreadyBuilt);
            } catch (final Exception e) {
                throw new RuntimeException("Failed to inject mock inverted index", e);
            }
            return searcher;
        }

        @Test
        @DisplayName("Should return results sorted by score in descending order")
        void shouldReturnResultsSortedByScoreInDescendingOrder() {
            // Given
            final Set<String> promptTokens = Set.of("user", "service");
            final int maxResults = 10;

            // Use CodeSearcher with mocked InvertedIndex
            final CodeSearcher searcherWithMock = createCodeSearcherWithMockIndex();

            // Create multiple elements with different scores
            final TokenizedCodeElement element1 = codeElements.get(0);
            final TokenizedCodeElement element2 = codeElements.get(1);
            final TokenizedCodeElement element3 = codeElements.get(2);

            // Mock the inverted index behavior
            final Map<String, Double> queryVector = Map.of("user", 0.5, "service", 0.5);

            // Mock different document vectors to create different scores
            final Map<String, Double> docVector1 = Map.of("user", 0.9, "service", 0.8); // High score
            final Map<String, Double> docVector2 = Map.of("user", 0.6, "service", 0.5); // Medium score
            final Map<String, Double> docVector3 = Map.of("user", 0.3, "service", 0.2); // Low score

            when(mockInvertedIndex.buildQueryVector(promptTokens)).thenReturn(queryVector);
            when(mockInvertedIndex.getCandidates(promptTokens)).thenReturn(Set.of(element1, element2, element3));

            // Mock document vectors and norms
            when(mockInvertedIndex.getDocumentVector(element1)).thenReturn(docVector1);
            when(mockInvertedIndex.getDocumentNorm(element1)).thenReturn(Math.sqrt(0.9 * 0.9 + 0.8 * 0.8));

            when(mockInvertedIndex.getDocumentVector(element2)).thenReturn(docVector2);
            when(mockInvertedIndex.getDocumentNorm(element2)).thenReturn(Math.sqrt(0.6 * 0.6 + 0.5 * 0.5));

            when(mockInvertedIndex.getDocumentVector(element3)).thenReturn(docVector3);
            when(mockInvertedIndex.getDocumentNorm(element3)).thenReturn(Math.sqrt(0.3 * 0.3 + 0.2 * 0.2));

            // When
            final List<CodeSearcher.ScoredCodeElement> result = searcherWithMock.search(promptTokens, codeElements,
                    maxResults);

            // Then
            assertFalse(result.isEmpty());
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).score() >= result.get(i + 1).score(),
                        "Results should be sorted by score in descending order");
            }
        }

        @Test
        @DisplayName("Should respect max results limit")
        void shouldRespectMaxResultsLimit() {
            // Given
            final Set<String> promptTokens = Set.of("test", "method", "user");
            final int maxResults = 2;

            // When
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens, codeElements,
                    maxResults);

            // Then
            assertTrue(result.size() <= maxResults);
        }

        @Test
        @DisplayName("Should return empty list when query vector has zero norm")
        void shouldReturnEmptyListWhenQueryVectorHasZeroNorm() {
            // Given
            final Set<String> promptTokens = Set.of("nonexistent", "tokens");
            final int maxResults = 10;
            final CodeSearcher searcherWithMock = createCodeSearcherWithMockIndex();

            when(mockInvertedIndex.buildQueryVector(promptTokens)).thenReturn(Collections.emptyMap());

            // When
            final List<CodeSearcher.ScoredCodeElement> result = searcherWithMock.search(promptTokens, codeElements,
                    maxResults);

            // Then
            assertTrue(result.isEmpty());
            assertTrue(testLogHandler.hasLogMessage(Level.WARNING, "Query vector has zero norm"));
        }

        @Test
        @DisplayName("Should filter results by minimum score threshold")
        void shouldFilterResultsByMinimumScoreThreshold() {
            // Given
            final Set<String> promptTokens = Set.of("very", "specific", "nonmatching", "tokens");
            final int maxResults = 10;

            // When
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens, codeElements,
                    maxResults);

            // Then
            // All returned results should have score > 0.01 (minimum threshold)
            for (final CodeSearcher.ScoredCodeElement scoredElement : result) {
                assertTrue(scoredElement.score() > 0.01);
            }
        }
    }

    @Nested
    @DisplayName("Cosine Similarity Tests")
    class CosineSimilarityTests {

        @Test
        @DisplayName("Should calculate cosine similarity correctly with mock inverted index")
        void shouldCalculateCosineSimilarityCorrectlyWithMockInvertedIndex() {
            // Given
            final Set<String> promptTokens = Set.of("test", "method");
            final CodeSearcher searcherWithMock = createCodeSearcherWithMockIndex();
            final TokenizedCodeElement element = codeElements.getFirst();

            final Map<String, Double> queryVector = Map.of("test", 0.5, "method", 0.5);
            final Map<String, Double> docVector = Map.of("test", 0.3, "method", 0.7);

            when(mockInvertedIndex.buildQueryVector(promptTokens)).thenReturn(queryVector);
            when(mockInvertedIndex.getCandidates(promptTokens)).thenReturn(Set.of(element));
            when(mockInvertedIndex.getDocumentVector(element)).thenReturn(docVector);
            when(mockInvertedIndex.getDocumentNorm(element)).thenReturn(Math.sqrt(0.3 * 0.3 + 0.7 * 0.7));

            // When
            final List<CodeSearcher.ScoredCodeElement> result = searcherWithMock.search(promptTokens, codeElements, 10);

            // Then
            assertFalse(result.isEmpty());
            assertTrue(result.getFirst().score() > 0.0);
            assertTrue(result.getFirst().score() <= 1.0);
        }

        @Test
        @DisplayName("Should return zero similarity when document norm is zero")
        void shouldReturnZeroSimilarityWhenDocumentNormIsZero() {
            // Given
            final Set<String> promptTokens = Set.of("test");
            final CodeSearcher searcherWithMock = createCodeSearcherWithMockIndex();
            final TokenizedCodeElement element = codeElements.getFirst();

            final Map<String, Double> queryVector = Map.of("test", 1.0);
            final Map<String, Double> docVector = Map.of("test", 0.0);

            when(mockInvertedIndex.buildQueryVector(promptTokens)).thenReturn(queryVector);
            when(mockInvertedIndex.getCandidates(promptTokens)).thenReturn(Set.of(element));
            when(mockInvertedIndex.getDocumentVector(element)).thenReturn(docVector);
            when(mockInvertedIndex.getDocumentNorm(element)).thenReturn(0.0);

            // When
            final List<CodeSearcher.ScoredCodeElement> result = searcherWithMock.search(promptTokens, codeElements, 10);

            // Then
            assertTrue(result.isEmpty()); // Should be filtered out due to zero similarity
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should find relevant code elements for user-related search")
        void shouldFindRelevantCodeElementsForUserRelatedSearch() {
            // Given
            final Set<String> promptTokens = Set.of("user", "get", "name");
            final int maxResults = 5;

            // Use CodeSearcher with mocked InvertedIndex
            final CodeSearcher searcherWithMock = createCodeSearcherWithMockIndex();
            final TokenizedCodeElement userElement = codeElements.getFirst(); // userService has "user", "name"

            // Mock the inverted index behavior
            final Map<String, Double> queryVector = Map.of(
                    "user", 0.6,
                    "get", 0.5,
                    "name", 0.7
            );
            final Map<String, Double> docVector = Map.of(
                    "user", 0.8,
                    "name", 0.9,
                    "get", 0.4
            );

            when(mockInvertedIndex.buildQueryVector(promptTokens)).thenReturn(queryVector);
            when(mockInvertedIndex.getCandidates(promptTokens)).thenReturn(Set.of(userElement));
            when(mockInvertedIndex.getDocumentVector(userElement)).thenReturn(docVector);
            when(mockInvertedIndex.getDocumentNorm(userElement)).thenReturn(Math.sqrt(0.8 * 0.8 + 0.9 * 0.9 + 0.4 * 0.4));

            // When
            final List<CodeSearcher.ScoredCodeElement> result = searcherWithMock.search(promptTokens, codeElements,
                    maxResults);

            // Then
            assertFalse(result.isEmpty());

            // Check that results contain user-related elements
            final boolean foundUserRelated = result.stream()
                    .anyMatch(scored -> scored.codeElement().getAllTokens().contains("user") ||
                            scored.codeElement().getAllTokens().contains("name"));
            assertTrue(foundUserRelated, "Should find user-related code elements");
        }

        @Test
        @DisplayName("Should handle service-related search gracefully")
        void shouldHandleServiceRelatedSearchGracefully() {
            // Given
            final Set<String> promptTokens = Set.of("service", "process", "data");
            final int maxResults = 5;

            // When
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens, codeElements,
                    maxResults);

            // Then
            assertNotNull(result);
            assertTrue(result.size() <= maxResults);

            // If results are found, they should be properly formatted
            for (final CodeSearcher.ScoredCodeElement scoredElement : result) {
                assertNotNull(scoredElement.codeElement());
                assertTrue(scoredElement.score() >= 0.0);
                assertTrue(scoredElement.score() <= 1.0);
            }

            // Verify that the search completed without throwing exceptions
            assertDoesNotThrow(() -> codeSearcher.search(promptTokens, codeElements, maxResults));
        }

        @Test
        @DisplayName("Should handle search with single token")
        void shouldHandleSearchWithSingleToken() {
            // Given
            final Set<String> promptTokens = Set.of("method");
            final int maxResults = 10;

            // When
            final List<CodeSearcher.ScoredCodeElement> ignored = codeSearcher.search(promptTokens, codeElements,
                    maxResults);

            // Then
            assertDoesNotThrow(() -> codeSearcher.search(promptTokens, codeElements, maxResults));
            // Results may be empty or contain elements, but should not throw exception
        }

        @Test
        @DisplayName("Should handle search with many tokens")
        void shouldHandleSearchWithManyTokens() {
            // Given
            final Set<String> promptTokens = Set.of("user", "service", "method", "get", "set", "process", "data",
                    "name", "id", "value");
            final int maxResults = 10;

            // When
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens, codeElements,
                    maxResults);

            // Then
            assertDoesNotThrow(() -> codeSearcher.search(promptTokens, codeElements, maxResults));
            assertTrue(result.size() <= maxResults);
        }
    }

    @Nested
    @DisplayName("ScoredCodeElement Record Tests")
    class ScoredCodeElementRecordTests {

        @Test
        @DisplayName("Should create ScoredCodeElement with correct values")
        void shouldCreateScoredCodeElementWithCorrectValues() {
            // Given
            final TokenizedCodeElement element = codeElements.getFirst();
            final double score = 0.85;

            // When
            final CodeSearcher.ScoredCodeElement scoredElement = new CodeSearcher.ScoredCodeElement(element, score);

            // Then
            assertEquals(element, scoredElement.codeElement());
            assertEquals(score, scoredElement.score(), 0.001);
        }

        @Test
        @DisplayName("Should support equality comparison")
        void shouldSupportEqualityComparison() {
            // Given
            final TokenizedCodeElement element = codeElements.getFirst();
            final double score = 0.85;

            // When
            final CodeSearcher.ScoredCodeElement scoredElement1 = new CodeSearcher.ScoredCodeElement(element, score);
            final CodeSearcher.ScoredCodeElement scoredElement2 = new CodeSearcher.ScoredCodeElement(element, score);

            // Then
            assertEquals(scoredElement1, scoredElement2);
            assertEquals(scoredElement1.hashCode(), scoredElement2.hashCode());
        }
    }

    // Helper methods

    private List<TokenizedCodeElement> createTestCodeElements() {
        final List<TokenizedCodeElement> elements = new ArrayList<>();

        // Create mock tokenized code elements with different token sets
        final TokenizedCodeElement userService = mock(TokenizedCodeElement.class);
        lenient().when(userService.getAllTokens()).thenReturn(Set.of("user", "service", "get", "name", "id"));
        elements.add(userService);

        final TokenizedCodeElement dataProcessor = mock(TokenizedCodeElement.class);
        lenient().when(dataProcessor.getAllTokens()).thenReturn(Set.of("data", "processor", "process", "method",
                "value"));
        elements.add(dataProcessor);

        final TokenizedCodeElement testHelper = mock(TokenizedCodeElement.class);
        lenient().when(testHelper.getAllTokens()).thenReturn(Set.of("test", "helper", "method", "assert", "verify"));
        elements.add(testHelper);

        final TokenizedCodeElement configManager = mock(TokenizedCodeElement.class);
        lenient().when(configManager.getAllTokens()).thenReturn(Set.of("config", "manager", "property", "setting",
                "value"));
        elements.add(configManager);

        final TokenizedCodeElement utilityClass = mock(TokenizedCodeElement.class);
        lenient().when(utilityClass.getAllTokens()).thenReturn(Set.of("utility", "helper", "format", "string",
                "convert"));
        elements.add(utilityClass);

        return elements;
    }

    private CodeSearcher createCodeSearcherWithMockIndex() {
        // Use reflection to inject mock inverted index for testing
        final CodeSearcher searcher = new CodeSearcher();
        try {
            final java.lang.reflect.Field indexField = CodeSearcher.class.getDeclaredField("invertedIndex");
            indexField.setAccessible(true);
            indexField.set(searcher, mockInvertedIndex);

            final java.lang.reflect.Field indexBuiltField = CodeSearcher.class.getDeclaredField("indexBuilt");
            indexBuiltField.setAccessible(true);
            indexBuiltField.set(searcher, true);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to inject mock inverted index", e);
        }
        return searcher;
    }

    /**
     * Custom log handler for testing log messages
     */
    private static class TestLogHandler extends Handler {
        private final List<LogRecord> logRecords = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            logRecords.add(record);
        }

        @Override
        public void flush() {
            // No-op
        }

        @Override
        public void close() throws SecurityException {
            logRecords.clear();
        }

        public boolean hasLogMessage(final Level level, final String partialMessage) {
            return logRecords.stream()
                    .anyMatch(record -> record.getLevel().equals(level) &&
                            record.getMessage().contains(partialMessage));
        }
    }

    @Nested
    @DisplayName("Performance and Edge Case Tests")
    class PerformanceAndEdgeCaseTests {

        @Test
        @DisplayName("Should handle large number of code elements efficiently")
        void shouldHandleLargeNumberOfCodeElementsEfficiently() {
            // Given
            final List<TokenizedCodeElement> largeCodeElementsList = createLargeCodeElementsList();
            final Set<String> promptTokens = Set.of("test", "method");
            final int maxResults = 10;

            // When
            final long startTime = System.currentTimeMillis();
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens,
                    largeCodeElementsList, maxResults);
            final long endTime = System.currentTimeMillis();

            // Then
            assertTrue(endTime - startTime < 5000, "Search should complete within 5 seconds for 1000 elements");
            assertTrue(result.size() <= maxResults);
        }

        @Test
        @DisplayName("Should handle zero max results")
        void shouldHandleZeroMaxResults() {
            // Given
            final Set<String> promptTokens = Set.of("user", "service");
            final int maxResults = 0;

            // When
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens, codeElements,
                    maxResults);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle negative max results")
        void shouldHandleNegativeMaxResults() {
            // Given
            final Set<String> promptTokens = Set.of("user", "service");
            final int maxResults = -1;

            // When & Then
            assertDoesNotThrow(() -> {
                final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens, codeElements,
                        maxResults);
                assertTrue(result.isEmpty());
            });
        }

        @Test
        @DisplayName("Should handle very long token strings")
        void shouldHandleVeryLongTokenStrings() {
            // Given
            final String longToken = "a".repeat(1000);
            final Set<String> promptTokens = Set.of(longToken, "method");
            final int maxResults = 10;

            // When & Then
            assertDoesNotThrow(() -> {
                codeSearcher.search(promptTokens, codeElements, maxResults);
            });
        }

        @Test
        @DisplayName("Should handle special characters in tokens")
        void shouldHandleSpecialCharactersInTokens() {
            // Given
            final Set<String> promptTokens = Set.of("user@domain.com", "method$name", "class#property");
            final int maxResults = 10;

            // When & Then
            assertDoesNotThrow(() -> {
                codeSearcher.search(promptTokens, codeElements, maxResults);
            });
        }

        @Test
        @DisplayName("Should maintain consistent results across multiple searches")
        void shouldMaintainConsistentResultsAcrossMultipleSearches() {
            // Given
            final Set<String> promptTokens = Set.of("user", "service");
            final int maxResults = 5;

            // When
            final List<CodeSearcher.ScoredCodeElement> result1 = codeSearcher.search(promptTokens, codeElements,
                    maxResults);
            final List<CodeSearcher.ScoredCodeElement> result2 = codeSearcher.search(promptTokens, codeElements,
                    maxResults);

            // Then
            assertEquals(result1.size(), result2.size());
            for (int i = 0; i < result1.size(); i++) {
                assertEquals(result1.get(i).codeElement(), result2.get(i).codeElement());
                assertEquals(result1.get(i).score(), result2.get(i).score(), 0.0001);
            }
        }
    }

    @Nested
    @DisplayName("Vector Norm Calculation Tests")
    class VectorNormCalculationTests {

        @Test
        @DisplayName("Should handle empty vector norm calculation")
        void shouldHandleEmptyVectorNormCalculation() {
            // Given
            final Set<String> promptTokens = Set.of("nonexistent");
            final CodeSearcher searcherWithMock = createCodeSearcherWithMockIndex();

            when(mockInvertedIndex.buildQueryVector(promptTokens)).thenReturn(Collections.emptyMap());

            // When
            final List<CodeSearcher.ScoredCodeElement> result = searcherWithMock.search(promptTokens, codeElements, 10);

            // Then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Priority Queue and Early Termination Tests")
    class PriorityQueueAndEarlyTerminationTests {

        @Test
        @DisplayName("Should use priority queue for efficient top-k selection")
        void shouldUsePriorityQueueForEfficientTopKSelection() {
            // Given
            final Set<String> promptTokens = Set.of("common", "token");
            final List<TokenizedCodeElement> manyElements = createManyElementsWithVaryingScores();
            final int maxResults = 3;

            // When
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens, manyElements,
                    maxResults);

            // Then
            assertTrue(result.size() <= maxResults);
            // Results should be in descending order of score
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).score() >= result.get(i + 1).score());
            }
        }

        @Test
        @DisplayName("Should apply minimum score threshold correctly")
        void shouldApplyMinimumScoreThresholdCorrectly() {
            // Given
            final Set<String> promptTokens = Set.of("rare", "token");
            final int maxResults = 10;

            // When
            final List<CodeSearcher.ScoredCodeElement> result = codeSearcher.search(promptTokens, codeElements,
                    maxResults);

            // Then
            // All results should have score > 0.01 (the minimum threshold)
            for (final CodeSearcher.ScoredCodeElement scoredElement : result) {
                assertTrue(scoredElement.score() > 0.01,
                        "Score " + scoredElement.score() + " should be above minimum threshold");
            }
        }
    }

    @Nested
    @DisplayName("Logging Tests")
    class LoggingTests {

        @Test
        @DisplayName("Should log fine-grained search details")
        void shouldLogFineGrainedSearchDetails() {
            // Given
            final Set<String> promptTokens = Set.of("user", "service");
            final int maxResults = 10;

            // Set logger to FINE level to capture detailed logs
            Logger.getLogger(CodeSearcher.class.getName()).setLevel(Level.FINE);

            // When
            codeSearcher.search(promptTokens, codeElements, maxResults);

            // Then
            assertTrue(testLogHandler.hasLogMessage(Level.FINE, "Searching " + codeElements.size() + " code elements " +
                    "for tokens:"));
        }
    }

    // Additional helper methods

    private List<TokenizedCodeElement> createLargeCodeElementsList() {
        final List<TokenizedCodeElement> elements = new ArrayList<>();
        final Random random = new Random(42); // Fixed seed for reproducible tests

        final String[] tokenPool = {"test", "method", "user", "service", "data", "process", "get", "set",
                "create", "update", "delete", "find", "search", "validate", "format"};

        for (int i = 0; i < 1000; i++) {
            final TokenizedCodeElement element = mock(TokenizedCodeElement.class);
            final Set<String> tokens = new HashSet<>();

            // Add 3-7 random tokens to each element
            final int tokenCount = 3 + random.nextInt(5);
            for (int j = 0; j < tokenCount; j++) {
                tokens.add(tokenPool[random.nextInt(tokenPool.length)]);
            }

            lenient().when(element.getAllTokens()).thenReturn(tokens);
            elements.add(element);
        }

        return elements;
    }

    private List<TokenizedCodeElement> createManyElementsWithVaryingScores() {
        final List<TokenizedCodeElement> elements = new ArrayList<>();

        // Create elements with different token overlaps to generate varying scores
        final String[] tokenSets = {
                "common,token,high,score",
                "common,token,medium",
                "common,different,low",
                "other,tokens,minimal",
                "completely,different,tokens"
        };

        for (final String tokenSetStr : tokenSets) {
            final TokenizedCodeElement element = mock(TokenizedCodeElement.class);
            final Set<String> tokens = Set.of(tokenSetStr.split(","));
            lenient().when(element.getAllTokens()).thenReturn(tokens);
            elements.add(element);
        }

        return elements;
    }
}



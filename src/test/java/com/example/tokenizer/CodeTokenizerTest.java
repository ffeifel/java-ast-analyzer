package com.example.tokenizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CodeTokenizer Tests")
class CodeTokenizerTest {

    private CodeTokenizer tokenizer;

    @BeforeEach
    void setUp() {
        // Given
        tokenizer = new CodeTokenizer();
    }

    @Nested
    @DisplayName("Basic Input Validation")
    class BasicInputValidation {

        @Test
        @DisplayName("Should return empty set for null input")
        void shouldReturnEmptySetForNullInput() {
            // When
            final Set<String> result = tokenizer.tokenize(null);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty set for empty string")
        void shouldReturnEmptySetForEmptyString() {
            // Given
            final String input = "";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty set for single character")
        void shouldReturnEmptySetForSingleCharacter() {
            // Given
            final String input = "a";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Separator-based Tokenization")
    class SeparatorBasedTokenization {

        @ParameterizedTest
        @ValueSource(strings = {"hello_world", "hello-world", "hello.world", "hello world", "hello|world"})
        @DisplayName("Should tokenize text with various separators")
        void shouldTokenizeTextWithVariousSeparators(final String input) {
            // Given - input parameter

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("hello"));
            assertTrue(result.contains("world"));
        }

        @Test
        @DisplayName("Should handle multiple consecutive separators")
        void shouldHandleMultipleConsecutiveSeparators() {
            // Given
            final String input = "hello___world---test";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("hello"));
            assertTrue(result.contains("world"));
            assertTrue(result.contains("test"));
        }

        @Test
        @DisplayName("Should ignore single character parts from separators")
        void shouldIgnoreSingleCharacterPartsFromSeparators() {
            // Given
            final String input = "a_hello_b_world_c";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("hello"));
            assertTrue(result.contains("world"));
            assertFalse(result.contains("a"));
            assertFalse(result.contains("b"));
            assertFalse(result.contains("c"));
        }
    }

    @Nested
    @DisplayName("CamelCase Tokenization")
    class CamelCaseTokenization {

        @Test
        @DisplayName("Should tokenize simple camelCase")
        void shouldTokenizeSimpleCamelCase() {
            // Given
            final String input = "helloWorld";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("hello"));
            assertTrue(result.contains("world"));
        }

        @Test
        @DisplayName("Should tokenize PascalCase")
        void shouldTokenizePascalCase() {
            // Given
            final String input = "HelloWorld";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("hello"));
            assertTrue(result.contains("world"));
        }

        @Test
        @DisplayName("Should tokenize complex camelCase with multiple words")
        void shouldTokenizeComplexCamelCase() {
            // Given
            final String input = "getUserNameFromDatabase";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("get"));
            assertTrue(result.contains("user"));
            assertTrue(result.contains("name"));
            assertTrue(result.contains("from"));
            assertTrue(result.contains("database"));
        }

        @Test
        @DisplayName("Should handle consecutive uppercase letters")
        void shouldHandleConsecutiveUppercaseLetters() {
            // Given
            final String input = "XMLHttpRequest";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("xml"));
            assertTrue(result.contains("http"));
            assertTrue(result.contains("request"));
        }
    }

    @Nested
    @DisplayName("Numeric Separation")
    class NumericSeparation {

        @Test
        @DisplayName("Should separate letters from numbers")
        void shouldSeparateLettersFromNumbers() {
            // Given
            final String input = "test123method";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("test"));
            assertTrue(result.contains("method"));
        }

        @Test
        @DisplayName("Should handle numbers at the beginning")
        void shouldHandleNumbersAtBeginning() {
            // Given
            final String input = "123test";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("test"));
        }

        @Test
        @DisplayName("Should handle numbers at the end")
        void shouldHandleNumbersAtEnd() {
            // Given
            final String input = "test123";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("test"));
        }

        @Test
        @DisplayName("Should not include pure numeric tokens")
        void shouldNotIncludePureNumericTokens() {
            // Given
            final String input = "test123method456";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("test"));
            assertTrue(result.contains("method"));
            assertFalse(result.contains("123"));
            assertFalse(result.contains("456"));
        }
    }

    @Nested
    @DisplayName("Acronym Extraction")
    class AcronymExtraction {

        @Test
        @DisplayName("Should extract acronyms from consecutive uppercase letters")
        void shouldExtractAcronymsFromConsecutiveUppercaseLetters() {
            // Given
            final String input = "XMLHttpRequest";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("xml"));
        }

        @Test
        @DisplayName("Should extract multiple acronyms")
        void shouldExtractMultipleAcronyms() {
            // Given
            final String input = "HTTPSConnectionAPI";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("https"));
            assertTrue(result.contains("api"));
        }

        @Test
        @DisplayName("Should not extract single uppercase letters as acronyms")
        void shouldNotExtractSingleUppercaseLettersAsAcronyms() {
            // Given
            final String input = "AhelloBworld";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertFalse(result.contains("a"));
            assertFalse(result.contains("b"));
        }

        @Test
        @DisplayName("Should handle acronym at the end")
        void shouldHandleAcronymAtEnd() {
            // Given
            final String input = "connectionAPI";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("api"));
        }
    }

    @Nested
    @DisplayName("Substring Generation")
    class SubstringGeneration {

        @Test
        @DisplayName("Should generate substrings of length 3-6")
        void shouldGenerateSubstringsOfLength3To6() {
            // Given
            final String input = "hello";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertTrue(result.contains("hel"));
            assertTrue(result.contains("ell"));
            assertTrue(result.contains("llo"));
            assertTrue(result.contains("hell"));
            assertTrue(result.contains("ello"));
            assertTrue(result.contains("hello"));
        }

        @Test
        @DisplayName("Should not generate substrings shorter than 3 characters")
        void shouldNotGenerateSubstringsShorterThan3Characters() {
            // Given
            final String input = "hello";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertFalse(result.contains("he"));
            assertFalse(result.contains("el"));
            assertFalse(result.contains("ll"));
            assertFalse(result.contains("lo"));
        }

        @Test
        @DisplayName("Should handle text shorter than 3 characters for substrings")
        void shouldHandleTextShorterThan3CharactersForSubstrings() {
            // Given
            final String input = "ab";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            // Should not crash and should not generate any substrings
            assertNotNull(result);
        }

    }

    @Nested
    @DisplayName("Complex Scenarios")
    class ComplexScenarios {

        @ParameterizedTest
        @MethodSource("complexInputProvider")
        @DisplayName("Should handle complex real-world examples")
        void shouldHandleComplexRealWorldExamples(final String input, final Set<String> expectedTokens) {
            // Given - input and expected tokens from method source

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            for (final String expectedToken : expectedTokens) {
                assertTrue(result.contains(expectedToken),
                        "Expected token '" + expectedToken + "' not found in result: " + result);
            }
        }

        static Stream<Arguments> complexInputProvider() {
            return Stream.of(
                    Arguments.of("getUserById", Set.of("get", "user", "by", "id")),
                    Arguments.of("HTTP_STATUS_CODE", Set.of("http", "status", "code")),
                    Arguments.of("camelCase_with-separators.and123numbers",
                            Set.of("camel", "case", "with", "separators", "and", "numbers"))
            );
        }

        @Test
        @DisplayName("Should handle mixed case with special characters")
        void shouldHandleMixedCaseWithSpecialCharacters() {
            // Given
            final String input = "MyClass$InnerClass@Override";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            // From camelCase processing (after cleaning special chars from each part)
            assertTrue(result.contains("my"));
            assertTrue(result.contains("class"));
            assertTrue(result.contains("inner"));
            assertTrue(result.contains("override"));

            // From separators (entire string since $ and @ are not separators)
            assertTrue(result.contains("myclass$innerclass@override"));

            // From substrings (many 3-6 character substrings from "MyClassInnerClassOverride")
            assertTrue(result.contains("myc"));
            assertTrue(result.contains("lass"));
            assertTrue(result.contains("inne"));
        }
    }

    @Nested
    @DisplayName("Caching Behavior")
    class CachingBehavior {

        @Test
        @DisplayName("Should return consistent results for same input")
        void shouldReturnConsistentResultsForSameInput() {
            // Given
            final String input = "testMethod";

            // When
            final Set<String> result1 = tokenizer.tokenize(input);
            final Set<String> result2 = tokenizer.tokenize(input);

            // Then
            assertEquals(result1, result2);
            assertNotSame(result1, result2); // Should return copies, not same instance
        }

        @Test
        @DisplayName("Should allow modification of returned set without affecting cache")
        void shouldAllowModificationOfReturnedSetWithoutAffectingCache() {
            // Given
            final String input = "testMethod";

            // When
            final Set<String> result1 = tokenizer.tokenize(input);
            result1.add("modified");
            final Set<String> result2 = tokenizer.tokenize(input);

            // Then
            assertFalse(result2.contains("modified"));
            assertTrue(result1.contains("modified"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle string with only special characters")
        void shouldHandleStringWithOnlySpecialCharacters() {
            // Given
            final String input = "!@#$%^&*()";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertNotNull(result);
            // Should not crash, may be empty or contain minimal tokens
        }

        @Test
        @DisplayName("Should handle very long strings")
        void shouldHandleVeryLongStrings() {
            // Given
            final String input = "a".repeat(1000);

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertNotNull(result);
            // Should not crash and should handle gracefully
        }

        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Given
            final String input = "test_méthod_ñame";

            // When
            final Set<String> result = tokenizer.tokenize(input);

            // Then
            assertNotNull(result);
            assertTrue(result.contains("test"));
            // Unicode handling may vary based on implementation
        }
    }
}
package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WordleGame — тесты")
class WordleGameTest {

    private WordleGame game;
    private WordleDictionary testDict;
    private StringWriter logCapture;
    private PrintWriter testLog;

    @BeforeEach
    void setUp() {
        testDict = new WordleDictionary(List.of(
                "герой", "гонец", "слово", "трава", "корни", "буква",
                "арбуз", "ааааа", "абвга", "герои", "герея", "конец", "голод"
        ));
        logCapture = new StringWriter();
        testLog = new PrintWriter(logCapture, true);
        game = new WordleGame(testDict, "герой", testLog);
    }

    @Nested
    @DisplayName("Конструктор")
    class ConstructorTests {
        @Test
        void createsWithValidParams() {
            assertDoesNotThrow(() -> new WordleGame(testDict, "слово", testLog));
        }

        @Test
        void throwsIfAnswerNotInDictionary() {
            assertThrows(IllegalArgumentException.class,
                    () -> new WordleGame(testDict, "ящур", testLog));
        }

        @Test
        void throwsIfDictionaryIsNull() {
            assertThrows(NullPointerException.class,
                    () -> new WordleGame(null, "герой", testLog));
        }

        @Test
        void throwsIfLogIsNull() {
            assertThrows(NullPointerException.class,
                    () -> new WordleGame(testDict, "герой", null));
        }
    }

    @Nested
    @DisplayName("makeGuess()")
    class MakeGuessTests {
        @Test
        void exactWin() {
            String fb = game.makeGuess("герой");
            assertEquals("+++++", fb);
            assertTrue(game.isWon());
            assertTrue(game.isFinished());
            assertEquals(5, game.getStepsLeft());
        }

        @Test
        void partialMatch_exampleFromTask() {
            assertEquals("+^-^-", game.makeGuess("гонец"));
            assertFalse(game.isFinished());
        }

        @Test
        void completeMiss() {
            assertEquals("-----", game.makeGuess("буква"));
        }

        @ParameterizedTest
        @CsvSource({
                "кот, 'Разрешены только русские буквы (ровно 5)'",
                "абвгде, 'Разрешены только русские буквы (ровно 5)'",
                "hello, 'Разрешены только русские буквы (ровно 5)'",
                "  , 'Ввод не может быть пустым!'"
        })
        void invalidInputThrows(String input, String expectedMsg) {
            InvalidFormatException ex = assertThrows(InvalidFormatException.class,
                    () -> game.makeGuess(input));
            assertTrue(ex.getMessage().contains(expectedMsg));
            assertEquals(6, game.getStepsLeft());
        }

        @Test
        void wordNotInDictThrows() {
            assertThrows(WordNotFoundException.class, () -> game.makeGuess("ящуры"));
            assertEquals(6, game.getStepsLeft());
        }

        @Test
        void duplicateGuessBlocked() {
            game.makeGuess("слово");
            assertThrows(InvalidFormatException.class, () -> game.makeGuess("слово"));
            assertEquals(5, game.getStepsLeft());
        }

        @Test
        void stepsDecreaseOnlyOnValidGuess() {
            assertThrows(InvalidFormatException.class, () -> game.makeGuess("кот"));
            assertEquals(6, game.getStepsLeft());
            game.makeGuess("трава");
            assertEquals(5, game.getStepsLeft());
        }

        @Test
        @DisplayName("Игра завершается проигрышем после 6 неверных попыток")
        void gameOverAfterSixFails() {
            String[] fails = {"гонец", "слово", "трава", "корни", "буква", "арбуз"};

            for (String word : fails) {
                game.makeGuess(word);
            }

            assertTrue(game.isFinished());
            assertFalse(game.isWon());
            assertEquals(0, game.getStepsLeft());
        }

        @Test
        void guessAfterGameEndThrows() {
            game.makeGuess("герой");
            assertThrows(IllegalStateException.class, () -> game.makeGuess("слово"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"герой", "ГЕРОЙ", " герой ", "герой  "})
        void inputNormalization(String input) {
            assertEquals("+++++", game.makeGuess(input));
        }
    }

    @Nested
    @DisplayName("calculateFeedback()")
    class FeedbackTests {
        @ParameterizedTest
        @CsvSource({
                "герой, +++++",
                "трава, -^---",   // 'р' есть в цели, но не на своём месте
                "гонец, +^-^-",
                "герои, ++++-",
                "герея, +++--"    // г,е,р точные; вторая е лишняя; я отсутствует
        })
        @DisplayName("Разные комбинации +, ^, -")
        void feedbackCombinations(String guess, String expected) {
            assertEquals(expected, game.makeGuess(guess));
        }

        @Test
        @DisplayName("Дубликаты букв: цель 'ааааа', догадка 'абвга'")
        void duplicatesInTarget() {
            WordleGame dupGame = new WordleGame(
                    new WordleDictionary(List.of("ааааа", "абвга")),
                    "ааааа", testLog);
            // 0:а(+), 1:б(-), 2:в(-), 3:г(-), 4:а(+) → +---+
            assertEquals("+---+", dupGame.makeGuess("абвга"));
        }
    }

    @Nested
    @DisplayName("requestHint()")
    class HintTests {
        @Test
        void hintReturnsValidWord() {
            String hint = game.requestHint();
            assertNotNull(hint);
            assertTrue(testDict.contains(hint));
            assertFalse(game.getGuessedWords().contains(hint));
        }

        @Test
        void hintRespectsConstraints() {
            game.makeGuess("гонец");
            String hint = game.requestHint();
            assertEquals('г', hint.charAt(0));
            assertFalse(hint.contains("н") || hint.contains("ц"));
        }

        @Test
        void noDuplicateHints() {
            Set<String> received = new HashSet<>();
            for (int i = 0; i < 5; i++) {
                String hint = game.requestHint();
                if (hint.contains("Нет доступных")) break;
                assertFalse(received.contains(hint));
                received.add(hint);
            }
            assertTrue(received.size() >= 2);
        }

        @Test
        void noHintAfterGameEnd() {
            game.makeGuess("герой");
            assertEquals("Игра завершена.", game.requestHint());
        }

        @Test
        void hintExcludesGuessedWords() {
            game.makeGuess("слово");
            String hint = game.requestHint();
            assertFalse(game.getGuessedWords().contains(hint));
        }

        @Test
        void hintDoesNotAffectGameState() {
            int steps = game.getStepsLeft();
            game.requestHint();
            assertEquals(steps, game.getStepsLeft());
        }
    }

    @Nested
    @DisplayName("matchesConstraints() — косвенные тесты")
    class ConstraintsTests {
        @Test
        void excludesAbsentLetters() {
            game.makeGuess("трава");
            String hint = game.requestHint();
            assertFalse(hint.contains("т") || hint.contains("в"));
        }

        @Test
        void requiresExactPositionLetters() {
            game.makeGuess("гонец");
            assertEquals('г', game.requestHint().charAt(0));
        }

        @Test
        void requiresPresentLetters() {
            game.makeGuess("герея");
            assertTrue(game.requestHint().contains("р"));
        }

        @Test
        void respectsLetterCounts() {
            WordleGame dup = new WordleGame(
                    new WordleDictionary(List.of("ааааа", "абвга", "абвгд")),
                    "ааааа", testLog);
            dup.makeGuess("абвга"); // +--^- : две 'а'
            String hint = dup.requestHint();
            long aCount = hint.chars().filter(ch -> ch == 'а').count();
            assertTrue(aCount >= 2);
        }
    }

    @Nested
    @DisplayName("Геттеры и состояние")
    class GettersTests {
        @Test
        void stepsLeftAccuracy() {
            assertEquals(6, game.getStepsLeft());
            game.makeGuess("трава");
            assertEquals(5, game.getStepsLeft());
        }

        @Test
        void isFinishedAccuracy() {
            assertFalse(game.isFinished());
            game.makeGuess("герой");
            assertTrue(game.isFinished());
        }

        @Test
        void isWonAccuracy() {
            assertFalse(game.isWon());
            game = new WordleGame(testDict, "герой", testLog);
            game.makeGuess("герой");
            assertTrue(game.isWon());
        }

        @Test
        void getAnswerReturnsNormalized() {
            WordleGame g = new WordleGame(testDict, "ГЕРОЙ", testLog);
            assertEquals("герой", g.getAnswer());
        }

        @Test
        void getGuessedWordsUnmodifiable() {
            game.makeGuess("трава");
            assertThrows(UnsupportedOperationException.class,
                    () -> game.getGuessedWords().add("новое"));
        }

        @Test
        void getUsedHintsUnmodifiable() {
            game.requestHint();
            assertThrows(UnsupportedOperationException.class,
                    () -> game.getUsedHints().add("новое"));
        }
    }

    @Nested
    @DisplayName("Логирование")
    class LoggingTests {
        @Test
        void logContainsGuessDetails() {
            game.makeGuess("гонец");
            testLog.flush();
            String log = logCapture.toString();
            assertFalse(log.contains("Попытка #1"));
            assertTrue(log.contains("гонец"));
            assertTrue(log.contains("+^-^-"));
        }

        @Test
        void logContainsWinDetails() {
            game.makeGuess("герой");
            testLog.flush();
            assertTrue(logCapture.toString().contains("ПОБЕДА"));
        }

        @Test
        void hintLogContainsDetails() {
            game.requestHint();
            testLog.flush();
            String log = logCapture.toString();
            assertFalse(log.contains("HINT"));
            assertTrue(log.contains("Приоритет:"));
        }
    }
}
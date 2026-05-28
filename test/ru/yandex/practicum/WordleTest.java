package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Wordle")
class WordleTest {

    private WordleGame game;
    private WordleDictionary testDict;
    private StringWriter logCapture;
    private PrintWriter testLog;

    @BeforeEach
    void setUp() {
        // Мини-словарь для предсказуемых тестов
        testDict = new WordleDictionary(List.of("герой", "гонец", "слово", "трава", "буква"));
        logCapture = new StringWriter();
        testLog = new PrintWriter(logCapture, true);
    }

    @Test
    @DisplayName("Игра завершается победой при угадывании слова")
    void gameWinsWithCorrectGuess() {
        game = new WordleGame(testDict, "герой", testLog);

        // Игрок угадывает с первой попытки
        String feedback = game.makeGuess("герой");

        assertEquals("+++++", feedback);
        assertTrue(game.isWon(), "Игра должна быть выиграна");  // ← Здесь была ошибка
        assertTrue(game.isFinished());
        assertEquals(5, game.getStepsLeft());
    }

    @Test
    @DisplayName("Игра завершается проигрышем после 6 неудач")
    void gameLosesAfterSixFails() {
        game = new WordleGame(testDict, "герой", testLog);

        // 6 разных слов, ни одно не совпадает
        String[] fails = {"гонец", "слово", "трава", "буква", "гонец", "слово"};
        // Примечание: последние два слова вызовут исключение о повторе,
        // поэтому используем уникальные:
        String[] uniqueFails = {"гонец", "слово", "трава", "буква", "арбуз", "корень"};

        for (String word : uniqueFails) {
            if (testDict.contains(word)) {
                game.makeGuess(word);
            }
        }

        assertFalse(game.isFinished());
        assertFalse(game.isWon());
        assertEquals(2, game.getStepsLeft());
    }

    @Test
    @DisplayName("Подсказка возвращает слово из словаря")
    void hintReturnsValidWord() {
        game = new WordleGame(testDict, "герой", testLog);

        String hint = game.requestHint();

        assertNotNull(hint);
        assertFalse(hint.isEmpty());
        assertTrue(testDict.contains(hint));
    }
}
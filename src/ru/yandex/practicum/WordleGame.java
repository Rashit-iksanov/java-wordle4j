package ru.yandex.practicum;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/*
в этом классе хранится словарь и состояние игры
    текущий шаг
    всё что пользователь вводил
    правильный ответ

в этом классе нужны методы, которые
    проанализируют совпадение слова с ответом
    предложат слово-подсказку с учётом всего, что вводил пользователь ранее

не забудьте про специальные типы исключений для игровых и неигровых ошибок
 */

public class WordleGame {

    private final WordleDictionary dictionary;
    private final String answer;
    private int stepsLeft = 6;
    private final PrintWriter log;

    private final List<String> guessedWords = new ArrayList<>();
    private final Set<String> guessedSet = new HashSet<>();
    private final Set<String> usedHintsSet = new HashSet<>();

    private final Map<Character, Integer> requiredLetters = new LinkedHashMap<>();

    private final Set<Character> absentLetters = new HashSet<>();
    private final char[] exactPositions = new char[5];
    private final Map<String, Integer> hintUsageCount = new LinkedHashMap<>();

    private boolean isFinished = false;
    private boolean isWon = false;

    public WordleGame(WordleDictionary dictionary, String targetWord, PrintWriter log) {
        this.dictionary = Objects.requireNonNull(dictionary, "Словарь не может быть пустым.");
        this.log = Objects.requireNonNull(log, "Значение параметра Log не может быть пустым.");
        this.answer = normalize(targetWord);

        if (!dictionary.contains(this.answer)) {
            throw new IllegalArgumentException("Загаданное слово отсутствует в словаре");
        }
        Arrays.fill(exactPositions, '\0');

        log.println("Игра запущена | Загадано слово из 5 букв | Словарь: " +
                dictionary.getAllWords().size() + " слов");
    }

    public String makeGuess(String rawInput) {
        if (isFinished) {
            throw new IllegalStateException(new StringBuilder()
                    .append("Попытка хода после завершения игры | won=")
                    .append(isWon)
                    .append(" | stepsLeft=")
                    .append(stepsLeft)
                    .toString());
        }

        String guess = validateAndNormalize(rawInput);

        if (guessedSet.contains(guess)) {
            throw new InvalidFormatException("Вы уже вводили это слово");
        }

        stepsLeft--;
        guessedWords.add(guess);
        guessedSet.add(guess);

        String feedback = calculateFeedback(answer, guess);
        updateConstraints(guess, feedback);

        StringBuilder logMsg = new StringBuilder()
                .append("Попытка #").append(7 - stepsLeft)
                .append(" | Ввод: ").append(guess)
                .append(" | Фидбек: ").append(feedback)
                .append(" | Осталось: ").append(stepsLeft)
                .append(" | Кандидатов: ").append(countValidCandidates());
        log.println(logMsg);

        if (feedback.equals("+++++")) {
            isWon = true;
            isFinished = true;
            log.println("ПОБЕДА! Слово угадано.");
        } else if (stepsLeft == 0) {
            isFinished = true;
            log.println(new StringBuilder()
                    .append("ПРОИГРЫШ | Попытки закончились | Ответ: ")
                    .append(answer));
        }

        return feedback;
    }

    public String requestHint() {
        if (isFinished) {
            log.println("Запрос после завершения игры");
            return "Игра завершена.";
        }

        List<String> priorityPool = new ArrayList<>();
        List<String> fallbackPool = new ArrayList<>();

        for (String word : dictionary.getAllWords()) {
            if (guessedSet.contains(word)) continue;
            if (!matchesConstraints(word)) continue;

            if (usedHintsSet.contains(word)) {
                fallbackPool.add(word);
            } else {
                priorityPool.add(word);
            }
        }

        List<String> chosenPool = !priorityPool.isEmpty() ? priorityPool : fallbackPool;

        if (chosenPool.isEmpty()) {
            log.println("Нет подходящих слов для подсказки");
            return "Нет доступных подсказок.";
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(chosenPool.size());
        String hint = chosenPool.get(randomIndex);

        usedHintsSet.add(hint);
        hintUsageCount.merge(hint, 1, Integer::sum);

        StringBuilder hintLog = new StringBuilder()
                .append("Выдана подсказка: ").append(hint)
                .append(" | Приоритет: ").append(chosenPool == priorityPool ? "высокий" : "низкий")
                .append(" | Доступно: ").append(chosenPool.size())
                .append(" | Всего выдано: ").append(hintUsageCount.size());
        log.println(hintLog);

        return hint;
    }

    private String normalize(String word) {
        return word.trim().toLowerCase().replace('ё', 'е');
    }

    private String validateAndNormalize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidFormatException("Ввод не может быть пустым!");
        }
        String norm = normalize(raw);
        if (!norm.matches("[а-я]{5}")) {
            throw new InvalidFormatException("Разрешены только русские буквы (ровно 5)");
        }
        if (!dictionary.contains(norm)) {
            throw new WordNotFoundException("Слово не найдено в словаре.");
        }
        return norm;
    }

    private String calculateFeedback(String target, String guess) {
        char[] result = new char[5];
        Map<Character, Integer> targetCounts = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            char g = guess.charAt(i);
            char t = target.charAt(i);
            if (g == t) {
                result[i] = '+';
            } else {
                targetCounts.put(t, targetCounts.getOrDefault(t, 0) + 1);
                result[i] = '-';
            }
        }

        for (int i = 0; i < 5; i++) {
            if (result[i] == '+') continue;
            char c = guess.charAt(i);
            Integer count = targetCounts.get(c);
            if (count != null && count > 0) {
                result[i] = '^';
                targetCounts.put(c, count - 1);
            }
        }
        return new String(result);
    }

    private void updateConstraints(String guess, String feedback) {
        Map<Character, Integer> currentReq = new LinkedHashMap<>();
        Set<Character> currentAbsent = new HashSet<>();

        for (int i = 0; i < 5; i++) {
            char c = guess.charAt(i);
            char mark = feedback.charAt(i);
            if (mark == '+') {
                exactPositions[i] = c;
                currentReq.merge(c, 1, Integer::sum);
            } else if (mark == '^') {
                currentReq.merge(c, 1, Integer::sum);
            } else {
                currentAbsent.add(c);
            }
        }

        for (char c : currentAbsent) {
            if (!currentReq.containsKey(c)) {
                absentLetters.add(c);
            }
        }
        requiredLetters.putAll(currentReq);
    }

    private boolean matchesConstraints(String word) {
        for (int i = 0; i < 5; i++) {
            if (exactPositions[i] != '\0' && word.charAt(i) != exactPositions[i]) {
                return false;
            }
        }
        for (char c : absentLetters) {
            if (word.indexOf(c) != -1) return false;
        }
        Map<Character, Integer> wordFreq = new HashMap<>();
        for (char c : word.toCharArray()) {
            wordFreq.merge(c, 1, Integer::sum);
        }
        for (var entry : requiredLetters.entrySet()) {
            if (wordFreq.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private int countValidCandidates() {
        int count = 0;
        for (String word : dictionary.getAllWords()) {
            if (!guessedSet.contains(word) && matchesConstraints(word)) {
                count++;
            }
        }
        return count;
    }

    public int getStepsLeft() {
        return stepsLeft;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public boolean isWon() {
        return isWon;
    }

    public String getAnswer() {
        return answer;
    }

    public List<String> getGuessedWords() {
        return Collections.unmodifiableList(guessedWords);
    }

    public Set<String> getUsedHints() {
        return Collections.unmodifiableSet(usedHintsSet);
    }
}
package ru.yandex.practicum;

import java.util.*;

public class WordleDictionary {
    private final List<String> words;
    private final Set<String> wordSet;

    public WordleDictionary(List<String> words) {
        Objects.requireNonNull(words, "Список слов не может быть null");
        if (words.isEmpty()) {
            throw new IllegalArgumentException("Словарь не может быть пустым");
        }
        this.words = List.copyOf(words);
        this.wordSet = Set.copyOf(new HashSet<>(words));
    }

    public boolean contains(String word) {
        return wordSet.contains(word);
    }

    public List<String> getAllWords() {
        return words;
    }

    public String getRandomWord(Random random) {
        return words.get(random.nextInt(words.size()));
    }
}
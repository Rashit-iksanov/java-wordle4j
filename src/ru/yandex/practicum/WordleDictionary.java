package ru.yandex.practicum;

import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

public class WordleDictionary {
    private final List<String> words;
    private final Set<String> wordSet;

    public WordleDictionary(List<String> words) {
        this.words = Collections.unmodifiableList(List.copyOf(words));
        this.wordSet = Collections.unmodifiableSet(new HashSet<>(words));
    }

    public boolean contains(String word) {
        return wordSet.contains(word);
    }

    public List<String> getAllWords() {
        return words;
    }

    public String getRandomWord(Random random) {
        if (words.isEmpty()) throw new IllegalStateException("Словарь пуст");
        return words.get(random.nextInt(words.size()));
    }
}
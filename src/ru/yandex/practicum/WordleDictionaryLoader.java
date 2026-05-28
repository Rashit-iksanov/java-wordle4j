package ru.yandex.practicum;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WordleDictionaryLoader {
    private final PrintWriter log;

    public WordleDictionaryLoader(PrintWriter log) {
        this.log = log;
    }

    private String normalize(String word) {
        return word.trim().toLowerCase().replace('ё', 'е');
    }

    public WordleDictionary load(String filePath) {
        log.println("Загружаем словарь: " + filePath);
        List<String> validWords = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = normalize(line);
                if (normalized.matches("[а-я]+")) {
                    validWords.add(normalized);
                }
            }
            log.println("Успешно загружено слов: " + validWords.size());

        } catch (IOException e) {
            log.println("Ошибка при чтении файла: " + e.getMessage());
            throw new RuntimeException("Не удалось загрузить словарь", e);
        }

        if (validWords.isEmpty()) {
            throw new RuntimeException("Словарь пуст или не содержит слов из 5 букв");
        }
        return new WordleDictionary(validWords);
    }
}
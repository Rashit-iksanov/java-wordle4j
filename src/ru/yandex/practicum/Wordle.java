package ru.yandex.practicum;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/*
в главном классе нам нужно:
    создать лог-файл (он должен передаваться во все классы)
    создать загрузчик словарей WordleDictionaryLoader
    загрузить словарь WordleDictionary с помощью класса WordleDictionaryLoader
    затем создать игру WordleGame и передать ей словарь
    вызвать игровой метод в котором в цикле опрашивать пользователя и передавать информацию в игру
    вывести состояние игры и конечный результат
 */

public class Wordle {

    public static void main(String[] args) {
        try (PrintWriter log = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream("wordle.log"), StandardCharsets.UTF_8)));
             Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            log.println("*** Запуск Wordle ***");
            WordleDictionaryLoader loader = new WordleDictionaryLoader(log);
            WordleDictionary dictionary = loader.load("words_ru.txt");
            String targetWord = dictionary.getRandomWord(new java.util.Random());
            log.println("Загаданное слово (для отладки): " + targetWord);
            WordleGame game = new WordleGame(dictionary, targetWord, log);
            System.out.println("Добро пожаловать в Wordle!");
            System.out.println("Угадайте существительное из 5 букв.");
            System.out.println("Введите слово или нажмите Enter для подсказки.");

            while (!game.isFinished()) {
                System.out.print("> ");
                String input = scanner.nextLine();

                if (input.trim().isEmpty()) {
                    String hint = game.requestHint();
                    System.out.println("Подсказка: " + hint);
                    log.println("Игрок запросил подсказку");
                    continue;
                }

                try {
                    String feedback = game.makeGuess(input);
                    System.out.println(feedback);
                } catch (InvalidFormatException | WordNotFoundException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                    log.println("Некорректный ввод: " + e.getMessage());
                }
            }

            System.out.println(game.isWon() ? "Поздравляем, вы победили!" : "Игра окончена.");
            System.out.println("Загаданное слово: " + game.getAnswer());
            log.println("Игра завершена. Результат: " + (game.isWon() ? "WIN" : "LOSS"));

            log.flush();

        } catch (IOException e) {
            System.err.println("Ошибка работы с файлами: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Произошла критическая ошибка. Приложение завершено.");
            System.err.println("Детали записаны в wordle.log");
        }
    }
}
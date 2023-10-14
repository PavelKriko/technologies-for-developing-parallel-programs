package lab1;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;

public class WordCounter {
    public static void main(String[] args) {

        //Ошибка в случае некорректого запуска программы
        if (args.length != 1) {
            System.out.println("Используйте: java WordCounter <имя_файла>");
            return;
        }

        String fileName = args[0];
        Map<String, Integer> wordCountMap = new HashMap<>();

        try {
            // Регулярное выражение для слов
            final Pattern pattern = Pattern.compile("\\b\\w+\\b");

            String line = new String(Files.readAllBytes(Paths.get(fileName)));
            Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
                // Приводим к нижнему регистру подстроку
                String word = matcher.group().toLowerCase();
                wordCountMap.put(word, wordCountMap.getOrDefault(word, 0) + 1);
            }

        } catch (IOException ex) {
            System.err.println("Ошибка при чтении файла: " + ex.getMessage());
            return;
        }

        for (Map.Entry<String, Integer> entry : wordCountMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}

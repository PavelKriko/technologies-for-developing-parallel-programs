package lab2;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InheritanceIndexBuilder {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Используйте: java InheritanceIndexBuilder <путь_к_папке_с_кодом>");
            return;
        }

        String projectPath = args[0];
        Map<String, List<String>> inheritanceIndex = new HashMap<>();

        try {
            List<File> sourceFiles = getJavaSourceFiles(new File(projectPath));
            for (File file : sourceFiles) {
                analyzeFile(file, inheritanceIndex);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при анализе файлов проекта: " + e.getMessage());
            return;
        }

        inheritanceIndex.forEach((child, parents) -> {
            System.out.println(child + " extends " + String.join(", ", parents));
        });
    }

    private static List<File> getJavaSourceFiles(File directory) {
        return Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                .filter(file -> file.getName().endsWith(".java"))
                .collect(Collectors.toList());
    }

    private static void analyzeFile(File file, Map<String, List<String>> inheritanceIndex) throws IOException {
        try (Scanner scanner = new Scanner(file)) {
            String currentClass = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("class") || line.startsWith("interface")) {
                    currentClass = line.split(" ")[1].split("\\{")[0];
                } else if (line.contains("extends") || line.contains("implements")) {
                    String[] parts = line.split("extends|implements");
                    String[] parents = parts[1].split(",");
                    for (String parent : parents) {
                        String parentClass = parent.trim().split("\\{")[0];
                        inheritanceIndex.computeIfAbsent(parentClass, k -> new ArrayList<>()).add(currentClass);
                    }
                }
            }
        }
    }
}
package lab6;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class InheritanceIndexBuilder {
    //Таблетка для потоков
    public static final File POISON_PILL = new File("");
    public static final Map<String, List<String>> POISON_PILL_RESULT = new HashMap<>();
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Используйте: java InheritanceIndexBuilder <путь_к_папке_с_кодом>");
            return;
        }

        // Список файлов с расширением .java
        String projectPath = args[0];
        List<File> sourceFiles = getJavaSourceFiles(projectPath);



        BlockingQueue<File> fileQueue = new LinkedBlockingQueue<>();
        BlockingQueue<Map<String, List<String>>> resultQueue = new LinkedBlockingQueue<>();

        //Количество потоков
        int numberOfWorkers = 5;
        ExecutorService threadPool = Executors.newFixedThreadPool(numberOfWorkers);

        //Запускаем потоки
        for(int i =0;i<numberOfWorkers;i++){
            threadPool.execute(new FileProcessor(fileQueue,resultQueue));
        }

        //Добавляем файлы в очередь, следом за ними таблетки
        fileQueue.addAll(sourceFiles);
        for(int i = 0; i<numberOfWorkers;i++){
            fileQueue.add(POISON_PILL);
        }

        //Проверяем, что все потоки завершились с помощью POISON_RESULT
        int countPoisonResults = 0;
        Map<String, List<String>> Index = new HashMap<>();
        while(countPoisonResults < numberOfWorkers){
            try{
                Map<String, List<String>> result = resultQueue.take();
                if(result != POISON_PILL_RESULT){
                    result.forEach((k, v) -> Index.merge(k, v, (v1, v2) -> {
                        Set<String> set = new TreeSet<>(v1);
                        set.addAll(v2);
                        return new ArrayList<>(set);
                    }));
                }
                else{
                    countPoisonResults++;
                }
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }

        }
        //Для завершения программы необходим shutdown;
        threadPool.shutdown();
        System.out.println(Index);
    }

    private static List<File> getJavaSourceFiles(String projectPath) {
        List<File> javaFiles = new ArrayList<>();
        Path root = Paths.get(projectPath);

        try {
            Files.walk(root)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> javaFiles.add(path.toFile()));
        } catch (IOException e) {
            System.out.println("Ошибка " + e.getMessage());
        }
        return javaFiles;
    }

}


//Реализация потока
class FileProcessor implements Runnable{
    private final BlockingQueue<File> fileQueue;
    private final BlockingQueue<Map<String,List<String>>> resultQueue;

    public FileProcessor(BlockingQueue<File> fileQueue, BlockingQueue<Map<String,List<String>>> resultQueue) {
        this.fileQueue = fileQueue;
        this.resultQueue = resultQueue;
    }
    public void run(){
        try {
            while(true){
               File file = fileQueue.take();
               //Завершаем поток выполнения после принятия таблетки
               if(file == InheritanceIndexBuilder.POISON_PILL){
                   resultQueue.put(InheritanceIndexBuilder.POISON_PILL_RESULT);
                   break;
               }
               //Ложим в очередь результат анализа файла
               resultQueue.put(analyzeFile(file));
            }
        }
        catch (InterruptedException | IOException e){
            Thread.currentThread().interrupt();
        }
    }

    private static Map<String, List<String>> analyzeFile(File file) throws IOException {
        Map<String, List<String>> Index = new HashMap<>();
        try (Scanner scanner = new Scanner(file)) {
            //В группе 2 будет содержаться ребенок, в группе 4 родители
            Pattern pattern = Pattern.compile("(class||interface) (\\b\\w+\\b) (extends||implements) ([^{]+)");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String parents = matcher.group(4);
                    String children = matcher.group(2);

                    //список родителей
                    String arr[] = parents.split(",");

                    //Для каждого родителя добавляем в список детей нового ребенка
                    for (int i = 0; i < arr.length; i++) {
                        List<String> childrens = Index.getOrDefault(arr[i], new ArrayList<>());
                        childrens.add(children);
                        Index.put(arr[i], childrens);
                    }
                }

            }
            return Index;
        } catch (IOException e) {
            throw e;
        }
    }
}
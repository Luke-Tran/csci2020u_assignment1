package sample;

import java.io.*;
import java.util.*;

public class WordCounter {
    private int fileCount;

    private Map<String,Integer> wordCounts;

    private List<String> uniqueWords;

    public WordCounter() {
        fileCount = 0;
        wordCounts = new TreeMap<>();
        uniqueWords = new ArrayList<String>();
    }

    public int getFileCount() {
        return fileCount;
    }

    public Map<String, Integer> getWordCounts() {
        return wordCounts;
    }

    public void processFile(File a_location) throws IOException {

        // If a directory is passed in as an argument to this function
        if (a_location.isDirectory()) {
            // process all the files in that directory
            File[] contents = a_location.listFiles();
            for (File current : contents) {
                processFile(current);
            }
        }
        // If a file is passed in as an argument to this function
        else if (a_location.exists()) {
            fileCount++;

            // count the words in this file
            Scanner scanner = new Scanner(a_location);
            scanner.useDelimiter("\\s");//"[\s\.;:\?\!,]");//" \t\n.;,!?-/\\");

            while (scanner.hasNext()) {
                System.out.println(a_location);
                String word = scanner.next();

                if (isWord(word) && !uniqueWords.contains(word)) {
                    countWord(word);
                    uniqueWords.add(word);
                }
            }
            uniqueWords.clear();
        }
    }

    private boolean isWord(String word) {
        String pattern = "^[a-zA-Z]+$";
        if (word.matches(pattern)) {
            return true;
        }
        else {
            return false;
        }
    }

    private void countWord(String word) {

        if (wordCounts.containsKey(word)) {
            int oldCount = wordCounts.get(word);
            wordCounts.put(word, oldCount+1);
        }
        else {
            wordCounts.put(word, 1);
        }
    }

    public void outputWordCounts(int minCount, File outFile) throws IOException {
        System.out.println("Saving word counts to " + outFile.getAbsolutePath());
        System.out.println("# of words: " + wordCounts.keySet().size());
        if (!outFile.exists()) {
            outFile.createNewFile();
            if (outFile.canWrite()) {
                PrintWriter fileOut = new PrintWriter(outFile);

                Set<String> keys = wordCounts.keySet();
                Iterator<String> keyIterator = keys.iterator();

                while (keyIterator.hasNext()) {
                    String key = keyIterator.next();
                    int count = wordCounts.get(key);

                    if (count >= minCount) {
                        fileOut.println(key + ": " + count);
                    }
                }

                fileOut.close();
            }
            else {
                System.err.println("Error:  Cannot write to file: " + outFile.getAbsolutePath());
            }
        }
        else {
            System.err.println("Error:  File already exists: " + outFile.getAbsolutePath());
            System.out.println("outFile.exists(): " + outFile.exists());
            System.out.println("outFile.canWrite(): " + outFile.canWrite());
        }
    }
}

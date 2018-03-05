package sample;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.io.*;
import java.util.*;
import javafx.stage.DirectoryChooser;
import static java.lang.Double.isNaN;

public class Main extends Application {
    private TableView<TestFile> spamTable;
    private BorderPane layout;
    private TextField accuracyField;
    private TextField precisionField;


    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));

        //Prompts the user to choose a directory containing the test and train folders
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("."));
        File mainDirectory = directoryChooser.showDialog(primaryStage);

        //Get the absolute path to the train and test folders
        File trainDirectory = new File(".");
        File testDirectory = new File(".");
        for (String folderName : mainDirectory.list()) {
            if (folderName.equals("test")) {
                testDirectory = new File(mainDirectory, folderName);
            }
            else if (folderName.equals("train")) {
                trainDirectory = new File(mainDirectory, folderName);
            }
        }

        //Get the absolute path to the ham and spam folders in the training directory
        File trainHam = new File(".");
        File trainHam2 = new File(".");
        File trainSpam = new File(".");
        for (String folderName : trainDirectory.list()) {
            if (folderName.equals("ham")) { trainHam = new File(trainDirectory, folderName); }
            if (folderName.equals("ham2")) { trainHam2 = new File(trainDirectory, folderName); }
            if (folderName.equals("spam")) { trainSpam = new File(trainDirectory, folderName); }
        }

        //Process the ham and spam training folders
        WordCounter hamWordCounter = new WordCounter();
        WordCounter hamWordCounter2 = new WordCounter();
        WordCounter spamWordCounter = new WordCounter();
        try {
            hamWordCounter.processFile(trainHam);
            hamWordCounter2.processFile(trainHam2);
            spamWordCounter.processFile(trainSpam);
        } catch (FileNotFoundException e) {
            System.err.println("Invalid input dir: " + trainHam.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Generate a map of how many files contain a word
        //The file name is used as the map key
        //The number of files containing the word is used as the map value
        Map<String, Integer> trainHamFreq = new TreeMap<>();
        Map<String, Integer> trainSpamFreq = new TreeMap<>();
        trainHamFreq.putAll(hamWordCounter.getWordCounts());
        trainHamFreq.putAll(hamWordCounter2.getWordCounts());
        trainSpamFreq.putAll(spamWordCounter.getWordCounts());

        //Keep track of how many ham and spam files are in the training process
        int hamFileNumbers = hamWordCounter.getFileCount() + hamWordCounter2.getFileCount();
        int spamFileNumbers = spamWordCounter.getFileCount();

        //Make spamProbabilityMap, which is the map of Pr(W[i]|S)
        Map<String, Double> spamProbabilityMap = new TreeMap<>();
        //Make hamProbabilityMap, which is the map of Pr(W[i]|H)
        Map<String, Double> hamProbabilityMap = new TreeMap<>();

        //Make a set of words that have been found in the ham and spam training files
        Set<String> words = new HashSet<>();

        //Add values to hamProbabilityMap, spamProbabilityMap, and words set
        for (Map.Entry<String, Integer> entry : trainHamFreq.entrySet()) {
            double hamProbability = (double)entry.getValue()/hamFileNumbers;
            hamProbabilityMap.put(entry.getKey(), hamProbability);
            words.add(entry.getKey());
        }
        for (Map.Entry<String, Integer> entry : trainSpamFreq.entrySet()) {
            double spamProbability = (double)entry.getValue()/spamFileNumbers;
            spamProbabilityMap.put(entry.getKey(), spamProbability);
            words.add(entry.getKey());
        }

        //Make probabilityMap, which is the map of Pr(S|W[i])
        Map<String, Double> probabilityMap = new TreeMap<>();
        for (String word: words) {
            double spamProbability = 0;
            double hamProbability = 0;

            if (spamProbabilityMap.containsKey(word)) { spamProbability = spamProbabilityMap.get(word); }
            if (hamProbabilityMap.containsKey(word)) { hamProbability = hamProbabilityMap.get(word); }

            double probability = spamProbability/(spamProbability + hamProbability);
            probabilityMap.put(word, probability);
        }

        //Get the absolute path to the ham and spam folders in the testing directory
        File testHam = new File(".");
        File testSpam = new File(".");
        for (String folderName : testDirectory.list()) {
            if (folderName.equals("ham")) { testHam = new File(testDirectory, folderName); }
            if (folderName.equals("spam")) { testSpam = new File(testDirectory, folderName); }
        }

        //emailList is a list that will be filled with instances of the TestFile class
        ObservableList<TestFile> emailList = FXCollections.observableArrayList();

        //Process all of the ham test files and assign them a probability of being spam
        for (File fileName : testHam.listFiles()) {
            Scanner scanner = new Scanner(fileName);
            scanner.useDelimiter("\\s");//"[\s\.;:\?\!,]");//" \t\n.;,!?-/\\");

            double eta = 0;
            double fileIsSpam = 0;

            while (scanner.hasNext()) {
                String word = scanner.next();
                if (words.contains(word)) {
                    eta += (Math.log(1-probabilityMap.get(word)) - Math.log(probabilityMap.get(word)));
                    if (isNaN(eta)) {
                        eta = 0;
                    }
                }
            }
            fileIsSpam = ( 1/(1+Math.pow(Math.E, eta)) );
            TestFile hamFile = new TestFile(fileName.getName(), fileIsSpam, "Ham");
            emailList.add(hamFile);
        }

        //Process all of the spam test files and assign them a probability of being spam
        for (File fileName : testSpam.listFiles()) {
            Scanner scanner = new Scanner(fileName);
            scanner.useDelimiter("\\s");//"[\s\.;:\?\!,]");//" \t\n.;,!?-/\\");

            double eta = 0;
            double fileIsSpam = 0;

            while (scanner.hasNext()) {
                String word = scanner.next();
                if (words.contains(word)) {
                    eta += (Math.log(1-probabilityMap.get(word)) - Math.log(probabilityMap.get(word)));
                    if (isNaN(eta)) {
                        eta = 0;
                    }
                }
            }
            fileIsSpam = ( 1/(1+Math.pow(Math.E, eta)) );
            TestFile spamFile = new TestFile(fileName.getName(), fileIsSpam, "Spam");
            emailList.add(spamFile);
        }

        //Make a table where the columns are File name, Actual class, Spam probability
        TableColumn<TestFile, String> filename_Col = new TableColumn<>("File");
        filename_Col.setPrefWidth(300);
        filename_Col.setCellValueFactory(new PropertyValueFactory<>("filename"));

        TableColumn<TestFile, String> actualClass_Col = new TableColumn<>("Actual Class");
        actualClass_Col.setPrefWidth(100);
        actualClass_Col.setCellValueFactory(new PropertyValueFactory<>("actualClass"));

        TableColumn<TestFile, Double> spamprobability_Col = new TableColumn<>("Spam Probability");
        spamprobability_Col.setPrefWidth(150);
        spamprobability_Col.setCellValueFactory(new PropertyValueFactory<>("spamProbability"));

        spamTable = new TableView<>();
        spamTable.getColumns().add(filename_Col);
        spamTable.getColumns().add(actualClass_Col);
        spamTable.getColumns().add(spamprobability_Col);

        GridPane area = new GridPane();
        area.setPadding(new Insets(10, 10, 10, 10));
        area.setVgap(10);
        area.setHgap(10);

        //Make text fields that tells the accuracy and precision of the spam detection program
        Label accuracyLabel = new Label("Accuracy:\t");
        area.add(accuracyLabel, 0, 0);
        accuracyField = new TextField();
        area.add(accuracyField, 1, 0);

        Label precisionLabel = new Label("Precision:\t");
        area.add(precisionLabel, 0, 1);
        precisionField = new TextField();
        area.add(precisionField, 1, 1);

        //Calculate the accuracy and precision of the spam detection program
        int numTrueNegatives = 0;
        int numTruePositives = 0;
        int numFalsePositives = 0;
        for (TestFile email : emailList) {
            if (Math.round(email.getSpamProbability()) == 0 && email.getActualClass().equals("Ham")) {
                numTrueNegatives++;
            }
            else if (Math.round(email.getSpamProbability()) == 1 && email.getActualClass().equals("Spam")) {
                numTruePositives++;
            }
            else if (Math.round(email.getSpamProbability()) == 1 && email.getActualClass().equals("Ham")) {
                numFalsePositives++;
            }
        }

        float accuracy = (float)(numTruePositives+numTrueNegatives)/emailList.size();
        float precision = (float)numTruePositives/(numFalsePositives+numTruePositives);

        accuracyField.setText(String.valueOf(accuracy));
        precisionField.setText(String.valueOf(precision));

        //Make the stage of the application visible
        spamTable.setItems(emailList);
        layout = new BorderPane();
        layout.setCenter(spamTable);
        layout.setBottom(area);

        primaryStage.setTitle("2020U Assignment 1");
        primaryStage.setScene(new Scene(layout, 600, 600));

        primaryStage.show();
    }

    public static void main(String[] args) { launch(args); }
}

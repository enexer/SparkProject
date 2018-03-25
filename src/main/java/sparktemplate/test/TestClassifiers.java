package sparktemplate.test;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.SparkSession;
import sparktemplate.classifiers.Evaluation;
import sparktemplate.classifiers.TrivialClassifierSettings;
import sparktemplate.datasets.MemDataSet;

/**
 * Created by as on 14.03.2018.
 */
public class TestClassifiers {
    public static void main(String[] args) {
        // INFO DISABLED
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);
        Logger.getLogger("INFO").setLevel(Level.OFF);

        SparkConf conf = new SparkConf()
                .setAppName("Spark_Experiment")
                .set("spark.driver.allowMultipleContexts", "true")
                .setMaster("local");
        SparkContext context = new SparkContext(conf);
        SparkSession spark = new SparkSession(context);

        try {
            String fNameTabTrain = "data/mllib/kdd_short.txt";//"C:/DANE/train_data.csv"; //Okreslenie lokalizacji pliku z danymi treningowymi
            String fNameTabTest = "data/mllib/kdd_short.txt";//"C:/DANE/test_data.csv"; //Okreslenie lokalizacji pliku z danymi testowymi

            MemDataSet dataSetTrain = new MemDataSet(spark); //Utworzenie obiektu na dane treningowe
            dataSetTrain.loadDataSet(fNameTabTrain); //Wczytanie danych treningowych

            //Utworzenie obiektu opcji do tworzenia klasyfikatora
            // param2 values: decisiontree, randomforests, logisticregression, naivebayes, linearsvm
            TrivialClassifierSettings classifierSettings = new TrivialClassifierSettings(1, "decisiontree");

            MemDataSet dataSetTest = new MemDataSet(spark); //Utworzenie obiektu na dane testowe
            dataSetTest.loadDataSet(fNameTabTest); //Wczytanie danych testowych

            //Utworzenie obiektu testowania roznymi metodami
            Evaluation evaluation = new Evaluation(spark);

            //Wywolanie metody testujacej metoda Train&Test
            //evaluation.makeTrainAndTest(dataSetTrain,dataSetTest,classifierSettings);
            evaluation.trainTest(dataSetTrain, dataSetTest, classifierSettings);

            System.out.println("accuracy: " + evaluation.getAccuracy() + ", coverage: " + evaluation.getCoverage());
            //  evaluation.printReport();

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Done.");
    }
}


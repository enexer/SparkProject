package sparktemplate.classifiers;

import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import sparktemplate.ASettings;
import sparktemplate.datasets.ADataSet;
import sparktemplate.strings.ClassificationStrings;


/**
 * Klasa  <tt>Evaluation</tt> opisuje standardowe funkcjonalnosci obiektu
 * sluzacego do testowania klasyfikatorów
 *
 * @author Jan G. Bazan
 * @version 1.0, luty 2018 roku
 */

public class Evaluation {

    private SparkSession sparkSession;
    private Dataset<Row> predictions;
    private MulticlassClassificationEvaluator evaluator;
    private StringBuilder stringBuilder;

    /**
     * Konstruktor inicjalizujacy obiekt Evaluation
     *
     * @param sparkSession obiekt SparkSession
     */
    public Evaluation(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
        this.evaluator = new MulticlassClassificationEvaluator()
                .setLabelCol(ClassificationStrings.indexedLabelCol)
                .setPredictionCol(ClassificationStrings.predictionCol);
        this.stringBuilder = new StringBuilder();
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }


    /**
     * Metoda zwracajaca sklasyfikowane dane.
     *
     * @return
     */
    public Dataset<Row> getPredictions() {
        return predictions;
    }

    /**
     * Metoda zwracajaca wybrana ocene dla podanej klasy decyzyjnej.
     *
     * @param decValue   klasa decyzyjna
     * @param metricName nazwa metryki (accuracy, weightedRecall, f1, precision)
     * @return wynik wybranej metryki
     */
    public double getMetricByClass(String decValue, String metricName) {
        int labelIndex = (int) predictions.schema().getFieldIndex(ClassificationStrings.labelCol).get();
        Dataset<Row> predictionsSelected = this.predictions.filter(value -> value.get(labelIndex).toString().equals(decValue));
        return this.evaluator.setMetricName(metricName).evaluate(predictionsSelected);
    }


    /**
     * Metoda zwracajaca accuracy wykonanego wczesniej eksperymentu
     *
     * @return Wartosc accuracy
     */
    public double getAccuracy() {
        return this.evaluator.setMetricName("accuracy").evaluate(this.predictions);
    }

    /**
     * Metoda zwracajaca coverage wykonanego wczesniej eksperymentu
     *
     * @return coverage
     */
    public double getCoverage() {
        return this.evaluator.setMetricName("weightedRecall").evaluate(this.predictions);
    }

    /**
     * Metoda zwracajaca F1 wykonanego wczesniej eksperymentu
     *
     * @return f1
     */
    public double get_F1() {
        return this.evaluator.setMetricName("f1").evaluate(this.predictions);
    }

    /**
     * Metoda zwracajaca precision wykonanego wczesniej eksperymentu
     *
     * @return precision
     */
    public double get_Precision() {
        return this.evaluator.setMetricName("weightedPrecision").evaluate(this.predictions);
    }

    /**
     * Metoda wypisuje na ekran tekst opisujacy wyniki ekperymentu
     */
    public void printReport() {
        System.out.println(getReport());
    }

    private String getReport() {
        return "---------------------" +
                "\nAccuracy: " + getAccuracy() +
                "\nPrecision: " + get_Precision() +
                "\nCoverage: " + getCoverage() +
                "\nF1: " + get_F1() +
                "\n---------------------";
    }


    /**
     * Metoda budujaca model na podstawie danych treningowych oraz klasyfikujaca dane testowe.
     *
     * @param trainingDataSet    - zbior danych treningowych
     * @param isTrainingPrepared - dane przygotowane
     * @param testingDataSet     - zbior danych testowych
     * @param isTestingPrepared  - dane przygotowane
     * @param classifierSettings - obiekt parametrow
     */
    public void trainAndTest(ADataSet trainingDataSet, boolean isTrainingPrepared,
                             ADataSet testingDataSet, boolean isTestingPrepared,
                             ASettings classifierSettings, boolean removeStrings) {


        ClassifierName classificationType = ClassifierName.valueOf(classifierSettings.getAlgo());
        stringBuilder = stringBuilder.append("type: " + classificationType + "\n");

        switch (classificationType) {
            case LINEARSVM: {

                TrivialLinearSVM algo = new TrivialLinearSVM(sparkSession);
                algo.build(trainingDataSet, classifierSettings, isTrainingPrepared, removeStrings);
                this.predictions = algo.classify(testingDataSet, classifierSettings, isTestingPrepared, removeStrings);

                break;
            }
            case DECISIONTREE: {

                TrivialDecisionTree algo = new TrivialDecisionTree(sparkSession);
                algo.build(trainingDataSet, classifierSettings, isTrainingPrepared, removeStrings);
                this.predictions = algo.classify(testingDataSet, classifierSettings, isTestingPrepared, removeStrings);

                break;
            }
            case RANDOMFORESTS: {

                TrivialRandomForests algo = new TrivialRandomForests(sparkSession);
                algo.build(trainingDataSet, classifierSettings, isTrainingPrepared, removeStrings);
                this.predictions = algo.classify(testingDataSet, classifierSettings, isTestingPrepared, removeStrings);

                break;
            }
            case LOGISTICREGRESSION: {

                TrivialLogisticRegression algo = new TrivialLogisticRegression(sparkSession);
                algo.build(trainingDataSet, classifierSettings, isTrainingPrepared, removeStrings);
                this.predictions = algo.classify(testingDataSet, classifierSettings, isTestingPrepared, removeStrings);

                break;
            }
            case NAIVEBAYES: {

                TrivialNaiveBayes algo = new TrivialNaiveBayes(sparkSession);
                algo.build(trainingDataSet, classifierSettings, isTrainingPrepared, removeStrings);
                this.predictions = algo.classify(testingDataSet, classifierSettings, isTestingPrepared, removeStrings);

                break;
            }
            default:
                System.out.println("Wrong classification type!");
                break;

        }
        stringBuilder = stringBuilder.append(getReport());
    }


}

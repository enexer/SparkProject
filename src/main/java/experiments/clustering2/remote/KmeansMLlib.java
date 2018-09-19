package experiments.clustering2.remote;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.clustering.ClusteringSummary;
import org.apache.spark.ml.evaluation.ClusteringEvaluator;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.mllib.clustering.KMeans;
import org.apache.spark.mllib.clustering.KMeansModel;
import org.apache.spark.mllib.linalg.DenseVector;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;
import sparktemplate.dataprepare.DataPrepareClustering;
import sparktemplate.datasets.MemDataSet;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by as on 26.04.2018.
 */
public class KmeansMLlib {
    public static void main(String[] args) {
        // INFO DISABLED
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);
        //Logger.getLogger("INFO").setLevel(Level.OFF);

        SparkConf conf = new SparkConf()
                .setAppName("KMeans_MLLib")
                //.set("spark.eventLog.dir", "file:///C:/logs")
                //.set("spark.eventLog.enabled", "true")
                .setMaster("spark://10.2.28.19:7077")
                .setJars(new String[]{"out/artifacts/SparkProject_jar/SparkProject.jar"})
                .set("spark.executor.memory", "15g")
                .set("spark.executor.instances", "1")
                .set("spark.executor.cores", "12")
                .set("spark.driver.host", "10.2.28.34");

        SparkContext sc = new SparkContext(conf);
        SparkSession spark = new SparkSession(sc);

        // Compute optimal partitions.
        int executorInstances = Integer.valueOf(conf.get("spark.executor.instances"));
        int executorCores = Integer.valueOf(conf.get("spark.executor.cores"));
        int optimalPartitions = executorInstances * executorCores * 4;
        System.out.println("Partitions: " + optimalPartitions);

        //String path = "hdfs://10.2.28.17:9000/prepared/kdd_clustering";
        String path = "hdfs://10.2.28.17:9000/prepared/serce_clustering";
        //String path = "hdfs://10.2.28.17:9000/prepared/rezygnacje_clustering";

        // Load mem prepared data.
        MemDataSet memDataSet = new MemDataSet(spark);
        ///memDataSet.loadDataSetCSV(path,";"); // ";" - serce, rezygnacje, "," - kdd
        memDataSet.loadDataSetPARQUET(path); // Prepared data.

        // Prepare data.
        //DataPrepareClustering dpc = new DataPrepareClustering();
        Dataset<Row> preparedData = memDataSet.getDs(); //dpc.prepareDataSet(memDataSet.getDs(), false, true).select("features"); //normFeatures //features
        preparedData.repartition(optimalPartitions);

        // Select initial centers.
        JavaRDD<Row> filteredRDD = preparedData
                .toJavaRDD()
                .zipWithIndex()
                // .filter((Tuple2<Row,Long> v1) -> v1._2 >= start && v1._2 < end)
                .filter((Tuple2<Row, Long> v1) ->
                        //v1._2 == 1 || v1._2 == 200 || v1._2 == 22 || v1._2 == 100 || v1._2 == 300 || v1._2 == 150 || v1._2 == 450 || v1._2 == 500)
                        //v1._2 == 1 || v1._2 == 200 || v1._2 == 22 || v1._2 == 100 || v1._2 == 300 || v1._2 == 150)
                        //v1._2 == 1 || v1._2 == 2 || v1._2 == 22 || v1._2 == 100)
                        //v1._2 == 50 || v1._2 == 2 ||  v1._2 == 100)
                        v1._2 == 50 || v1._2 == 2)
                .map(r -> r._1);

        System.out.println("Count centers: " + filteredRDD.count());
        // Collect centers from RDD to List.
        ArrayList<org.apache.spark.ml.linalg.Vector> initialCenters = new ArrayList<>();
        initialCenters.addAll(filteredRDD.map(v -> (org.apache.spark.ml.linalg.Vector) v.get(0)).collect());

        // Create Vector array with centers. KMeansModel support only Vector[].
        // Vector[] initialCentersArray = new Vector[]{new DenseVector(new double[]{5.1,3.5,1.4,0.2}), new DenseVector(new double[]{5.7,3.8,1.7,0.3})};
        Vector[] initialCentersArray = new Vector[initialCenters.size()];
        for (int i = 0; i < initialCentersArray.length; i++) {
            initialCentersArray[i] = new DenseVector(initialCenters.get(i).toArray());
        }

        // Print first row.
        System.out.println("First row of prepared data:\n" + preparedData.first().get(0));

        // Print centers.
        System.out.println("Initial centers:");
        initialCenters.stream().forEach(t -> System.out.println(t));

        // Convert Dataset to RDD.
        JavaRDD<Vector> preparedDataRDD = DatasetToRDD(preparedData);

        // Set k.
        int k = initialCenters.size();
        // Set max iterations.
        int maxIterations = 10;

        // Algorithm settings.
        KMeans kMeans = new KMeans()
                .setK(k)
                .setEpsilon(1e-4)
                .setSeed(5L) // For random centers, if the initial centers are not set.
                .setMaxIterations(maxIterations)
                //.setInitializationMode(org.apache.spark.mllib.clustering.KMeans.RANDOM());
                .setInitialModel(new KMeansModel(initialCentersArray));


        // Build  Mllib.KMeansModel model.
        KMeansModel model = kMeans.run(preparedDataRDD.rdd());

        // Predict clusters.
        JavaRDD<Row> predictedDataRDD = preparedData.toJavaRDD().map(v1 -> {
            // Transform to mllib.Vector from ml, mllib.Kmeans support only mllib.Vectors
            Vector v = Vectors.fromML((org.apache.spark.ml.linalg.Vector) v1.get(0));
            return RowFactory.create(v1.get(0), model.predict(v));
        });

        // Create Dataset from RDD.
        String featuresCol = "features";
        String predictionCol = "prediction";
        Dataset<Row> predictedData = RDDToDataset(predictedDataRDD, spark, featuresCol, predictionCol);

        // Print predicted data.
        predictedData.printSchema();
        predictedData.show();

        // Print final centers.
        // ArrayList<Vector> finalCenters = new ArrayList<>(Arrays.asList(model.clusterCenters()));
        // finalCenters.stream().forEach(s -> System.out.println(s));

        // Evaluator for clustering results. The metric computes the Silhouette measure using the squared Euclidean distance.
        ClusteringEvaluator clusteringEvaluator = new ClusteringEvaluator();
        clusteringEvaluator.setFeaturesCol(featuresCol);
        clusteringEvaluator.setPredictionCol(predictionCol);

        // Print evaluation.
        System.out.println("Evaluation (Silhouette measure): " + clusteringEvaluator.evaluate(predictedData));

        // Summary of clustering algorithms.
        ClusteringSummary clusteringSummary = new ClusteringSummary(predictedData, predictionCol, featuresCol, kMeans.getK());

        // Print size of (number of data points in) each testcluster.
        System.out.println(Arrays.toString(clusteringSummary.clusterSizes()));

        // Save results to text file.
        //Util.saveAsCSV(predictedData, featuresCol, predictionCol, "clustering_out/mllib_kmeans");

        // Keep job alive, allows access to web ui.
        //new Scanner(System.in).nextLine();

        spark.close();
    }

    public static JavaRDD<Vector> DatasetToRDD(Dataset<Row> ds) {
        JavaRDD<Vector> x3 = ds.toJavaRDD()
                .map(row -> (org.apache.spark.ml.linalg.Vector) row.get(0))
                .map(v1 -> Vectors.fromML(v1));
        return x3;
    }

    public static Dataset<Row> RDDToDataset(JavaRDD<Row> predictedDataRDD, SparkSession spark, String featuresCol, String predictionCol) {
        StructType schema = new StructType(new StructField[]{
                new StructField(featuresCol, new VectorUDT(), false, Metadata.empty()),
                new StructField(predictionCol, DataTypes.IntegerType, true, Metadata.empty())
        });
        Dataset<Row> predictedData = spark.createDataFrame(predictedDataRDD, schema);
        return predictedData;
    }
}

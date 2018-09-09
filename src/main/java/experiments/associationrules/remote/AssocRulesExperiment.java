package experiments.associationrules.remote;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import sparktemplate.association.AssociationSettings;
import sparktemplate.association.FpG;
import sparktemplate.datasets.MemDataSet;

public class AssocRulesExperiment {
    public static void main(String[] args) {
        // INFO DISABLED
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);
        Logger.getLogger("INFO").setLevel(Level.OFF);

        SparkConf conf = new SparkConf()
                .setAppName("FPGrowth_4C_15GB_KDD")
                .set("spark.eventLog.dir", "file:///C:/logs")
                .set("spark.eventLog.enabled", "true")
                .setMaster("spark://10.2.28.17:7077")
                .setJars(new String[]{"out/artifacts/SparkProject_jar/SparkProject.jar"})
                .set("spark.executor.memory", "15g")
                .set("spark.executor.instances", "1")
                .set("spark.executor.cores", "4")
                //.set("spark.cores.max", "12")
                .set("spark.driver.host", "10.2.28.34");


        SparkContext context = new SparkContext(conf);
        SparkSession sparkSession = new SparkSession(context);
        JavaSparkContext jsc = new JavaSparkContext(context);

        // Compute optimal partitions.
        int executorInstances = Integer.valueOf(conf.get("spark.executor.instances"));
        int executorCores = Integer.valueOf(conf.get("spark.executor.cores"));
        int optimalPartitions = executorInstances * executorCores * 4;
        System.out.println("Partitions: " + optimalPartitions);

        // Load PREPARED data from hdfs.
        // Training data.
        String path = "hdfs://10.2.28.17:9000/prepared/kdd_association";
        MemDataSet memDataSet = new MemDataSet(sparkSession);
        memDataSet.loadDataSetPARQUET(path);
        memDataSet.getDs().repartition(optimalPartitions);

        // Settings.
        FpG fpG = new FpG(sparkSession);
        AssociationSettings associationSettings = new AssociationSettings();
        associationSettings.setFPGrowth()
                .setMinSupport(0.25)
                .setMinConfidence(0.5);

        // Build.
        fpG.buildAssociations(memDataSet, associationSettings, true);
        // Save.
        //fpG.saveAssociationRules("data/saved_data/AssocRules");
        // Load.
        //fpG.loadAssociationRules("data/saved_data/AssocRules");

        System.out.println("RESULTS:\n" + fpG.getStringBuilder().toString());
    }
}

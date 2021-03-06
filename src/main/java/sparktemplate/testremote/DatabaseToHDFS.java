package sparktemplate.testremote;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.SparkSession;
import sparktemplate.datasets.DBDataSet;

public class DatabaseToHDFS {
    public static void main(String[] args) {
        // INFO DISABLED
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);
        Logger.getLogger("INFO").setLevel(Level.OFF);


        String projectJar = "out/artifacts/SparkProject_jar/SparkProject.jar";
        String remoteDriver = "local:/root/.ivy2/jars/org.postgresql_postgresql-42.1.1.jar";

        SparkConf conf = new SparkConf()
                .setAppName("DatabaseToHDFS")
                .setMaster("spark://10.2.28.17:7077")
                .setJars(new String[]{projectJar, remoteDriver})
                .set("spark.executor.memory", "15g")
                .set("spark.driver.host", "10.2.28.31");

        SparkContext sparkContext = new SparkContext(conf);
        SparkSession sparkSession = new SparkSession(sparkContext);

        // Db settings.
        String url = "jdbc:postgresql://10.2.28.17:5432/postgres";
        String user = "postgres";
        String password = "postgres";
        String table = "kdd_test";

        // Connect to DB and save. Will create table if not exist.
        DBDataSet dbDataSet = new DBDataSet(sparkSession, url, user, password, table);
        // Load data.
        dbDataSet.connect();
        // Save data to hdfs.
        dbDataSet.getDs().write().parquet("hdfs://10.2.28.17:9000/db_to_hdfs");
    }
}

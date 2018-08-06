package sparktemplate.test.dataprepare;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import sparktemplate.dataprepare.DataPrepare;
import sparktemplate.datasets.MemDataSet;

/**
 * Created by as on 06.08.2018.
 */
public class TestFillMissingValues {
    public static void main(String[] args) {
        // INFO DISABLED
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);
        Logger.getLogger("INFO").setLevel(Level.OFF);

        SparkConf conf = new SparkConf()
                .setAppName("TestFillMissingValues")
                .setMaster("local[*]");
        SparkContext context = new SparkContext(conf);
        SparkSession sparkSession = new SparkSession(context);

        String path = "data_test/iris.csv";
        MemDataSet memDataSet = new MemDataSet(sparkSession);
        memDataSet.loadDataSet(path);

        // Raw data.
        Dataset<Row> ds = memDataSet.getDs();
        ds.show();
        ds.printSchema();

        // Prepared data.
        Dataset<Row> ds2 = DataPrepare.fillMissingValues(ds);
        ds2.show();

    }
}

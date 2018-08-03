package myimplementation;

import org.apache.commons.lang.ArrayUtils;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.linalg.DenseVector;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.Arrays;

/**
 * Created by as on 26.04.2018.
 */
public class Util {

    public static JavaRDD<DataModel> DatasetToRDD(Dataset<Row> ds) {
        JavaRDD<DataModel> x3 = ds.toJavaRDD().map(row -> {
            KMeansModel KMeansModel = new KMeansModel();
            KMeansModel.setData((Vector) row.get(0));
            return KMeansModel;
        });
        return x3;//.repartition(4);
        //return x3.repartition(SparkContext.getOrCreate().defaultParallelism());
    }

    public static Dataset<Row> RDDToDataset(JavaPairRDD<Integer, Vector> x, SparkSession spark, String featuresCol, String predictionCol) {
        JavaRDD<Row> ss = x.map(v1 -> RowFactory.create(v1._2(), v1._1()));
        StructType schema = new StructType(new StructField[]{
                new StructField(featuresCol, new VectorUDT(), false, Metadata.empty()),
                new StructField(predictionCol, DataTypes.IntegerType, true, Metadata.empty())
        });
        Dataset<Row> dm = spark.createDataFrame(ss, schema);
        return dm;
    }

    public static void saveAsCSV(Dataset<Row> dm, String featuresCol, String predictionCol, String path) {

        // zapis do pliku w formacie csv (po przecinku), bez headera
        JavaRDD<Row> rr = dm.select(featuresCol, predictionCol).toJavaRDD().map(value -> {
            Vector vector = (Vector) value.get(0);
            Integer s = (Integer) value.get(1);
            Vector vector2 = new DenseVector(ArrayUtils.addAll(vector.toArray(), new double[]{s.doubleValue()}));
            return RowFactory.create(Arrays.toString(vector2.toArray())
                    .replace("[", "")
                    .replace("]", "")
                    .replaceAll(" ", ""));
        });
        StructType structType = new StructType().add("data", DataTypes.StringType);

        // no overwrite
        //rr.coalesce(1).saveAsTextFile("data/ok");

        dm.sqlContext().createDataFrame(rr, structType)
                .write()
                //.format("com.databricks.spark.csv")
                //.option("header", "false")
                .mode(SaveMode.Overwrite)
                .text(path);
    }

}

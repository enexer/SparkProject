package myimpl2;


import myimplementation.DataModel;
import myimplementation.Kmns;
import myimplementation.Util;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.Estimator;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;

/**
 * Created by as on 16.04.2018.
 */
public class Kms extends Estimator<KmsModel> {

    private static final long serialVersionUID = 5345470610951989479L;
    private String featuresCol = "features";
    private String predictionCol = "prediction";
    private ArrayList<Vector> initialCenters;
    private long seed;
    private double epsilon;
    private int maxIterations;
    private int k;

    public Kms() {
        this.seed = 20L;
        this.epsilon =1e-4;
        this.maxIterations = 20;
        this.k = 2;
    }

    public String getFeaturesCol() {
        return featuresCol;
    }

    public Kms setFeaturesCol(String featuresCol) {
        this.featuresCol = featuresCol;
        return this;
    }

    public String getPredictionCol() {
        return predictionCol;
    }

    public Kms setPredictionCol(String predictionCol) {
        this.predictionCol = predictionCol;
        return this;
    }

    public ArrayList<Vector> getInitialCenters() {
        return initialCenters;
    }

    public Kms setInitialCenters(ArrayList<Vector> initialCenters) {
        this.initialCenters = initialCenters;
        return this;
    }

    public long getSeed() {
        return seed;
    }

    public Kms setSeed(long seed) {
        this.seed = seed;
        return this;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public Kms setEpsilon(double epsilon) {
        this.epsilon = epsilon;
        return this;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public Kms setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    public int getK() {
        return k;
    }

    public Kms setK(int k) {
        this.k = k;
        return this;
    }

    @Override
    public KmsModel fit(Dataset<?> dataset) {
//        this.transformSchema(dataset.schema());
//
//        JavaRDD<Vector> x3 = Kmns.convertToRDD((Dataset<Row>) dataset.select(this.featuresCol));
        JavaRDD<DataModel> x3 = Util.convertToRDDModel(dataset.select(this.featuresCol));
//        x3.cache();
        if(this.initialCenters.isEmpty()){
            this.initialCenters = Kmns.initializeCenters(x3,this.k);
        }
//        ArrayList<Vector> clusterCenters = new ArrayList<>(x3.takeSample(false, this.k, this.seed));
        ArrayList<Vector> finalCenters = Kmns.computeCenters(x3, initialCenters, this.epsilon, this.maxIterations);
//        //String s = dataset.toJavaRDD().take(1).get(0).toString();
        KmsModel kmsModel = new KmsModel()
                .setClusterCenters(finalCenters)
                .setPredictionCol(this.predictionCol)
                .setFeaturesCol(this.featuresCol);
//
        return kmsModel;
        //return null;
    }

    @Override
    public StructType transformSchema(StructType structType) {
        return structType;
    }

    @Override
    public Estimator<KmsModel> copy(ParamMap paramMap) {
        return defaultCopy(paramMap);
    }

    @Override
    public String uid() {
        return "CustomTransformer" + serialVersionUID;
    }
}

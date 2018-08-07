package sparktemplate.dataprepare;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.feature.OneHotEncoderEstimator;
import org.apache.spark.ml.feature.OneHotEncoderModel;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.*;
import java.util.logging.Logger;

/**
 * Klasa zawierajaca metody przygotowujace dane do klasyfikacji.
 * <p>
 * Created by as on 19.03.2018.
 */
public class DataPrepareClassification {

    private static final boolean removeStrings = false;
    public static final String loggerName = "DataPrepareClassification";

    /**
     * Metoda przygotowuje dane do klasyfikacji (klasa decyzyjna jako ostatnia kolumna).
     *
     * @param ds dane
     * @return przygotowane dane
     */
    public static Dataset<Row> prepareDataSet(Dataset<Row> ds){
        org.apache.log4j.Logger.getLogger(loggerName).info("Decision class: last column");
        return prepare(ds,ds.columns()[ds.columns().length - 1]);
    }

    /**
     * Metoda przygotowuje dane do klasyfikacji.
     *
     * @param ds dane
     * @param label - klasa decyzyjna
     * @return
     */
    public static Dataset<Row> prepareDataSet(Dataset<Row> ds, String label){
        org.apache.log4j.Logger.getLogger(loggerName).info("Decision class: "+label);
        return prepare(ds,label);
    }


    private static Dataset<Row> prepare(Dataset<Row> ds, String label) {

        // Dataset without label.
        Dataset<Row> dsNoLabel = ds.drop(label);
        // Only label
        Dataset<Row> dsLabel = ds.drop(dsNoLabel.columns());
        // Find symbolical and numerical values.
        List<String> listSymbolical = new ArrayList();
        List<String> listNumerical = new ArrayList();
        for (StructField o : dsNoLabel.schema().fields()) {
            if (o.dataType().equals(DataTypes.StringType)) {
                listSymbolical.add(o.name());
            } else {
                listNumerical.add(o.name());
            }
        }
        String[] numericalArray = listNumerical.toArray(new String[0]);
        String[] symbolicalArray = listSymbolical.toArray(new String[0]);

        // Only numerical columns without label.
        Dataset<Row> dsNumerical = dsNoLabel.drop(symbolicalArray);
        // Only symbolical columns without label.
        Dataset<Row> dsSymbolical = dsNoLabel.drop(numericalArray);
        // Symbolical column names.
        String[] symbolicalColumnNames = dsSymbolical.columns();
        // Numerical column names.
        String[] numericalColumnNames = dsNumerical.columns();

        // Prepared dataset.
        Dataset<Row> dsPrepared;


        // Prepare data for numerical values if they are all or remove symbolical values.
        if (numericalColumnNames.length + 1 == ds.columns().length || removeStrings) {
            org.apache.log4j.Logger.getLogger(loggerName).info("Only numerical values, removeStrings: "+removeStrings);
            // Convert features to Vector.
            // Combines a given list of columns into a single vector column.
            VectorAssembler assembler = new VectorAssembler()
                    .setInputCols(numericalColumnNames)
                    .setOutputCol("features");
            // Transform and drop unnecessary columns. Remains only Vector and label.
            Dataset<Row> vectorNum = assembler.transform(ds).drop(numericalColumnNames);
            // Rename label column.
            dsPrepared = vectorNum.withColumnRenamed(label, "label");
        }
        // Prepare data for numerical and symbolical values. Convert symbolical to numerical.
        else {
            org.apache.log4j.Logger.getLogger(loggerName).info("Numerical and symbolical values, removeStrings: "+removeStrings);
            // Use StringIndexer on each symbolical column with Pipeline.
            PipelineStage[] pipelineStages = new PipelineStage[symbolicalColumnNames.length];
            for (int i = 0; i < pipelineStages.length; i++) {
                // Get current column name.
                String currentCol = symbolicalColumnNames[i];
                // Create indexer on column.
                StringIndexer indexer = new StringIndexer()
                        .setInputCol(currentCol)
                        .setOutputCol(currentCol + "*");
                // Add indexer to pipeline.
                pipelineStages[i] = indexer;
            }
            // Set stages to pipeline.
            Pipeline pipeline = new Pipeline().setStages(pipelineStages);
            // Fit.
            PipelineModel pipelineModel = pipeline.fit(ds);
            // Transform. Dataset will contain additional columns created by StringIndexer.
            // When the original symbolical column names are e.g "col4,col5", columns from StringIndexer are "col4*,col5*".
            Dataset<Row> dsAfterPipelineTransform = pipelineModel.transform(ds);
            // Drop original columns, remains only created by StringIndexer.
            Dataset<Row> dsAfterStringIndexer = dsAfterPipelineTransform.drop(ds.columns());


            // Column names created by StringIndexer.
            String[] afterStringIndexer = dsAfterStringIndexer.columns();
            // Future column names created by OneHotEncoder.
            String[] afterOneHot = new String[afterStringIndexer.length];

            for (int i = 0; i < afterOneHot.length; i++) {
                afterOneHot[i] = new StringBuffer().append(afterStringIndexer[i]).append("*").toString();
            }

            // OneHotEncoder Maps a column of category indices to a column of binary vectors.
            OneHotEncoderEstimator encoderHot = new OneHotEncoderEstimator()
                    .setInputCols(afterStringIndexer)
                    .setOutputCols(afterOneHot)
                    //.setHandleInvalid("keep") // Keep invalid and assign extra value.
                    .setDropLast(false);  // Avoid removing last value.

            // Fit.
            OneHotEncoderModel oneHotEncoderModel = encoderHot.fit(dsAfterPipelineTransform);
            // Transform and drop remained StringIndexer columns.
            Dataset<Row> dsAfterOneHotEncoder = oneHotEncoderModel.transform(dsAfterPipelineTransform).drop(dsAfterStringIndexer.columns());

            // Convert OneHotEncoder columns to Vector.
            VectorAssembler assembler = new VectorAssembler()
                    .setInputCols(afterOneHot)
                    .setOutputCol("featuresOHE");

            // Transform.
            Dataset<Row> dsVectorOHE = assembler.transform(dsAfterOneHotEncoder);

            // Only symbolical values in dataset.
            if (symbolicalColumnNames.length + 1 == ds.columns().length) {
                org.apache.log4j.Logger.getLogger(loggerName).info("Only symbolical values in dataset.");
                // Delete unnecessary columns.
                String[] colsForDelete = dsAfterOneHotEncoder.drop(label).columns();
                Dataset<Row> dsFeaturesLabel = dsVectorOHE.drop(colsForDelete);
                // Rename columns.
                dsPrepared = dsFeaturesLabel
                        .withColumnRenamed(label, "label")
                        .withColumnRenamed("featuresOHE", "features");
            }
            // Mixed symbolical and numerical values in dataset.
            else {
                org.apache.log4j.Logger.getLogger(loggerName).info("Mixed symbolical and numerical values in dataset.");
                // Convert numerical columns to Vector.
                VectorAssembler assembler2 = new VectorAssembler()
                        .setInputCols(numericalColumnNames)
                        .setOutputCol("featuresNUM");

                // Transform.
                Dataset<Row> dsVectorNum = assembler2.transform(dsVectorOHE);

                // Convert Vector from OneHotEncoder(symbolical values)
                // and Vector from numerical values into one Vector.
                VectorAssembler assembler3 = new VectorAssembler()
                        .setInputCols(new String[]{"featuresOHE", "featuresNUM"})
                        .setOutputCol("features");

                // Transform.
                Dataset<Row> dsVectorOHEAndNUM = assembler3.transform(dsVectorNum);

                // Column for delete.
                String[] colsForDelete = dsVectorNum.drop(dsLabel.columns()).columns();
                // Delete unnecessary columns.
                Dataset<Row> dsFeaturesLabel = dsVectorOHEAndNUM.drop(colsForDelete);
                // Rename column.
                dsPrepared = dsFeaturesLabel.withColumnRenamed(label, "label");
            }
        }
        return dsPrepared;
    }
}

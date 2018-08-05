package kmeans_implementation;

import org.apache.spark.ml.linalg.Vector;

import java.io.Serializable;

/**
 * Created by as on 02.06.2018.
 */
public class KMeansModel implements Serializable, DataModel {

    private Vector data;

    public Vector getData() {
        return data;
    }

    public void setData(Vector data) {
        this.data = data;
    }

}

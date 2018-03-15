package sparktemplate.datasets;

import org.apache.spark.rdd.JdbcRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import sparktemplate.DataRecord;

import java.sql.*;



/**
 * Klasa  <tt>DBDataSet</tt> reprezentuje zbior daych przechowywanych w bazie danych
 *
 * @author Jan G. Bazan
 * @version 1.0, luty 2018 roku
 */

public class DBDataSet {

    //Typy atrybutow prosze samemu ustalic, ale polecam tak jak w API WEKA

    public SparkSession sparkSession;
    public Dataset<Row> ds;
    private String url, user, password, table;
    private final String driver = "com.mysql.jdbc.Driver";
    private ResultSet rs;
    public Statement st;

    public Dataset<Row> getDs() {
        return ds;
    }

    public DBDataSet(SparkSession sparkSession, String url, String user, String password, String table) {
        this.sparkSession = sparkSession;
        this.url = url;
        this.user = user;
        this.password = password;
        this.table = table;
    }

    public void connect() //Polaczenie z baza danych
    {
        //Uwaga: Najlepiej, aby typ wartosci atrybutów był automatycznie rozpoznawany
        //Jesli bedzie to trudne dla Was, to można zalozyc, że pomocniczo definiowany jest plik tekstowy, w którym opisane jest powiazanie typu w bazie SQL 
        //oraz typem kolumn dla Sparka.

        // Spark
        this.ds = sparkSession.read()
                .option("driver", driver)
                .option("url", url)
                .option("dbtable", table)
                .option("user", user)
                .option("password", password)
                .option("inferSchema", true)
                .format("org.apache.spark.sql.execution.datasources.jdbc.DefaultSource")
                .load();

        // JDBC

        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url, user, password);
            String query = "SELECT * FROM "+table+"";
            this.st = conn.createStatement();
            this.rs = st.executeQuery(query);
        } catch (Exception e) {
            System.err.println("DB exception!!!");
            System.err.println(e.getMessage());
        }


    }

    //-------------

    public int getNoAttr() //Mozliwość sprawdzenia ile jest atrybutow (kolumn) w tablicy
    {
        return ds.columns().length;
    }

    public String getAttrName(int attributeIndex) //Mozliwość sprawdzenia nazwy atrybutu o podanym numerze
    {
        return ds.columns()[0];
    }

    //-----------------
    //Ogladanie wierszy tabeli jest strumieniowe, tzn. zaczynamy od pierwszego wiersza, a poźniej kursor przenosi sie na kolejny wiersz, 
    //który dostajemy metodą getNextRecord

    public DataRecord getFirstRecord() //Zwrocenie informacji o pierwszym wierszu danych
    {
        return new DataRecord(ds.first(),ds.schema());
    }

    public DataRecord getNextRecord() //Zwrocenie informacji o nastepnym wierszu danych
    {
        //Uwaga: Jeśli juz nie ma nastepnego powinien zwrocic null

        try {
            if(rs.next()){
                Row row = RowFactory.create(JdbcRDD.resultSetToObjectArray(rs));
                return new DataRecord(row,ds.schema());
            }else{
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public int getNoRecord() //Mozliwość sprawdzenia ile jest wierszy w tablicy
    {
        return (int) ds.count();
    }

}


package sparktemplate.datasets;

import org.apache.spark.rdd.JdbcRDD;
import org.apache.spark.sql.*;
import sparktemplate.dataprepare.DataPrepare;
import sparktemplate.datarecord.DataRecord;

import java.sql.*;


/**
 * Klasa  <tt>DBDataSet</tt> reprezentuje zbior daych przechowywanych w bazie danych
 *
 * @author Jan G. Bazan
 * @version 1.0, luty 2018 roku
 */

public class DBDataSet implements ADataSet {

    private SparkSession sparkSession;
    private Dataset<Row> ds;
    private String url, user, password, table;
    private final String driver = "org.postgresql.Driver";
    private final String driver2 = "com.mysql.jdbc.Driver";
    private ResultSet rs;
    private Statement st;
    private boolean connected = false;

    /**
     * Konstruktor inicjalizujacy obiekt DBDataSet.
     *
     * @param sparkSession obiekt sparkSession
     * @param url          sciezka do bazy danych
     * @param user         nazwa uzytkownika
     * @param password     haslo
     * @param table        tabela w bazie
     */
    public DBDataSet(SparkSession sparkSession, String url, String user, String password, String table) {
        this.sparkSession = sparkSession;
        this.url = url;
        this.user = user;
        this.password = password;
        this.table = table;
    }

    @Override
    public Dataset<Row> getDs() {
        if (connected) {
            return ds;
        } else {
            System.err.println("You should call connect before this action.");
            return null;
        }
    }

    /**
     * Metoda inicjalizujaca polaczenie z baza.
     */
    public void connect() {
        // Spark
        try {
            this.ds = sparkSession.read()
                    .option("driver", driver)
                    .option("url", url)
                    .option("dbtable", table)
                    .option("user", user)
                    .option("password", password)
                    .option("inferSchema", true)
                    .format("org.apache.spark.sql.execution.datasources.jdbc.DefaultSource")
                    .load();

        } catch (Exception e) {
            System.err.println("DB exception!");
            System.err.println(e.getMessage());
        }

        // JDBC
        try {
            Class.forName(driver);
            Connection conn = DriverManager.getConnection(url, user, password);
            String query = "SELECT * FROM " + table + "";
            this.st = conn.createStatement();
            this.rs = st.executeQuery(query);
        } catch (Exception e) {
            System.err.println("DB exception!");
            System.err.println(e.getMessage());
        }

        connected = true;
    }

    /**
     * Metoda zapisujaca zbior danych do bazy. Domyslnie dane zostana dodane do istniejacych.
     *
     * @param dataset dane do zapisania w bazie
     */
    public void save(Dataset<Row> dataset) {
        save(dataset, SaveMode.Append);
    }

    /**
     * Metoda zapisujaca zbior danych do bazy.
     *
     * @param dataset
     * @param mode Metoda zapisu (Append, Overwrite, ...)
     */
    public void save(Dataset<Row> dataset, SaveMode mode) {
        try {
            dataset.write()
                    .option("driver", driver)
                    .option("url", url + "?rewriteBatchedStatements=true")
                    .option("dbtable", table)
                    .option("user", user)
                    .option("password", password)
                    .option("inferSchema", true)
                    .format("org.apache.spark.sql.execution.datasources.jdbc.DefaultSource")
                    .mode(mode)
                    .save();
        } catch (Exception e) {
            System.err.println("DB exception: " + e);
        }
    }

    /**
     * Metoda zwracajaca ilosc atrybutow (kolumn) w tablicy.
     *
     * @return
     */
    public int getNoAttr() {
        return ds.columns().length;
    }

    /**
     * Metoda zwracajaca nazwe atrubutu.
     *
     * @param attributeIndex numer atrybutu (kolumny), (numeracja od 0)
     * @return nazwa atrybutu
     */
    public String getAttrName(int attributeIndex) {
        return ds.columns()[0];
    }

    /**
     * Metoda zwracajaca pierwszy wiersz bezposrednio z bazy dzieki JDBC.
     * Ogladanie wierszy tabeli jest strumieniowe, tzn. zaczynamy od pierwszego wiersza, a poźniej kursor przenosi sie na kolejny wiersz,
     * który dostajemy metodą getNextRecord.
     *
     * @return pojedynczy wiersz jako obiekt DataRecord
     */
    public DataRecord getFirstRecord() {
        return new DataRecord(ds.first(), ds.schema());
    }

    public void getFirstRecord2(){
        this.ds =  DataPrepare.createDataSet(getFirstRecord(), this.sparkSession);
    }

    /**
     * Metoda zwracajaca kolejne wiersze danych. Moze zostac uzyta do otrzymania pierwszego wiersza.
     * Jeśli juz nie ma nastepnego zwraca null.
     *
     * @return pojedynczy wiersz jako obiekt DataRecord
     */
    public DataRecord getNextRecord() {

        try {
            if (rs.next()) {
                Row row = RowFactory.create(JdbcRDD.resultSetToObjectArray(rs));
                return new DataRecord(row, ds.schema());
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getNextRecord2(){
        this.ds =  DataPrepare.createDataSet(getNextRecord(),this.sparkSession);
    }

    /**
     * Metoda sprawdzajaca ile jest wierszy w tablicy
     *
     * @return liczba wierszy w tablicy (Za pomoca metody sparka .count())
     */
    public int getNoRecord() {
        return (int) ds.count();
    }

}


package prestoComm;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableSet;
import helper_classes.ColumnInfo;
import helper_classes.DBData;
import helper_classes.SchemaInfo;
import helper_classes.TableInfo;

import java.io.*;
import java.sql.*;
import java.util.*;

public class PrestoMediator {

    private String url = "jdbc:presto://127.0.0.1:8080";
    final String JDBC_DRIVER = "com.facebook.presto.jdbc.PrestoDriver";
    private Connection conn;
    private String user = "test";
    private String pass = null;
    private DBConnector prestoConnector;
    private ResultSet res;
    private Statement stmt;

    public PrestoMediator(){
        prestoConnector = new DBConnector(url, JDBC_DRIVER, user, pass);

    }

    public static void main (String[] args){
        PrestoMediator connector = new PrestoMediator();
        //connector.getTableData("select * from mongodb.products.products");
        connector.makeQuery("describe mongodb.products.products");
        //connector.getDBData("mongodb");
        //connector.makeQuery("describe prestodb.public.catalog_page");
    }

    public boolean createConnection(){
        //Open a connection
        conn = prestoConnector.setConnection();
        if (conn == null)
            return false;
        return true;
    }


    public void makeQuery(String query){
        Statement stmt = null;
        try {
            //Register JDBC driver
            Class.forName(JDBC_DRIVER);
            //Execute a query
            stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery(query);
            ResultSetMetaData rsmd = res.getMetaData();
            if (rsmd.getColumnCount() > 1){
                //get all columns
                while (res.next()) {
                    for (int i = 1; i < rsmd.getColumnCount(); i++){
                        String name = rsmd.getColumnName(i);
                        System.out.print(name+": " + res.getString(name)+", ");
                    }
                    System.out.println("");
                }
            }
            else{
                //only one column
                ImmutableSet.Builder<String> set = ImmutableSet.builder();
                while (res.next()) {
                    set.add(res.getString(1));
                }
                Set setFinal = set.build();
                setFinal.size();
                Iterator<String> it = setFinal.iterator();
                while(it.hasNext()){
                    System.out.println(it.next());
                }
            }
            //Clean-up environment
            res.close();
            stmt.close();
        } catch (SQLException se) {
            //Handle errors for JDBC
            se.printStackTrace();
        } catch (Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        } finally {
            //finally block used to close resources
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    public void closeConection(){
        prestoConnector.closeConn();
    }

    /**
     * Given information about a database, create a .properties file for presto to be able to access that database
     * @param dbModel - database used identified by DBModel enum
     * @param serverUrl - the url to access the db. Must be in the form url:port
     * @param user
     * @param pass
     */
    public void createDBFileProperties(DBModel dbModel, String serverUrl, String user, String pass, String dbName){
        DBData dbData = new DBData(dbName, dbModel, serverUrl, user, pass);
        createDBFileProperties(dbData);
    }

    /**
     * Given information about a database, create a .properties file for presto to be able to access that database
     * @param dbData - database information (url, auth data, db type...)
     */
    public void createDBFileProperties(DBData dbData){
        //delete any possible http:// on server url or incorrect '/'
        Writer writer = null;
        String configFileName = Constants.PRESTO_PROPERTIES_FOLDER+dbData.getDbModel()+"_"+dbData.getUrl()+"_"+dbData.getDbName()+".properties";
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFileName), "utf-8"));
            writer.write("connector.name = " + dbData.getDbModel().toString().toLowerCase()+"\n");
            writer.write(writeDBTypePropertiesFile(dbData));
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //returns a string with the proper config text for the .properties file for a specific DB
    private String writeDBTypePropertiesFile(DBData dbData){
        String config = "";
        final boolean authNeeded = ( user != null && !user.isEmpty()) && ( pass != null && !pass.isEmpty());
        if (dbData.getDbModel().equals(DBModel.MongoDB)){
            config += "mongodb.seeds = "+dbData.getUrl()+";\n";
            if (authNeeded) {
                config += "mongodb.credentials = " + user + ":" + pass + "@" + dbData.getDbName() + "\n";
            }
        }
        else if (dbData.getDbModel().equals(DBModel.Redis)){
            config += "redis.table-names="+dbData.getDbName()+"\n";//list of all tables in the database (schema1.table1,schema1.table2)
            config += "redis.nodes="+dbData.getUrl()+"\n";
            if ( pass != null && !pass.isEmpty()) {
                config += "redis.password="+pass+"\n";
            }
        }
        else if (dbData.getDbModel().equals(DBModel.Cassandra)){//TODO: if port is not default (9042), cassandra.native-protocol-port must be defined with the port
            config += "cassandra.contact-points="+dbData.getUrl()+"\n";
            if (authNeeded) {
                config += "cassandra.username="+user+"\n";
                config += "cassandra.password="+pass+"\n";
            }
        }

        else {
            //relational dbs (mysql, sql server or postgresql) have identical .properties files
            config += "connection-url=jdbc:" + dbData.getDbModel().toString().toLowerCase() + "://"+ dbData.getUrl() +"/"+dbData.getDbName()+"\n";
            config += "connection-user="+user+"\n";
            config += "connection-password="+pass+"\n";
        }
        return config;
    }

    //given a list of catalogs (DBs), returns info about schemas, tables and columns in each one of them
    public Map<String, List<SchemaInfo>> getDBData(List<String> catalogs){
        stmt = null;
        res = null;
        createConnection();//or should this be done outside?
        Set<String> info = null;
        Map<String, List<SchemaInfo>> dbsInfo = new HashMap<>();//for each catalog (aka db), store info about schemas, tables and columns
        try {
            //Register JDBC driver
            Class.forName(JDBC_DRIVER);
            // for every catalog, get the schemas
            for (String catalog : catalogs) {
                Set <String> schemasInCatalog = getOneColumnResultQuery("show schemas from "+catalog, true);
                List<SchemaInfo> schemaInfos = new ArrayList<>();
                //for every schema, obtain tables
                for (String schema : schemasInCatalog){
                    Set <String> tablesInSchema = getOneColumnResultQuery("show tables from "+ catalog+"."+schema, true);
                    List<TableInfo> tableInfos = new ArrayList<>();
                    //for every table, obtain column information
                    for (String table : tablesInSchema){
                        //diferent!! more than one column
                        List <Map> columnsInTable = getColumnsResultQuery("describe "+catalog+"."+schema+".\""+table+"\"", true);
                        List<ColumnInfo> columnInfos = new ArrayList<>();
                        for (Map entry : columnsInTable){
                            //a column of the table in the form (column name -> column value)
                            columnInfos.add(new ColumnInfo(entry.get("Column").toString(), entry.get("Type").toString()));
                        }
                        TableInfo tableInfo = new TableInfo(table, columnInfos);
                        tableInfos.add(tableInfo);
                    }
                    SchemaInfo schemaInfo = new SchemaInfo(schema, tableInfos);
                    schemaInfos.add(schemaInfo);
                }
                dbsInfo.put(catalog, schemaInfos);
            }
            //Clean-up environment
            prestoConnector.closeConn();
            res.close();
            stmt.close();
        } catch (SQLException se) {
            //Handle errors for JDBC
            se.printStackTrace();
        } catch (Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        } finally {
            //finally block used to close resources
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return dbsInfo;
    }


    private Set<String> getOneColumnResultQuery(String query, boolean print) throws SQLException {
        Set<String> info = null;
        stmt = conn.createStatement();
        res = stmt.executeQuery(query);

        ImmutableSet.Builder<String> set = ImmutableSet.builder();
        //Extract data from result set
        while (res.next()) {
            set.add(res.getString(1));
        }
        info = set.build();
        Iterator<String> it = info.iterator();
        if (print){
            System.out.println("query: "+query +"\n Results:");
            while (it.hasNext()) {
                System.out.println(it.next());
            }
        }
        return info;
    }

    private List<Map> getColumnsResultQuery(String query, boolean print) throws SQLException {
        Set<String> info = null;
        stmt = conn.createStatement();
        ResultSet res = stmt.executeQuery(query);
        ResultSetMetaData rsmd = res.getMetaData();
        List<Map> records = new ArrayList<>();
        while (res.next()) {
            Map <String, String> row = new HashMap<>();//store a row from the records. Stores each column in the form (column name -> column value)
            for (int i = 1; i < rsmd.getColumnCount(); i++){
                String name = rsmd.getColumnName(i);
                row.put(name,res.getString(name));
                if (print)
                    System.out.print(name+": " + res.getString(name)+", ");
            }
            records.add(row);
            if (print)
                System.out.println("");
        }
        return records;
    }



}

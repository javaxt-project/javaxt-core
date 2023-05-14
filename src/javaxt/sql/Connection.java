package javaxt.sql;
import java.sql.SQLException;
import javaxt.utils.Generator;
import java.util.*;

//******************************************************************************
//**  Connection Class
//******************************************************************************
/**
 *   Used to open and close a connection to a database and execute queries.
 *
 ******************************************************************************/

public class Connection implements AutoCloseable {


    private java.sql.Connection Conn = null;
    private long Speed;
    private Database database;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Connection(){}


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Connection(java.sql.Connection conn){
        open(conn);
    }


  //**************************************************************************
  //** isOpen
  //**************************************************************************
  /** Used to determine whether the connection is open.
   */
    public boolean isOpen(){
        return !isClosed();
    }


  //**************************************************************************
  //** isClosed
  //**************************************************************************
  /** Used to determine whether the connection is closed.
   */
    public boolean isClosed(){
        try{
            return Conn.isClosed();
        }
        catch(Exception e){
            return true;
        }
    }


  //**************************************************************************
  //** getConnectionSpeed
  //**************************************************************************
  /** Used to retrieve the time it took to open the database connection
   * (in milliseconds)
   */
    public long getConnectionSpeed(){
        return Speed;
    }


  //**************************************************************************
  //** getConnection
  //**************************************************************************
  /** Used to retrieve the java.sql.Connection for this Connection
   */
    public java.sql.Connection getConnection(){
        return Conn;
    }



  //**************************************************************************
  //** Open
  //**************************************************************************
  /** Used to open a connection to the database.
   *
   *  @param ConnectionString A jdbc connection string/url. All connection URLs
   *  have the following form:
   *  <pre> jdbc:[dbVendor]://[dbName][propertyList] </pre>
   *
   *  Example:
   *  <pre> jdbc:derby://temp/my.db;user=admin;password=mypassword </pre>
   */
    public boolean open(String ConnectionString) throws SQLException {
        return open(new Database(ConnectionString));
    }


  //**************************************************************************
  //** Open
  //**************************************************************************
  /** Used to open a connection to the database.
   */
    public boolean open(Database database) throws SQLException {

        long startTime = System.currentTimeMillis();
        this.database = database;


      //Load JDBC Driver
        java.sql.Driver Driver = (java.sql.Driver) database.getDriver().load();


        //if (Conn!=null && Conn.isOpen()) Conn.close();


        String url = database.getURL();
        String username = database.getUserName();
        String password = database.getPassword();

        java.util.Properties properties = database.getProperties();
        if (properties==null) properties = new java.util.Properties();
        if (username!=null){
            properties.put("user", username);
            properties.put("password", password);
        }


        Conn = Driver.connect(url, properties);


        boolean isClosed = Conn.isClosed();



        Speed = System.currentTimeMillis()-startTime;
        return isClosed;
    }


  //**************************************************************************
  //** open
  //**************************************************************************
  /** Used to open a connection to the database using a JDBC Connection. This
   *  is particularly useful when using JDBC connection pools.
   */
    public boolean open(java.sql.Connection conn){

        boolean isClosed = true;
        try{
            database = new Database(conn);
            Conn = conn;
            isClosed = Conn.isClosed();
        }
        catch(Exception e){
            //System.out.println("Failed");
            //System.out.println(database.getDriver().getVendor() + " ERROR: " + e.toString());
            isClosed = true;
        }

        Speed = 0;
        return isClosed;
    }


  //**************************************************************************
  //** close
  //**************************************************************************
  /** Used to close a connection to the database, freeing up connections
   */
    public void close(){
        //System.out.println("Closing connection...");
        try{Conn.close();}
        catch(Exception e){
            //e.printStackTrace();
        }
    }


  //**************************************************************************
  //** getRecords
  //**************************************************************************
  /** Used to execute a SQL statement and returns Records as an iterator.
   *  Example:
   <pre>
    try (javaxt.sql.Connection conn = db.getConnection()){
        for (javaxt.sql.Record record : conn.getRecords("select distinct(first_name) from contacts")){
            System.out.println(record.get(0));
        }
    }
    catch(Exception e){
        e.printStackTrace();
    }
   </pre>
   *  Note that records returned by this method are read-only.
   */
    public Generator<javaxt.sql.Record> getRecords(String sql) throws SQLException {
        boolean readOnly = true;
        HashMap<String, Object> props = new HashMap<>();
        props.put("readOnly", readOnly);
        if (readOnly) props.put("fetchSize", 1000);
        return getRecords(sql, props);
    }


  //**************************************************************************
  //** getRecords
  //**************************************************************************
  /** Used to execute a SQL statement and returns a Records as an iterator.
   *  Example:
   <pre>
    try (javaxt.sql.Connection conn = db.getConnection()){
        for (javaxt.sql.Record record : conn.getRecords(

            "select * from contacts",

            new HashMap&lt;String, Object&gt;() {{
                put("readOnly", true);
                put("fetchSize", 1000);
            }}
        ))
        {

            System.out.println(record.get("first_name") + " " + record.get("last_name"));
        }
    }
    catch(Exception e){
        e.printStackTrace();
    }
   </pre>
   *  @param sql Query statement. This parameter is required.
   *  @param props Recordset options (e.g. readOnly, fetchSize, batchSize).
   *  See the Recordset class for more information about this properties. This
   *  parameter is optional.
   */
    public Generator<javaxt.sql.Record> getRecords(final String sql, Map<String, Object> props) throws SQLException {

        if (props==null) props = new HashMap<>();
        if (props.isEmpty()){
            props.put("readOnly", true);
            props.put("fetchSize", 1000);
        }

        Boolean readOnly = new Value(props.get("readOnly")).toBoolean();
        if (readOnly==null) readOnly = true;
        Integer fetchSize = new Value(props.get("fetchSize")).toInteger();
        if (fetchSize==null) fetchSize = 1000;
        Integer batchSize = new Value(props.get("batchSize")).toInteger();
        if (batchSize==null) batchSize = 0;


        final boolean _readOnly = readOnly;
        final int _fetchSize = fetchSize;
        final int _batchSize = batchSize;


        final Connection conn = this;
        try (Generator g = new Generator<javaxt.sql.Record>(){
            private Recordset rs;

            @Override
            public void run() throws InterruptedException {
                rs = new Recordset();
                if (_readOnly) rs.setFetchSize(_fetchSize);
                try{
                    rs.open(sql, conn, _readOnly);
                    if (!_readOnly) rs.setBatchSize(_batchSize);
                    while (rs.next()){
                        this.yield(rs.getRecord());
                    }
                }
                catch(Exception e){
                    RuntimeException ex = new RuntimeException(e.getMessage());
                    ex.setStackTrace(e.getStackTrace());
                    throw ex;
                }
            }

            @Override
            public void close() {
                if (rs!=null) rs.close();
            }
        }){
            return g;
        }
    }


  //**************************************************************************
  //** getRecord
  //**************************************************************************
    public javaxt.sql.Record getRecord(String sql) throws SQLException {
        HashMap<String, Object> props = new HashMap<>();
        props.put("readOnly", true);
        props.put("fetchSize", 1);

        javaxt.sql.Record record = null;
        try (Recordset rs = getRecordset(sql, props)){
            if (rs.hasNext()) record = rs.getRecord();
        }
        return record;
    }


  //**************************************************************************
  //** getRecordset
  //**************************************************************************
  /** Used to execute a SQL statement and return an open Recordset. The caller
   *  must explicitly close the Recordset when finished or invoke the
   *  getRecordset() method it in a try/catch statement.
   *  @param sql Query statement. This parameter is required.
   *  @param props Recordset options (e.g. readOnly, fetchSize, batchSize).
   *  See the getRecords() method for an example of how to set properties.
   *  This parameter is optional.
   */
    public Recordset getRecordset(String sql, Map<String, Object> props) throws SQLException {

        if (props==null) props = new HashMap<>();
        if (props.isEmpty()){
            props.put("readOnly", true);
            props.put("fetchSize", 1000);
        }

        Boolean readOnly = new Value(props.get("readOnly")).toBoolean();
        if (readOnly==null) readOnly = true;
        Integer fetchSize = new Value(props.get("fetchSize")).toInteger();
        if (fetchSize==null) fetchSize = 1000;
        Integer batchSize = new Value(props.get("batchSize")).toInteger();
        if (batchSize==null) batchSize = 0;

        Recordset rs = new Recordset();
        if (readOnly) rs.setFetchSize(fetchSize);
        rs.open(sql, this, readOnly);
        if (!readOnly) rs.setBatchSize(batchSize);
        return rs;
    }


  //**************************************************************************
  //** getRecordset
  //**************************************************************************
  /** Used to execute a SQL statement and return an open Recordset. The caller
   *  must explicitly close the Recordset when finished or invoke the
   *  getRecordset() method it in a try/catch statement. Example usage:
   <pre>
    try (javaxt.sql.Connection conn = db.getConnection()){

      //Open recordset
        javaxt.sql.Recordset rs = conn.getRecordset("select * from contacts", true);

      //Iterate through the records
        while (rs.next()){

          //Do something with the record. Example:
            System.out.println(rs.getValue(0));
        }

      //Close the recordset
        rs.close();
    }
    catch(Exception e){
        e.printStackTrace();
    }
   </pre>
   *  @param sql Query statement. This parameter is required.
   *  @param readOnly If true, will
   */
    public Recordset getRecordset(String sql, boolean readOnly) throws SQLException {
        HashMap<String, Object> props = new HashMap<>();
        props.put("readOnly", readOnly);
        if (readOnly) props.put("fetchSize", 1000);
        return getRecordset(sql, props);
    }


  //**************************************************************************
  //** getRecordset
  //**************************************************************************
    public Recordset getRecordset(String sql) throws SQLException {
        return getRecordset(sql, true);
    }


  //**************************************************************************
  //** execute
  //**************************************************************************
  /** Used to execute a prepared sql statement (e.g. "delete from my_table").
   */
    public void execute(String sql) throws SQLException {
        java.sql.PreparedStatement stmt = null;
        try{
            stmt = Conn.prepareStatement(sql);
            stmt.execute();
        }
        catch(Exception e){
            throw e;
        }
        finally{
            if (stmt!=null) stmt.close();
        }
    }


  //**************************************************************************
  //** commit
  //**************************************************************************
  /** Used to explicitly commit changes made to the database.
   */
    public void commit() throws SQLException {
        execute("COMMIT");
    }


  //**************************************************************************
  //** getDatabase
  //**************************************************************************
  /** Used to return database information associated with this connection.
   */
    public Database getDatabase(){
        return database;
    }
}
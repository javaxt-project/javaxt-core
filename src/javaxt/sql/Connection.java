package javaxt.sql;
import java.sql.SQLException;
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
  /** Returns true if the connection is open.
   */
    public boolean isOpen(){
        return !isClosed();
    }


  //**************************************************************************
  //** isClosed
  //**************************************************************************
  /** Returns true if the connection is closed.
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
  //** open
  //**************************************************************************
  /** Used to open a connection to the database using a JDBC connection
   *  string. Returns true if the connection was opened successfully.
   *
   *  @param ConnectionString A jdbc connection string/url. All connection
   *  URLs have the following form:
   *  <pre> jdbc:[dbVendor]://[dbName][propertyList] </pre>
   *
   *  Example:
   *  <pre> jdbc:derby://temp/my.db;user=admin;password=mypassword </pre>
   */
    public boolean open(String ConnectionString) throws SQLException {
        return open(new Database(ConnectionString));
    }


  //**************************************************************************
  //** open
  //**************************************************************************
  /** Used to open a connection to the database using a javaxt.sql.Database.
   *  Returns true if the connection was opened successfully.
   */
    public boolean open(Database database) throws SQLException {

        long startTime = System.currentTimeMillis();
        this.database = database;


        ConnectionPool connectionPool = database.getConnectionPool();
        if (connectionPool==null){

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
                if (password!=null) properties.put("password", password);
            }


            Conn = Driver.connect(url, properties);
        }
        else{
            Conn = connectionPool.getConnection().getConnection();
        }


        boolean isClosed = Conn.isClosed();


        Speed = System.currentTimeMillis()-startTime;
        return !isClosed;
    }


  //**************************************************************************
  //** open
  //**************************************************************************
  /** Used establish a connection to the database using a previously opened
   *  java.sql.Connection. Returns true if the connection is open.
   *  @param conn An open java.sql.Connection
   *  @param database Used to associate a database instance with this
   *  connection. In doing so, you can avoid a potentially costly call parse
   *  connection metadata.
   */
    public boolean open(java.sql.Connection conn, Database database){
        this.database = database;
        return open(conn);
    }


  //**************************************************************************
  //** open
  //**************************************************************************
  /** Used establish a connection to the database using a previously opened
   *  java.sql.Connection. Returns true if the connection is open.
   */
    public boolean open(java.sql.Connection conn){

        boolean isClosed;
        try{
            if (database==null) database = new Database(conn);
            Conn = conn;
            isClosed = Conn.isClosed();
        }
        catch(Exception e){
            //System.out.println("Failed");
            //System.out.println(database.getDriver().getVendor() + " ERROR: " + e.toString());
            isClosed = true;
        }

        Speed = 0;
        return !isClosed;
    }


  //**************************************************************************
  //** close
  //**************************************************************************
  /** Used to close a connection to the database, freeing up server resources.
   *  It is imperative that the database connection is closed after it is no
   *  longer needed, especially if the connection came from a ConnectionPool.
   *  That said, this class implements the AutoCloseable interface so you do
   *  not have to call this method if the connection was opened as part of a
   *  "try" statement. Example:
   <pre>
    try (javaxt.sql.Connection conn = database.getConnection()){

    }
    catch(Exception e){
        e.printStackTrace();
    }
   </pre>
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
    for (javaxt.sql.Record record : conn.getRecords("select id from contacts")){
        System.out.println(record.get(0));
    }
   </pre>
   *  Note that records returned by this method are read-only.
   */
    public Iterable<javaxt.sql.Record> getRecords(String sql) throws SQLException {
        return getRecords(sql, null);
    }


  //**************************************************************************
  //** getRecords
  //**************************************************************************
  /** Used to execute a SQL statement and returns a Records as an iterator.
   *  Example:
   <pre>
    for (javaxt.sql.Record record : conn.getRecords(

        "select first_name, last_name from contacts",

        new HashMap&lt;String, Object&gt;() {{
            put("readOnly", true);
            put("fetchSize", 1000);
        }}
    ))
    {
        System.out.println(record.get("first_name") + " " + record.get("last_name"));
    }
   </pre>
   *  @param sql Query statement. This parameter is required.
   *  @param props Recordset options (e.g. readOnly, fetchSize, batchSize).
   *  See the Recordset class for more information about this properties. This
   *  parameter is optional.
   */
    public Iterable<javaxt.sql.Record> getRecords(String sql, Map<String, Object> props) throws SQLException {
        return new RecordIterator(getRecordset(sql, props));
    }


  //**************************************************************************
  //** getRecord
  //**************************************************************************
  /** Returns a single record from the database. Example:
   <pre>
    javaxt.sql.Record record = conn.getRecord("select count(*) from contacts");
    if (record!=null) System.out.println(record.get(0));
   </pre>
   *  Note that records returned by this method are read-only.
   */
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
  /** Used to execute a SQL statement and return an open Recordset. The caller
   *  must explicitly close the Recordset when finished or invoke the
   *  getRecordset() method it in a try/catch statement. See the other
   *  getRecordset() for an example. Note that records returned by this method
   *  are read-only.
   */
    public Recordset getRecordset(String sql) throws SQLException {
        return getRecordset(sql, true);
    }


  //**************************************************************************
  //** execute
  //**************************************************************************
  /** Used to execute a prepared sql statement (e.g. "delete from my_table").
   */
    public void execute(String sql) throws SQLException {
        try (java.sql.PreparedStatement stmt = Conn.prepareStatement(sql)){
            stmt.execute();
            try { Conn.commit(); } catch(Exception e){}
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


  //**************************************************************************
  //** RecordIterator
  //**************************************************************************
  /** Class used to iterate through records in a Recordset
   */
    private class RecordIterator implements Iterable<javaxt.sql.Record>, AutoCloseable {
        private final Recordset rs;

        public RecordIterator(Recordset rs){
            this.rs = rs;
        }

        @Override
        public java.util.Iterator<javaxt.sql.Record> iterator() {
            return new java.util.Iterator<javaxt.sql.Record>(){
                @Override
                public boolean hasNext(){
                    return rs.hasNext();
                }
                @Override
                public javaxt.sql.Record next(){
                    Field[] fields = rs.getFields();
                    Field[] clones = new Field[fields.length];
                    for (int i=0; i<fields.length; i++){
                        clones[i] = fields[i].clone();
                        clones[i].Value = fields[i].Value;
                    }
                    javaxt.sql.Record record = new javaxt.sql.Record(clones);
                    rs.moveNext();
                    return record;
                }
            };
        }


        @Override
        public void close() {
            if (rs!=null) rs.close();
        }
    }
}
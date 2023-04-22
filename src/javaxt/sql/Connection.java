package javaxt.sql;
import java.sql.SQLException;
import javaxt.utils.Generator;
import java.util.*;

//******************************************************************************
//**  Connection Class
//******************************************************************************
/**
 *   Used to open and close a connection to a database.
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
        boolean isClosed = true;


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


        isClosed = Conn.isClosed();


        long endTime = System.currentTimeMillis();
        Speed = endTime-startTime;
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
  //** getRecordset
  //**************************************************************************
  /** Used to execute a SQL statement and returns a Recordset as an iterator.
   *  Example:
   <pre>
    try (javaxt.sql.Connection conn = db.getConnection()){
        for (Recordset rs : conn.getRecordset("select distinct(first_name) from contacts")){
            System.out.println(rs.getValue(0));
        }
    }
    catch(Exception e){
        e.printStackTrace();
    }
   </pre>
   *  Note that records returned by this method are read-only. See the other
   *  getRecordset() methods for options to create or update records.
   */
    public Generator<Recordset> getRecordset(String sql) throws SQLException {
        return getRecordset(sql, true);
    }


  //**************************************************************************
  //** getRecordset
  //**************************************************************************
  /** Used to execute a SQL statement and returns a Recordset as an iterator.
   *  Provides an option to return records that are read-only or editable. To
   *  perform a query with read-only records, do something like this:
   <pre>
    try (javaxt.sql.Connection conn = db.getConnection()){
        for (Recordset rs : conn.getRecordset("select * from contacts", true)){
            System.out.println(rs.getValue(0));
        }
    }
    catch(Exception e){
        e.printStackTrace();
    }
   </pre>
   *  To insert records, do something like this:
   <pre>
    try (javaxt.sql.Connection conn = db.getConnection()){
        for (Recordset rs : conn.getRecordset("select * from contacts where id=-1", false)){
            rs.addNew();
            rs.setValue("first_name", "John");
            rs.setValue("last_name", "Smith");
            rs.update();
        }
    }
    catch(Exception e){
        e.printStackTrace();
    }
   </pre>
   *  To update existing records, do something like this:
   <pre>
    try (javaxt.sql.Connection conn = db.getConnection()){
        for (Recordset rs : conn.getRecordset("select * from contacts where last_name='Smith'", false)){
            String firstName = rs.getValue("first_name").toString();
            if (firstName.equals("John")){
                rs.setValue("name", "Jonathan");
                rs.update();
            }
        }
    }
    catch(Exception e){
        e.printStackTrace();
    }
   </pre>
   */
    public Generator<Recordset> getRecordset(String sql, boolean readOnly) throws SQLException {
        HashMap<String, Object> props = new HashMap<>();
        props.put("readOnly", readOnly);
        if (readOnly) props.put("fetchSize", 1000);
        return getRecordset(sql, props);
    }


  //**************************************************************************
  //** getRecordset
  //**************************************************************************
  /** Used to execute a SQL statement and returns a Recordset as an iterator.
   *  Example:
   <pre>
    try (javaxt.sql.Connection conn = db.getConnection()){
        for (Recordset rs : conn.getRecordset("select * from contacts",
            new HashMap&lt;String, Object&gt;() {{
                put("readOnly", true);
                put("fetchSize", 1000);
            }}))
        {

            System.out.println(rs.getValue("first_name") + " " + rs.getValue("last_name"));
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
    public Generator<Recordset> getRecordset(final String sql, Map<String, Object> props) throws SQLException {

        if (props==null){
            props = new HashMap<>();
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
        try (Generator g = new Generator<Recordset>(){
            private Recordset rs;

            @Override
            public void run() throws InterruptedException {
                rs = new Recordset();
                if (_readOnly) rs.setFetchSize(_fetchSize);
                try{
                    rs.open(sql, conn, _readOnly);
                    if (!_readOnly) rs.setBatchSize(_batchSize);
                    while (rs.next()){
                        this.yield(rs);
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
                //System.out.println("Closing recordset...");
                if (rs!=null) rs.close();
            }
        }){
            return g;
        }
    }


  //**************************************************************************
  //** execute
  //**************************************************************************
  /** Used to execute a prepared sql statement (e.g. "delete from my_table").
   */
    public void execute(String sql) throws SQLException {
        java.sql.PreparedStatement preparedStmt = Conn.prepareStatement(sql);
        preparedStmt.execute();
        preparedStmt.close();
        preparedStmt = null;
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
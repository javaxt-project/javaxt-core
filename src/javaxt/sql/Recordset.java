package javaxt.sql;
import java.sql.SQLException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

//******************************************************************************
//**  Recordset Class
//*****************************************************************************/
/**
 *   Used to query and update records in a database.
 *
 ******************************************************************************/

public class Recordset {

    private java.sql.ResultSet rs = null;
    private java.sql.Statement stmt = null;
    private int x;
    private boolean isReadOnly = true;
    private String sqlString = null;

    private Connection Connection = null;
    private Driver driver = null;
    private boolean autoCommit = true;

    private Value GeneratedKey;

    private java.util.ArrayList keys = new java.util.ArrayList();


   /**
    * Returns a value that describes if the Recordset object is open, closed,
    * connecting, executing or retrieving data
    */
    public int State = 0;

   /**
    * Returns true if the current record position is after the last record,
    * otherwise false.
    */
    public boolean EOF = false;

   /**
    * An array of fields. Each field contains information about a column in a
    * Recordset object. There is one Field object for each column in the
    * Recordset.
    */
    private Field[] Fields = null;


   /**
    * An array of tables found in the database
    */
    private Table[] Tables = null;


   /**
    * Sets or returns the maximum number of records to return to a Recordset
    * object from a query.
    */
    public int MaxRecords = 1000000000;

   /**
    * Returns the number of records in a Recordset object. This property is a
    * bit unreliable. Recommend using the getRecordCount() method instead.
    */
    public int RecordCount;

   /**
    * Returns the time it took to execute a given query. Units are in
    * milliseconds
    */
    public long QueryResponseTime;

   /**
    * Returns the total elapsed time between open and close operations. Units
    * are in milliseconds
    */
    public long EllapsedTime;

   /**
    * Returns the elapsed time it took to retrieve additional metadata not
    * correctly supported by the jdbc driver. Units are in milliseconds.
    */
    public long MetadataQueryTime;
    private long startTime, endTime;


    private String queryID;
    private static AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private static final Thread shutdownHook = getShutdownHook();
    private static final ConcurrentHashMap<String, Recordset> queries =
    new ConcurrentHashMap<String, Recordset>();


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class.
   */
    public Recordset(){
        if (shuttingDown.get()) throw new IllegalStateException("JVM shutting down");
    }


  //**************************************************************************
  //** getShutdownHook
  //**************************************************************************
  /** Adds a listener to the jvm to watch for shutdown events.
   */
    private static Thread getShutdownHook(){
        Thread shutdownHook = new Thread() {
            public void run() {
                shuttingDown.set(true);
                synchronized(queries){
                    java.util.Iterator<String> it = queries.keySet().iterator();
                    while (it.hasNext()){
                        Recordset rs = queries.get(it.next());
                        java.sql.Statement stmt = rs.stmt;
                        if (stmt!=null){
                            try{stmt.cancel();} catch(Exception e){}
                            try{stmt.close();} catch(Exception e){}
                        }
                    }
                    queries.clear();
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        return shutdownHook;
    }


  //**************************************************************************
  //** isOpen
  //**************************************************************************
  /** Returns true if the recordset is open. This method is only supported on
   *  Java 1.6 or higher. Otherwise, the method will return false.
   */
    public boolean isOpen(){
        if (State!=0) {
            //return !rs.isClosed();

            int javaVersion = javaxt.utils.Java.getVersion();
            if (javaVersion<6) return false;
            else{
                try{
                    return !((Boolean) rs.getClass().getMethod("isClosed").invoke(rs, null));
                }
                catch(Exception e){
                    return false;
                }
            }
        }
        else return false;
    }


  //**************************************************************************
  //** isReadOnly
  //**************************************************************************
  /** Returns true if records are read-only. */

    public boolean isReadOnly(){
        return isReadOnly;
    }


  //**************************************************************************
  //** Open
  //**************************************************************************
  /** Used to execute a query and access records in the database. Records
   *  fetched using this method cannot be updated or deleted and new records
   *  cannot be inserted into the database.
   *
   *  @param sql SQL Query. Example: "SELECT * FROM EMPLOYEE"
   *  @param conn An active connection to the database.
   */
    public java.sql.ResultSet open(String sql, Connection conn) throws SQLException {
        return open(sql,conn,true);
    }


  //**************************************************************************
  //** Open
  //**************************************************************************
  /** Used to execute a query and access records in the database.
   *
   *  @param sqlString SQL Query. Example: "SELECT * FROM EMPLOYEE"
   *  @param Connection An active connection to the database.
   *  @param ReadOnly Set whether the records are read-only. If true, records
   *  fetched using this method cannot be updated or deleted and new records
   *  cannot be inserted into the database. If false, records can be updated
   *  or deleted and new records can be inserted into the database.
   */
    public java.sql.ResultSet open(String sqlString, Connection Connection, boolean ReadOnly) throws SQLException {
        if (shuttingDown.get()) throw new IllegalStateException("JVM shutting down");

        rs = null;
        stmt = null;
        State = 0;
        EOF = true;
        Tables = null;
        this.sqlString = sqlString;
        this.Connection = Connection;
        this.isReadOnly = ReadOnly;
        this.driver = Connection.getDatabase().getDriver();
        if (driver==null) driver = new Driver("","","");


        if (Connection==null) throw new SQLException("Connection is null.");
        if (Connection.isClosed()) throw new SQLException("Connection is closed.");


        startTime = System.currentTimeMillis();


        java.sql.Connection Conn = Connection.getConnection();
        autoCommit = Conn.getAutoCommit();
        queryID = UUID.randomUUID().toString();
        synchronized(queries){
            queries.put(queryID, this);
        }



      //Read-Only Connection
        if (ReadOnly){

            try{

              //Set AutoCommit to false when fetchSize is specified.
              //Otherwise it will fetch back all the records at once
                if (fetchSize!=null) Conn.setAutoCommit(false);


              //DB2 and SQLite only support forward cursors
                if (driver.equals("DB2") || driver.equals("SQLite")){
                    stmt = Conn.createStatement(rs.TYPE_FORWARD_ONLY, rs.CONCUR_READ_ONLY);
                }


                else if (driver.equals("PostgreSQL")){
                    if (fetchSize!=null){
                        stmt = Conn.createStatement(rs.TYPE_FORWARD_ONLY, rs.CONCUR_READ_ONLY, rs.FETCH_FORWARD);
                    }
                    else{
                        stmt = Conn.createStatement(rs.TYPE_SCROLL_INSENSITIVE, rs.CONCUR_READ_ONLY);
                    }

                }


              //Default Connection
                else{
                    try{
                        stmt = Conn.createStatement(rs.TYPE_SCROLL_INSENSITIVE, rs.CONCUR_READ_ONLY);
                    }
                    catch(SQLException e){
                        stmt = Conn.createStatement();
                    }
                }

                if (fetchSize!=null) stmt.setFetchSize(fetchSize);
                rs = stmt.executeQuery(sqlString);
                State = 1;
            }
            catch(SQLException e){
                //System.out.println("ERROR Open RecordSet: " + e.toString());
                synchronized(queries){
                    queries.remove(queryID);
                }
                throw e;
            }
        }

      //Read-Write Connection
        else{
            try{

              //SYBASE Connection
                if (driver.equals("SYBASE")){
                    if (fetchSize!=null) Conn.setAutoCommit(false);
                    stmt = Conn.createStatement(rs.TYPE_FORWARD_ONLY,rs.CONCUR_UPDATABLE);
                    if (fetchSize!=null) stmt.setFetchSize(fetchSize);
                    rs = stmt.executeQuery(sqlString);
                    State = 1;
                }

              //SQLite Connection
                else if (driver.equals("SQLite")){
                    if (fetchSize!=null) Conn.setAutoCommit(false);
                    stmt = Conn.createStatement(rs.TYPE_FORWARD_ONLY,rs.CONCUR_READ_ONLY); //xerial only seems to support this cursor
                    if (fetchSize!=null) stmt.setFetchSize(fetchSize);
                    rs = stmt.executeQuery(sqlString);
                    State = 1;
                }

              //DB2 Connection
                else if (driver.equals("DB2")){
                    try{
                        if (fetchSize!=null) Conn.setAutoCommit(false);
                        stmt = Conn.createStatement(rs.TYPE_SCROLL_SENSITIVE,rs.CONCUR_UPDATABLE);
                        if (fetchSize!=null) stmt.setFetchSize(fetchSize);
                        rs = stmt.executeQuery(sqlString);
                        State = 1;
                    }
                    catch(Exception e){
                        //System.out.println("createStatement(rs.TYPE_SCROLL_SENSITIVE,rs.CONCUR_UPDATABLE) Error:");
                        //System.out.println(e.toString());
                        rs = null;
                    }
                    if (rs==null){
                        try{
                            if (fetchSize!=null) Conn.setAutoCommit(false);
                            stmt = Conn.createStatement(rs.TYPE_FORWARD_ONLY,rs.CONCUR_UPDATABLE);
                            if (fetchSize!=null) stmt.setFetchSize(fetchSize);
                            rs = stmt.executeQuery(sqlString);
                            State = 1;
                        }
                        catch(Exception e){
                            //System.out.println("createStatement(rs.TYPE_FORWARD_ONLY,rs.CONCUR_UPDATABLE) Error:");
                            //System.out.println(e.toString());
                        }
                    }

                }

              //Default Connection
                else{
                    if (fetchSize!=null) Conn.setAutoCommit(false);
                    stmt = Conn.createStatement(rs.TYPE_SCROLL_SENSITIVE,rs.CONCUR_UPDATABLE);
                    if (fetchSize!=null) stmt.setFetchSize(fetchSize);
                    rs = stmt.executeQuery(sqlString);
                    /*
                    stmt.execute(sqlString, java.sql.Statement.RETURN_GENERATED_KEYS);
                    rs = stmt.getResultSet();
                    */
                    State = 1;
                }


            }
            catch(SQLException e){
                //System.out.println("ERROR Open RecordSet (RW): " + e.toString());
                synchronized(queries){
                    queries.remove(queryID);
                }
                throw e;
            }


        }


        endTime = System.currentTimeMillis();
        QueryResponseTime = endTime-startTime;

        init();

        return rs;
    }


  //**************************************************************************
  //** open
  //**************************************************************************
  /** Used to initialize a Recordset using a standard Java ResultSet
   */
    public void open(java.sql.ResultSet resultSet){
        if (shuttingDown.get()) throw new IllegalStateException("JVM shutting down");
        startTime = System.currentTimeMillis();
        EOF = true;
        rs = resultSet;
        queryID = UUID.randomUUID().toString();
        synchronized(queries){
            queries.put(queryID, this);
        }
        init();
    }


  //**************************************************************************
  //** init
  //**************************************************************************
  /** Used to initialize fields
   */
    private void init(){
        try{

          //Create Fields
            java.sql.ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            Fields = new Field[cols];
            for (int i=1; i<=cols; i++) {
                 Fields[i-1] = new Field(i, rsmd);
            }
            rsmd = null;

            x=-1;

            if (rs!=null){
                if (rs.next()){

                    EOF = false;
                    for (int i=1; i<=cols; i++) {
                         Fields[i-1].Value = new Value(rs.getObject(i));
                    }
                    x+=1;
                }

              //Get Additional Metadata
                //long mStart = java.util.Calendar.getInstance().getTimeInMillis();
                //RecordCount = getRecordCount();
                //updateFields();
                //long mEnd = java.util.Calendar.getInstance().getTimeInMillis();
                //MetadataQueryTime = mEnd-mStart;
                MetadataQueryTime = 0;
            }


        }
        catch(SQLException e){
            //e.printStackTrace();
            //throw e;
        }
    }


  //**************************************************************************
  //** close
  //**************************************************************************
  /** Closes the Recordset freeing up database and jdbc resources.
   */
    public void close(){


      //Close recordset
        try{
            if (State==1) executeBatch();
            if (!isReadOnly) commit();
            if (rs!=null) rs.close();
            if (stmt!=null){

              //PostgreSQL (possibly others) will continue executing a long
              //query even after closing the recordset. The only way to stop
              //a long-running query is to cancel the statement. Note that
              //cancelling a statement in SQLite calls sqlite3_interrupt()
              //which causes issues when inserting, updating, or deleting
              //records.
                if (driver.equals("PostgreSQL")){
                    try{ stmt.cancel(); }
                    catch(Exception e){}
                }

                try{ stmt.close(); }
                catch(Exception e){}
            }
        }
        catch(SQLException e){
            e.printStackTrace();
            SQLException ex = e.getNextException();
            if (ex!=null) ex.printStackTrace();
        }


      //Remove recordset from the list of queries
        synchronized(queries){
            queries.remove(queryID);
        }


      //Reset autocommit
        try{
            Connection.getConnection().setAutoCommit(autoCommit);
        }
        catch(Exception e){
            //e.printStackTrace();
        }


        State = 0;
        rs = null;
        stmt = null;
        driver = null;
        sqlString = null;
        keys.clear();

        if (Fields!=null){
            for (Field f : Fields){
                f.clear();
                f = null;
            }
            Fields = null;
        }

        endTime = System.currentTimeMillis();
        EllapsedTime = endTime-startTime;
    }



  //**************************************************************************
  //** getDatabase
  //**************************************************************************
  /**  Returns connection information to the database.   */

    public Database getDatabase(){
        return this.Connection.getDatabase();
    }



    private Integer fetchSize = null;

  //**************************************************************************
  //** setFetchSize
  //**************************************************************************
  /** This method changes the block fetch size for server cursors. This may
   *  help avoid out of memory exceptions when retrieving a large number of
   *  records from the database. Set this method BEFORE opening the recordset.
   */
    public void setFetchSize(int fetchSize){
        if (fetchSize>0) this.fetchSize = fetchSize;
    }

  //**************************************************************************
  //** getConnection
  //**************************************************************************
 /**  Returns the JDBC Connection used to create/open the recordset.  */

    public Connection getConnection(){
        return Connection;
    }

  //**************************************************************************
  //** Commit
  //**************************************************************************
  /** Used to explicitly commit an sql statement. May be useful for bulk
   *  update and update statements, depending on the underlying DBMS.
   */
    public void commit(){
        try{
            //stmt.executeQuery("COMMIT");
            java.sql.Connection Conn = Connection.getConnection();
            Conn.commit();
        }
        catch(Exception e){
            //System.out.println(e.toString());
        }
    }


    private boolean InsertOnUpdate = false;


  //**************************************************************************
  //** AddNew
  //**************************************************************************
  /** Used to prepare the driver to insert new records to the database. Used
   *  in conjunction with the update method.
   */
    public void addNew(){
        if (State==1){
            InsertOnUpdate = true;
            for (int i=1; i<=Fields.length; i++) {
                Field Field = Fields[i-1];
                Field.Value = null;
                Field.RequiresUpdate = false;
            }
        }
    }


  //**************************************************************************
  //** Update
  //**************************************************************************
  /** Used to add or update a record in a table. Note that inserts can be
   *  batched using the setBatch() method to improve performance. When
   *  performing batch inserts, the update statements are queued and executed
   *  only after the batch size is reached.
   */
    public void update() throws SQLException {
        if (isReadOnly) throw new SQLException("Read only!");
        if (State!=1) throw new SQLException("Recordset is closed!");
        if (!isDirty()) return;


      //Generate list of fields that require updates
        java.util.ArrayList<Field> fields = new java.util.ArrayList<Field>();
        for (Field field : Fields){
            if (field.getName()!=null && field.RequiresUpdate) fields.add(field);
        }
        int numUpdates = fields.size();


      //Get table name
        String tableName = Fields[0].getTable();
        String schemaName = Fields[0].getSchema();
        if (tableName==null){
            updateFields();
            tableName = Fields[0].getTable();
            schemaName = Fields[0].getSchema();
        }
        else{
            if (schemaName==null){
                updateFields();
                schemaName = Fields[0].getSchema();
            }
        }
        //if (tableName.contains(" ")) tableName = "[" + tableName + "]";
        tableName = escape(tableName);
        schemaName = escape(schemaName);
        if (schemaName!=null) tableName = schemaName + "." + tableName;



      //Construct a SQL insert/update statement
        StringBuffer sql = new StringBuffer();
        if (InsertOnUpdate){
            sql.append("INSERT INTO " + tableName + " (");
            for (int i=0; i<numUpdates; i++){
                String colName = escape(fields.get(i).getName());
                sql.append(colName);
                if (numUpdates>1 && i<numUpdates-1){
                    sql.append(",");
                }
            }
            sql.append(") VALUES (");
            for (int i=0; i<numUpdates; i++){
                if (i>0) sql.append(",");
                sql.append(getQ(fields.get(i)));
            }
            sql.append(")");
        }
        else{
            sql.append("UPDATE " + tableName + " SET ");
            for (int i=0; i<numUpdates; i++){
                String colName = escape(fields.get(i).getName());
                sql.append(colName);
                sql.append("=");
                sql.append(getQ(fields.get(i)));
                if (numUpdates>1 && i<numUpdates-1){
                    sql.append(", ");
                }
            }

          //Find primary key for the table. This slows things down
          //quite a bit but we need it for the "where" clause.
            if (keys.isEmpty()){
                try{
                    java.sql.Connection Conn = Connection.getConnection();
                    java.sql.DatabaseMetaData dbmd = Conn.getMetaData();
                    java.sql.ResultSet r2 = dbmd.getTables(null,null,Fields[0].getTable(),new String[]{"TABLE"});
                    if (r2.next()) {
                        Table table = new Table(r2, dbmd);
                        Key[] arr = table.getPrimaryKeys();
                        if (arr!=null){
                            for (int i=0; i<arr.length; i++){
                                Key key = arr[i];
                                Field field = getField(key.getColumn());
                                if (field!=null) keys.add(field);
                            }
                        }
                    }
                    r2.close();
                }
                catch(Exception e){
                }
            }



          //Build the where clause
            if (!keys.isEmpty()){
                sql.append(" WHERE ");
                for (int i=0; i<keys.size(); i++){
                    Object key = keys.get(i);
                    Field field;
                    if (key instanceof String){
                        field = getField((String) key);
                        keys.set(i, field);
                    }
                    else{
                        field = (Field) key;
                    }
                    fields.add(field);
                    if (i>0) sql.append(" AND ");
                    String colName = escape(field.getName());
                    sql.append(colName); sql.append("=?");
                }
            }
            else{

              //Since we don't have any keys, use the original where clause
                String where = new Parser(this.sqlString).getWhereString();
                if (where!=null){
                    sql.append(" WHERE "); sql.append(where);
                }


              //Find how many records will be affected by this update
                int numRecords;
                java.sql.ResultSet r2 = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName + (where==null? "" : " WHERE " + where));
                try{
                    numRecords = r2.getInt(1);
                }
                catch(Exception e){
                    try{
                        r2.first(); //SQLServer needs this!
                        numRecords = r2.getInt(1);
                    }
                    catch(Exception ex){
                        numRecords = Integer.MAX_VALUE;
                    }
                }
                r2.close();


              //Warn user that there might be a problem with the update
                if (numRecords>1){
                    StringBuffer msg = new StringBuffer();
                    msg.append("WARNING: Updating " + tableName + " table without a unique key.\r\n");
                    msg.append("Multiple rows may be affected with this update.\r\n");
                    try{ int x = 1/0; } catch(Exception e){
                        java.io.ByteArrayOutputStream bas = new java.io.ByteArrayOutputStream();
                        java.io.PrintStream s = new java.io.PrintStream(bas, true);
                        e.printStackTrace(s);
                        s.close();
                        boolean append = false;
                        for (String line : bas.toString().split("\n")){
                            if (append){
                                msg.append("\t");
                                msg.append(line.trim());
                                msg.append("\r\n");
                            }
                            if (!append && line.contains(this.getClass().getCanonicalName())) append = true;
                        }
                        System.err.println(msg);
                    }
                }
            }
        }


      //Get prepared statement
        java.sql.PreparedStatement stmt;
        java.sql.Connection Conn = Connection.getConnection();
        if (batchSize>1){
            if (batchedStatements==null) batchedStatements = new java.util.HashMap<String, java.sql.PreparedStatement>();
            stmt = batchedStatements.get(sql.toString());
            if (stmt==null){
                stmt = Conn.prepareStatement(sql.toString());
                batchedStatements.put(sql.toString(), stmt);
                Conn.setAutoCommit(false);
            }
        }
        else{
            try{
                stmt = Conn.prepareStatement(sql.toString(), java.sql.Statement.RETURN_GENERATED_KEYS);
            }
            catch(Exception e){ //not all databases support auto generated keys
                stmt = Conn.prepareStatement(sql.toString());
            }
        }


      //Set values using a prepared statement
        update(stmt, fields);


      //Update
        if (batchSize==1){
            try{
                stmt.executeUpdate();
            }
            catch(SQLException e){

                StringBuffer err = new StringBuffer();
                err.append("Error executing update:\n");
                err.append(sql.toString());
                err.append("\n");
                //err.append("\n  Values:\n");
                for (int i=0; i<fields.size(); i++) {
                    if (i>0) err.append("\n");
                    Field field = fields.get(i);
                    err.append("  - " + field.getName() + ": ");
                    String val = field.getValue().toString();
                    if (val!=null && val.length()>100) val = val.substring(0, 100) + "...";
                    err.append(val);
                }

                e.setNextException(new SQLException(err.toString()));
                throw e;
            }


            if (InsertOnUpdate){
                try{
                    java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        this.GeneratedKey = new Value(generatedKeys.getString(1));
                    }
                }
                catch(Exception e){
                    //not all databases support auto generated keys
                }
                InsertOnUpdate = false;
            }

        }
        else{
            stmt.addBatch();
            numBatches++;

            if (numBatches==batchSize){
                executeBatch();
            }
        }
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to set values in a PreparedStatement for inserting or updating
   *  records.
   */
    protected static void update(java.sql.PreparedStatement stmt, java.util.ArrayList<Field> fields) throws SQLException {

        try{ stmt.clearParameters(); }
        catch(Exception e){}


        int id = 1;
        for (int i=0; i<fields.size(); i++) {

            Field field = fields.get(i);
            String FieldType = field.Class.toLowerCase();
            if (FieldType.contains(".")) FieldType = FieldType.substring(FieldType.lastIndexOf(".")+1);
            Value FieldValue = field.getValue();


          //Special case for SQL Functions
            if (FieldValue.toObject() instanceof Function){
                Function function = (Function) FieldValue.toObject();
                if (function.hasValues()){
                    for (Object obj : function.getValues()){
                        stmt.setObject(id, obj);
                        id++;
                    }
                }
                else{
                    //Do nothing!
                }
                continue; //Prevent the id from incrementing
            }


            if (FieldType.indexOf("string") >= 0)
            stmt.setString(id, FieldValue.toString());

            else if (FieldType.indexOf("int")>=0){
                Integer val = FieldValue.toInteger();
                if (val==null) stmt.setNull(id, java.sql.Types.INTEGER);
                else stmt.setInt(id, val);
            }

            else if (FieldType.indexOf("short")>=0){
                Short val = FieldValue.toShort();
                if (val==null) stmt.setNull(id, java.sql.Types.SMALLINT);
                else stmt.setShort(id, val);
            }

            else if (FieldType.indexOf("long")>=0){
                Long val = FieldValue.toLong();
                if (val==null) stmt.setNull(id, java.sql.Types.BIGINT);
                else stmt.setLong(id, val);
            }

            else if (FieldType.indexOf("double")>=0){
                Double val = FieldValue.toDouble();
                if (val==null) stmt.setNull(id, java.sql.Types.DOUBLE);
                else stmt.setDouble(id, val);
            }

            else if (FieldType.indexOf("float")>=0){
                Float val = FieldValue.toFloat();
                if (val==null) stmt.setNull(id, java.sql.Types.FLOAT);
                else stmt.setFloat(id, val);
            }

            else if (FieldType.indexOf("bool")>=0){
                Boolean val = FieldValue.toBoolean();
                if (val==null) stmt.setNull(id, java.sql.Types.BIT);
                else stmt.setBoolean(id, val);
            }

            else if (FieldType.indexOf("decimal")>=0)
            stmt.setBigDecimal(id, FieldValue.toBigDecimal());

            else if (FieldType.indexOf("timestamp")>=0)
            stmt.setTimestamp(id, FieldValue.toTimeStamp());

            else if (FieldType.indexOf("date")>=0)
            stmt.setDate(id, new java.sql.Date(FieldValue.toDate().getTime()));

            else if (FieldType.indexOf("object")>=0)
            stmt.setObject(id, FieldValue.toObject());

            else if (FieldType.indexOf("map")>=0) //PostgreSQL HStore
            stmt.setObject(id, FieldValue.toString(), java.sql.Types.OTHER);

            else{
                //System.out.println(i + " " + field.getName() + " " + FieldType);
                stmt.setObject(id, FieldValue.toObject());
            }


            id++;
        }
    }


  //**************************************************************************
  //** escape
  //**************************************************************************

    private String escape(String colName){
        if (colName==null) return null;
        String[] keywords = Database.getReservedKeywords(Connection);
        colName = colName.trim();
        if (colName.contains(" ") && !colName.startsWith("[")){
            colName = "[" + colName + "]";
        }
        for (String keyWord : keywords){
            if (colName.equalsIgnoreCase(keyWord)){
                colName = "\"" + colName + "\"";
                break;
            }
        }
        return colName;
    }


  //**************************************************************************
  //** getQ
  //**************************************************************************
  /** Returns an SQL fragment used to generate prepared statements. Typically,
   *  this method simply returns a "?". However, if the value for the field
   *  contains a function, or contains a spatial data type, additional
   *  processing is required to generate a valid SQL statement. Note that
   *  this method will update the Class and Value attributes for the Field
   *  if the field value is a Geometry type.
   */
    private String getQ(Field field){

        if (field==null || field.getValue().isNull()) return "?";


      //Find out what kind of data we're dealing with
        Object value = field.getValue().toObject();


      //Special case for SQL Functions
        if (value instanceof Function){
            Function function = (Function) value;
            return function.getFunction();
        }



      //Special case for geometry types
        java.lang.Package _package = value.getClass().getPackage();
        String packageName = _package==null ? "" : _package.getName();
        if (packageName.startsWith("javaxt.geospatial.geometry")){
            String STGeomFromText = getSTGeomFromText(field, Connection);
            field.Value = new Value(value.toString());
            field.Class = "java.lang.String";
            return STGeomFromText + "(?,4326)";
        }
        else if (packageName.startsWith("com.vividsolutions.jts.geom") ||
            packageName.startsWith("org.locationtech.jts.geom")){
            String STGeomFromText = getSTGeomFromText(field, Connection);
            field.Value = new Value(value.toString());
            field.Class = "java.lang.String";
            int srid = 4326; //getSRID();
            try{
                java.lang.reflect.Method method = value.getClass().getMethod("getSRID");
                if (method!=null){
                    Object obj = method.invoke(value, null);
                    if (obj!=null){
                        srid = (Integer) obj;
                        if (srid==0) srid = 4326;
                    }
                }
            }
            catch(Exception e){
            }
            return STGeomFromText + "(?," + srid + ")";
        }


        return "?";
    }


  //**************************************************************************
  //** getSTGeomFromText
  //**************************************************************************
  /** Returns a vendor-specific STGeomFromText fragment for inserting/updating
   *  geometry data.
   */
    protected static String getSTGeomFromText(Field field, Connection conn){
        javaxt.sql.Driver driver = conn.getDatabase().getDriver();
        if (driver.equals("SQLServer")){
            String geo = field.Class.toLowerCase();
            if (!geo.equals("geometry") && !geo.equals("geography")){
                geo = null;
                try{
                    Recordset rs = new Recordset();
                    rs.open("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_NAME='" + field.getTable() + "' AND COLUMN_NAME='" + field.getName() + "'",
                    conn);
                    geo = rs.getValue(0).toString();
                    rs.close();
                }
                catch(SQLException e){
                    //e.printStackTrace();
                }
                if (geo==null) geo = "geometry";
                else geo = geo.toLowerCase();
            }
            return geo + "::STGeomFromText"; //geometry vs geography
        }
        else if (driver.equals("DB2")){
            return "db2GSE.ST_GeomFromText";
        }

        return "ST_GeomFromText"; //PostgreSQL
    }



    private int numBatches=0;
    private int batchSize=1;
    private java.util.HashMap<String, java.sql.PreparedStatement> batchedStatements;


  //**************************************************************************
  //** setBatchSize
  //**************************************************************************
  /** Used to set the number of records to insert or update in a batch. By
   *  default, this value is set to 1 so that records are inserted/updated one
   *  at a time. By setting a larger number, more records are inserted at a
   *  time which can significantly improve performance.
   */
    public void setBatchSize(int batchSize){
        if (batchSize>0) this.batchSize = batchSize;
    }


  //**************************************************************************
  //** getBatchSize
  //**************************************************************************
  /** Returns the number of records that will be inserted or updated in a
   *  batch. By default, this value is set to 1 so that records are inserted/
   *  updated one at a time.
   */
    public int getBatchSize(){
        return batchSize;
    }


  //**************************************************************************
  //** setUpdateKeys
  //**************************************************************************
  /** Used to identify unique fields in a table for updates. Typically, this
   *  is the primary key(s) or a column with a unique constraint. In most
   *  cases, you do not need to call this method when updating records.
   *  Instead, an attempt is made to find the primary keys for the table using
   *  recordset metadata. However, this lookup may slow things down a bit and
   *  doesn't work on tables that don't have a primary key.
   *  @param keys List of column names (String) or javaxt.sql.Field
   */
    public void setUpdateKeys(Object...keys){
        for (Object key : keys){
            if (key instanceof String){
                if (isOpen()){
                    this.keys.add(getField((String) key));
                }
                else{
                    this.keys.add(key);
                }
            }
            else if (key instanceof Field){
                this.keys.add(key);
            }
            else{
                throw new IllegalArgumentException("Unsupported key type: " + key.getClass());
            }
        }
    }


  //**************************************************************************
  //** setUpdateKey
  //**************************************************************************
  /** Used to identify a unique field in a table for updates. Typically, this
   *  is the primary key or a column with a unique constraint. Please see
   *  setUpdateKeys() for more information.
   *  @param key Column name (String) or javaxt.sql.Field
   */
    public void setUpdateKey(Object key){
        setUpdateKeys(key);
    }


  //**************************************************************************
  //** executeBatch
  //**************************************************************************
  /** Returns the total number of rows that were updated.
   */
    private int executeBatch() throws SQLException {
        if (batchedStatements==null) return 0;
        int ttl = 0;
        java.util.Iterator<String> it = batchedStatements.keySet().iterator();
        while (it.hasNext()){
            java.sql.PreparedStatement stmt = batchedStatements.get(it.next());

            int[] rowsUpdated = stmt.executeBatch();
            if (rowsUpdated.length>0) ttl+=rowsUpdated.length;

            java.sql.Connection Conn = Connection.getConnection();
            if (Conn.getAutoCommit()==false){
                Conn.commit();
            }
        }
        batchedStatements.clear();
        numBatches = 0;
        return ttl;
    }


  //**************************************************************************
  //** getGeneratedKey
  //**************************************************************************
  /** Returns an auto-generated key created after inserting a record in the
   *  database. If this Statement object did not generate any keys, an empty
   *  Value object is returned.
   */
    public Value getGeneratedKey(){
        return GeneratedKey;
    }


  //**************************************************************************
  //** getFields
  //**************************************************************************
  /** Used to retrieve the an array of fields in the current record.
   */
    public Field[] getFields(){
        Field[] arr = new Field[Fields.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = Fields[i].clone();
            arr[i].Value = Fields[i].Value;
        }
        return arr;
    }


  //**************************************************************************
  //** getField
  //**************************************************************************
  /** Returns a specific field in the array of fields. Returns null if the
   *  field name is not found.
   */
    public Field getField(String FieldName){
        if (Fields==null || Fields.length==0) return null;

        if (FieldName==null) return null;
        FieldName = FieldName.trim();
        if (FieldName.length()==0) return null;

        String[] arr = FieldName.split("\\.");

        for (Field field : Fields) {

            String fieldName = field.getName();
            if (fieldName==null) continue;

            String tableName = field.getTable()==null? "" : field.getTable();
            String schemaName = field.getSchema()==null? "" : field.getSchema();

            if (arr.length==3){
                 if (fieldName.equalsIgnoreCase(arr[2]) && tableName.equalsIgnoreCase(arr[1]) && schemaName.equalsIgnoreCase(arr[0])){
                     return field;
                 }
            }
            else if (arr.length==2){
                if (fieldName.equalsIgnoreCase(arr[1]) && tableName.equalsIgnoreCase(arr[0])){
                     return field;
                }
            }
            else if (arr.length==1){
                if (fieldName.equalsIgnoreCase(arr[0])) return field;
            }
        }

        return null;
    }


  //**************************************************************************
  //** getField
  //**************************************************************************
  /** Returns a specific field in the array of fields. Returns null if the
   *  index is out of range.
   */
    public Field getField(int i){
        if (Fields!=null && i<Fields.length){
            return Fields[i];
        }
        else{
            return null;
        }
    }

  //**************************************************************************
  //** getValue
  //**************************************************************************
  /** Returns the Value associated with a given field. Note the if the field
   *  doesn't exist in the result set, the method will return still return a
   *  Value. You can use the isNull() method on the Value to determine whether
   *  the value is null.
   */
    public Value getValue(String FieldName){
        Field field = getField(FieldName);
        if (field!=null) return field.getValue();
        return new Value(null);
    }


  //**************************************************************************
  //** getValue
  //**************************************************************************
  /** Returns the Value associated with a given field. Note the if the field
   *  doesn't exist in the result set, the method will return still return a
   *  Value. You can use the isNull() method on the Value to determine whether
   *  the value is null.
   */
    public Value getValue(int i){
        if (Fields!=null && i<Fields.length){
            return Fields[i].getValue();
        }
        return new Value(null);
    }


  //**************************************************************************
  //** isDirty
  //**************************************************************************
  /** Returns true if any of the fields have been modified. You can find which
   *  field has been modified using the Field.isDirty() method. Example:
   <pre>
    if (rs.isDirty()){
        for (javaxt.sql.Field field : rs.getFields()){
            if (field.isDirty()){
                String val = field.getValue().toString();
                System.out.println(field.getName() + ": " + val);
            }
        }
    }
   </pre>
   */
    public boolean isDirty(){
        for (Field field : Fields){
            if (field.isDirty()) return true;
        }
        return false;
    }


  //**************************************************************************
  //** SetValue
  //**************************************************************************

    public void setValue(String FieldName, Value FieldValue){
        if (State==1){
            for (int i=0; i<Fields.length; i++ ) {
                String name = Fields[i].getName();
                if (name!=null){
                    if (name.equalsIgnoreCase(FieldName)){
                        if (FieldValue==null) FieldValue = new Value(null);

                       //Update the Field Value as needed.
                         if (!Fields[i].getValue().equals(FieldValue)){
                             Fields[i].Value = FieldValue;
                             Fields[i].RequiresUpdate = true;
                         }
                         break;
                    }
                }
            }
        }
    }


  //**************************************************************************
  //** SetValue
  //**************************************************************************
  /** Set Value with an Object value.  */

    public void setValue(String FieldName, Object FieldValue){
        setValue(FieldName, new Value(FieldValue));
    }

  //**************************************************************************
  //** SetValue
  //**************************************************************************
  /**  Set Value with a Boolean value */

    public void setValue(String FieldName, boolean FieldValue){
        setValue(FieldName, new Value(FieldValue));
    }

  //**************************************************************************
  //** SetValue
  //**************************************************************************
  /**  Set Value with a Long value */

    public void setValue(String FieldName, long FieldValue){
        setValue(FieldName, new Value(FieldValue));
    }

  //**************************************************************************
  //** SetValue
  //**************************************************************************
  /**  Set Value with an Integer value */

    public void setValue(String FieldName, int FieldValue){
        setValue(FieldName, new Value(FieldValue));
    }

  //**************************************************************************
  //** SetValue
  //**************************************************************************
  /**  Set Value with a Double value */

    public void setValue(String FieldName, double FieldValue){
        setValue(FieldName, new Value(FieldValue));
    }

  //**************************************************************************
  //** SetValue
  //**************************************************************************
  /**  Set Value with a Short value */

    public void setValue(String FieldName, short FieldValue){
        setValue(FieldName, new Value(FieldValue));
    }


  //**************************************************************************
  //** hasNext
  //**************************************************************************
  /** Returns true if the recordset has more records.
   */
    public boolean hasNext(){
        return !EOF;
    }


  //**************************************************************************
  //** MoveNext
  //**************************************************************************
  /** Move the cursor to the next record in the recordset. */

    public boolean moveNext(){

        if (EOF == true) return false;

        if (x>=MaxRecords-1) {
            EOF = true;
            return false;
        }
        else{
            try{
                if (rs.next()){
                    for (int i=1; i<=Fields.length; i++) {
                        Field Field = Fields[i-1];
                        Field.Value = new Value(rs.getObject(i));
                        Field.RequiresUpdate = false;
                    }
                    x+=1;
                    return true;
                }
                else{
                    EOF = true;
                    return false;
                }
            }
            catch(Exception e){
                EOF = true;
                return false;
                //System.out.println("ERROR MoveNext: " + e.toString());
            }
        }
        //return false;
    }


  //**************************************************************************
  //** Move
  //**************************************************************************
  /**  Moves the cursor to n-number of rows in the database. Typically this
   *   method is called before iterating through a recordset.
   */
    public void move(int numRecords){

        boolean tryAgain = false;

        //Scroll to record using the standard absolute() method
        //Does NOT work with rs.TYPE_FORWARD_ONLY cursors

        try{
            rs.absolute(numRecords);
            x+=numRecords;
        }
        catch(Exception e){
            tryAgain = true;
            //System.err.println("ERROR Move: " + e.toString());
        }


        //Scroll to record using an iterator
        //Workaround for rs.TYPE_FORWARD_ONLY cursors

        try{
            if (tryAgain){
                int rowPosition = rs.getRow();
                while ( rs.getRow() < (numRecords+rowPosition)){
                    if (rs.next()){
                        x++;
                    }
                    else{
                        EOF = true;
                        break;
                    }
                }
            }
        }
        catch(Exception e){}


      //Update Field
        try{
            for (int i=1; i<=Fields.length; i++) {
                 Field Field = Fields[i-1];
                 Field.Value = new Value(rs.getObject(i));
                 Field.RequiresUpdate = false;
            }
        }
        catch(Exception e){}

    }


  //**************************************************************************
  //** updateFields
  //**************************************************************************
  /** Used to populate the Table and Schema attributes for each Field in the
   *  Fields Array.
   */
    private void updateFields(){

        if (Fields==null) return;


     //Check whether any of the fields are missing table or schema information
        boolean updateFields = false;
        for (Field field : Fields){

          //Check the field name. If it's missing, then the field is probably
          //derived from a function. In some cases, it may be trivial to find
          //the column name (e.g. COUNT, SUM, MIN, MAX, etc). In other cases,
          //it is not so simple (e.g. "SELECT (FIRSTNAME || ' ' || LASTNAME)").
          //In any event, this function cannot currently find table or schema
          //for a field without a name.
            String fieldName = field.getName();
            if (fieldName==null) continue;

            if (field.getTable()==null){
                updateFields = true;
                break;
            }

            if (field.getSchema()==null){
                updateFields = true;
                break;
            }
        }
        if (!updateFields) return;


      //Get selected tables from the SQL
        String[] selectedTables = new Parser(sqlString).getTables();


      //Match selected tables to tables found in this database
        java.util.ArrayList<Table> tables = new java.util.ArrayList<Table>();
        if (Tables==null) Tables = Database.getTables(Connection);
        for (String selectedTable : selectedTables){
            String tableName;
            String schemaName;
            int idx = selectedTable.indexOf(".");
            if (idx>-1){
                tableName = selectedTable.substring(idx+1);
                schemaName = selectedTable.substring(0, idx);
            }
            else{
                tableName = selectedTable;
                schemaName = null;
            }

            for (Table table : Tables){
                if (tableName.equalsIgnoreCase(table.getName())){

                    if (schemaName==null){
                        tables.add(table);
                        break;
                    }
                    else{
                        if (schemaName.equalsIgnoreCase(table.getSchema())){
                            tables.add(table);
                            break;
                        }
                    }
                }
            }
        }




      //Iterate through all the fields and update the Table and Schema attributes
        for (Field field : Fields){

            if (field.getTable()==null){

              //Update Table and Schema
                Column[] columns = getColumns(field, tables);
                if (columns!=null){
                    Column column = columns[0]; //<-- Need to implement logic to
                    field.setTableName(column.getTable().getName());
                    field.setSchemaName(column.getTable().getSchema());
                }
            }

            if (field.getSchema()==null) {

              //Update Schema
                for (Table table : tables){
                    if (table.getName().equalsIgnoreCase(field.getTable())){
                        field.setSchemaName(table.getSchema());
                        break;
                    }
                }
            }
        }

        tables.clear();
        tables = null;
    }


  //**************************************************************************
  //** getColumns
  //**************************************************************************
  /**  Used to find a column in the database that corresponds to a given field.
   *   This method is only used when a field/column's parent table is unknown.
   */
    private Column[] getColumns(Field field, java.util.ArrayList<Table> tables){

        java.util.ArrayList<Column> matches = new java.util.ArrayList<Column>();

        for (Table table : tables){
            for (Column column : table.getColumns()){
                if (column.getName().equalsIgnoreCase(field.getName())){
                    matches.add(column);
                }
            }
        }

        if (matches.isEmpty()) return null;
        if (matches.size()==1) return new Column[]{matches.get(0)};
        if (matches.size()>1){

            java.util.ArrayList<Column> columns = new java.util.ArrayList<Column>();
            for (Column column : matches){
                if (column.getType().equalsIgnoreCase(field.Type)){
                    columns.add(column);
                }
            }

            if (columns.isEmpty()) return null;
            else return columns.toArray(new Column[columns.size()]);
        }
        return null;
    }


  //**************************************************************************
  //** getRecordCount
  //**************************************************************************
  /** Used to retrieve the total record count. Note that this method may be
   *  slow.
   */
    public long getRecordCount(){

        try{
            int currRow = rs.getRow(); rs.last(); int size = rs.getRow();
            rs.absolute(currRow); // go back to the old row
            return size;
        }
        catch(Exception e){

            Long numRecords = null;

            String sql = new Parser(sqlString).setSelect("count(*)");
            Recordset rs = new Recordset();
            try{
                rs.open(sql, Connection);
                numRecords = rs.getValue(0).toLong();
                rs.close();
            }
            catch(SQLException ex){
                rs.close();
            }

            if (numRecords!=null) return numRecords;
            else return -1L;
        }
    }


//  //**************************************************************************
//  //** Finalize
//  //**************************************************************************
//  /** Method *should* be called by Java garbage collector once this class is
//   *  disposed.
//   */
//    protected void finalize() throws Throwable {
//       close();
//       super.finalize();
//    }
}
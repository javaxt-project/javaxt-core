package javaxt.sql;
import java.sql.SQLException;

//******************************************************************************
//**  Recordset Class
//*****************************************************************************/
/**
 *    Used to query, update, and create new records in a database. It is
 *    intended to simplify many of the complexities and nuances associated
 *    the standard Java/JDBC Resultset. The class is modeled somewhat after
 *    the old Microsoft ADODB Resultset class.
 *
 ******************************************************************************/

public class Recordset {
    
    private java.sql.ResultSet rs = null;
    private java.sql.Connection Conn = null;
    private java.sql.Statement stmt = null;
    private int x;
    private String sqlString = null;
    //private Parser sqlParser = null;
    
    private Connection Connection = null;


    private Value GeneratedKey;




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
    * Returns the total ellapsed time between open and close operations. Units 
    * are in milliseconds
    */
    public long EllapsedTime;
    
   /**
    * Returns the ellapsed time it took to retrieve additional metadata not 
    * correctly supported by the jdbc driver. Units are in milliseconds.
    */
    public long MetadataQueryTime;
    private long startTime, endTime;
    

  //**************************************************************************
  //** isOpen
  //**************************************************************************
  /** Used to determing whether the recordset is open. */
    
    public boolean isOpen(){
        if (State!=0) {
            //return !rs.isClosed();

            String[] arr = System.getProperty("java.version").split("\\.");
            if (Integer.valueOf(arr[0]).intValue()==1 && Integer.valueOf(arr[1]).intValue()<6) return false;
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
    
    private boolean isReadOnly = true;
    
    public boolean isReadOnly(){
        return isReadOnly;
    }


    
  //**************************************************************************
  //** Creates a new instance of Recordset
  //**************************************************************************
  
    public Recordset(){}
    

    
  //**************************************************************************
  //** Open
  //**************************************************************************
  /**  Opens a database element that gives you access to records in a table
   *   (e.g. the results of a query).
   *
   *   @param sql SQL Query
   *   @param conn An active connection to the database.
   */    
    public java.sql.ResultSet open(String sql, Connection conn) throws SQLException {
        return open(sql,conn,true);
    }
    
  //**************************************************************************
  //** Open
  //**************************************************************************
  /**  Opens a database element that gives you access to records in a table
   *   (e.g. the results of a query).
   *
   *   @param sqlString SQL Query
   *   @param Connection An active connection to the database.
   *   @param ReadOnly Set whether the database connection is r/o or r/w
   */
    public java.sql.ResultSet open(String sqlString, Connection Connection, boolean ReadOnly) throws SQLException {

        rs = null;
        stmt = null;
        State = 0;
        EOF = true;
        this.sqlString = sqlString;
        this.Connection = Connection;
        this.isReadOnly = ReadOnly;


        if (Connection==null) throw new java.sql.SQLException("Connection is null.");
        if (Connection.isClosed()) throw new java.sql.SQLException("Connection is closed.");


        startTime = java.util.Calendar.getInstance().getTimeInMillis();
        Conn = Connection.getConnection();


      //Wrap table and column names in quotes (Special Case for PostgreSQL)
        /*
        if (Connection.getDatabase().getDriver().equals("PostgreSQL")){
            try{
                Parser sqlParser = new Parser(sqlString);
                boolean wrapElements = false;
                String[] exposedElements = sqlParser.getExposedDataElements();
                for (int i=0; i<exposedElements.length; i++){
                    String element = exposedElements[i];
                    if (javaxt.utils.string.hasUpperCase(element)){
                        wrapElements = true;
                        break;
                    }
                }
                wrapElements = false;
                if (wrapElements){
                    sqlString = sqlParser.addQuotes();
                    //System.out.println(sqlString);
                }
            }
            catch(Exception e){
                System.out.println("WARNING: Failed to parse SQL");
                e.printStackTrace();
            }
        }
        */



      //Read-Only Connection
        if (ReadOnly){

            try{

              //Set AutoCommit to false when fetchSize is specified.
              //Otherwise it will fetch back all the records at once
                if (fetchSize!=null) Conn.setAutoCommit(false);

              //DB2 Connection
                if (Connection.getDatabase().getDriver().equals("DB2")){
                    stmt = Conn.createStatement(rs.TYPE_FORWARD_ONLY, rs.CONCUR_READ_ONLY);
                }

              //Default Connection
                else{
                    stmt = Conn.createStatement(rs.TYPE_SCROLL_INSENSITIVE, rs.CONCUR_READ_ONLY);
                }

                if (fetchSize!=null) stmt.setFetchSize(fetchSize);
                rs = stmt.executeQuery(sqlString);
                State = 1;
            }
            catch(Exception e){
                //System.out.println("ERROR Open RecordSet: " + e.toString());
            }
            if (State!=1){
                try{
                    if (fetchSize!=null) Conn.setAutoCommit(false);
                    stmt = Conn.createStatement();
                    if (fetchSize!=null) stmt.setFetchSize(fetchSize);
                    rs = stmt.executeQuery(sqlString);
                    State = 1;

                }
                catch(SQLException e){
                    //System.out.println("ERROR Open RecordSet: " + e.toString());
                    throw e;
                }
            }

        }

      //Read-Write Connection
        else{
            try{

                Driver driver = Connection.getDatabase().getDriver();

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
                    //System.out.println("WARNING: DB2 JDBC Driver does not currently support the insertRow() method. " +
                    //                   "Will attempt to execute an SQL insert statement instead.");
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
                throw e;
            }


        }


        endTime = java.util.Calendar.getInstance().getTimeInMillis();
        QueryResponseTime = endTime-startTime;

        try{
            
          //Create Fields 
            java.sql.ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            Fields = new Field[cols];
            for (int i=1; i<=cols; i++) {
                 Field Field = new Field();
                 Field.Name = rsmd.getColumnName(i);
                 
                 
               //Sybase fails on these calls
                 try{ Field.Table = rsmd.getTableName(i);   } catch(Exception e){}
                 try{ Field.Schema = rsmd.getSchemaName(i); } catch(Exception e){}
                 try{ Field.Type = rsmd.getColumnTypeName(i);   } catch(Exception e){}
                 try{ Field.Class = rsmd.getColumnClassName(i); } catch(Exception e){}

                 if (Field.Table!=null){
                     if (Field.Table.trim().length()==0) Field.Table = null;
                 }
                 if (Field.Schema!=null){
                     if (Field.Schema.trim().length()==0) Field.Schema = null;
                 }
                 if (Field.Type!=null){
                     if (Field.Type.trim().length()==0) Field.Type = null;
                 }
                 if (Field.Class!=null){
                     if (Field.Class.trim().length()==0) Field.Class = null;
                 }
                  
                 //Field.Value = getVal(rs.getString(i));
                 //System.out.println(Field.Schema + "." + Field.Table + "." + Field.Name);
                 
                 Fields[i-1] = Field;                 
            }
            
            x=-1;
            
            if (rs!=null){
                if (rs.next()){
                    
                    EOF = false;
                    for (int i=1; i<=cols; i++) {
                         Fields[i-1].Value = new Value(rs.getString(i));
                    }
                    x+=1;
                }

              //Get Additional Metadata
                long mStart = java.util.Calendar.getInstance().getTimeInMillis();
                //RecordCount = getRecordCount();
                updateFields();
                long mEnd = java.util.Calendar.getInstance().getTimeInMillis();
                MetadataQueryTime = mEnd-mStart;
                //System.out.println(MetadataQueryTime);
            }
            
            
        }
        catch(java.sql.SQLException e){
            //e.printStackTrace();
            //throw e;
        }
        
        return rs;
    }
    
    
  //**************************************************************************
  //** Close
  //**************************************************************************  
  /**  Closes a Recordset freeing up database and jdbc resources.   */
    
    public void close(){
        if (State==1){
            try{
                rs.close();
                stmt.close();
                State = 0;
            }
            catch(java.sql.SQLException e){
            }
        }
        endTime = java.util.Calendar.getInstance().getTimeInMillis();
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
  /** Used to explicitely commit an sql statement. May be useful for bulk
   *  update and update statements, depending on the underlying DBMS.
   */
    public void commit(){
        try{
            //stmt.executeQuery("COMMIT");
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
  /**  Used to prepare the driver to insert new records to the database. Used
   *   in conjunction with the update method.
   */    
    public void addNew(){
        if (State==1){
            try{

              //DB2 and Xerial's SQLite JDBC driver don't seem to support
              //moveToInsertRow or insertRow so we'll have to create an SQL
              //Insert statement instead. To do so, we'll use the Fields array
              //to store new values...
                Driver driver = Connection.getDatabase().getDriver();
                if (driver.equals("DB2") || driver.equals("SQLite")){
                }
                else{ //for all other cases...
                    rs.moveToInsertRow();
                }
                InsertOnUpdate = true;
                for (int i=1; i<=Fields.length; i++) {
                    Field Field = Fields[i-1];
                    Field.Value = null;
                    Field.RequiresUpdate = false;
                }
            }
            catch(Exception e){
                System.out.println("AddNew ERROR: " + e.toString());
            }
        }
    }
    
    
  //**************************************************************************
  //** Update
  //**************************************************************************  
  /** Used to add or update a record in the recordset. */

    public void update() throws java.sql.SQLException {
        if (State==1){
            try{                
                
              //Determine whether to use a prepared statement or a resultset to add/update records
                boolean usePreparedStatement = false;
                Driver driver = Connection.getDatabase().getDriver();
                if (driver.equals("DB2") || driver.equals("SQLite") ){
                    usePreparedStatement = true;
                }
                else if (driver.equals("PostgreSQL")){
                  //PostGIS doesn't support JDBC inserts either...
                    usePreparedStatement = true;
                    for (int i=0; i<Fields.length; i++ ) { 
                         Object value = null;
                         if (Fields[i].Value!=null) value = Fields[i].Value.toObject();
                         if (value!=null){
                            //Special case for PostGIS geometry types...
                            if (value.getClass().getPackage().getName().startsWith("javaxt.geospatial.geometry")){                                
                                Fields[i].Value = new Value(getGeometry(value));
                                break;
                            }
                         }
                    }
                    
                }
                
                
              //Set/update records in the resultset
                if (usePreparedStatement==false){

                    for (int i=0; i<Fields.length; i++ ) {
                        String FieldName = Fields[i].Name;
                        String FieldType = Fields[i].Class; 
                        Value FieldValue = Fields[i].Value;
                        
                        if (Fields[i].RequiresUpdate){

                            if (FieldType.indexOf("String")>=0)
                            rs.updateString(FieldName, FieldValue.toString());

                            if (FieldType.indexOf("Integer")>=0)
                            rs.updateInt(FieldName, FieldValue.toInteger());

                            if (FieldType.indexOf("Short")>=0)
                            rs.updateShort(FieldName, FieldValue.toShort());

                            if (FieldType.indexOf("Long")>=0)
                            rs.updateLong(FieldName, FieldValue.toLong());

                            if (FieldType.indexOf("Double")>=0)
                            rs.updateDouble(FieldName, FieldValue.toDouble());

                            if (FieldType.indexOf("Timestamp")>=0)
                            rs.updateTimestamp(FieldName, FieldValue.toTimeStamp());

                            if (FieldType.indexOf("Date")>=0)
                            rs.updateDate(FieldName, new java.sql.Date(FieldValue.toDate().getTime()));
                            
                            if (FieldType.indexOf("Bool")>=0)
                            rs.updateBoolean(FieldName, FieldValue.toBoolean() );

                            if (FieldType.indexOf("Object")>=0)
                            rs.updateObject(FieldName, FieldValue.toObject());

                            if (FieldValue!=null){
                                if (FieldValue.toObject().getClass().getPackage().getName().startsWith("javaxt.geospatial.geometry")){
                                    rs.updateObject(FieldName, getGeometry(FieldValue));
                                }
                            }
                        
                        }

                    }

                    rs.updateRow();
                }
                else{

                    if (!isDirty()) return;

                    java.util.ArrayList<String> cols = new java.util.ArrayList<String>();
                    for (int i=0; i<Fields.length; i++){
                        if (Fields[i].RequiresUpdate){
                            cols.add(Fields[i].getName());
                        }
                    }

                    int numUpdates = cols.size();

                    StringBuffer sql = new StringBuffer();

                    if (InsertOnUpdate){
                        sql.append("INSERT INTO " + Fields[0].getTable() + " (");
                        for (int i=0; i<numUpdates; i++){
                            sql.append(cols.get(i));
                            if (numUpdates>1 && i<numUpdates-1){
                                sql.append(",");
                            }
                        }
                        sql.append(") VALUES (");
                        for (int i=0; i<numUpdates; i++){
                            sql.append("?");
                            if (numUpdates>1 && i<numUpdates-1){
                                sql.append(",");
                            }
                        }
                        sql.append(")");
                    }
                    else{
                        sql.append("UPDATE " + Fields[0].getTable() + " SET ");
                        for (int i=0; i<numUpdates; i++){
                            sql.append(cols.get(i));
                            sql.append("=?");
                            if (numUpdates>1 && i<numUpdates-1){
                                sql.append(", ");
                            }
                        }
                        String where = new Parser(this.sqlString).getWhereString();
                        if (where!=null){
                            sql.append(" WHERE "); sql.append(where);
                        }
                    }

                    java.sql.PreparedStatement stmt = Conn.prepareStatement(sql.toString(), java.sql.Statement.RETURN_GENERATED_KEYS);
                    int id = 1;
                    for (int i=0; i<Fields.length; i++ ) {
                        String FieldType = Fields[i].Class;
                        Value FieldValue = Fields[i].Value;


                        if (Fields[i].RequiresUpdate){
                            if (FieldType.indexOf("String")>=0)
                            stmt.setString(id, FieldValue.toString());

                            if (FieldType.indexOf("Integer")>=0)
                            stmt.setInt(id, FieldValue.toInteger());

                            if (FieldType.indexOf("Short")>=0)
                            stmt.setShort(id, FieldValue.toShort());

                            if (FieldType.indexOf("Long")>=0)
                            stmt.setLong(id, FieldValue.toLong());

                            if (FieldType.indexOf("Double")>=0)
                            stmt.setDouble(id, FieldValue.toDouble());

                            if (FieldType.indexOf("Timestamp")>=0)
                            stmt.setTimestamp(id, FieldValue.toTimeStamp());

                            if (FieldType.indexOf("Date")>=0)
                            stmt.setDate(id, new java.sql.Date(FieldValue.toDate().getTime()));

                            if (FieldType.indexOf("Bool")>=0)
                            stmt.setBoolean(id, FieldValue.toBoolean() );

                            if (FieldType.indexOf("Object")>=0)
                            stmt.setObject(id, FieldValue.toObject() );

                            if (FieldValue!=null){
                                try{
                                    if (FieldValue.toObject().getClass().getPackage().getName().startsWith("javaxt.geospatial.geometry")){
                                        stmt.setObject(id, getGeometry(FieldValue));
                                    }
                                }
                                catch(Exception e){}
                            }

                            id++;
                        }
                    }
                    stmt.executeUpdate();
                    


                    if (InsertOnUpdate){
                        java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            this.GeneratedKey = new Value(generatedKeys.getString(1));
                        }

                        InsertOnUpdate = false;
                    }

                    
                    //stmt.close();
                }
            }
            catch(java.sql.SQLException e){
                throw e;
            }
        }
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
  //** getField
  //************************************************************************** 
  /** Returns a specific field in the array of fields. Returns null if the
   *  field name is not found.
   */
    public Field getField(String FieldName){
        if (Fields!=null){
            
            String[] arr = FieldName.split("\\.");
            FieldName = arr[arr.length-1];
            
            for (int i=0; i<Fields.length; i++ ) {
                
                 if (arr.length==3){
                     if (Fields[i].Name.equalsIgnoreCase(FieldName) && Fields[i].Table.equalsIgnoreCase(arr[1]) && Fields[i].Schema.equalsIgnoreCase(arr[0])){
                         return Fields[i];
                     }
                 }
                 else if (arr.length==2){
                     if (Fields[i].Name.equalsIgnoreCase(FieldName) && Fields[i].Table.equalsIgnoreCase(arr[0])){
                         return Fields[i];
                     }
                 }
                 else{
                     if (Fields[i].Name.equalsIgnoreCase(FieldName)){
                         return Fields[i];
                     }
                 }
                 
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
            return Fields[i].Value;
        }
        return new Value(null);
    }


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
                 String name = Fields[i].Name;
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
                        Field.Value = new Value(rs.getString(i));
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
                System.out.println("ERROR MoveNext: " + e.toString());
            }
        }        
        return false;
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
                 Field.Value = new Value(rs.getString(i));
                 Field.RequiresUpdate = false;
            }   
        }
        catch(Exception e){}
        
    }
    
    

    

  //**************************************************************************
  //** updateFields
  //**************************************************************************
  /**  Used to populate the Table and Column attributes for each Field in the 
   *   Fields Array.
   */    
    private void updateFields(){

        if (Fields==null) return;
        

      //Iterate through all the fields and update any 
        java.util.Vector<Table> tables = null;
        for (int i=0; i<Fields.length; i++){
             if (Fields[i].Table==null || Fields[i].Schema==null){
                 
                 
               //Get list of tables found in this database
                 if (tables==null){
                     tables = new java.util.Vector<Table>();
                     String[] selectedTables = new Parser(this.sqlString).getTables();
                     for (Table table : Connection.getDatabase().getTables()){

                         for (String selectedTable : selectedTables){
                             if (selectedTable.contains(".")) selectedTable = selectedTable.substring(selectedTable.indexOf("."));
                             if (selectedTable.equalsIgnoreCase(table.getName())){
                                 tables.add(table);
                             }
                         }
                     }
                 }
                 
               
                 if (Fields[i].Table==null){                     
                     
                   //Update Table and Schema
                     Column[] columns = getColumns(Fields[i], tables);
                     if (columns!=null){
                         Column column = columns[0]; //<-- Need to implement logic to 
                         Fields[i].Table = column.getTable().getName();
                         Fields[i].Schema = column.getTable().getSchema();
                     }
                 }
                 else{
                     
                   //Update Schema
                     java.util.Iterator<Table> it = tables.iterator();
                     while (it.hasNext()){
                        Table table = it.next();
                        if (table.getName().equalsIgnoreCase(Fields[i].Table)){
                            Fields[i].Schema = table.getSchema();
                            break;
                         }
                     }
                     
                     
                 }

             }
        }

    }
    
  //**************************************************************************
  //** getColumns
  //**************************************************************************
  /**  Used to find a column in the database that corresponds to a given field.
   *   This method is only used when a field/column's parent table is unknown.
   */
    private Column[] getColumns(Field field, java.util.Vector<Table> tables){

        java.util.Vector matches = new java.util.Vector();

        java.util.Iterator<Table> it = tables.iterator();
        while (it.hasNext()){
            Table table = it.next();
             for (Column column : table.getColumns()){
                  if (column.getName().equalsIgnoreCase(field.Name)){
                      matches.add(column);
                  }
             }
        }

        if (matches.size()==0) return null;
        if (matches.size()==1) return new Column[]{(Column) matches.get(0)};
        if (matches.size()>1){

            java.util.Vector<Column> columns = new java.util.Vector<Column>();
            for (int i=0; i<matches.size(); i++){
                 Column column = (Column) matches.get(i);
                 if (column.getType().equalsIgnoreCase(field.Type)){
                     columns.add(column);
                 }
            }

            if (columns.size()==0) return null;
            else{
                Column[] arr = new Column[columns.size()];
                for (int i=0; i<arr.length; i++){
                    arr[i] = columns.get(i);
                }
                return arr;
            }

        }
        return null;
    }



    
  //**************************************************************************
  //** getRecordCount
  //**************************************************************************
  /** Used to retrieve the total record count. Note that this method may be
   *  slow.
   */    
    public int getRecordCount(){
        
        try{
            int currRow = rs.getRow(); rs.last(); int size = rs.getRow();
            rs.absolute(currRow); // go back to the old row
            return size;
        }
        catch(Exception e){

            Integer numRecords = null;

            String sql = new Parser(sqlString).setSelect("count(*)");
            Recordset rs = new Recordset();
            try{
                rs.open(sql, Connection);
                numRecords = rs.getValue(0).toInteger();
                rs.close();
            }
            catch(SQLException ex){
                rs.close();
            }

            if (numRecords!=null) return numRecords;
            else return -1;
        }
    }    
    
    

  //**************************************************************************
  //** hasNext
  //**************************************************************************
  /**  Returns true if the recordset has more elements.
   */
    
    public boolean hasNext(){
        return !EOF;
    }

    
  //**************************************************************************
  //** getFields
  //**************************************************************************
  /**  Used to retrieve the an array of fields (columns) found in the current 
   *   row.
   */
    
    public Field[] getFields(){
        return Fields;
    }
    
    
    
    private String getGeometry(Object FieldValue){
        
      //Get geometry type
        String geometryType = FieldValue.getClass().getCanonicalName().toString();
        geometryType = geometryType.substring(geometryType.lastIndexOf(".")+1).trim().toUpperCase();


        Driver driver = Connection.getDatabase().getDriver();
        
        if (driver.equals("PostgreSQL")){
            return "ST_GeomFromText('" + FieldValue.toString() + "', 4326)";
        }
        else if (driver.equals(Database.SQLServer)){
            return "STGeomFromText('" + FieldValue.toString() + "', 4326)";
        }
        else if (Connection.getDatabase().getDriver().equals("DB2")){
            if (geometryType.equals("LINE")){
                String line = FieldValue.toString().toUpperCase().replace("LINESTRING","LINE");
                return "db2GSE.ST_LINE('" + line + "', 2000000000)";
            }
            else{
                return "db2GSE.ST_" + geometryType + "('" + FieldValue.toString() + "', 2000000000)";
            }
        }
        
        
        return null;
        
    }



  //**************************************************************************
  //** Finalize
  //**************************************************************************
  /** Method *should* be called by Java garbage collector once this class is
   *  disposed.
   */
    protected void finalize() throws Throwable {
       close();
       super.finalize();
    }
}
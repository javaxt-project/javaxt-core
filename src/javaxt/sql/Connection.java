package javaxt.sql;
import java.sql.*;

//******************************************************************************
//**  Connection Class
//******************************************************************************
/**
 *   Used to connect to a database via JDBC
 *
 ******************************************************************************/

public class Connection {    

    
    private java.sql.Connection Conn = null;
    private long Speed;
    private Database database;


  //**************************************************************************
  //** Creates a new instance of ADODB Connection
  //**************************************************************************
    
    public Connection(){
    }
    

        
  //**************************************************************************
  //** isOpen
  //**************************************************************************
  /** Used to determine whether the connection is open. */
    
    public boolean isOpen(){
        return !isClosed();
    } 

  //**************************************************************************
  //** isClosed
  //**************************************************************************
  /** Used to determine whether the connection is closed. */

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
  /** Used to retrieve the java.sql.Connection for this Connection */
    
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
    
    public boolean open(String ConnectionString){
        this.database = new Database(ConnectionString);
        return open(database);
    }
                                      
    
    
  //**************************************************************************
  //** Open
  //**************************************************************************
  /** Used to open a connection to the database. */
    
    public boolean open(Database database){
                
        long startTime = java.util.Calendar.getInstance().getTimeInMillis();
        this.database = database;
        boolean isClosed = true;
        
        try{
            Conn = database.connect(); 
            //if (Conn.isClosed()) throw new Exception(Conn.getWarnings());
            isClosed = Conn.isClosed();
            
        }
        catch(Exception e){
            //System.out.println("Failed");
            //System.out.println(database.getDriver().getVendor() + " ERROR: " + e.toString());
            isClosed = true;
        }
        
        long endTime = java.util.Calendar.getInstance().getTimeInMillis();
        Speed = endTime-startTime;
        return isClosed;
    }


  //**************************************************************************
  //** Open
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
  //** Close
  //**************************************************************************
  /** Used to close a connection to the database, freeing up connections */
    
    public void close(){
        try{Conn.close();}
        catch(Exception e){}
    } 
    
    
  //**************************************************************************
  //** Execute
  //**************************************************************************
  /** Used to execute a prepared sql statement (e.g. "delete from my_table").
   *  Returns a boolean to indicate whether the command was successful.
   */
    public boolean execute(String sql){
        try{
            PreparedStatement preparedStmt = Conn.prepareStatement(sql);
            preparedStmt.execute();
            preparedStmt.close();
            preparedStmt = null;
            return true;
        }
        catch(Exception e){
            System.out.println(e.toString() + "\r\nSQL: " + sql);
            return false;
        }
    } 
    
    
  //**************************************************************************
  //** Commit
  //**************************************************************************
  /** Used to explicitely commit changes made to the database. */
    
    public void commit(){
        execute("COMMIT");
    }
    
  //**************************************************************************
  //** getDatabase
  //**************************************************************************
  /** Used to return database information associated with this connection. */
    
    public Database getDatabase(){
        return database;
    }

}

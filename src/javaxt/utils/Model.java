package javaxt.utils;
import javaxt.sql.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.concurrent.ConcurrentHashMap;

//******************************************************************************
//**  Model
//******************************************************************************
/**
 *   Base class for models generated by the javaxt-orm library.
 *
 ******************************************************************************/

public abstract class Model {

    
    protected Long id;
    
    private final String tableName;
    
    private static ConcurrentHashMap<String, PreparedStatement> 
    sqlCache = new ConcurrentHashMap<String, PreparedStatement>();
    
    private static ConcurrentHashMap<String, ConnectionPool> 
    connPool = new ConcurrentHashMap<String, ConnectionPool>();
    
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
    protected Model(String tableName){
        this.tableName = tableName;
    }


  //**************************************************************************
  //** getID
  //**************************************************************************
    public Long getID(){
        return id;
    }

    
  //**************************************************************************
  //** setID
  //**************************************************************************
    public void setID(Long id){
        this.id = id;
    }


  //**************************************************************************
  //** init
  //**************************************************************************
    protected void init(long id) throws SQLException {
        try{

            
          //Get or create a prepared statement from the sql cache
            PreparedStatement stmt;
            synchronized(sqlCache){
                String query = "select * from " + tableName + " where id=?";
                stmt = sqlCache.get(query);
                if (stmt==null){
                    Connection conn = getConnection(this.getClass());
                    stmt = conn.getConnection().prepareStatement(query);
                    sqlCache.put(query, stmt);
                    sqlCache.notify();
                    
                    //TODO: Launch thread to close idle connections
                }
            }
            

          //Execute prepared statement
            stmt.setLong(1, id);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (!rs.next()){
                rs.close();
                throw new IllegalArgumentException();
            }

            update(rs);
            rs.close();
        
        }
        catch(SQLException e){ 

            
          //Execute query without a prepared statement
            Connection conn = null;
            try{
                conn = getConnection(this.getClass());
          
                Recordset rs = new Recordset();
                rs.open("select * from " + tableName + " where id=" + id, conn);
                if (rs.EOF){
                    rs.close();
                    conn.close();
                    throw new IllegalArgumentException();
                }

                update(rs);

                rs.close();
                conn.close();
            }
            catch(SQLException ex){
                if (conn!=null) conn.close();
                throw ex;
            }            
        }
    }
    
    
  //**************************************************************************
  //** update
  //**************************************************************************
  /** Used to set/update fields using a record from the database.
   */
    protected abstract void update(Object rs) throws SQLException;
    

  //**************************************************************************
  //** delete
  //**************************************************************************
  /** Used to delete a object from the database.
   */
    public void delete() throws SQLException {
        if (id==null) return;
        Connection conn = null;
        try{
            conn = getConnection(this.getClass());
            conn.execute("delete from " + tableName + " where id=" + id);
            conn.close();
        }
        catch(SQLException e){
            if (conn!=null) conn.close();
            throw e;
        }
    }

    
  //**************************************************************************
  //** getID
  //**************************************************************************
  /** Used to find an object in the database using a given set of constraints.
   */
    protected Long getID(Object...args) throws SQLException {
        Long id = null;
        StringBuilder str = new StringBuilder("select id from "); 
        str.append(tableName); 
        str.append(" where ");
        for (int i=0; i<args.length-1; i++){
            str.append(args[i]);
            i++;
            Object val = args[i];
            if (val instanceof String){
                str.append("'");
                str.append(val.toString().replace("'", "''"));
                str.append("'");
            }
            else{
                str.append(val);
            }
            if (i<args.length-2) str.append(" and ");
        }
        
        Connection conn = null;
        try{
            conn = getConnection(this.getClass());
            javaxt.sql.Recordset rs = new javaxt.sql.Recordset();
            rs.open(str.toString(), conn);
            if (!rs.EOF) id = rs.getValue(0).toLong();
            rs.close();
            conn.close();
        }
        catch(SQLException e){
            if (conn!=null) conn.close();
            throw e;
        }

        return id;
    }


  //**************************************************************************
  //** getValue
  //**************************************************************************
    protected javaxt.sql.Value getValue(Object rs, String key) throws SQLException {
        if (rs instanceof java.sql.ResultSet){
            return new javaxt.sql.Value(((java.sql.ResultSet) rs).getObject(key));
        }
        else{
            return ((Recordset) rs).getValue(key);
        }
    }
    
    
  //**************************************************************************
  //** getConnection
  //**************************************************************************
  /** Returns a database connection associated with a given class.
   */
    protected static Connection getConnection(Class c) throws SQLException {
        ConnectionPool connectionPool = null;
        synchronized(connPool){
            connectionPool = connPool.get(c.getName());
        }
        if (connectionPool!=null){
            return connectionPool.getConnection();
        }
        else{
            throw new SQLException("Failed to find connection for " + c.getName());
        }
    }    
    
    
  //**************************************************************************
  //** register
  //**************************************************************************
  /** Used to associate a model with a connection pool.
   */
    public static void register(Class c, ConnectionPool connectionPool){
        synchronized(connPool){
            connPool.put(c.getName(), connectionPool);
            connPool.notifyAll();
        }
    }
}
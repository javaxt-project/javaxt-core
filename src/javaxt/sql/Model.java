package javaxt.sql;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.concurrent.ConcurrentHashMap;

//******************************************************************************
//**  Model
//******************************************************************************
/**
 *   Base class for persistence models generated by the javaxt-orm library.
 *
 ******************************************************************************/

public abstract class Model {
    
    protected Long id;
    
    protected final String tableName;
    private String[] keywords;
    
    private static ConcurrentHashMap<String, PreparedStatement> 
    sqlCache = new ConcurrentHashMap<String, PreparedStatement>();
    
    private static ConcurrentHashMap<String, ConnectionPool> 
    connPool = new ConcurrentHashMap<String, ConnectionPool>();
    
    private static ConcurrentHashMap<String, String[]> 
    reservedKeywords = new ConcurrentHashMap<String, String[]>();


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    protected Model(String tableName){
        
      //Get SQL reserved keywords 
        synchronized(reservedKeywords){
            keywords = reservedKeywords.get(this.getClass().getName());
        }
        
        
      //Set table name
        this.tableName = escape(tableName, keywords);
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
    protected final void init(long id, String...fieldNames) throws SQLException {
        
        
        StringBuilder sql = new StringBuilder("select ");
        for (int i=0; i<fieldNames.length; i++){
            if (i>0) sql.append(", ");
            String fieldName = fieldNames[i];
            //TODO: Update escape function to escape fields inside of functions
            sql.append(escape(fieldName, keywords));
        }
        sql.append(" from ");
        sql.append(tableName);
        sql.append(" where id=");
        
        
        try{
            
          //Execute query using a prepared statement
            synchronized(sqlCache){
                
              //Get or create a prepared statement from the sql cache
                String query = sql.toString() + "?";
                PreparedStatement stmt = sqlCache.get(query);
                if (stmt==null){
                    Connection conn = getConnection(this.getClass());
                    stmt = conn.getConnection().prepareStatement(query);
                    sqlCache.put(query, stmt);
                    sqlCache.notify();
                    
                    //TODO: Launch thread to close idle connections
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

        }
        catch(IllegalArgumentException e){ 
            throw e;
        }
        catch(Exception e){ 

            
          //Execute query without a prepared statement
            Connection conn = null;
            try{
                conn = getConnection(this.getClass());
          
                Recordset rs = new Recordset();
                String query = sql.toString() + id;
                rs.open(query, conn);
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
  //** _get
  //**************************************************************************
  /** Used to find a model in the database using a given set of constraints.
   *  Example: 
    <pre>
        _get(Contact.class, "firstname=", "John", "lastname=", "Smith");
        _get(Contact.class, 123);
    </pre>
   */
    protected static Object _get(Class c, Object...args) throws SQLException {
        
        if (args.length==1){
            if ((args[0] instanceof Long) || (args[0] instanceof Integer)){
                try{
                    long id;
                    if (args[0] instanceof Long) id = (Long) args[0];
                    else id = new Long((Integer) args[0]);
                    return c.getConstructor(long.class).newInstance(id);
                }
                catch(Exception e){
                    return null;
                }
            }
            else{
                return null;
            }
        }

        
      //Get tableName
        String tableName = null;
        try{ tableName = ((Model) c.newInstance()).tableName; }
        catch(Exception e){}
        
        
      //Build sql to find the model id
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
        
        
      //Execute query
        Long id = null;
        Connection conn = null;
        try{
            conn = getConnection(c);
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

        
      //Return model
        if (id!=null){
            try{ return c.getConstructor(long.class).newInstance(id); }
            catch(Exception e){}
        }
        return null;
    }

    
  //**************************************************************************
  //** _find
  //**************************************************************************
  /** Returns an array of models from the database using a given set of 
   *  constraints.
   */
    protected static Object[] _find(Class c, Object...args) throws SQLException {
        return null;
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
  //** init
  //**************************************************************************
  /** Used to associate a model with a connection pool.
    <pre>
        for (Jar.Entry entry : jar.getEntries()){
            String name = entry.getName();
            if (name.endsWith(".class")){
                name = name.substring(0, name.length()-6).replace("/", ".");
                Class c = Class.forName(name);
                if (javaxt.utils.Model.class.isAssignableFrom(c)){
                    javaxt.utils.Model.init(c, database.getConnectionPool());
                }
            }
        }
    </pre>
   */
    public static void init(Class c, ConnectionPool connectionPool){
        
      //Associate model with the connection pool
        synchronized(connPool){
            connPool.put(c.getName(), connectionPool);
            connPool.notifyAll();
        }
        
        
      //Get reserved keywords associated with the database
        String[] keywords = null;
        Connection conn = null;
        try{
            conn = connectionPool.getConnection();
            keywords = javaxt.sql.Database.getReservedKeywords(conn);
            conn.close();
        }
        catch(Exception e){
            if (conn!=null) conn.close();
        }
        
        synchronized(reservedKeywords){
            reservedKeywords.put(c.getName(), keywords);
            reservedKeywords.notifyAll();
        }
    }
    
    
  //**************************************************************************
  //** escape
  //**************************************************************************
  /** Used to wrap column and table names in quotes if the name is a reserved
   *  SQL keyword.
   */
    protected String escape(String colName, String[] keywords){
        /*
        //TODO: Check whether the colName contains a function. Otherwise, the
        //following logic won't work...
        colName = colName.trim();
        if (colName.contains(" ") && !colName.startsWith("[")){
            colName = "[" + colName + "]";
        }
        */
        if (keywords==null) return colName;
        for (String keyWord : keywords){
            if (colName.equalsIgnoreCase(keyWord)){
                colName = "\"" + colName + "\"";
                break;
            }
        }
        return colName;
    }
}
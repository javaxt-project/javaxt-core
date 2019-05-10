package javaxt.sql;
import javaxt.json.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import javaxt.io.Jar;

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
    private final String modelName;
    private final HashMap<String, String> fieldMap;
    private String[] keywords;

    private static ConcurrentHashMap<String, PreparedStatement>
    sqlCache = new ConcurrentHashMap<String, PreparedStatement>();

    private static ConcurrentHashMap<String, PreparedStatement>
    insertStatements = new ConcurrentHashMap<String, PreparedStatement>();

    private static ConcurrentHashMap<String, ConnectionPool>
    connPool = new ConcurrentHashMap<String, ConnectionPool>();

    private static ConcurrentHashMap<String, String[]>
    reservedKeywords = new ConcurrentHashMap<String, String[]>();

    private static ConcurrentHashMap<String, Field[]>
    fields = new ConcurrentHashMap<String, Field[]>();


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Legacy constructor. Relies on the init(long id, String...fieldNames)
   *  method when initializing the model using a record in the database.
   *  @deprecated This constructor will be removed in a future release.
   */
    protected Model(String tableName){
        this(tableName, null);
    }

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to create a new instance of this class.
   *  @param tableName The name of the table in the database associated with
   *  this model.
   *  @param fieldMap Used to map fields to column names. The key is the
   *  declared field name in the model and the value is the column name in the
   *  database. Example: fieldMap.put("countryCode", country_code");
   *  You do not need to include the "id" field.
   */
    protected Model(String tableName, HashMap<String, String> fieldMap){
        synchronized(reservedKeywords){
            keywords = reservedKeywords.get(this.getClass().getName());
        }
        this.tableName = escape(tableName);
        this.modelName = this.getClass().getSimpleName();
        this.fieldMap = fieldMap;
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
  /** Used to initialize the model using a record in the database.
   *  @param id Primary key in the database table associated with this model.
   */
    protected final void init(long id) throws SQLException {

        ArrayList<String> fieldNames = new ArrayList<String>();
        for (java.lang.reflect.Field f : this.getClass().getDeclaredFields()){
            String fieldName = f.getName();
            String columnName = fieldMap.get(fieldName);
            if (columnName!=null){

                Class c = f.getType();


                if (ArrayList.class.isAssignableFrom(c)){
                    //Do nothing, probably a model
                    continue;
                }

                String className = c.getSimpleName();
                if (className.equals("Geometry")){
                    fieldNames.add("ST_AsText(" + columnName + ") as " + columnName);
                }
                else{
                    fieldNames.add(columnName);
                }
            }
        }
        init(id, fieldNames.toArray(new String[fieldNames.size()]));
    }


  //**************************************************************************
  //** init
  //**************************************************************************
  /** Used to initialize the model using a record in the database.
   *  @param id Primary key in the database table associated with this model.
   *  @param fieldNames A comma-delimited list of column names found in the
   *  database table that backs this model. The column names will be used in
   *  a select statement to populate the fields in the model. The column names
   *  may include SQL functions in which case there must be an alias that maps
   *  to a field. For example, "ST_AsText(coordinate) as coordinate".
   *  @deprecated This method will be removed in a future release.
   */
    protected final void init(long id, String...fieldNames) throws SQLException {


        StringBuilder sql = new StringBuilder("select ");
        for (int i=0; i<fieldNames.length; i++){
            if (i>0) sql.append(", ");
            String fieldName = fieldNames[i];
            sql.append(escape(fieldName));
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
                this.id = id;

                rs.close();
            }

        }
        catch(IllegalArgumentException e){
            throw new SQLException(modelName + " not found");
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
                    throw new SQLException(modelName + " not found");
                }

                update(rs);
                this.id = id;

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
  //** update
  //**************************************************************************
  /** Used to set/update fields using a JSON representation of this class.
   */
    protected abstract void update(JSONObject json) throws SQLException;


  //**************************************************************************
  //** save
  //**************************************************************************
  /** Used to persist the model in the database.
   */
    public void save() throws SQLException {


      //Get list if fields and thier associated values
        LinkedHashMap<java.lang.reflect.Field, Object> fields = getFields();
        Iterator<java.lang.reflect.Field> it;


      //Identify and remove fields that we do not want to update in the database
        ArrayList<java.lang.reflect.Field> arr = new ArrayList<java.lang.reflect.Field>();
        it = fields.keySet().iterator();
        while (it.hasNext()){
            java.lang.reflect.Field f = it.next();
            String fieldName = f.getName();
            Class fieldType = f.getType();

            if (fieldType.equals(java.util.ArrayList.class)){
                arr.add(f);
            }

            if (fieldName.equalsIgnoreCase("id")){
                arr.add(f);
            }
        }
        for (java.lang.reflect.Field f : arr){
            fields.remove(f);
        }


      //Update values as needed
        for (Map.Entry<java.lang.reflect.Field, Object> entry : fields.entrySet()) {
            Object val = entry.getValue();
            if (val!=null){
                if (val instanceof Model){
                    entry.setValue(((Model) val).getID());
                }
                else if (val instanceof JSONObject){
                    JSONObject json = (JSONObject) val;
                    if (json.isEmpty()) entry.setValue(null);
                }
            }
        }





        if (id==null){ //insert new record using prepared statement (faster than recordset)


            String className = this.getClass().getName();
            Field[] dbFields;
            synchronized(this.fields){
                dbFields = this.fields.get(className);
            }



          //Get or create prepared statement
            PreparedStatement stmt;
            synchronized(insertStatements){
                stmt = insertStatements.get(className);
                if (stmt==null){
                    Connection conn = getConnection(this.getClass());

                    StringBuilder sql = new StringBuilder();
                    sql.append("INSERT INTO " + tableName + " (");
                    it = fields.keySet().iterator();
                    while (it.hasNext()){
                        java.lang.reflect.Field f = it.next();
                        String columnName = fieldMap.get(f.getName());
                        sql.append(escape(columnName));
                        if (it.hasNext()) sql.append(",");
                    }
                    sql.append(") VALUES (");
                    it = fields.keySet().iterator();
                    while (it.hasNext()){
                        java.lang.reflect.Field f = it.next();
                        Class fieldType = f.getType();
                        String packageName = fieldType.getPackage()==null ? "" :
                                             fieldType.getPackage().getName();

                        String q = "?";
                        if (packageName.startsWith("javaxt.json") ||
                            packageName.startsWith("org.json")){
                            javaxt.sql.Driver driver = conn.getDatabase().getDriver();
                            if (driver.equals("PostgreSQL")){
                                q = "?::jsonb";
                            }
                        }
                        else if (packageName.startsWith("javaxt.geospatial.geometry") ||
                            packageName.startsWith("com.vividsolutions.jts.geom")){


                            String columnName = fieldMap.get(f.getName());
                            String STGeomFromText = null;
                            for (Field field : dbFields){
                                if (field.getName().equals(columnName)){
                                    STGeomFromText = Recordset.getSTGeomFromText(field, conn);
                                    break;
                                }
                            }
                            q = STGeomFromText + "(?,?)";
                        }


                        sql.append(q);
                        if (it.hasNext()) sql.append(",");
                    }
                    sql.append(")");


                    stmt = conn.getConnection().prepareStatement(sql.toString(), java.sql.Statement.RETURN_GENERATED_KEYS);
                    insertStatements.put(className, stmt);
                    insertStatements.notify();
                }
            }



          //Generate list of database fields for insert
            ArrayList<Field> updates = new ArrayList<Field>();
            it = fields.keySet().iterator();
            while (it.hasNext()){
                java.lang.reflect.Field f = it.next();
                Class fieldType = f.getType();
                String packageName = fieldType.getPackage()==null ? "" :
                                     fieldType.getPackage().getName();



              //Get value. Replace with function as needed
                Object val = fields.get(f);
                if (packageName.startsWith("javaxt.geospatial.geometry") ||
                    packageName.startsWith("com.vividsolutions.jts.geom")){
                    int srid = 4326; //getSRID();
                    try{
                        java.lang.reflect.Method method = fieldType.getMethod("getSRID");
                        if (method!=null){
                            Object obj = method.invoke(val, null);
                            if (obj!=null){
                                srid = (Integer) obj;
                                if (srid==0) srid = 4326;
                            }
                        }
                    }
                    catch(Exception e){
                    }
                    val = new Function(null, new Object[]{
                        val == null ? null : val.toString(),
                        srid
                    });
                }
                else if (packageName.startsWith("javaxt.json") ||
                    packageName.startsWith("org.json")){
                    val = new Function(null, new Object[]{
                        val == null ? null : val.toString()
                    });
                }



                boolean foundField = false;
                String columnName = fieldMap.get(f.getName());
                for (Field field : dbFields){
                    if (field.getName().equals(columnName)){
                        field.Value = new Value(val);
                        updates.add(field);
                        foundField = true;
                        break;
                    }
                }

                if (!foundField){
                    throw new SQLException("Model/Schema mismatch. Failed to find " + columnName + " in the database.");
                }
            }


          //Insert record
            Recordset.update(stmt, updates);
            stmt.executeUpdate();


          //Get id
            java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                id = new Value(generatedKeys.getString(1)).toLong();
            }


        }
        else{ //update existing record

            Connection conn = null;
            try{
                conn = getConnection(this.getClass());

                javaxt.sql.Driver driver = conn.getDatabase().getDriver();
                if (driver==null) driver = new Driver("","","");

                Recordset rs = new Recordset();
                rs.open("select * from " + tableName + " where id=" + id, conn, false);
                if (rs.EOF){
                    rs.addNew();
                    rs.setValue("id", id);
                }
                it = fields.keySet().iterator();
                while (it.hasNext()){
                    java.lang.reflect.Field f = it.next();
                    String columnName = fieldMap.get(f.getName());
                    Object val = fields.get(f);
                    if (val instanceof JSONObject || val instanceof JSONArray){
                        if (driver.equals("PostgreSQL")){
                            rs.setValue(columnName, new javaxt.sql.Function(
                                "?::jsonb", new Object[]{
                                    val.toString()
                                }
                            ));
                        }
                        else{
                            rs.setValue(columnName, val.toString());
                        }
                    }
                    else{
                        rs.setValue(columnName, val);
                    }
                }
                rs.update();
                rs.close();
                conn.close();
            }
            catch(SQLException e){
                if (conn!=null) conn.close();
                throw e;
            }
        }
    }


  //**************************************************************************
  //** delete
  //**************************************************************************
  /** Used to delete the model from the database.
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
  //** toJson
  //**************************************************************************
  /** Returns a JSON representation of this model.
   */
    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        if (id!=null) json.set("id", id);

        LinkedHashMap<java.lang.reflect.Field, Object> fields = getFields();
        Iterator<java.lang.reflect.Field> it = fields.keySet().iterator();
        while (it.hasNext()){
            java.lang.reflect.Field f = it.next();
            String fieldName = f.getName();
            Object val = fields.get(f);

            if (val!=null){

              //Check if the val is a Model or an array of Models. If so,
              //convert the val to JSON
                if (val instanceof java.util.ArrayList){
                    java.util.ArrayList list = (java.util.ArrayList) val;
                    if (!list.isEmpty()){
                        Class c = list.get(0).getClass();
                        if (javaxt.sql.Model.class.isAssignableFrom(c)){
                            JSONArray arr = new JSONArray();
                            for (Object obj : list){
                                arr.add(((Model) obj).toJson());
                            }
                            val = arr;
                        }
                    }
                }
                else if (val instanceof Model){
                    val = ((Model) val).toJson();
                }

                json.set(fieldName, val);
            }
        }
        return json;
    }


  //**************************************************************************
  //** equals
  //**************************************************************************
    public boolean equals(Object obj){
        if (id!=null){
            if (this.getClass().isAssignableFrom(obj.getClass())){
                return obj.hashCode()==hashCode();
            }
        }
        return false;
    }


  //**************************************************************************
  //** hashCode
  //**************************************************************************
    public int hashCode(){
        return id==null ? -1 : (int) (id ^ (id >>> 32));
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



      //Build sql to find the model id
        String sql = getSQL(c, args);


      //Execute query
        Long id = null;
        Connection conn = null;
        try{
            conn = getConnection(c);
            javaxt.sql.Recordset rs = new javaxt.sql.Recordset();
            rs.open(sql, conn);
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

      //Build sql using args
        String sql = getSQL(c, args);


      //Execute query
        java.util.ArrayList<Long> ids = new java.util.ArrayList<Long>();
        Connection conn = null;
        try{
            conn = getConnection(c);
            javaxt.sql.Recordset rs = new javaxt.sql.Recordset();
            rs.open(sql, conn);
            while (rs.hasNext()){
                ids.add(rs.getValue(0).toLong());
                rs.moveNext();
            }
            rs.close();
            conn.close();
        }
        catch(SQLException e){
            if (conn!=null) conn.close();
            throw e;
        }


      //Return model
        if (!ids.isEmpty()){
            java.util.ArrayList arr = new java.util.ArrayList(ids.size());
            for (long id : ids){
                try{
                    arr.add(c.getConstructor(long.class).newInstance(id));
                }
                catch(Exception e){}
            }
            return arr.toArray();
        }

        return new Object[0];
    }


  //**************************************************************************
  //** getSQL
  //**************************************************************************
  /** Returns a sql statement used to generate a list of model IDs
   */
    private static String getSQL(Class c, Object...args){

      //Get tableName
        String tableName = null;
        try{ tableName = ((Model) c.newInstance()).tableName; }
        catch(Exception e){}

        StringBuilder str = new StringBuilder("select ");
        str.append(tableName);
        str.append(".id from ");
        str.append(tableName);

        if (args.length>1){
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
        }
        return str.toString();
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
  /** Used to associate a model with a database connection pool. This allows
   *  queries and other database metadata to be cached.
    <pre>
        for (Jar.Entry entry : jar.getEntries()){
            String name = entry.getName();
            if (name.endsWith(".class")){
                name = name.substring(0, name.length()-6).replace("/", ".");
                Class c = Class.forName(name);
                if (javaxt.sql.Model.class.isAssignableFrom(c)){
                    javaxt.sql.Model.init(c, database.getConnectionPool());
                }
            }
        }
    </pre>
   */
    public static void init(Class c, ConnectionPool connectionPool){

        String className = c.getName();

      //Associate model with the connection pool
        synchronized(connPool){
            connPool.put(className, connectionPool);
            connPool.notifyAll();
        }


      //Add database metadata
        Connection conn = null;
        try{
            conn = connectionPool.getConnection();


          //Get reserved keywords associated with the database
            String[] keywords = Database.getReservedKeywords(conn);
            synchronized(reservedKeywords){
                reservedKeywords.put(className, keywords);
                reservedKeywords.notifyAll();
            }


          //Generate list of fields
            String tableName = ((Model) c.newInstance()).tableName;
            Recordset rs = new Recordset();
            rs.open("select * from " + tableName + " where id is null", conn);
            synchronized(fields){
                fields.put(className, rs.getFields());
                fields.notifyAll();
            }
            rs.close();
            conn.close();
        }
        catch(Exception e){
            if (conn!=null) conn.close();
            e.printStackTrace();
        }
    }


  //**************************************************************************
  //** init
  //**************************************************************************
  /** Used to associate all the models found in a JAR file with a database
   *  connection pool. This allows queries and other database metadata to be
   *  cached.
   */
    public static void init(javaxt.io.Jar jar, ConnectionPool connectionPool){
        for (Class c : jar.getClasses()){
            if (javaxt.sql.Model.class.isAssignableFrom(c)){
                init(c, connectionPool);
            }
        }
    }


  //**************************************************************************
  //** escape
  //**************************************************************************
  /** Used to wrap column and table names in quotes if the name is a reserved
   *  SQL keyword.
   */
    protected String escape(String colName){
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


  //**************************************************************************
  //** getFields
  //**************************************************************************
  /** Returns a list of private fields in the class and any associated values.
   */
    private LinkedHashMap<java.lang.reflect.Field, Object> getFields(){
        LinkedHashMap<java.lang.reflect.Field, Object> fields =
        new LinkedHashMap<java.lang.reflect.Field, Object>();
        for (java.lang.reflect.Field f : this.getClass().getDeclaredFields()){
            String fieldName = f.getName();
            if (fieldMap.containsKey(fieldName)){
                Object val = null;
                try{
                    f.setAccessible(true);
                    val = f.get(this);
                }
                catch(Exception e){}
                fields.put(f, val);
            }
        }
        return fields;
    }
}
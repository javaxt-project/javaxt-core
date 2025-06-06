package javaxt.sql;
import javaxt.json.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

//******************************************************************************
//**  Model
//******************************************************************************
/**
 *   Base class for persistence models generated by the javaxt-orm library.
 *
 ******************************************************************************/

public abstract class Model {

    protected Long id;
    private String tableName; //escaped table name
    private final String modelName;
    private final HashMap<String, String> fieldMap;
    private String[] keywords;

    private static ConcurrentHashMap<String, PreparedStatement>
    sqlCache = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, PreparedStatement>
    insertStatements = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, ConnectionPool>
    connPool = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, String[]>
    reservedKeywords = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, Field[]>
    fields = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, String[]>
    tables = new ConcurrentHashMap<>();



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
    protected Model(String tableName, Map<String, String> fieldMap){

      //Set modelName
        Class c = this.getClass();
        String className = c.getName();
        modelName = c.getSimpleName();


      //Set keywords
        synchronized(reservedKeywords){
            keywords = reservedKeywords.get(className);
        }


      //Set tableInfo
        synchronized(tables){
            String[] tableInfo = tables.get(className);
            if (tableInfo==null){
                tableInfo = getTableInfo(tableName);
                tables.put(className, tableInfo);
            }
            this.tableName = tableInfo[0];
        }


      //Set fieldMap
        this.fieldMap = new HashMap<>();
        Iterator<String> it = fieldMap.keySet().iterator();
        while (it.hasNext()){
            String key = it.next();
            String val = fieldMap.get(key);
            this.fieldMap.put(key, val);
        }
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


      //Generate a list of fields names
        ArrayList<String> fieldNames = new ArrayList<>();
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


      //Compile query
        StringBuilder sql = new StringBuilder("select ");
        boolean addID = true;
        for (int i=0; i<fieldNames.size(); i++){
            if (i>0) sql.append(", ");
            String fieldName = fieldNames.get(i);
            if (fieldName.equalsIgnoreCase("id")) addID = false;
            sql.append(escape(fieldName));
        }
        if (addID) sql.append(", id");
        sql.append(" from ");
        sql.append(tableName);
        sql.append(" where id=");


      //Execute query using a PreparedStatement stored in the sqlCache. Note
      //that PreparedStatements are not thread-safe and we are only caching one
      //PreparedStatement per model. As a result, we can only execute one
      //query at a time. In the future, we could add an option to allow users
      //to specify the number of PreparedStatement per model.
        PreparedStatement stmt = null;
        String query = sql.toString() + "?";
        try{
            synchronized(sqlCache){

              //Get or create prepared statement
                stmt = sqlCache.get(query);
                if (stmt==null){
                    Connection conn = getConnection(this.getClass());
                    stmt = conn.getConnection().prepareStatement(query);
                    sqlCache.put(query, stmt);
                    sqlCache.notify();
                }


              //Execute prepared statement
                stmt.setLong(1, id);
                try (java.sql.ResultSet rs = stmt.executeQuery()){
                    if (rs.next()){
                        update(rs);
                        this.id = id;
                    }
                    else{
                        throw new IllegalArgumentException();
                    }
                }
            }
        }
        catch(IllegalArgumentException e){
            throw new SQLException(modelName + " not found");
        }
        catch(Exception e){


            if (stmt!=null && stmt.getConnection().isClosed()){

              //Remove the statement from the cache
                synchronized(sqlCache){
                    sqlCache.remove(query);
                    sqlCache.notifyAll();
                }


              //Try closing the statement
                try{stmt.close();}catch(Exception ex){}


              //Call this method again
                init(id);
                return;
            }


          //If we're still here, execute query without a prepared statement
            try (Connection conn = getConnection(this.getClass())){

                query = sql.toString() + id;

                try (Recordset rs = conn.getRecordset(query)){
                    rs.open(query, conn);
                    if (rs.EOF) throw new SQLException(modelName + " not found");

                    update(rs);
                    this.id = id;
                }
            }
            catch(SQLException ex){
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

      //Get class name
        String className = this.getClass().getName();


      //Get list of fields and thier associated values
        LinkedHashMap<java.lang.reflect.Field, Object> fields = getFields();
        Iterator<java.lang.reflect.Field> it;


      //Identify and remove fields that we do not want to update in the database
        ArrayList<java.lang.reflect.Field> arr = new ArrayList<>();
        it = fields.keySet().iterator();
        while (it.hasNext()){
            java.lang.reflect.Field f = it.next();
            String fieldName = f.getName();
            Class fieldType = f.getType();

            if (fieldType.equals(ArrayList.class)){
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
        int numNulls = 0;
        for (Map.Entry<java.lang.reflect.Field, Object> entry : fields.entrySet()) {
            Object val = entry.getValue();
            if (val!=null){
                if (val instanceof Model){
                    Model model = (Model) val;
                    model.save();
                    entry.setValue(model.getID());
                }
                else if (val instanceof JSONObject){
                    JSONObject json = (JSONObject) val;
                    if (json.isEmpty()) entry.setValue(null);
                }
            }
            if (entry.getValue()==null) numNulls++;
        }


      //Check if there's anything to save
        if (numNulls==fields.size()) return;



        if (id==null){ //insert new record using prepared statement (faster than recordset)


          //Get database fields associated with the model
            Field[] dbFields;
            synchronized(this.fields){ dbFields = this.fields.get(className); }
            if (dbFields==null) throw new SQLException(
                "Failed to retrieve metadata for " + className + ". " +
                "The model may not have been initialized. See Model.init()");


          //Generate list of database fields for insert
            ArrayList<Field> updates = new ArrayList<>();
            it = fields.keySet().iterator();
            while (it.hasNext()){
                java.lang.reflect.Field f = it.next();
                Class fieldType = f.getType();
                String packageName = fieldType.getPackage()==null ? "" :
                                     fieldType.getPackage().getName();



              //Get value. Replace with function as needed
                Object val = fields.get(f);
                if (packageName.startsWith("javaxt.geospatial.geometry") ||
                    packageName.startsWith("com.vividsolutions.jts.geom") ||
                    packageName.startsWith("org.locationtech.jts.geom")){
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
                    if (field.getName().equalsIgnoreCase(columnName)){
                        field = field.clone();
                        field.setValue(new Value(val));
                        updates.add(field);
                        foundField = true;
                        break;
                    }
                }

                if (!foundField){
                    throw new SQLException("Model/Schema mismatch. Failed to find " + columnName + " in the database.");
                }
            }



          //Create new record. Note that PreparedStatements are not thread-safe
          //and we are only caching one PreparedStatement per model. As a
          //result, we can only insert one record per model at a time. If
          //needed, we could add an option to allow users to specify the number
          //of PreparedStatement per model.
            synchronized(insertStatements){

              //Get or create prepared statement
                PreparedStatement stmt = insertStatements.get(className);
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
                            packageName.startsWith("com.vividsolutions.jts.geom") ||
                            packageName.startsWith("org.locationtech.jts.geom")){


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



                    stmt = conn.getConnection().prepareStatement(sql.toString(), new String[]{"id"});
                    insertStatements.put(className, stmt);
                    insertStatements.notify();
                }


              //Insert record
                try{
                    Recordset.update(stmt, updates);
                    stmt.executeUpdate();
                }
                catch(SQLException e){

                    if (stmt!=null && stmt.getConnection().isClosed()){
                        synchronized(insertStatements){
                            insertStatements.remove(className);
                            insertStatements.notifyAll();
                        }
                        save();
                    }
                    else{
                        throw Exception("Failed to save " + className + ". " + e.getMessage(), e);
                    }
                }


              //Get id
                java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    id = new Value(generatedKeys.getString(1)).toLong();
                }

            }

        }
        else{ //update existing record

            try (Connection conn = getConnection(this.getClass())){

                javaxt.sql.Driver driver = conn.getDatabase().getDriver();
                if (driver==null) driver = new Driver("","","");

                try(Recordset rs = conn.getRecordset(
                    "select * from " + tableName + " where id=" + id, false)){
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
                }

            }
            catch(SQLException e){
                throw Exception("Failed to update " + className + "#" + id + ". " + e.getMessage(), e);
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
        try (Connection conn = getConnection(this.getClass())){
            conn.execute("delete from " + tableName + " where id=" + id);
        }
        catch(SQLException e){
            String className = this.getClass().getName();
            throw Exception("Failed to delete " + className + "#" + id + ". " + e.getMessage(), e);
        }
    }


  //**************************************************************************
  //** getException
  //**************************************************************************
  /** Used to build a custom exception when saving or deleting records.
   */
    private SQLException Exception(String err, SQLException e){
        SQLException ex = new SQLException(err);
        ArrayList<StackTraceElement> stackTrace = new ArrayList<>();
        boolean addElement = false;
        StackTraceElement[] arr = ex.getStackTrace();
        for (int i=2; i<arr.length; i++){
            StackTraceElement el = arr[i];
            if (!el.getClassName().contains("reflect")) addElement = true;
            if (addElement) stackTrace.add(el);
        }
        ex.setStackTrace(stackTrace.toArray(new StackTraceElement[stackTrace.size()]));
        ex.setNextException(e);
        return ex;
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
                if (val instanceof ArrayList){
                    ArrayList list = (ArrayList) val;
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
        try (Connection conn = getConnection(c)){
            javaxt.sql.Record record = conn.getRecord(sql);
            if (record!=null) id = record.get(0).toLong();
        }
        catch(SQLException e){
            throw e;
        }


      //Return model
        if (id!=null){
            try{
                return c.getConstructor(long.class).newInstance(id);
            }
            catch(Exception e){
                throw new SQLException("Failed to instantiate model for " + id);
            }
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
        ArrayList<Long> ids = new ArrayList<>();
        try (Connection conn = getConnection(c)){
            for (javaxt.sql.Record record : conn.getRecords(sql)){
                ids.add(record.get(0).toLong());
            }
        }


      //Return model
        if (!ids.isEmpty()){
            ArrayList arr = new ArrayList(ids.size());
            for (long id : ids){
                try{
                    arr.add(c.getConstructor(long.class).newInstance(id));
                }
                catch(Exception e){
                    throw new SQLException("Failed to instantiate model for " + id);
                }
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
  /** Used to initialize a Model and associate it with a database connection
   *  pool. This allows queries and other database metadata to be cached.
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
    public static void init(Class c, ConnectionPool connectionPool) throws SQLException {


      //Check if class is a model
        if (!Model.class.isAssignableFrom(c)){
            throw new IllegalArgumentException();
        }


      //Get class name
        String className = c.getName();




      //Get database connection
        try (Connection conn = connectionPool.getConnection()){


          //Associate model with the connection pool
            synchronized(connPool){
                connPool.put(className, connectionPool);
                connPool.notifyAll();
            }


          //Get reserved keywords associated with the database
            String[] keywords = Database.getReservedKeywords(conn);
            synchronized(reservedKeywords){
                reservedKeywords.put(className, keywords);
                reservedKeywords.notifyAll();
            }


          //Generate list of fields
            Model model = ((Model) c.newInstance());
            String sql = "select * from " + model.tableName + " where id is null";
            try (Recordset rs = conn.getRecordset(sql)){
                synchronized(fields){
                    fields.put(className, rs.getFields());
                    fields.notifyAll();
                }
            }

        }
        catch(Exception e){
            SQLException ex = new SQLException("Failed to initialize Model: " + className);
            ex.setStackTrace(e.getStackTrace());
            throw ex;
        }
    }


  //**************************************************************************
  //** init
  //**************************************************************************
  /** Used to initialize all the Models found in a jar file and associate them
   *  with a database connection pool. This allows queries and other database
   *  metadata to be cached.
   */
    public static void init(javaxt.io.Jar jar, ConnectionPool connectionPool) throws SQLException {
        for (Class c : jar.getClasses()){
            if (javaxt.sql.Model.class.isAssignableFrom(c)){
                init(c, connectionPool);
            }
        }
    }


  //**************************************************************************
  //** getTableName
  //**************************************************************************
  /** Returns the name of the table backing a given Model
   */
    public static String getTableName(Model model){
        return model.tableName;
    }


  //**************************************************************************
  //** setSchemaName
  //**************************************************************************
  /** Provides an option to override the default schema used by a model
   */
    public static void setSchemaName(String schemaName, Class c){
        if (!javaxt.sql.Model.class.isAssignableFrom(c)) return;
        String className = c.getName();
        try{

          //Instantiate model to initialize table info
            Model model = ((Model) c.newInstance());


          //Update table info
            synchronized(tables){
                String[] tableInfo = tables.get(className);
                String tableName = tableInfo[1];
                if (schemaName!=null) tableName = schemaName + "." + tableName;
                tableInfo = model.getTableInfo(tableName);
                tables.put(className, tableInfo);
            }
        }
        catch(Exception e){
            Exception ex = new Exception("Failed to update schema for Model: " + className);
            ex.setStackTrace(e.getStackTrace());
            throw new RuntimeException(ex);
        }
    }


  //**************************************************************************
  //** getTableInfo
  //**************************************************************************
  /** Used to parse a given string and extract table info (e.g. schema name,
   *  table name, etc).
   */
    private String[] getTableInfo(String tableName){
        String schemaName;
        String escapedTableName;
        int idx = tableName.indexOf(".");
        if (idx>-1){
            schemaName = tableName.substring(0, idx);
            tableName = tableName.substring(idx+1);
            escapedTableName = escape(schemaName) + "." + tableName;
        }
        else{
            schemaName = null;
            escapedTableName = escape(tableName);
        }
        return new String[]{escapedTableName, tableName, schemaName};
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
        LinkedHashMap<java.lang.reflect.Field, Object> fields = new LinkedHashMap<>();
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
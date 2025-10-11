package javaxt.sql;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;

//******************************************************************************
//**  Database
//******************************************************************************
/**
 *   Used to encapsulate database connection information, open connections to
 *   the database, and execute queries.
 *
 ******************************************************************************/

public class Database implements Cloneable {

    private String name; //name of the catalog used to store tables, views, etc.
    private String host;
    private Integer port;
    private String username;
    private String password;
    private Driver driver;
    private java.util.Properties properties;
    private String querystring;
    private ConnectionPoolDataSource ConnectionPoolDataSource;
    private static final Class<?>[] stringType = { String.class };
    private static final Class<?>[] integerType = { Integer.TYPE };
    private ConnectionPool connectionPool;
    private int maxConnections = 15;
    private Table[] tables = null;
    private String[] catalogs = null;
    private boolean cacheMetadata = false;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class. Note that you will need to set the
   *  name, host, port, username, password, and driver in order to create a
   *  connection to the database.
   */
    public Database(){
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class.
   *  @param name Name of the catalog used to store tables, views, etc.
   *  @param host Server name or IP address.
   *  @param port Port number used to establish connections to the database.
   *  @param username Username used to log into the database
   *  @param password Password used to log into the database
   */
    public Database(String name, String host, int port, String username, String password, Driver driver) {
        this.name = name;
        this.host = host;
        this.port = port>0 ? port : null;
        this.username = username;
        this.password = password;
        this.driver = driver;
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a java.sql.Connection.
   */
    public Database(java.sql.Connection conn){
        try{
            DatabaseMetaData dbmd = conn.getMetaData();
            this.name = conn.getCatalog();
            this.username = dbmd.getUserName();
            parseURL(dbmd.getURL());
            //dbmd.getDriverName();
        }
        catch(Exception e){
            //e.printStackTrace();
        }
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a jdbc connection string.
   *  Username and password may be appended to the end of the connection string
   *  in the property list.
   *  @param connStr A jdbc connection string/url. All connection URLs
   *  have the following form:
   *  <pre> jdbc:[dbVendor]://[dbName][propertyList] </pre>
   *
   *  Examples:
   *  <p>Derby:</p>
   *  <pre> jdbc:derby://temp/my.db;user=admin;password=mypassword </pre>
   *  <p>SQL Server:</p>
   *  <pre> jdbc:sqlserver://192.168.0.80;databaseName=master;user=admin;password=mypassword </pre>
   */
    public Database(String connStr){
        parseURL(connStr);
    }


  //**************************************************************************
  //** parseURL
  //**************************************************************************
  /** Used to parse a JDBC connection string (url)
   */
    private void parseURL(String connStr){

        String[] arrConnStr = connStr.split(";");
        String jdbcURL = arrConnStr[0];

      //Update jdbc url for URL parser
        if (!jdbcURL.contains("//")){
            String protocol = jdbcURL.substring(jdbcURL.indexOf(":")+1);

            protocol = "jdbc:" + protocol.substring(0, protocol.indexOf(":")) + ":";
            String path = jdbcURL.substring(protocol.length());
            jdbcURL = protocol + "//" + path;
        }

      //Parse url and extract connection parameters
        javaxt.utils.URL url = new javaxt.utils.URL(jdbcURL);
        host = url.getHost();
        port = url.getPort();
        driver = Driver.findDriver(url.getProtocol());
        if (driver==null){
            driver = new Driver(null, null, url.getProtocol());
        }

        if (name==null){
            name = url.getPath();
            if (this.name!=null && this.name.startsWith("/")){
                this.name = this.name.substring(1);
            }
        }
        querystring = url.getQueryString();
        if (querystring.length()==0) querystring = null;


      //Extract additional connection parameters
        for (int i=1; i<arrConnStr.length; i++) {

            String[] arrParams = arrConnStr[i].split("=");
            String paramName = arrParams[0].toLowerCase();
            String paramValue = arrParams[1];

            if (paramName.equals("database")){
                this.name = paramValue;
            }
            else if (paramName.equals("user")){
                this.username = paramValue;
            }
            else if (paramName.equals("password")){
                this.password = paramValue;
            }
            else if (paramName.equalsIgnoreCase("derby.system.home")){
                //if (System.getProperty("derby.system.home")==null)
                System.setProperty("derby.system.home", paramValue);
            }
            else{
              //Extract additional properties
                if (properties==null) properties = new java.util.Properties();
                properties.put(arrParams[0], arrParams[1]);
            }
        }
    }


  //**************************************************************************
  //** setName
  //**************************************************************************
  /** Sets the name of the catalog used to store tables, views, etc.
   */
    public void setName(String name){
        this.name = name;
    }


  //**************************************************************************
  //** getName
  //**************************************************************************
  /** Gets the name of the catalog used to store tables, views, etc.
   */
    public String getName(){
        return name;
    }


  //**************************************************************************
  //** setHost
  //**************************************************************************
  /** Used to set the path to the database (server name and port).
   */
    public void setHost(String host, int port){
        this.host = host;
        this.port = port;
    }


  //**************************************************************************
  //** setHost
  //**************************************************************************
  /** Used to set the path to the database.
   *  @param host Server name/port (e.g. localhost:9080) or a path to a file
   *  (e.g. /temp/firebird.db)
   */
    public void setHost(String host){
        if (host==null){
            this.host = null;
        }
        else{
            host = host.trim();
            if (host.contains(":")){
                try{
                    this.host = host.substring(0, host.indexOf(":"));
                    this.port = Integer.valueOf(host.substring(host.indexOf(":")+1));
                }
                catch(Exception e){
                    this.host = host; //eg file paths
                }
            }
            else{
                this.host = host;
            }
        }
    }


  //**************************************************************************
  //** getHost
  //**************************************************************************
  /** Returns the name or IP address of the server or a physical path to the
   *  database file.
   */
    public String getHost(){
        return host;
    }


  //**************************************************************************
  //** setPort
  //**************************************************************************

    public void setPort(int port){
        this.port = port;
    }

    public Integer getPort(){
        return port;
    }


  //**************************************************************************
  //** setDriver
  //**************************************************************************

    public void setDriver(Driver driver){
        this.driver = driver;
    }


  //**************************************************************************
  //** setDriver
  //**************************************************************************
  /** Used to find a driver that corresponds to a given vendor name, class
   *  name, or protocol.
   */
    public void setDriver(String driver){ //throw exception?
        this.driver = Driver.findDriver(driver);
    }


    public void setDriver(java.sql.Driver driver){
        this.driver = new Driver(driver);
    }

    public void setDriver(Class driver){
        this.driver = Driver.findDriver(driver.getCanonicalName());
    }


  //**************************************************************************
  //** getDriver
  //**************************************************************************

    public Driver getDriver(){
        return driver;
    }


  //**************************************************************************
  //** setUserName
  //**************************************************************************

    public void setUserName(String username){
        this.username = username;
    }

    public String getUserName(){
        return username;
    }

  //**************************************************************************
  //** setPassword
  //**************************************************************************

    public void setPassword(String password){
        this.password = password;
    }

    public String getPassword(){
        return password;
    }


    public void setProperties(java.util.Properties properties){
        this.properties = properties;
    }

    public java.util.Properties getProperties(){
        return properties;
    }

  //**************************************************************************
  //** getConnectionString
  //**************************************************************************
  /** Returns a JDBC connection string used to connect to the database.
   *  Username and password are appended to the end of the url.
   */
    public String getConnectionString(){

        String path = getURL(false);
        if (username!=null) path += ";user=" + username;
        if (password!=null) path += ";password=" + password;
        return path;
    }


  //**************************************************************************
  //** getURL
  //**************************************************************************
  /** Used to construct a JDBC connection string
   */
    protected String getURL(boolean appendProperties){

      //Update Server Name
        String server = host;
        if (port!=null && port>0) server += ":" + port;
        String vendor = driver.getVendor();
        if (vendor==null) vendor = "";
        if (vendor.equals("Derby") || vendor.equals("SQLite")){
            server = ":" + server;
        }


      //Update Initial Catalog
        String database = "";
        if (name!=null) {

            if (name.trim().length()>0){

                if (vendor.equals("SQLServer")){
                    database = ";databaseName=" + name;
                }
                else if (vendor.equals("Oracle")){
                    database = ":" + name; //only tested with thin driver
                }
                else if (vendor.equals("Derby")){
                    database = ";databaseName=" + name;
                }
                else{
                    database = "/" + name;
                }

            }
        }

      //Append querystring as needed
        if (querystring!=null) database += "?" + querystring;


      //Set Path
        String path = driver.getProtocol() + "://";



      //Update path as needed
        if (vendor.equals("Sybase")){
            if (path.toLowerCase().contains((CharSequence) "tds:")==false){
                path = driver.getProtocol() + "Tds:";
            }
        }
        else if (vendor.equals("Oracle")){
            path = driver.getProtocol() + ":thin:@"; //only tested with thin driver
        }
        else if (vendor.equals("Derby") || vendor.equals("SQLite")){
            path = driver.getProtocol();
        }
        else if (vendor.equals("H2")){

          //Special case for newer versions of H2. In the 2.x releases of H2,
          //the protocol changed for embedded file databases. The following
          //logic will update the path to set the correct protocol depending
          //on which version of the driver we have.
            if (driver.getProtocol().equals("jdbc:h2")){
                java.sql.Driver d = driver.getDriver();
                try{
                    if (d==null) d = driver.load();
                    if (d.getMajorVersion()>1){
                        path = driver.getProtocol() + ":file:";
                    }
                }
                catch(Exception e){
                }
            }
        }



      //Assemble Connection String
        String url = path + server + database;
        if (appendProperties){
            StringBuilder props = new StringBuilder();
            if (properties!=null){
                java.util.Iterator it = properties.keySet().iterator();
                while (it.hasNext()){
                    Object key = it.next();
                    Object val = properties.get(key);
                    props.append(";" + key + "=" + val);
                }
            }
            url+= props.toString();
        }
        return url;
    }


  //**************************************************************************
  //** getConnection
  //**************************************************************************
  /** Returns a connection to the database. If a connection pool has been
   *  initialized, a connection is returned from the pool. Otherwise, a new
   *  connection is created. In either case, the connection must be closed
   *  immediately after use. See Connection.close() for details.
   */
    public Connection getConnection() throws SQLException {
        Connection connection = new Connection();
        connection.open(this);
        return connection;
    }


  //**************************************************************************
  //** initConnectionPool
  //**************************************************************************
  /** Used to initialize a connection pool. Subsequent called to the
   *  getConnection() method will return connections from the pool.
   */
    public void initConnectionPool() throws SQLException {
        if (connectionPool!=null) return;
        initConnectionPool(new ConnectionPool(this, maxConnections));
    }


  //**************************************************************************
  //** initConnectionPool
  //**************************************************************************
  /** Used to configure the Database class to use a specific instance of a
   *  javaxt.sql.ConnectionPool. Subsequent called to the getConnection()
   *  method will return connections from the pool.
   *  @param cp An instance of a javaxt.sql.ConnectionPool.
   */
    public void initConnectionPool(ConnectionPool cp) throws SQLException {
        if (connectionPool!=null) return;
        connectionPool = cp;

      //Create Shutdown Hook to clean up the connection pool on exit
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (connectionPool!=null){
                    //System.out.println("\r\nShutting down connection pool...");
                    try{
                        connectionPool.close();
                    }
                    catch(Exception e){
                        //e.printStackTrace();
                    }
                }
            }
        });
    }


  //**************************************************************************
  //** terminateConnectionPool
  //**************************************************************************
  /** Used to terminate the connection pool, closing all active connections.
   */
    public void terminateConnectionPool() throws SQLException {
        if (connectionPool!=null){
            connectionPool.close();
            connectionPool = null;
        }
    }


  //**************************************************************************
  //** setConnectionPoolSize
  //**************************************************************************
  /** Used to specify the size of the connection pool. The pool size must be
   *  set before initializing the connection pool. If the pool size is not
   *  defined, the connection pool will default to 15.
   */
    public void setConnectionPoolSize(int maxConnections){
        if (connectionPool!=null) return;
        this.maxConnections = maxConnections;
    }


  //**************************************************************************
  //** getConnectionPoolSize
  //**************************************************************************
  /** Returns the size of the connection pool.
   */
    public int getConnectionPoolSize(){
        return maxConnections;
    }


  //**************************************************************************
  //** getConnectionPool
  //**************************************************************************
  /** Returns the connection pool that was created via the initConnectionPool
   *  method. Returns null if the connection pool has not been not initialized
   *  or if the connection pool has been terminated.
   */
    public ConnectionPool getConnectionPool(){
        return connectionPool;
    }


  //**************************************************************************
  //** setConnectionPoolDataSource
  //**************************************************************************
  /** Used to set the ConnectionPoolDataSource for the database. Typically,
   *  the getConnectionPoolDataSource() method is used to create a
   *  ConnectionPoolDataSource. This method allows you to specify a different
   *  ConnectionPoolDataSource.
   */
    public void setConnectionPoolDataSource(ConnectionPoolDataSource dataSource){
        this.ConnectionPoolDataSource = dataSource;
    }


  //**************************************************************************
  //** getConnectionPoolDataSource
  //**************************************************************************
  /** Used to instantiate a ConnectionPoolDataSource for the database. The
   *  ConnectionPoolDataSource is typically used to create a JDBC Connection
   *  Pool.
   */
    public ConnectionPoolDataSource getConnectionPoolDataSource() throws SQLException {

        if (ConnectionPoolDataSource!=null) return ConnectionPoolDataSource;


        if (driver==null) throw new SQLException(
            "Failed to create a ConnectionPoolDataSource. Please specify a driver.");

        String className = null;
        java.util.HashMap<String, Object> methods = new java.util.HashMap<>();


        if (driver.equals("sqlite")){

            className = "org.sqlite.SQLiteConnectionPoolDataSource";
            methods.put("setUrl", "jdbc:sqlite:" + host);

            /*
            javax.sql.DataSource sqliteDS = new DataSource();
            sqliteDS.setURL ("jdbc:sqlite://" + name);
            dataSource = sqliteDS;
            */
        }
        else if (driver.equals("derby")){

            className = ("org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource");

            methods.put("setDatabaseName", host);
            methods.put("setCreateDatabase", "create");

        }
        else if (driver.equals("h2")){

            className = ("org.h2.jdbcx.JdbcDataSource");


          //URL requirements changed from 1.x to 2.x. Starting with 2.x, we
          //now have to add properties to the end of the url
            String url = null;
            java.sql.Driver d = driver.getDriver();
            try{
                if (d==null) d = driver.load();
                if (d.getMajorVersion()>1){
                    url = getURL(true);
                }
            }
            catch(Exception e){}


            methods.put("setURL", url==null ? getURL(false) : url);
            methods.put("setUser", username);
            methods.put("setPassword", password);

        }
        else if (driver.equals("sqlserver")){ //mssql

            className = ("com.microsoft.sqlserver.jdbc.SQLServerXADataSource");

            methods.put("setDatabaseName", name);
            methods.put("setServerName", host);
            methods.put("setUser", username);
            methods.put("setPassword", password);

        }
        else if (driver.equals("postgresql")){ //pgsql

            className = ("org.postgresql.ds.PGConnectionPoolDataSource");

            methods.put("setDatabaseName", name);
            methods.put("setServerName", host);
            methods.put("setPortNumber", port);
            methods.put("setUser", username);
            methods.put("setPassword", password);

        }
        else if (driver.equals("mysql")){

            className = ("com.mysql.cj.jdbc.MysqlConnectionPoolDataSource");

            methods.put("setDatabaseName", name);
            methods.put("setServerName", host);
            methods.put("setPortNumber", port); //setPort?
            methods.put("setUser", username);
            methods.put("setPassword", password);

        }
        else if (driver.equals("oracle")){

            String connDriver = "thin";
            String connService = "";


            className = ("oracle.jdbc.pool.OracleConnectionPoolDataSource");

            methods.put("setDriverType", connDriver);
            methods.put("setServerName", host);
            methods.put("setPortNumber", port);
            methods.put("setServiceName", connService);
            methods.put("setUser", username);
            methods.put("setPassword", password);
        }
        else if (driver.equals("jtds")){

            className = ("net.sourceforge.jtds.jdbcx.JtdsDataSource");

            methods.put("setDatabaseName", name);
            methods.put("setServerName", host);
            methods.put("setUser", username);
            methods.put("setPassword", password);

        }

      //Instantiate the ConnectionPoolDataSource
        if (className!=null){
            try{
                Class classToLoad = Class.forName(className);
                Object instance = classToLoad.newInstance();

                java.util.Iterator<String> it = methods.keySet().iterator();
                while (it.hasNext()){
                    String methodName = it.next();
                    Object parameter = methods.get(methodName);
                    if (parameter!=null){
                        java.lang.reflect.Method method = null;
                        if (parameter instanceof String)
                            method = classToLoad.getMethod(methodName, stringType);
                        else if (parameter instanceof Integer)
                            method = classToLoad.getMethod(methodName, integerType);

                        if (method!=null) method.invoke(instance, new Object[] { parameter });
                    }
                }
                ConnectionPoolDataSource = (ConnectionPoolDataSource) instance;
                return ConnectionPoolDataSource;
            }
            catch(Exception e){
                throw new SQLException("Failed to instantiate the ConnectionPoolDataSource.", e);
            }

        }

        throw new SQLException("Failed to find a suitable ConnectionPoolDataSource.");
    }


  //**************************************************************************
  //** getRecord
  //**************************************************************************
  /** Used to retrieve a single record from this database.
   */
    public javaxt.sql.Record getRecord(String sql) throws SQLException {
        try (Connection conn = getConnection()){
            return conn.getRecord(sql);
        }
    }


  //**************************************************************************
  //** getRecords
  //**************************************************************************
  /** Used to retrieve records from this database. Note that this method
   *  relies on a Generator to yield records. This is fine for relatively
   *  small record sets. However, for large record sets, we recommend opening
   *  a database connection first and calling Connection.getRecords() like
   *  this:
   <pre>
        try (Connection conn = database.getConnection()){
            return conn.getRecords(sql);
        }
   </pre>
   */
    public Iterable<javaxt.sql.Record> getRecords(String sql) throws SQLException {
        return new javaxt.utils.Generator<javaxt.sql.Record>(){
            public void run() throws InterruptedException {
                try (Connection conn = getConnection()){

                    for (javaxt.sql.Record record : conn.getRecords(sql)){
                        try{
                            this.yield(record);
                        }
                        catch(InterruptedException e){
                            return;
                        }
                    }

                }
                catch(Exception e){
                    RuntimeException ex = new RuntimeException(e.getMessage());
                    ex.setStackTrace(e.getStackTrace());
                    throw ex;
                }
            }
        };
    }


  //**************************************************************************
  //** getTables
  //**************************************************************************
  /** Used to retrieve an array of tables and columns found in this database.
   */
    public Table[] getTables() throws SQLException {
        if (tables!=null) return tables;
        try (Connection conn = getConnection()){
            return getTables(conn);
        }
    }


  //**************************************************************************
  //** getTables
  //**************************************************************************
  /** Used to retrieve an array of tables and columns found in a database.
   */
    public static Table[] getTables(Connection conn){
        Database database = conn.getDatabase();
        if (database!=null){
            if (database.tables!=null) return database.tables;
        }

        java.util.ArrayList<Table> tables = new java.util.ArrayList<>();
        try{
            DatabaseMetaData dbmd = conn.getConnection().getMetaData();
            try(ResultSet rs = dbmd.getTables(null,null,null,getTableFilter(database))){
                while (rs.next()) {
                    tables.add(new Table(rs, dbmd));
                }
            }
        }
        catch(Exception e){
        }
        Table[] arr = tables.toArray(new Table[tables.size()]);
        if (database!=null){
            if (database.cacheMetadata) database.tables = arr;
        }
        return arr;
    }


  //**************************************************************************
  //** getTableNames
  //**************************************************************************
  /** Used to retrieve an array of table names found in the database. If a
   *  table is part of a schema, the schema name is prepended to the table
   *  name. This method is significantly faster than the getTables() method
   *  which returns the full metadata for each table.
   */
    public String[] getTableNames() throws SQLException {
        java.util.ArrayList<javaxt.utils.Record> tableNames = new java.util.ArrayList<>();


        if (tables!=null){
            for (Table table : tables){
                javaxt.utils.Record record = new javaxt.utils.Record();
                record.set("schema", table.getSchema());
                record.set("table", table.getName());
                tableNames.add(record);
            }
        }
        else{
            try (Connection conn = getConnection()){

                DatabaseMetaData dbmd = conn.getConnection().getMetaData();
                try (ResultSet rs = dbmd.getTables(null,null,null,getTableFilter(this))){
                    while (rs.next()) {
                        javaxt.utils.Record record = new javaxt.utils.Record();
                        record.set("schema", rs.getString("TABLE_SCHEM"));
                        record.set("table", rs.getString("TABLE_NAME"));
                        tableNames.add(record);
                    }
                }
            }
        }

        String[] arr = new String[tableNames.size()];
        for (int i=0; i<arr.length; i++){
            javaxt.utils.Record record = tableNames.get(i);
            String tableName = record.get("table").toString();
            String schemaName = record.get("schema").toString();
            if (schemaName!=null && !schemaName.isEmpty()){
                tableName = schemaName + "." + tableName;
            }
            arr[i] = tableName;
        }
        java.util.Arrays.sort(arr);
        return arr;
    }


  //**************************************************************************
  //** getTableFilter
  //**************************************************************************
  /** Returns a filter used to generate a list of tables via DatabaseMetaData
   */
    private static String[] getTableFilter(Database database){
        if (database!=null){
            Driver driver = database.getDriver();
            if (driver!=null && driver.equals("PostgreSQL")){
                return new String[]{"TABLE", "FOREIGN TABLE"};
            }
        }
        return new String[]{"TABLE"};
    }


  //**************************************************************************
  //** getCatalogs
  //**************************************************************************
  /** Used to retrieve a list of available catalogs (aka databases) found on
   *  this server.
   */
    public String[] getCatalogs() throws SQLException{
        try (Connection conn = getConnection()){
            return getCatalogs(conn);
        }
    }


  //**************************************************************************
  //** getCatalogs
  //**************************************************************************
  /**  Used to retrieve a list of available catalogs (aka databases) found on
   *   a server.
   */
    public static String[] getCatalogs(Connection conn){
        Database database = conn.getDatabase();
        if (database!=null){
            if (database.catalogs!=null) return database.catalogs;
        }

        java.util.TreeSet<String> catalogs = new java.util.TreeSet<String>();
        try{
            DatabaseMetaData dbmd = conn.getConnection().getMetaData();
            try (ResultSet rs = dbmd.getCatalogs()){
                while (rs.next()) {
                    catalogs.add(rs.getString(1));
                }
            }
        }
        catch(Exception e){
            return null;
        }

        String[] arr = catalogs.toArray(new String[catalogs.size()]);
        if (database!=null){
            if (database.cacheMetadata) database.catalogs = arr;
        }
        return arr;
    }


  //**************************************************************************
  //** getReservedKeywords
  //**************************************************************************
  /** Returns a list of reserved keywords for the current database.
   */
    public String[] getReservedKeywords() throws Exception {
        try(Connection conn = this.getConnection()){
            return getReservedKeywords(conn);
        }
    }


  //**************************************************************************
  //** getReservedKeywords
  //**************************************************************************
  /** Returns a list of reserved keywords for a given database.
   */
    public static String[] getReservedKeywords(Connection conn){
        Database database = conn.getDatabase();
        javaxt.sql.Driver driver = database.getDriver();
        if (driver==null) driver = new Driver("","","");

        if (driver.equals("Firebird")){
            return fbKeywords;
        }
        else if (driver.equals("SQLServer")){
            return msKeywords;
        }
        else if (driver.equals("H2")){


          //Check if a "MODE" is set
            String mode = "";
            java.util.Properties properties = database.getProperties();
            if (properties!=null){
                Object o = properties.get("MODE");
                if (o!=null) mode = o.toString();
            }


            if (mode.equalsIgnoreCase("PostgreSQL")){
                return java.util.stream.Stream.concat(
                    java.util.Arrays.stream(h2Keywords),
                    java.util.Arrays.stream(pgKeywords)
                ).toArray(String[]::new);
            }
            else{
                return h2Keywords;
            }
        }
        else if (driver.equals("PostgreSQL")){

          //Try to get reserved keywords from the database. Note that in PostgreSQL
          //"non-reserved" keywords are key words that are explicitly known to
          //the parser but are allowed as column or table names. Therefore, we
          //will ignore "non-reserved" keywords from our query.
            if (pgKeywords==null){
                java.util.HashSet<String> arr = new java.util.HashSet<>();

                try (java.sql.Statement stmt = conn.getConnection().createStatement(
                    java.sql.ResultSet.TYPE_FORWARD_ONLY,
                    java.sql.ResultSet.CONCUR_READ_ONLY,
                    java.sql.ResultSet.FETCH_FORWARD)){

                    try (java.sql.ResultSet rs = stmt.executeQuery(
                    "select word from pg_get_keywords() where catcode='R'")){
                        while(rs.next()){
                            arr.add(rs.getString(1));
                        }
                    }
                }
                catch(java.sql.SQLException e){
                    e.printStackTrace();
                }
                String[] keywords = new String[arr.size()];
                int i=0;
                java.util.Iterator<String> it = arr.iterator();
                while (it.hasNext()){
                    keywords[i] = it.next();
                    i++;
                }
                pgKeywords = keywords;
            }

            return pgKeywords;
        }
        else{
            return ansiKeywords;
        }
    }


  //**************************************************************************
  //** enableMetadataCache
  //**************************************************************************
  /** Used to enable/disable metadata caching. If caching is enabled, calls
   *  to getTables() and getCatalogs() will return cached results. This is
   *  appropriate if the database schema doesn't change often and may increase
   *  performance when inserting and updating records via the Recordset class.
   *  @param b If true, will cache database metadata. If false, will disable
   *  metadata caching and delete any information than was previously cached.
   */
    public void enableMetadataCache(boolean b){
        cacheMetadata = b;
        if (b==false){
            tables = null;
            catalogs = null;
        }
    }


  //**************************************************************************
  //** addModel
  //**************************************************************************
  /** Used to register a javaxt.sql.Model with the database. The model is
   *  initialized and associated with the database connection pool. Once the
   *  model is initialized, it can be used to find records in the database
   *  and execute CRUD operations.
   *  @param c A Java class that extends the javaxt.sql.Model abstract class.
   */
    public void addModel(Class c) throws SQLException {


      //Check if class is a model
        if (!Model.class.isAssignableFrom(c)){
            throw new IllegalArgumentException();
        }


      //Initialize connectionPool as needed
        if (connectionPool==null) initConnectionPool();
        if (connectionPool==null){
            throw new SQLException("Connection pool has not been initialized");
        }

        Model.init(c, connectionPool);
    }


  //**************************************************************************
  //** displayDbProperties
  //**************************************************************************
    public static void displayDbProperties(Connection conn){
        if (conn==null){
            System.out.println("Error: Connection is null");
            return;
        }

        try{
            java.sql.DatabaseMetaData dm = conn.getConnection().getMetaData();
            System.out.println("Driver Information");
            System.out.println("\tDriver Name: "+ dm.getDriverName());
            System.out.println("\tDriver Version: "+ dm.getDriverVersion ());
            System.out.println("\nDatabase Information ");
            System.out.println("\tDatabase Name: "+ dm.getDatabaseProductName());
            System.out.println("\tDatabase Version: "+ dm.getDatabaseProductVersion());
            System.out.println("Avalilable Catalogs ");

            try (java.sql.ResultSet rs = dm.getCatalogs()){
                while(rs.next()){
                    System.out.println("\tcatalog: "+ rs.getString(1));
                }
            }
        }
        catch(Exception e){
           e.printStackTrace();
        }
    }


  //**************************************************************************
  //** toString
  //**************************************************************************
  /**  Returns database connection information encapsulated by this class.
   */
    public String toString(){
        StringBuffer str = new StringBuffer();
        str.append("Name: " + name + "\r\n");
        str.append("Host: " + host + "\r\n");
        str.append("Port: " + port + "\r\n");
        str.append("UserName: " + username + "\r\n");
        str.append("Driver: " + driver + "\r\n");
        str.append("URL: " + getURL(false) + "\r\n");
        str.append("ConnStr: " + this.getConnectionString());
        return str.toString();
    }


  //**************************************************************************
  //** clone
  //**************************************************************************
    public Database clone(){
        Database db = new Database(name, host, port==null ? -1 : port, username, password, driver);
        if (properties!=null) db.properties = (java.util.Properties) properties.clone();
        db.querystring = querystring;
        return db;
    }


  /** Firebird reserved keywords. */
    private static final String[] fbKeywords = new String[]{
        "ADD","ADMIN","ALL","ALTER","AND","ANY","AS","AT","AVG","BEGIN","BETWEEN",
        "BIGINT","BIT_LENGTH","BLOB","BOTH","BY","CASE","CAST","CHAR","CHAR_LENGTH",
        "CHARACTER","CHARACTER_LENGTH","CHECK","CLOSE","COLLATE","COLUMN","COMMIT",
        "CONNECT","CONSTRAINT","COUNT","CREATE","CROSS","CURRENT","CURRENT_CONNECTION",
        "CURRENT_DATE","CURRENT_ROLE","CURRENT_TIME","CURRENT_TIMESTAMP",
        "CURRENT_TRANSACTION","CURRENT_USER","CURSOR","DATE","DAY","DEC","DECIMAL",
        "DECLARE","DEFAULT","DELETE","DISCONNECT","DISTINCT","DOUBLE","DROP","ELSE",
        "END","ESCAPE","EXECUTE","EXISTS","EXTERNAL","EXTRACT","FETCH","FILTER",
        "FLOAT","FOR","FOREIGN","FROM","FULL","FUNCTION","GDSCODE","GLOBAL","GRANT",
        "GROUP","HAVING","HOUR","IN","INDEX","INNER","INSENSITIVE","INSERT","INT",
        "INTEGER","INTO","IS","JOIN","LEADING","LEFT","LIKE","LONG","LOWER","MAX",
        "MAXIMUM_SEGMENT","MERGE","MIN","MINUTE","MONTH","NATIONAL","NATURAL","NCHAR",
        "NO","NOT","NULL","NUMERIC","OCTET_LENGTH","OF","ON","ONLY","OPEN","OR",
        "ORDER","OUTER","PARAMETER","PLAN","POSITION","POST_EVENT","PRECISION",
        "PRIMARY","PROCEDURE","RDB$DB_KEY","REAL","RECORD_VERSION","RECREATE",
        "RECURSIVE","REFERENCES","RELEASE","RETURNING_VALUES","RETURNS","REVOKE",
        "RIGHT","ROLLBACK","ROW_COUNT","ROWS","SAVEPOINT","SECOND","SELECT","SENSITIVE",
        "SET","SIMILAR","SMALLINT","SOME","SQLCODE","SQLSTATE","START","SUM","TABLE",
        "THEN","TIME","TIMESTAMP","TO","TRAILING","TRIGGER","TRIM","UNION","UNIQUE",
        "UPDATE","UPPER","USER","USING","VALUE","VALUES","VARCHAR","VARIABLE","VARYING",
        "VIEW","WHEN","WHERE","WHILE","WITH","YEAR"
    };


  /** SQLServer reserved keywords. Source:
   *  https://msdn.microsoft.com/en-us/library/ms189822.aspx
   */
    private static final String[] msKeywords = new String[]{
        "ADD","ALL","ALTER","AND","ANY","AS","ASC","AUTHORIZATION","BACKUP","BEGIN",
        "BETWEEN","BREAK","BROWSE","BULK","BY","CASCADE","CASE","CHECK","CHECKPOINT",
        "CLOSE","CLUSTERED","COALESCE","COLLATE","COLUMN","COMMIT","COMPUTE",
        "CONSTRAINT","CONTAINS","CONTAINSTABLE","CONTINUE","CONVERT","CREATE","CROSS",
        "CURRENT","CURRENT_DATE","CURRENT_TIME","CURRENT_TIMESTAMP","CURRENT_USER",
        "CURSOR","DATABASE","DBCC","DEALLOCATE","DECLARE","DEFAULT","DELETE","DENY",
        "DESC","DISK","DISTINCT","DISTRIBUTED","DOUBLE","DROP","DUMP","ELSE","END",
        "ERRLVL","ESCAPE","EXCEPT","EXEC","EXECUTE","EXISTS","EXIT","EXTERNAL",
        "FETCH","FILE","FILLFACTOR","FOR","FOREIGN","FREETEXT","FREETEXTTABLE",
        "FROM","FULL","FUNCTION","GOTO","GRANT","GROUP","HAVING","HOLDLOCK","IDENTITY",
        "IDENTITY_INSERT","IDENTITYCOL","IF","IN","INDEX","INNER","INSERT","INTERSECT",
        "INTO","IS","JOIN","KEY","KILL","LEFT","LIKE","LINENO","LOAD","MERGE","NATIONAL",
        "NOCHECK","NONCLUSTERED","NOT","NULL","NULLIF","OF","OFF","OFFSETS","ON",
        "OPEN","OPENDATASOURCE","OPENQUERY","OPENROWSET","OPENXML","OPTION","OR",
        "ORDER","OUTER","OVER","PERCENT","PIVOT","PLAN","PRECISION","PRIMARY","PRINT",
        "PROC","PROCEDURE","PUBLIC","RAISERROR","READ","READTEXT","RECONFIGURE",
        "REFERENCES","REPLICATION","RESTORE","RESTRICT","RETURN","REVERT","REVOKE",
        "RIGHT","ROLLBACK","ROWCOUNT","ROWGUIDCOL","RULE","SAVE","SCHEMA","SECURITYAUDIT",
        "SELECT","SEMANTICKEYPHRASETABLE","SEMANTICSIMILARITYDETAILSTABLE",
        "SEMANTICSIMILARITYTABLE","SESSION_USER","SET","SETUSER","SHUTDOWN","SOME",
        "STATISTICS","SYSTEM_USER","TABLE","TABLESAMPLE","TEXTSIZE","THEN","TO",
        "TOP","TRAN","TRANSACTION","TRIGGER","TRUNCATE","TRY_CONVERT","TSEQUAL",
        "UNION","UNIQUE","UNPIVOT","UPDATE","UPDATETEXT","USE","USER","VALUES",
        "VARYING","VIEW","WAITFOR","WHEN","WHERE","WHILE","WITH","WITHIN GROUP",
        "WRITETEXT"
    };


  /** H2 reserved keywords. Source:
   *  http://www.h2database.com/html/advanced.html#keywords
   */
    private static final String[] h2Keywords = new String[]{
        "ALL","AND","ARRAY","AS","BETWEEN","BOTH","CASE","CHECK","CONSTRAINT",
        "CROSS","CURRENT_DATE","CURRENT_TIME","CURRENT_TIMESTAMP","CURRENT_USER",
        "DISTINCT","EXCEPT","EXISTS","FALSE","FETCH","FILTER","FOR","FOREIGN",
        "FROM","FULL","GROUP","GROUPS","HAVING","IF","ILIKE","IN","INNER",
        "INTERSECT","INTERSECTS","INTERVAL","IS","JOIN","KEY","LEADING","LEFT","LIKE",
        "LIMIT","LOCALTIME","LOCALTIMESTAMP","MINUS","NATURAL","NOT","NULL","OFFSET",
        "ON","OR","ORDER","OVER","PARTITION","PRIMARY","QUALIFY","RANGE","REGEXP",
        "RIGHT","ROW","_ROWID_","ROWNUM","ROWS","SELECT","SYSDATE","SYSTIME",
        "SYSTIMESTAMP","TABLE","TODAY","TOP","TRAILING","TRUE","UNION","UNIQUE",
        "VALUE","VALUES","WHERE","WINDOW","WITH","YEAR"
    };


  /** PostgreSQL reserved keywords generated via the following query using PG15:
   <pre>
    select string_agg('"' || UPPER(word) || '"', ',') from pg_get_keywords() where catcode='R';
   </pre>
   */
    private static String[] pgKeywords = new String[]{
        "ALL","ANALYSE","ANALYZE","AND","ANY","ARRAY","AS","ASC","ASYMMETRIC",
        "BOTH","CASE","CAST","CHECK","COLLATE","COLUMN","CONSTRAINT","CREATE",
        "CURRENT_CATALOG","CURRENT_DATE","CURRENT_ROLE","CURRENT_TIME",
        "CURRENT_TIMESTAMP","CURRENT_USER","DEFAULT","DEFERRABLE","DESC",
        "DISTINCT","DO","ELSE","END","EXCEPT","FALSE","FETCH","FOR","FOREIGN",
        "FROM","GRANT","GROUP","HAVING","IN","INITIALLY","INTERSECT","INTO",
        "LATERAL","LEADING","LIMIT","LOCALTIME","LOCALTIMESTAMP","NOT","NULL",
        "OFFSET","ON","ONLY","OR","ORDER","PLACING","PRIMARY","REFERENCES",
        "RETURNING","SELECT","SESSION_USER","SOME","SYMMETRIC","TABLE","THEN",
        "TO","TRAILING","TRUE","UNION","UNIQUE","USER","USING","VARIADIC",
        "WHEN","WHERE","WINDOW","WITH"
    };


  /** Superset of SQL-92, SQL-99, SQL-2003 reserved keywords. */
    private static String[] ansiKeywords = new String[]{
        "ABSOLUTE","ACTION","ADD","AFTER","ALL","ALLOCATE","ALTER","AND","ANY","ARE",
        "ARRAY","AS","ASC","ASENSITIVE","ASSERTION","ASYMMETRIC","AT","ATOMIC",
        "AUTHORIZATION","AVG","BEFORE","BEGIN","BETWEEN","BIGINT","BINARY","BIT",
        "BIT_LENGTH","BLOB","BOOLEAN","BOTH","BREADTH","BY","CALL","CALLED","CASCADE",
        "CASCADED","CASE","CAST","CATALOG","CHAR","CHAR_LENGTH","CHARACTER","CHARACTER_LENGTH",
        "CHECK","CLOB","CLOSE","COALESCE","COLLATE","COLLATION","COLUMN","COMMIT",
        "CONDITION","CONNECT","CONNECTION","CONSTRAINT","CONSTRAINTS","CONSTRUCTOR",
        "CONTAINS","CONTINUE","CONVERT","CORRESPONDING","COUNT","CREATE","CROSS",
        "CUBE","CURRENT","CURRENT_DATE","CURRENT_DEFAULT_TRANSFORM_GROUP","CURRENT_PATH",
        "CURRENT_ROLE","CURRENT_TIME","CURRENT_TIMESTAMP","CURRENT_TRANSFORM_GROUP_FOR_TYPE",
        "CURRENT_USER","CURSOR","CYCLE","DATA","DATE","DAY","DEALLOCATE","DEC","DECIMAL",
        "DECLARE","DEFAULT","DEFERRABLE","DEFERRED","DELETE","DEPTH","DEREF","DESC",
        "DESCRIBE","DESCRIPTOR","DETERMINISTIC","DIAGNOSTICS","DISCONNECT","DISTINCT",
        "DO","DOMAIN","DOUBLE","DROP","DYNAMIC","EACH","ELEMENT","ELSE","ELSEIF",
        "END","EQUALS","ESCAPE","EXCEPT","EXCEPTION","EXEC","EXECUTE","EXISTS","EXIT",
        "EXTERNAL","EXTRACT","FALSE","FETCH","FILTER","FIRST","FLOAT","FOR","FOREIGN",
        "FOUND","FREE","FROM","FULL","FUNCTION","GENERAL","GET","GLOBAL","GO","GOTO",
        "GRANT","GROUP","GROUPING","HANDLER","HAVING","HOLD","HOUR","IDENTITY","IF",
        "IMMEDIATE","IN","INDICATOR","INITIALLY","INNER","INOUT","INPUT","INSENSITIVE",
        "INSERT","INT","INTEGER","INTERSECT","INTERVAL","INTO","IS","ISOLATION",
        "ITERATE","JOIN","KEY","LANGUAGE","LARGE","LAST","LATERAL","LEADING","LEAVE",
        "LEFT","LEVEL","LIKE","LOCAL","LOCALTIME","LOCALTIMESTAMP","LOCATOR","LOOP",
        "LOWER","MAP","MATCH","MAX","MEMBER","MERGE","METHOD","MIN","MINUTE","MODIFIES",
        "MODULE","MONTH","MULTISET","NAMES","NATIONAL","NATURAL","NCHAR","NCLOB",
        "NEW","NEXT","NO","NONE","NOT","NULL","NULLIF","NUMERIC","OBJECT","OCTET_LENGTH",
        "OF","OLD","ON","ONLY","OPEN","OPTION","OR","ORDER","ORDINALITY","OUT","OUTER",
        "OUTPUT","OVER","OVERLAPS","PAD","PARAMETER","PARTIAL","PARTITION","PATH",
        "POSITION","PRECISION","PREPARE","PRESERVE","PRIMARY","PRIOR","PRIVILEGES",
        "PROCEDURE","PUBLIC","RANGE","READ","READS","REAL","RECURSIVE","REF","REFERENCES",
        "REFERENCING","RELATIVE","RELEASE","REPEAT","RESIGNAL","RESTRICT","RESULT",
        "RETURN","RETURNS","REVOKE","RIGHT","ROLE","ROLLBACK","ROLLUP","ROUTINE",
        "ROW","ROWS","SAVEPOINT","SCHEMA","SCOPE","SCROLL","SEARCH","SECOND","SECTION",
        "SELECT","SENSITIVE","SESSION","SESSION_USER","SET","SETS","SIGNAL","SIMILAR",
        "SIZE","SMALLINT","SOME","SPACE","SPECIFIC","SPECIFICTYPE","SQL","SQLCODE",
        "SQLERROR","SQLEXCEPTION","SQLSTATE","SQLWARNING","START","STATE","STATIC",
        "SUBMULTISET","SUBSTRING","SUM","SYMMETRIC","SYSTEM","SYSTEM_USER","TABLE",
        "TABLESAMPLE","TEMPORARY","THEN","TIME","TIMESTAMP","TIMEZONE_HOUR","TIMEZONE_MINUTE",
        "TO","TRAILING","TRANSACTION","TRANSLATE","TRANSLATION","TREAT","TRIGGER",
        "TRIM","TRUE","UNDER","UNDO","UNION","UNIQUE","UNKNOWN","UNNEST","UNTIL",
        "UPDATE","UPPER","USAGE","USER","USING","VALUE","VALUES","VARCHAR","VARYING",
        "VIEW","WHEN","WHENEVER","WHERE","WHILE","WINDOW","WITH","WITHIN","WITHOUT",
        "WORK","WRITE","YEAR","ZONE"
    };

}
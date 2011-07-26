package javaxt.sql;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
//import java.sql.DriverManager;

//******************************************************************************
//**  Database
//******************************************************************************
/**
 *   Object used to represent all of the information required to connect to a
 *   database. 
 *
 ******************************************************************************/

public class Database {
    
    private String name; //name of the catalog used to store tables, views, etc.
    private String host;
    private Integer port = -1;
    private String username;
    private String password;
    private Driver driver; 
    //private String url;
    //private String path;
    private String props;

    private java.sql.Connection Connection = null;
    private java.sql.Driver Driver = null;

    
    
    /** Static list of drivers and corresponding metadata */
    private static Driver[] drivers = new Driver[]{
        new Driver("SQLServer","com.microsoft.sqlserver.jdbc.SQLServerDriver","jdbc:sqlserver"),
        new Driver("DB2","com.ibm.db2.jcc.DB2Driver","jdbc:db2"), //"COM.ibm.db2.jdbc.net.DB2Driver"
        new Driver("Sybase","com.sybase.jdbc3.jdbc.SybDriver","jdbc:sybase"),
        new Driver("PostgreSQL","org.postgresql.Driver","jdbc:postgresql"),
        new Driver("Derby","org.apache.derby.jdbc.EmbeddedDriver","jdbc:derby"),
        new Driver("SQLite","org.sqlite.JDBC","jdbc:sqlite"),
        new Driver("Microsoft Access","sun.jdbc.odbc.JdbcOdbcDriver","jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)}"),

      //The rest of these drivers have not been tested
        new Driver("FrontBase", "com.frontbase.jdbc.FBJDriver", "jdbc:FrontBase"),
        new Driver("Informix", "com.informix.jdbc.IfxDriver", "jdbc:informix-sqli"),
        new Driver("Cache", "com.intersys.jdbc.CacheDriver", "jdbc:Cache"),
        new Driver("microsoft", "com.microsoft.jdbc.sqlserver.SQLServerDriver", "jdbc:microsoft"),
        new Driver("Mimer", "com.mimer.jdbc.Driver", "jdbc:mimer"),
        new Driver("MySQL", "com.mysql.jdbc.Driver", "jdbc:mysql"),
        new Driver("Teradata", "com.ncr.teradata.TeraDriver", "jdbc:teradata"),
        new Driver("Pervasive", "com.pervasive.jdbc.v2.Driver", "jdbc:pervasive"),
        new Driver("Pointbase", "com.pointbase.jdbc.jdbcUniversalDriver", "jdbc:pointbase"),
        new Driver("pointbase micro", "com.pointbase.me.jdbc.jdbcDriver", "jdbc:pointbase:micro"),
        new Driver("Daffodil", "in.co.daffodil.db.jdbc.DaffodilDBDriver", "jdbc:daffodil"),
        new Driver("daffodilDB", "in.co.daffodil.db.rmi.RmiDaffodilDBDriver", "jdbc:daffodilDB"),
        new Driver("JTDS", "net.sourceforge.jtds.jdbc.Driver", "jdbc:jtds"), //Open source JDBC 3.0 type 4 driver for Microsoft SQL Server and Sybase ASE
        new Driver("Oracle", "oracle.jdbc.driver.OracleDriver", "jdbc:oracle"),
        new Driver("derby net", "org.apache.derby.jdbc.ClientDriver", "jdbc:derby:net"),
        //new Driver("derby //", "org.apache.derby.jdbc.ClientDriver", "jdbc:derby://"),
        new Driver("Firebird", "org.firebirdsql.jdbc.FBDriver", "jdbc:firebirdsql"),
        new Driver("H2", "org.h2.Driver", "jdbc:h2"),
        new Driver("HyperSQL", "org.hsqldb.jdbcDriver", "jdbc:hsqldb"),
        new Driver("odbc", "sun.jdbc.odbc.JdbcOdbcDriver", "jdbc:odbc")

    };
    
    /** Microsoft SQL Server database driver. */
    public static Driver SQLServer = findDriver("SQLServer");

    /** IBM DB2 database driver. */
    public static Driver DB2 = findDriver("DB2");

    /** Sybase ASE database driver. */
    public static Driver Sybase = findDriver("Sybase");

    /** PostgreSQL database driver. */
    public static Driver PostgreSQL = findDriver("PostgreSQL");

    /** Derby database driver. */
    public static Driver Derby = findDriver("Derby");
    
    /** SQLite database driver. */
    public static Driver SQLite = findDriver("SQLite");

    /** Microsoft Access database driver. */
    public static Driver Access = findDriver("Microsoft Access");
    
    public static Driver FrontBase = findDriver("FrontBase");
    public static Driver Informix = findDriver("Informix");
    public static Driver Cache = findDriver("Cache");
    public static Driver Mimer = findDriver("Mimer");
    public static Driver MySQL = findDriver("MySQL");
    public static Driver Teradata = findDriver("Teradata");
    public static Driver Pervasive = findDriver("Pervasive");
    public static Driver Pointbase = findDriver("Pointbase");
    //public static Driver pointbase micro = findDriver("pointbase micro");
    public static Driver Daffodil = findDriver("Daffodil");
    //public static Driver daffodilDB = findDriver("daffodilDB");
    public static Driver JTDS = findDriver("JTDS");
    public static Driver Oracle = findDriver("Oracle");
    public static Driver Firebird = findDriver("Firebird");
    public static Driver H2 = findDriver("H2");
    public static Driver HyperSQL = findDriver("HyperSQL");
    public static Driver ODBC = findDriver("odbc");



  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /**  Creates a new instance of this class. Note that you will need to set the
   *   name, host, port, username, password, and driver in order to create
   *   a connection to the database.
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
        this.port = port;
        this.username = username;
        this.password = password;
        this.driver = driver;
        //this.url = this.getURL();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /**  Creates a new instance of Database using a jdbc connection. */
    
    public Database(java.sql.Connection conn){
        try{
            this.Connection = conn;
            DatabaseMetaData dbmd = conn.getMetaData();
            this.name = conn.getCatalog();
            this.username = dbmd.getUserName();
            parseURL(dbmd.getURL());
            //dbmd.getDriverName();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


    
  //**************************************************************************
  //** Constructor 
  //**************************************************************************
  /** Creates a new instance of Database using a jdbc connection string.
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
        driver = findDriver(url.getProtocol());        
        if (name==null){
            name = url.getPath();
            if (this.name!=null && this.name.startsWith("/")){
                this.name = this.name.substring(1);
            }
        }
        

      //Extract additional connection parameters
        StringBuffer props = null;
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
                if (props==null) props = new StringBuffer();
                props.append(arrParams[0] + "=" + arrParams[1] + ";");
            }
        }
        if (props!=null) this.props = props.toString();
    }
    
    
    
  //**************************************************************************
  //** setName 
  //**************************************************************************
  /** Sets the name of the catalog used to store tables, views, etc. */

    public void setName(String name){
        this.name = name;
    }


  //**************************************************************************
  //** getName
  //**************************************************************************
  /** Gets the name of the catalog used to store tables, views, etc. */

    public String getName(){
        return name;
    }
   
    

    
    
  //**************************************************************************
  //** setHost
  //**************************************************************************
  /** Used to set the path to the database (server name and port). */

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
        host = host.trim();
        if (host.contains(":")){
            try{
            this.host = host.substring(0, host.indexOf(":"));
            this.port = cint(host.substring(host.indexOf(":")+1));
            }
            catch(Exception e){
                this.host = host;
            }
        }
        else{
            this.host = host;
        }
    }
    
    private int cint(String str){return Integer.valueOf(str).intValue(); }
    
    
    
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
    
    public int getPort(){
        return port;
    }

    
  //**************************************************************************
  //** setDriver
  //**************************************************************************
    
    public void setDriver(Driver driver){
        this.driver = driver;
    }
    
    public void setDriver(String driver){
        this.driver = findDriver(driver);
    }

    public void setDriver(java.sql.Driver driver){
        this.Driver = driver;
        this.driver = findDriver(driver.getClass().getCanonicalName());
    }

    
    
  //**************************************************************************
  //** getDriver
  //**************************************************************************
    
    public Driver getDriver(){
        return driver;
    }

    
  //**************************************************************************
  //** findDriver
  //**************************************************************************
  /** Used to try to find a driver that corresponds to the vendor name, package 
   *  name, or protocol.
   */
    private static Driver findDriver(String driverName){
        for (int i=0; i<drivers.length; i++){
             Driver driver = drivers[i];
             if (driver.equals(driverName)){ 
                 return driver;
             }
        }
        return null;
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

    

    
  //**************************************************************************
  //** getConnectionString
  //**************************************************************************
  /** Returns a JDBC connection string used to connect to the database.
   *  Username and password are appended to the end of the url.
   */
    public String getConnectionString(){
                    
      //Set User Info
        String path = getURL();
        if (username!=null) path += ";user=" + username;
        if (password!=null) path += ";password=" + password;      
        return path;
        
    }

    
  //**************************************************************************
  //** getURL
  //**************************************************************************
  /** Used to construct a JDBC connection string
   */
    private String getURL(){

      //Update Server Name
        String server = host;
        if (port!=null && port>0) server += ":" + port;
        if (driver.getVendor().equals("Derby") || driver.getVendor().equals("SQLite")){
            server = ":" + server;
        }




      //Update Initial Catalog
        String database = "";
        if (name!=null) {

            if (name.trim().length()>0){

                if (driver.getVendor().equals("SQLServer")){
                    database = ";databaseName=" + name;
                }
                else if (driver.getVendor().equals("Derby")){
                    database = ";databaseName=" + name;
                }
                else{
                    database = "/" + name;
                }

            }
        }


        if (driver.getVendor().equals("SQLite")){
            //database = path;
        }
        
        

      //Set Path
        String path = "";
        path = driver.getProtocol() + "://";
        
                
      //Special case for Sybase
        if (driver.getVendor().equals("Sybase")){
            if (path.toLowerCase().contains((CharSequence) "tds:")==false){
                path = driver.getProtocol() + "Tds:"; 
            }
        }
        else if (driver.getVendor().equals("Derby") || driver.getVendor().equals("SQLite")){
            path = driver.getProtocol();
        }



      //Set properties
        String properties = "";
        if (props!=null) properties = ";" + props;



        //System.out.println("Return: " + path + server + database + properties);
        
      //Assemble Connection String
        return path + server + database + properties;

        //return "jdbc:firebirdsql:local:c:/Data/DC Projects/SignatureAnalyst.fdb";
    }
    
    


    
  //**************************************************************************
  //** getConnection
  //**************************************************************************
  /** Used to open a connection to the database. Note the the connection will
   *  need to be closed afterwards.
   */     
    public Connection getConnection(){
        Connection connection = new Connection();
        connection.open(this);
        return connection;
    }



    

    
    
  //**************************************************************************
  //** Connect
  //**************************************************************************
  /**  Used to open a java.sql.Connection to the database. This is a protected 
   *   method called from the connection object in this package.
   */     
    protected java.sql.Connection connect() throws Exception {
          
        if (Driver==null){            
            //System.out.print("Loading Driver...");
            Driver = (java.sql.Driver) Class.forName(driver.getPackageName()).newInstance(); 
            //DriverManager.registerDriver(Driver);
            //System.out.println("Done");
        }
        
        
        if (Connection==null || Connection.isClosed()){
            
            //System.out.print("Attempting to connect...");
            String url = getURL();
            //System.out.println(url);

            java.util.Properties properties = new java.util.Properties();
            if (username!=null){
                properties.put("user", username);
                properties.put("password", password);
            }
            
            Connection = Driver.connect(url, properties);
            //Connection = DriverManager.getConnection(url, username, password);
            //System.out.println("Done");
        }

        return Connection;
    }



  //**************************************************************************
  //** getTables
  //**************************************************************************
  /**  Used to retrieve an array of tables found in this database. This method 
   *   should be called after a connection has been made to the target database.
   */    
    public Table[] getTables(){
        try{
            java.util.TreeSet<Table> tables = new java.util.TreeSet<Table>();
            DatabaseMetaData dbmd = Connection.getMetaData(); 
            ResultSet rs  = dbmd.getTables(null,null,null,new String[]{"TABLE"});
            while (rs.next()) {
                tables.add(new Table(rs, dbmd));  
            }
            rs.close();
            return tables.toArray(new Table[tables.size()]);
        }
        catch(Exception e){
            return null;
        }
    }



  //**************************************************************************
  //** getCatalogs
  //**************************************************************************
  /**  Used to retrieve a list of available databases found on this server.
   */
    public String[] getCatalogs(){
        try{

            java.util.Vector catalogs = new java.util.Vector();
            DatabaseMetaData dbmd = Connection.getMetaData();
            ResultSet rs  = dbmd.getCatalogs();
            while (rs.next()) {
                catalogs.add(rs.getString(1));
            }
            rs.close();

            String[] array = new String[catalogs.size()];
            for (int i=0; i<array.length; i++){
                array[i] = (String) catalogs.get(i);
            }
            return array;
        }
        catch(Exception e){
            printError(e);
            return null;
        }
    }


    public void displayDbProperties(){

        java.sql.DatabaseMetaData dm = null;
        java.sql.ResultSet rs = null;
        try{
           if(Connection!=null){
                dm = Connection.getMetaData();
                System.out.println("Driver Information");
                System.out.println("\tDriver Name: "+ dm.getDriverName());
                System.out.println("\tDriver Version: "+ dm.getDriverVersion ());
                System.out.println("\nDatabase Information ");
                System.out.println("\tDatabase Name: "+ dm.getDatabaseProductName());
                System.out.println("\tDatabase Version: "+ dm.getDatabaseProductVersion());
                System.out.println("Avalilable Catalogs ");

                rs = dm.getCatalogs();
                while(rs.next()){
                     System.out.println("\tcatalog: "+ rs.getString(1));
                }
                rs.close();
                rs = null;

           }else System.out.println("Error: No active Connection");
        }catch(Exception e){
           e.printStackTrace();
        }
        dm=null;
    }

    
    
    
    private static void printError(Exception e){
        System.out.println(e.toString());
        StackTraceElement[] arr = e.getStackTrace();
        for (int i=0; i<arr.length; i++){
             System.out.println(arr[i].toString());
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
        str.append("URL: " + getURL() + "\r\n");
        str.append("ConnStr: " + this.getConnectionString());
        return str.toString();
    }

}

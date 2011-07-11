package javaxt.sql;

//******************************************************************************
//**  Driver
//******************************************************************************
/**
 *   Used to encapsulate information for a driver used to create a database
 *   connection.
 *
 ******************************************************************************/

public class Driver {
    
    private String vendor;
    private String driver;
    private String protocol;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class. Here are some common examples:
   <pre>
    new Driver("SQLServer","com.microsoft.sqlserver.jdbc.SQLServerDriver","jdbc:sqlserver");
    new Driver("DB2","com.ibm.db2.jcc.DB2Driver","jdbc:db2");
    new Driver("Sybase","com.sybase.jdbc3.jdbc.SybDriver","jdbc:sybase");
    new Driver("PostgreSQL","org.postgresql.Driver","jdbc:postgresql");
    new Driver("Derby","org.apache.derby.jdbc.EmbeddedDriver","jdbc:derby");
   </pre>
   *  @param vendor Name the database/vendor. This keyword used extensively in
   *  the javaxt.sql.Recordset class to accomodate inconsistant JDBC implementations
   *  between database vendors. As such, please use the names provided in the
   *  examples above when connecting to SQL Server, DB2, Sybase, and PostgreSQL.
   *  Other databases have not been tested and do not require reserved keywords.
   *  @param driver Class name used to create a java.sql.Driver.
   *  @param protocol Protocol used in the jdbc connection string.
   */
    public Driver(String vendor, String driver, String protocol){
        this.vendor = vendor;
        this.driver = driver;
        this.protocol = protocol;
    }


  //**************************************************************************
  //** getProtocol
  //**************************************************************************
  /** Returns the url protocol used in the jdbc connection string (e.g.
   *  jdbc:sqlserver, jdbc:db2, jdbc:sybase, jdbc:postgresql, jdbc:derby).
   */
    public String getProtocol(){
        return protocol;
    }


  //**************************************************************************
  //** getPackageName
  //**************************************************************************
  /** Returns the class name used to create a new java.sql.Driver (e.g.
   *  com.microsoft.sqlserver.jdbc.SQLServerDriver).
   */
    public String getPackageName(){
        return driver;
    }


  //**************************************************************************
  //** getVendor
  //**************************************************************************
  /** Returns the name the database/vendor (e.g. SQLServer, DB2, Sybase, etc.)
   */
    public String getVendor(){
        return vendor;
    }
    

    public boolean equals(Object obj){
        if (obj instanceof Driver){
            Driver driver = (Driver) obj;
            if (driver.getPackageName().equalsIgnoreCase(this.getPackageName()) &&
                driver.getProtocol().toLowerCase().startsWith(this.getProtocol()) &&
                driver.getVendor().equalsIgnoreCase(this.getVendor())
            ){ 
                 return true;
            }
            else{                
                return false;
            }
        }
        else if(obj instanceof java.lang.String){
            String driverName = obj.toString();
            if (driverName.equalsIgnoreCase(this.getPackageName()) ||
                driverName.toLowerCase().startsWith(this.getProtocol().toLowerCase()) ||
                driverName.equalsIgnoreCase(this.getVendor())
            ){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            return false;
        }
    }
    

  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns the name the database/vendor. Same as getVendor()
   */
    public String toString(){
        return this.getVendor();
    }
}

package javaxt.sql;

//******************************************************************************
//**  Field Class
//******************************************************************************
/**
 *   Used to represent a field in a Recordset.
 *
 ******************************************************************************/

public class Field {

    private String name = null;
    private String type = null;
    private Value value = null;
    private String tableName = null;
    private String schema = null;
    private String className = null;
    private boolean requiresUpdate = false;
    private Table table;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    protected Field(int i, java.sql.ResultSetMetaData rsmd){
         try{ name = getValue(rsmd.getColumnName(i)); } catch(Exception e){}
         try{ tableName = getValue(rsmd.getTableName(i)); } catch(Exception e){}
         try{ schema = getValue(rsmd.getSchemaName(i)); } catch(Exception e){}
         try{ type = getValue(rsmd.getColumnTypeName(i)); } catch(Exception e){}
         try{ className = getValue(rsmd.getColumnClassName(i)); } catch(Exception e){}


       //Special case. Discovered that the column name was returning a
       //table prefix when performing a union quiries with SQLite
        if (name!=null && name.contains(".")){
            String[] arr = name.split("\\.");
            if (arr.length==3){
                name = arr[2];
                tableName = arr[1];
                schema = arr[0];
            }
            else if (arr.length==2){
                name = arr[1];
                tableName = arr[0];
            }
            else if (arr.length==1){
                name = arr[0];
            }
        }
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    private Field(){}


  //**************************************************************************
  //** clone
  //**************************************************************************
    public Field clone(){
        Field field = new Field();
        field.name = name;
        field.type = type;
        field.tableName = tableName;
        field.schema = schema;
        field.className = className;
        field.table = table;
        return field;
    }


  //**************************************************************************
  //** getName
  //**************************************************************************
  /** Returns the name of the column associated with this field. Returns null
   *  if the column name is unknown.
   */
    public String getName(){
        return name;
    }


  //**************************************************************************
  //** getType
  //**************************************************************************
  /** Returns the column type name (e.g. VARCHAR, INTEGER, BLOB, etc).
   */
    public String getType(){
        return type;
    }


  //**************************************************************************
  //** getClassName
  //**************************************************************************
  /** Returns the Java class name that is associated with the column type.
   *  For example, most JDBC drivers map VARCHAR columns to a java.lang.String.
   *  In this case, the method would return "java.lang.String" for the field
   *  class name.
   */
    public String getClassName(){
        return className;
    }

    protected void setClassName(String className){
        this.className = className;
    }


  //**************************************************************************
  //** getValue
  //**************************************************************************
  /** Returns the value for this field.
   */
    public Value getValue(){
        if (value==null) value = new Value(null);
        return value;
    }

    protected void setValue(Value value){
        this.value = value;
    }


  //**************************************************************************
  //** getTable
  //**************************************************************************
  /** @deprecated This method will be refactored in a future release. Use the
   *  getTableName() method to get the name of the table
   */
    public String getTable(){
        return tableName;
    }

    protected Table getT(){ //<- rename to getTable() in a future release...
        return table;
    }

    protected void setTable(Table table){
        this.table = table;
    }


  //**************************************************************************
  //** getTableName
  //**************************************************************************
  /** Returns the name of the table in which this field is found. Returns null
   *  if the table name is unknown.
   */
    public String getTableName(){
        return tableName;
    }

    protected void setTableName(String tableName){
        this.tableName = getValue(tableName);
    }


  //**************************************************************************
  //** getSchema
  //**************************************************************************
  /** Returns the name of the schema to which this field belongs. Schemas are
   *  used to group objects in the database and are often used for access
   *  control. Returns null if the schema name is unknown.
   */
    protected String getSchema(){
        return schema;
    }

    protected void setSchemaName(String schema){
        this.schema = getValue(schema);
    }


  //**************************************************************************
  //** isDirty
  //**************************************************************************
  /** Returns true if the value for this field has changed.
   */
    public boolean isDirty(){
        return requiresUpdate;
    }

    protected void requiresUpdate(boolean b){
        requiresUpdate = b;
    }

  //**************************************************************************
  //** toString
  //**************************************************************************
    public String toString(){
        return name;
    }


  //**************************************************************************
  //** clear
  //**************************************************************************
  /** Used to delete all the attributes of this field.
   */
    protected void clear(){
        name = null;
        type = null;
        value = new Value(null);
        tableName = null;
        schema = null;
        className = null;
        table = null;
    }


  //**************************************************************************
  //** getValue
  //**************************************************************************
    private String getValue(String str){
        if (str!=null){
            if (str.trim().length()==0) return null;
        }
        return str;
    }
}
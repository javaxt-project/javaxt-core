package javaxt.sql;

//******************************************************************************
//**  Field Class
//******************************************************************************
/**
 *   Used to represent a field in a Recordset.
 *
 ******************************************************************************/

public class Field {

    protected int Count;
    protected String Name = null;
    protected String Type = null;
    protected Value Value = null;
    protected String Table = null;
    private String Schema = null;
    protected String Class = null;
    protected boolean RequiresUpdate = false;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class. */

    protected Field(int i, java.sql.ResultSetMetaData rsmd){
         try{ Name = rsmd.getColumnName(i); } catch(Exception e){}
         try{ Table = getValue(rsmd.getTableName(i)); } catch(Exception e){}
         try{ Schema = getValue(rsmd.getSchemaName(i)); } catch(Exception e){}
         try{ Type = getValue(rsmd.getColumnTypeName(i)); } catch(Exception e){}
         try{ Class = getValue(rsmd.getColumnClassName(i)); } catch(Exception e){}
    }


  //**************************************************************************
  //** getName
  //**************************************************************************
  /** Returns the name of the column for this field. */
    
    public String getName(){
        return Name;
    }
  
  //**************************************************************************
  //** getValue
  //**************************************************************************
  /** Returns the associated with this field. */
    
    public Value getValue(){
        if (Value==null) Value = new Value(null);
        return Value;
    }


  //**************************************************************************
  //** getTable
  //**************************************************************************
  /** Returns the name of the table in which this field is found. */

    public String getTable(){
        return Table;
    }


    protected String getSchema(){
        return Schema;
    }

    protected void setSchema(String schema){
        Schema = getValue(schema);
    }

    public boolean isDirty(){
        return RequiresUpdate;
    }
    

    protected void clear(){
        Name = null;
        Type = null;
        Value = new Value(null);
        Table = null;
        Schema = null;
        Class = null;
    }

    private String getValue(String str){
        if (str!=null){
            if (str.trim().length()==0) return null;
        }
        return str;
    }
}
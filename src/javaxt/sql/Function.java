package javaxt.sql;

//******************************************************************************
//**  Function Class
//******************************************************************************
/**
 *  The Function class is used to encapsulate SQL functions when inserting or
 *  updating records via the javaxt.sql.Recordset class. Example:
 <pre>
    rs.setValue("LAST_UPDATE", new javaxt.sql.Function("NOW()"));
    rs.setValue("DATEDIFF_TEST", new javaxt.sql.Function("DATEDIFF(year, '2012/04/28', '2014/04/28')"));
 </pre>
 *
 *  Note that functions can be parameterized using standard JDBC syntax using
 *  question marks (? characters) like this:
 <pre>
    JSONObject json = new JSONObject();
    rs.setValue("info", new javaxt.sql.Function("?::jsonb", json.toString()));
 </pre>
 *
 *  Parameterizing values is especially useful when dealing with strings and
 *  other values that may have invalid characters. It is also extremely useful
 *  when performing batch inserts.
 *
 ******************************************************************************/

public class Function {

    private String function;
    private Object[] values;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a function (e.g. "NOW()")
   */
    public Function(String function){
        this.function = function;
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a parameterized function.
   */
    public Function(String function, Object... values){
        this.function = function;

        try{
            if (values[0] instanceof Object[]){
                values = (Object[]) values[0];
            }
        }
        catch(Exception e){
        }

        this.values = values;
    }


  //**************************************************************************
  //** getFunction
  //**************************************************************************
  /** Returns the function supplied in the constructor
   */
    public String getFunction(){
        return function;
    }


  //**************************************************************************
  //** hasValues
  //**************************************************************************
  /** Returns true if values were supplied to the constructor
   */
    public boolean hasValues(){
        if (values!=null){
            return (values.length>0);
        }
        return false;
    }


  //**************************************************************************
  //** getValues
  //**************************************************************************
  /** Returns an array of values that were supplied in the constructor
   */
    public Object[] getValues(){
        return values;
    }

    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns the function supplied in the constructor
   */
    public String toString(){
        return function;
    }
}
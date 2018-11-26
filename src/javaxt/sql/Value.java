package javaxt.sql;

//******************************************************************************
//**  Value Class
//******************************************************************************
/**
 *   Used to represent a value for a given field in the database. The value can
 *   be converted into a number of Java primatives (strings, integers, doubles,
 *   booleans, etc).
 *
 ******************************************************************************/

public class Value extends javaxt.utils.Value {

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of Value. */

    public Value(Object value){
        super(value);
    }

    
  //**************************************************************************
  //** toTimeStamp
  //**************************************************************************
  /** Returns the value as a java.sql.Timestamp. Returns a null if there was a 
   *  problem converting the value to a Timestamp or if the value is null.
   */
    public java.sql.Timestamp toTimeStamp(){
        Object obj = super.toObject();
        if (obj!=null){
            if (obj instanceof java.sql.Timestamp){
                return (java.sql.Timestamp) obj;
            }
            else{
                javaxt.utils.Date date = toDate();
                if (date!=null){
                    return new java.sql.Timestamp(date.getDate().getTime());
                }
            }
        }
        return null;
    }

    
  //**************************************************************************
  //** toArray
  //**************************************************************************
  /** If the value is a java.sql.Array, returns the output from the 
   *  java.sql.Array.getArray() method. Returns a null if there was a problem 
   *  converting the value to a java.sql.Array or if the value is null.
   */
    public Object toArray(){
        Object obj = super.toObject();
        if (obj!=null){
            try{
                return ((java.sql.Array) obj).getArray();
            }
            catch(Exception e){
            }
        }

        return null;
    }
    
    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns the value as a String. Returns a null if there was a problem 
   *  converting the value to a String or if the value is null.
   */
    public String toString(){
        Object obj = super.toObject();
        if (obj!=null){
            if (obj instanceof java.sql.Clob){
                String str = null;
                java.sql.Clob clob = (java.sql.Clob) obj;
                try{
                    long len = clob.length();
                    if (len < Integer.MIN_VALUE || len > Integer.MAX_VALUE){
                        //CLOB size too big for String!
                    }
                    else{
                        str = clob.getSubString(1, (int) clob.length());
                    }
                }
                catch(Exception e){
                }
                finally{
                    if (clob!=null){
                        try{
                            clob.free();
                        }
                        catch(Exception e){}
                    }
                }
                return str;
            }
            else{
                return obj.toString();
            }
        }
        
        return null;
    }
}
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

public class Value {
    
    private Object value = null;
    
    protected Value(Object value){
        this.value = value;
    }
    
    public Object toObject(){
        return value;
    }
    
    
    public Integer toInteger(){
        try{
            return Integer.valueOf(value+"");
        }
        catch(Exception e){
            return null;
        }
    }
    
    
    public Short toShort(){
        try{
            return Short.valueOf(value+"");
        }
        catch(Exception e){
            return null;
        }
    }
    
    
    public Double toDouble(){
        try{
            return Double.valueOf(value+"");
        }
        catch(Exception e){
            return null;
        }
    }
    
    
    public Long toLong(){
        try{
            return Long.valueOf(value+"");
        }
        catch(Exception e){
            return null;
        }
    }


    public java.math.BigDecimal toBigDecimal(){
        try{
            return java.math.BigDecimal.valueOf(toDouble());
        }
        catch(Exception e){
            return null;
        }
    }
    
    
    public javaxt.utils.Date toDate(){
        try{
            javaxt.utils.Date date = new javaxt.utils.Date(value.toString());
            if (date.failedToParse()) return null;
            else return date;
        }
        catch(Exception e){
            return null;
        }
    }

    public java.sql.Timestamp toTimeStamp(){
        return new java.sql.Timestamp(toDate().getDate().getTime());
    }
    
    public boolean isNull(){
        return value==null;
    }
    
    
  //**************************************************************************
  //** toBoolean
  //**************************************************************************
  /**  Returns a boolean value for the field. */
    
    public Boolean toBoolean(){
        if (value!=null){
            value = value.toString().toLowerCase().trim();

            if (value.equals("true")) return true;
            if (value.equals("false")) return false;

            if (value.equals("yes")) return true;
            if (value.equals("no")) return false;

            if (value.equals("y")) return true;
            if (value.equals("n")) return false;

            if (value.equals("t")) return true;
            if (value.equals("f")) return false;

            if (value.equals("1")) return true;
            if (value.equals("0")) return false;

        }
        return null;
    }
    
  //**************************************************************************
  //** isNumeric
  //**************************************************************************
  /**  Used to determine if the value is numeric. */
    
    public boolean isNumeric(){
        if (toDouble()==null) return false;
        else return true;
    }

    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns a string representation of the object by calling the object's
   *  native toString() method. Returns a null if the object itself is null.
   */
    public String toString(){
        if (value==null) return null;
        else return value.toString();
    }
    
    

}

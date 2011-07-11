package javaxt.utils;

//******************************************************************************
//**  Value Class
//******************************************************************************
/**
 *   A general purpose wrapper for Objects stored in an Array. This class is
 *   used to store/retrieve strings, integers, doubles, booleans, etc. in
 *   HashMaps and Lists.
 *
 ******************************************************************************/

public class Value {

    
    private Object value = null;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of Value. */

    public Value(Object value){
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



    public String toString(){
        if (value==null) return null;
        else return value.toString();
    }

}
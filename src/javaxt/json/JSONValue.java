package javaxt.json;

//******************************************************************************
//**  JSONValue Class
//******************************************************************************
/**
 *   Used to represent a value associated with a key in a JSONObject or an
 *   entry in a JSONArray. The value can be converted into a number of Java 
 *   primatives (strings, integers, doubles, booleans, etc).
 *
 ******************************************************************************/

public class JSONValue extends javaxt.utils.Value {

    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class. 
   */
    public JSONValue(Object value){
        super(value);
    }

    
  //**************************************************************************
  //** toJSONObject
  //**************************************************************************
  /** Returns the value as a JSONObject. Returns a null if there was a problem
   *  converting the value to an JSONObject or if the value is null.
   */
    public JSONObject toJSONObject(){
        Object obj = super.toObject();
        if (obj!=null){
            if (obj instanceof JSONObject) {
                return (JSONObject) obj;
            }
        }
        return null;
    }
    
    
  //**************************************************************************
  //** toJSONArray
  //**************************************************************************
  /** Returns the value as a JSONArray. Returns a null if there was a problem
   *  converting the value to an JSONArray or if the value is null.
   */
    public JSONArray toJSONArray(){
        Object obj = super.toObject();
        if (obj!=null){
            if (obj instanceof JSONArray) {
                return (JSONArray) obj;
            }
        }
        return null;
    }
    
    
  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns the value associated with an index in a JSONArray, assuming the  
   *  underlying object represented by this class is a JSONArray. This is
   *  shorthand for value.toJSONArray().get(i);
   */
    public JSONValue get(int i){
        Object obj = super.toObject();
        if (obj!=null){
            if (obj instanceof JSONArray) {
                return ((JSONArray) obj).get(i);
            }
        }
        return new JSONValue(null);
    }
    
    
  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns the value associated with a key in a JSONObject, assuming the
   *  underlying object represented by this class is a JSONObject. This is
   *  shorthand for value.toJSONObject().get(key);
   */
    public JSONValue get(String key) {
        if (key == null) return new JSONValue(null);
        Object obj = super.toObject();
        if (obj!=null){
            if (obj instanceof JSONObject) {
                return ((JSONObject) obj).get(key);
            }
        }
        return new JSONValue(null);
    }
}
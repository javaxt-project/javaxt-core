package javaxt.utils;
import java.util.Map;

//******************************************************************************
//**  Record Class
//******************************************************************************
/**
 *   The Record class is used to store an ordered list of key value pairs.
 *
 ******************************************************************************/

public class Record {

    private final java.util.LinkedHashMap<String, Object> map;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Record() {
        map = new java.util.LinkedHashMap<String, Object>();
    }


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns the value associated with a key.
   */
    public Value get(String key) {
        if (key == null) return new Value(null);
        return new Value(map.get(key));
    }


  //**************************************************************************
  //** set
  //**************************************************************************
  /** Used to set the value for a given key.
   */
    public void set(String key, Object value) {

        if (key == null) {
            throw new NullPointerException("Null key.");
        }

        if (value instanceof Value){
            value = ((Value) value).toObject();
        }

        map.put(key, value);
    }


  //**************************************************************************
  //** has
  //**************************************************************************
  /** Returns true if the key exists in the JSONObject.
   */
    public boolean has(String key) {
        return this.map.containsKey(key);
    }


  //**************************************************************************
  //** remove
  //**************************************************************************
  /** Remove a name and its value, if present. Returns the value that was
   *  associated with the name, or null if there was no value.
   */
    public Value remove(String key) {
        Object o = map.remove(key);
        if (o instanceof Value) return (Value) o;
        else return new Value(o);
    }


  //**************************************************************************
  //** isNull
  //**************************************************************************
  /** Returns true if there is no value associated with the key.
   */
    public boolean isNull(String key) {
        return map.get(key)==null;
    }


  //**************************************************************************
  //** isEmpty
  //**************************************************************************
  /** Returns true if there are no entries in the JSONObject.
   */
    public boolean isEmpty(){
        return map.isEmpty();
    }


  //**************************************************************************
  //** keySet
  //**************************************************************************
  /** Returns a set of keys of the JSONObject. Modifying this key Set will
   *  also modify the JSONObject. Use with caution.
   */
    public java.util.Set<String> keySet() {
        return this.map.keySet();
    }


  //**************************************************************************
  //** entrySet
  //**************************************************************************
    public java.util.Set<Map.Entry<String, Object>> entrySet() {
        return this.map.entrySet();
    }


  //**************************************************************************
  //** length
  //**************************************************************************
  /** Returns the number of keys in the record.
   */
    public int length() {
        return this.map.size();
    }


  //**************************************************************************
  //** equals
  //**************************************************************************
  /** Returns true if the given object is a Record and the Record has the
   *  same key/value pairs as this class. Note that the order of the key/value
   *  pairs is not important.
   */
    public boolean equals(Object obj){
        if (obj instanceof Record){
            Record record = (Record) obj;
            if (record.length()==this.length()){

                for (String key : this.keySet()){
                    if (!record.has(key)) return false;
                    Object val = this.get(key).toObject();
                    Object val2 = record.get(key).toObject();
                    if (val==null){
                        if (val2!=null) return false;
                    }
                    else{
                        if (!val.equals(val2)) return false;
                    }
                }

                return true;
            }
        }
        return false;
    }

}
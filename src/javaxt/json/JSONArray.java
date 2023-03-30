package javaxt.json;
import javaxt.json.JSONObject.JSONTokener;
import java.io.IOException;
import java.io.Writer;
import javaxt.utils.Value;

//******************************************************************************
//**  JSONArray
//******************************************************************************
/**
 *   A JSON array is simply an array of objects. The string representation of a
 *   JSON array is a widely-used standard format for exchanging data. The string
 *   begins with a left square bracket "[" and ends with a right square bracket
 *   "]". Each object in the array is separated by comma ",".
 *
 *   @author Source adapted from json.org (2016-08-15)
 *
 ******************************************************************************/

public class JSONArray implements Iterable<JSONValue> {

    private final java.util.ArrayList<JSONValue> arr;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to create a new/empty array.
   */
    public JSONArray() {
        arr = new java.util.ArrayList<JSONValue>();
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to create a new array from a String (e.g. "[1,2,3]").
   */
    public JSONArray(String source) throws JSONException {
        this(new JSONTokener(source));
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    protected JSONArray(JSONTokener x) throws JSONException {
        this();
        if (x.nextClean() != '[') {
            throw x.syntaxError("A JSONArray text must start with '['");
        }

        char nextChar = x.nextClean();
        if (nextChar == 0) {
            // array is unclosed. No ']' found, instead EOF
            throw x.syntaxError("Expected a ',' or ']'");
        }
        if (nextChar != ']') {
            x.back();
            for (;;) {
                if (x.nextClean() == ',') {
                    x.back();
                    //arr.add(JSONObject.NULL);
                } else {
                    x.back();
                    add(x.nextValue());
                }
                switch (x.nextClean()) {
                case 0:
                    // array is unclosed. No ']' found, instead EOF
                    throw x.syntaxError("Expected a ',' or ']'");
                case ',':
                    nextChar = x.nextClean();
                    if (nextChar == 0) {
                        // array is unclosed. No ']' found, instead EOF
                        throw x.syntaxError("Expected a ',' or ']'");
                    }
                    if (nextChar == ']') {
                        return;
                    }
                    x.back();
                    break;
                case ']':
                    return;
                default:
                    throw x.syntaxError("Expected a ',' or ']'");
                }
            }
        }
    }


  //**************************************************************************
  //** iterator
  //**************************************************************************
    @Override
    public java.util.Iterator<JSONValue> iterator() {
        return arr.iterator();
    }


  //**************************************************************************
  //** length
  //**************************************************************************
  /** Returns the number of elements in the JSONArray, included nulls.
   */
    public int length() {
        return arr.size();
    }


  //**************************************************************************
  //** isEmpty
  //**************************************************************************
  /** Returns true if there are no entries in the array.
   */
    public boolean isEmpty(){
        return arr.isEmpty();
    }


  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns the object value associated with an index.
   */
    public JSONValue get(int index) {
        return new JSONValue(opt(index));
    }


  //**************************************************************************
  //** add
  //**************************************************************************
  /** Appends an object to the array.
   */
    public void add(Object object) throws JSONException {

        JSONValue v;
        Object o;
        if (object instanceof JSONValue){
            v = (JSONValue) object;
            o = v.toObject();
        }
        else if (object instanceof Value){
            o = ((Value) object).toObject();
            v = new JSONValue(object);
        }
        else{
            o = object;
            v = new JSONValue(object);
        }


        JSONObject.testValidity(o);

        arr.add(v);
    }


  //**************************************************************************
  //** set
  //**************************************************************************
  /** Updates an object to the array. Returns the original value that was
   *  associated with the index.
   */
    public JSONValue set(int index, Object object){

        JSONValue v;
        Object o;
        if (object instanceof JSONValue){
            v = (JSONValue) object;
            o = v.toObject();
        }
        else if (object instanceof Value){
            o = ((Value) object).toObject();
            v = new JSONValue(object);
        }
        else{
            o = object;
            v = new JSONValue(object);
        }

        JSONObject.testValidity(o);

        Object obj = arr.set(index, v);
        return new JSONValue(obj);
    }


  //**************************************************************************
  //** remove
  //**************************************************************************
  /** Remove entry. Returns the value that was associated with the index.
   */
    public JSONValue remove(int index) {
        return index >= 0 && index < this.length()
            ? new JSONValue(arr.remove(index))
            : new JSONValue(null);
    }


  //**************************************************************************
  //** equals
  //**************************************************************************
  /** Returns true if the given object is a JSONArray and the JSONArray
   *  contains the same entries as this array. Order is important.
   */
    public boolean equals(Object obj){
        if (obj instanceof JSONArray){
            JSONArray arr = (JSONArray) obj;
            if (arr.length()==this.length()){
                for (int i=0; i<this.arr.size(); i++){
                    Object val = this.arr.get(i);
                    Object val2 = arr.get(i).toObject();
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


  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns a printable, displayable, transmittable representation of the
   *  array. For compactness, no unnecessary whitespace is added. If it is not
   *  possible to produce a syntactically correct JSON text then null will be
   *  returned instead.
   */
    @Override
    public String toString() {
        try {
            return this.toString(0);
        }
        catch (Exception e) {
            return null;
        }
    }


  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns a printable, displayable, transmittable representation of the
   *  array.
   */
    public String toString(int indentFactor) {
        try{
            java.io.StringWriter sw = new java.io.StringWriter();
            synchronized (sw.getBuffer()) {
                return this.write(sw, indentFactor, 0).toString();
            }
        }
        catch(Exception e){
            return null;
        }
    }


  //**************************************************************************
  //** write
  //**************************************************************************
  /** Write the contents of the JSONArray as JSON text to a writer.
   */
    protected Writer write(Writer writer, int indentFactor, int indent)
        throws JSONException {
        try {
            boolean commanate = false;
            int length = this.length();
            writer.write('[');

            if (length == 1) {
                try {
                    JSONObject.writeValue(writer, arr.get(0).toObject(), indentFactor, indent);
                }
                catch (Exception e) {
                    throw new JSONException("Unable to write JSONArray value at index: 0", e);
                }
            }
            else if (length != 0) {
                final int newindent = indent + indentFactor;

                for (int i = 0; i < length; i += 1) {
                    if (commanate) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    JSONObject.indent(writer, newindent);
                    try {
                        JSONObject.writeValue(writer, arr.get(i).toObject(), indentFactor, newindent);
                    }
                    catch (Exception e) {
                        throw new JSONException("Unable to write JSONArray value at index: " + i, e);
                    }
                    commanate = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                JSONObject.indent(writer, indent);
            }
            writer.write(']');
            return writer;
        }
        catch (IOException e) {
            throw new JSONException(e);
        }
    }


  //**************************************************************************
  //** opt
  //**************************************************************************
  /** Returns an object value, or null if there is no object at that index.
   */
    private Object opt(int index) {
        return (index < 0 || index >= this.length()) ? null : arr.get(index);
    }
}
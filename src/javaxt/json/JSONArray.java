package javaxt.json;
import javaxt.utils.Value;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

//******************************************************************************
//**  JSONArray
//******************************************************************************
/**
 *   A JSONArray is an ordered sequence of values. Its external text form is a
 *   string wrapped in square brackets with commas separating the values.
 * 
 *   @author json.org
 *   @version 2016-08-15
 *
 ******************************************************************************/

public class JSONArray implements Iterable<Object> {

    private final ArrayList<Object> myArrayList;

    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to create a new/empty array.
   */
    public JSONArray() {
        this.myArrayList = new ArrayList<Object>();
    }

    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Used to create a new array from a String.
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
                    this.myArrayList.add(JSONObject.NULL);
                } else {
                    x.back();
                    this.myArrayList.add(x.nextValue());
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
    public Iterator<Object> iterator() {
        return this.myArrayList.iterator();
    }

    
  //**************************************************************************
  //** iterator
  //**************************************************************************
  /** Returns the number of elements in the JSONArray, included nulls.
   */
    public int length() {
        return this.myArrayList.size();
    }
    
    
  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns the object value associated with an index.
   */
    public Value get(int index) {
        return new Value(opt(index));
    }


  //**************************************************************************
  //** getJSONArray
  //**************************************************************************
  /** Returns the JSONArray associated with an index.
   */
    public JSONArray getJSONArray(int index) {
        Object object = opt(index);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        return null;
    }

    
  //**************************************************************************
  //** getJSONObject
  //**************************************************************************
  /** Returns the JSONObject associated with an index.
   */
    public JSONObject getJSONObject(int index) {
        Object object = opt(index);
        if (object instanceof JSONObject) {
            return (JSONObject) object;
        }
        return null;
    }


  //**************************************************************************
  //** isNull
  //**************************************************************************
  /** Returns true if the value at the index is null, or if there is no value.
   */
    public boolean isNull(int index) {
        return JSONObject.NULL.equals(this.opt(index));
    }



  //**************************************************************************
  //** put
  //**************************************************************************
  /** Appends a boolean value. This increases the array's length by one.
   */
    public void add(boolean value) {
        add(value ? Boolean.TRUE : Boolean.FALSE);
    }


  //**************************************************************************
  //** put
  //**************************************************************************
  /** Appends a double value. This increases the array's length by one.
   */
    public void add(double value) throws JSONException {
        Double d = new Double(value);
        JSONObject.testValidity(d);
        add(d);
    }
    
    
  //**************************************************************************
  //** put
  //**************************************************************************
  /** Appends an int value. This increases the array's length by one.
  */
    public void add(int value) {
        add(new Integer(value));
    }
    
    
  //**************************************************************************
  //** put
  //**************************************************************************
  /** Appends an long value. This increases the array's length by one.
   */
    public void add(long value) {
        add(new Long(value));
    }


  //**************************************************************************
  //** put
  //**************************************************************************
  /** Appends an object value. This increases the array's length by one.
   *  @param value An object value. The value should be a Boolean, Double,
   *  Integer, JSONArray, JSONObject, Long, String, or a JSONObject.NULL object.
   */
    public void add(Object value) {
        myArrayList.add(value);
    }


  //**************************************************************************
  //** remove
  //**************************************************************************
  /** Remove entry. Returns the value that was associated with the index, or 
   *  null if there was no value.
   */
    public Object remove(int index) {
        return index >= 0 && index < this.length()
            ? this.myArrayList.remove(index)
            : null;
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
                    JSONObject.writeValue(writer, this.myArrayList.get(0),
                            indentFactor, indent);
                } catch (Exception e) {
                    throw new JSONException("Unable to write JSONArray value at index: 0", e);
                }
            } else if (length != 0) {
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
                        JSONObject.writeValue(writer, this.myArrayList.get(i),
                                indentFactor, newindent);
                    } catch (Exception e) {
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
        } catch (IOException e) {
            throw new JSONException(e);
        }
    }
    
    
  //**************************************************************************
  //** opt
  //**************************************************************************
  /** Returns an object value, or null if there is no object at that index.
   */
    private Object opt(int index) {
        return (index < 0 || index >= this.length()) ? null : myArrayList.get(index);
    }
}
package javaxt.json;
import javaxt.utils.Value;
import java.io.IOException;
import java.io.Writer;
import java.util.Map.Entry;
import java.util.*;

//******************************************************************************
//**  JSONObject
//******************************************************************************
/**
 *   Used to create and parse JSON documents. JSON documents are an unordered
 *   collection of name/value pairs. Its external form is a string wrapped in 
 *   curly braces with colons between the names and values, and commas between 
 *   the values and names.
 * 
 *   @author json.org
 *   @version 2016-08-15
 *
 ******************************************************************************/

public class JSONObject {
    
    private final Map<String, Object> map;
    public static final Object NULL = new Null();

    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public JSONObject() {
        this.map = new HashMap<String, Object>();
    }
    

  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Construct a JSONObject from a source JSON text string. This is the most
   *  commonly used JSONObject constructor.
   *
   * @param source A string beginning with <code>{</code>&nbsp;<small>(left
   *  brace)</small> and ending with <code>}</code> &nbsp;<small>(right brace)
   *  </small>.
   */
    public JSONObject(String source) throws JSONException {
        this(new JSONTokener(source));
    }

    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Construct a JSONObject from a JSONTokener.
   */
    protected JSONObject(JSONTokener x) throws JSONException {
        this();
        char c;
        String key;

        if (x.nextClean() != '{') {
            throw x.syntaxError("A JSONObject text must begin with '{'");
        }
        for (;;) {
            c = x.nextClean();
            switch (c) {
            case 0:
                throw x.syntaxError("A JSONObject text must end with '}'");
            case '}':
                return;
            default:
                x.back();
                key = x.nextValue().toString();
            }

            // The key is followed by ':'.

            c = x.nextClean();
            if (c != ':') {
                throw x.syntaxError("Expected a ':' after a key");
            }
            
            // Use syntaxError(..) to include error location
            
            if (key != null) {
                // Check if key exists
                if (this.opt(key) != null) {
                    // key already exists
                    throw x.syntaxError("Duplicate key \"" + key + "\"");
                }
                // Only add value if non-null
                Object value = x.nextValue();
                if (value!=null) {
                    this.put(key, value);
                }
            }

            // Pairs are separated by ','.

            switch (x.nextClean()) {
            case ';':
            case ',':
                if (x.nextClean() == '}') {
                    return;
                }
                x.back();
                break;
            case '}':
                return;
            default:
                throw x.syntaxError("Expected a ',' or '}'");
            }
        }
    }

    
  //**************************************************************************
  //** get
  //**************************************************************************
  /** Returns the value associated with a key.
   */
    public Value get(String key) {
        if (key == null) return new Value(null);
        return new Value(this.opt(key));
    }

    
  //**************************************************************************
  //** getJSONArray
  //**************************************************************************
  /** Returns the JSONArray associated with a key.
   */
    public JSONArray getJSONArray(String key) {
        Object object = this.opt(key);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        return null;
    }

    
  //**************************************************************************
  //** getJSONObject
  //**************************************************************************
  /** Returns the JSONObject associated with a key.
   */
    public JSONObject getJSONObject(String key) {
        Object object = this.opt(key);
        if (object instanceof JSONObject) {
            return (JSONObject) object;
        }
        return null;
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
  //** isNull
  //**************************************************************************
  /** Returns true if there is no value associated with the key or if the 
   *  value is a JSONObject.NULL object.
   */
    public boolean isNull(String key) {
        return JSONObject.NULL.equals(this.opt(key));
    }

    
  //**************************************************************************
  //** keys
  //**************************************************************************
  /** Returns an enumeration of the keys of the JSONObject. Modifying this key 
   *  Set will also modify the JSONObject. Use with caution.
   */
    public Iterator<String> keys() {
        return this.keySet().iterator();
    }
    
    
  //**************************************************************************
  //** keySet
  //**************************************************************************
  /** Returns a set of keys of the JSONObject. Modifying this key Set will 
   *  also modify the JSONObject. Use with caution.
   */
    public Set<String> keySet() {
        return this.map.keySet();
    }

    
  //**************************************************************************
  //** entrySet
  //**************************************************************************
    private Set<Entry<String, Object>> entrySet() {
        return this.map.entrySet();
    }
    
    
  //**************************************************************************
  //** length
  //**************************************************************************
  /** Returns the number of keys in the JSONObject.
   */
    public int length() {
        return this.map.size();
    }


  //**************************************************************************
  //** put
  //**************************************************************************
  /** Put a key/boolean pair in the JSONObject.
   */
    public JSONObject put(String key, boolean value) throws JSONException {
        this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }


  //**************************************************************************
  //** put
  //**************************************************************************
  /** Put a key/double pair in the JSONObject.
   */
    public JSONObject put(String key, double value) throws JSONException {
        this.put(key, Double.valueOf(value));
        return this;
    }
    
    
  //**************************************************************************
  //** put
  //**************************************************************************
  /** Put a key/float pair in the JSONObject.
   */
    public JSONObject put(String key, float value) throws JSONException {
        this.put(key, Float.valueOf(value));
        return this;
    }

    
  //**************************************************************************
  //** put
  //**************************************************************************
  /** Put a key/int pair in the JSONObject.
   */
    public JSONObject put(String key, int value) throws JSONException {
        this.put(key, Integer.valueOf(value));
        return this;
    }

    
  //**************************************************************************
  //** put
  //**************************************************************************
  /** Put a key/long pair in the JSONObject.
   */
    public JSONObject put(String key, long value) throws JSONException {
        this.put(key, Long.valueOf(value));
        return this;
    }
    

  //**************************************************************************
  //** put
  //**************************************************************************
  /** Put a key/value pair in the JSONObject. If the value is null, then the
   *  key will be removed from the JSONObject if it is present.
   *  @param key A key string.
   *  @param value An object which is the value. It should be of one of these
   *  types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,  
   *  or the JSONObject.NULL object.
   */
    public JSONObject put(String key, Object value) throws JSONException {
        if (key == null) {
            throw new NullPointerException("Null key.");
        }
        if (value != null) {
            testValidity(value);
            this.map.put(key, value);
        } else {
            this.remove(key);
        }
        return this;
    }
    
    
  //**************************************************************************
  //** remove
  //**************************************************************************
  /** Remove a name and its value, if present. Returns the value that was 
   *  associated with the name, or null if there was no value.
   */
    public Object remove(String key) {
        return this.map.remove(key);
    }

    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns the JSONObject as a String. For compactness, no whitespace is
   *  added. If this would not result in a syntactically correct JSON text,
   *  then null will be returned instead.
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
  /** Returns a pretty-printed JSON text of this JSONObject.
   * @param indentFactor The number of spaces to add to each level of indentation.
   */
    public String toString(int indentFactor) {
        try{
            java.io.StringWriter w = new java.io.StringWriter();
            synchronized (w.getBuffer()) {
                return this.write(w, indentFactor, 0).toString();
            }
        }
        catch(Exception e){
            return null;
        }
    }

    
  //**************************************************************************
  //** writeValue
  //**************************************************************************
    protected static final Writer writeValue(Writer writer, Object value,
            int indentFactor, int indent) throws JSONException, IOException {
        if (value == null || value.equals(null)) {
            writer.write("null");
//        } else if (value instanceof JSONString) {
//            Object o;
//            try {
//                o = ((JSONString) value).toJSONString();
//            } catch (Exception e) {
//                throw new JSONException(e);
//            }
//            writer.write(o != null ? o.toString() : quote(value.toString()));
        } else if (value instanceof Number) {
            // not all Numbers may match actual JSON Numbers. i.e. fractions or Imaginary
            final String numberAsString = numberToString((Number) value);
            try {
                // Use the BigDecimal constructor for its parser to validate the format.
                @SuppressWarnings("unused")
                java.math.BigDecimal testNum = new java.math.BigDecimal(numberAsString);
                // Close enough to a JSON number that we will use it unquoted
                writer.write(numberAsString);
            } catch (NumberFormatException ex){
                // The Number value is not a valid JSON number.
                // Instead we will quote it as a string
                quote(numberAsString, writer);
            }
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else if (value instanceof Enum<?>) {
            writer.write(quote(((Enum<?>)value).name()));
        } else if (value instanceof JSONObject) {
            ((JSONObject) value).write(writer, indentFactor, indent);
        } else if (value instanceof JSONArray) {
            ((JSONArray) value).write(writer, indentFactor, indent);
//        } else if (value instanceof Map) {
//            Map<?, ?> map = (Map<?, ?>) value;
//            new JSONObject(map).write(writer, indentFactor, indent);
//        } else if (value instanceof Collection) {
//            Collection<?> coll = (Collection<?>) value;
//            new JSONArray(coll).write(writer, indentFactor, indent);
//        } else if (value.getClass().isArray()) {
//            new JSONArray(value).write(writer, indentFactor, indent);
        } else {
            quote(value.toString(), writer);
        }
        return writer;
    }


  //**************************************************************************
  //** write
  //**************************************************************************
    private Writer write(Writer writer, int indentFactor, int indent)
            throws JSONException {
        try {
            boolean commanate = false;
            final int length = this.length();
            writer.write('{');

            if (length == 1) {
            	final Entry<String,?> entry = this.entrySet().iterator().next();
                final String key = entry.getKey();
                writer.write(quote(key));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                try{
                    writeValue(writer, entry.getValue(), indentFactor, indent);
                } catch (Exception e) {
                    throw new JSONException("Unable to write JSONObject value for key: " + key, e);
                }
            } else if (length != 0) {
                final int newindent = indent + indentFactor;
                for (final Entry<String,?> entry : this.entrySet()) {
                    if (commanate) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    indent(writer, newindent);
                    final String key = entry.getKey();
                    writer.write(quote(key));
                    writer.write(':');
                    if (indentFactor > 0) {
                        writer.write(' ');
                    }
                    try {
                        writeValue(writer, entry.getValue(), indentFactor, newindent);
                    } catch (Exception e) {
                        throw new JSONException("Unable to write JSONObject value for key: " + key, e);
                    }
                    commanate = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write('}');
            return writer;
        } catch (IOException exception) {
            throw new JSONException(exception);
        }
    }
    
    
  //**************************************************************************
  //** indent
  //**************************************************************************
    protected static final void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    
  //**************************************************************************
  //** testValidity
  //**************************************************************************
    protected static void testValidity(Object o) throws JSONException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
                    throw new JSONException(
                            "JSON does not allow non-finite numbers.");
                }
            } else if (o instanceof Float) {
                if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
                    throw new JSONException(
                            "JSON does not allow non-finite numbers.");
                }
            }
        }
    }
    
    
  //**************************************************************************
  //** numberToString
  //**************************************************************************
  /** Produce a string from a Number.
   */
    private static String numberToString(Number number) throws JSONException {
        if (number == null) {
            throw new JSONException("Null pointer");
        }
        testValidity(number);

        // Shave off trailing zeros and decimal point, if possible.

        String string = number.toString();
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0
                && string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }
    
    
  //**************************************************************************
  //** opt
  //**************************************************************************
  /** Get an optional value associated with a key.
   * @return An object which is the value, or null if there is no value.
   */
    private Object opt(String key) {
        return key == null ? null : this.map.get(key);
    }
    
    
  //**************************************************************************
  //** quote
  //**************************************************************************
  /** Returns a String correctly formatted for insertion in a JSON text.
   */
    private static String quote(String string) {
        java.io.StringWriter sw = new java.io.StringWriter();
        synchronized (sw.getBuffer()) {
            try {
                return quote(string, sw).toString();
            } catch (IOException ignored) {
                // will never happen - we are writing to a string writer
                return "";
            }
        }
    }

    private static Writer quote(String string, Writer w) throws IOException {
        if (string == null || string.length() == 0) {
            w.write("\"\"");
            return w;
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();

        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                w.write('\\');
                w.write(c);
                break;
            case '/':
                if (b == '<') {
                    w.write('\\');
                }
                w.write(c);
                break;
            case '\b':
                w.write("\\b");
                break;
            case '\t':
                w.write("\\t");
                break;
            case '\n':
                w.write("\\n");
                break;
            case '\f':
                w.write("\\f");
                break;
            case '\r':
                w.write("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                        || (c >= '\u2000' && c < '\u2100')) {
                    w.write("\\u");
                    hhhh = Integer.toHexString(c);
                    w.write("0000", 0, 4 - hhhh.length());
                    w.write(hhhh);
                } else {
                    w.write(c);
                }
            }
        }
        w.write('"');
        return w;
    }
    
    
  //**************************************************************************
  //** Null Class
  //**************************************************************************
  /** JSONObject.NULL is equivalent to the value that JavaScript calls null,
   *  whilst Java's null is equivalent to the value that JavaScript calls
   *  undefined.
   */
    private static final class Null {

        /**
         * There is only intended to be a single instance of the NULL object,
         * so the clone method returns itself.
         *
         * @return NULL.
         */
        @Override
        protected final Object clone() {
            return this;
        }

        /**
         * A Null object is equal to the null value and to itself.
         *
         * @param object
         *            An object to test for nullness.
         * @return true if the object parameter is the JSONObject.NULL object or
         *         null.
         */
        @Override
        public boolean equals(Object object) {
            return object == null || object == this;
        }
        /**
         * A Null object is equal to the null value and to itself.
         *
         * @return always returns 0.
         */
        @Override
        public int hashCode() {
            return 0;
        }

        /**
         * Get the "null" string value.
         *
         * @return The string "null".
         */
        @Override
        public String toString() {
            return "null";
        }
    }
}
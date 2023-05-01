package javaxt.sql;
import javaxt.json.JSONObject;
import java.util.HashMap;
import java.util.ArrayList;

//******************************************************************************
//**  Record Class
//******************************************************************************
/**
 *   Used to represent a record returned from a SQL query
 *
 ******************************************************************************/

public class Record { //extends javaxt.utils.Record

    protected Field[] fields;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    protected Record(Field[] fields){
        if (fields==null) fields = new Field[0];
        this.fields = fields;
    }


  //**************************************************************************
  //** update
  //**************************************************************************
  /** Called by the move() and addNew() methods in Recordset
   */
    protected void update(java.sql.ResultSet rs){
        try{
            for (int i=1; i<=fields.length; i++) {
                Field Field = fields[i-1];
                Field.Value = rs==null ? null : new Value(rs.getObject(i));
                Field.RequiresUpdate = false;
            }
        }
        catch(Exception e){}
    }


  //**************************************************************************
  //** getFields
  //**************************************************************************
  /** Used to retrieve the an array of fields in the current record.
   */
    public Field[] getFields(){
        Field[] arr = new Field[fields.length];
        for (int i=0; i<arr.length; i++){
            arr[i] = fields[i].clone();
            arr[i].Value = fields[i].Value;
        }
        return arr;
    }


  //**************************************************************************
  //** getField
  //**************************************************************************
  /** Returns a specific field in the array of fields. Returns null if the
   *  field name is not found.
   */
    public Field getField(String FieldName){
        if (fields.length==0) return null;

        if (FieldName==null) return null;
        FieldName = FieldName.trim();
        if (FieldName.length()==0) return null;

        String[] arr = FieldName.split("\\.");

        for (Field field : fields) {

            String fieldName = field.getName();
            if (fieldName==null) continue;

            String tableName = field.getTable()==null? "" : field.getTable();
            String schemaName = field.getSchema()==null? "" : field.getSchema();

            if (arr.length==3){
                 if (fieldName.equalsIgnoreCase(arr[2]) && tableName.equalsIgnoreCase(arr[1]) && schemaName.equalsIgnoreCase(arr[0])){
                     return field;
                 }
            }
            else if (arr.length==2){
                if (fieldName.equalsIgnoreCase(arr[1]) && tableName.equalsIgnoreCase(arr[0])){
                     return field;
                }
            }
            else if (arr.length==1){
                if (fieldName.equalsIgnoreCase(arr[0])) return field;
            }
        }

        return null;
    }


  //**************************************************************************
  //** getField
  //**************************************************************************
  /** Returns a specific field in the array of fields. Returns null if the
   *  index is out of range.
   */
    public Field getField(int i){
        if (i>-1 && i<fields.length){
            return fields[i];
        }
        else{
            return null;
        }
    }


  //**************************************************************************
  //** getValue
  //**************************************************************************
  /** Returns the Value associated with a given field. Note the if the field
   *  doesn't exist in the result set, the method will return still return a
   *  Value. You can use the isNull() method on the Value to determine whether
   *  the value is null.
   */
    public Value get(String fieldName){
        Field field = getField(fieldName);
        if (field!=null) return field.getValue();
        return new Value(null);
    }



  //**************************************************************************
  //** getValue
  //**************************************************************************
  /** Returns the Value associated with a given field. Note the if the field
   *  doesn't exist in the result set, the method will return still return a
   *  Value. You can use the isNull() method on the Value to determine whether
   *  the value is null.
   */
    public Value get(int i){
        if (fields!=null && i<fields.length){
            return fields[i].getValue();
        }
        return new Value(null);
    }


  //**************************************************************************
  //** setValue
  //**************************************************************************
    public void set(String FieldName, Value FieldValue){

        for (Field field : fields){
            String name = field.getName();
            if (name!=null){
                if (name.equalsIgnoreCase(FieldName)){
                    if (FieldValue==null) FieldValue = new Value(null);

                  //Update the Field Value as needed.
                    if (!field.getValue().equals(FieldValue)){
                        field.Value = FieldValue;
                        field.RequiresUpdate = true;
                    }
                    break;
                }
            }
        }

    }


  //**************************************************************************
  //** isDirty
  //**************************************************************************
  /** Returns true if any of the fields have been modified. You can find which
   *  field has been modified using the Field.isDirty() method. Example:
   <pre>
    if (record.isDirty()){
        for (javaxt.sql.Field field : record.getFields()){
            if (field.isDirty()){
                String val = field.getValue().toString();
                System.out.println(field.getName() + ": " + val);
            }
        }
    }
   </pre>
   */
    public boolean isDirty(){
        for (Field field : fields){
            if (field.isDirty()) return true;
        }
        return false;
    }


  //**************************************************************************
  //** clear
  //**************************************************************************
    protected void clear(){
        if (fields==null) return;
        for (Field f : fields){
            f.clear();
            f = null;
        }
        fields = null;
    }


  //**************************************************************************
  //** toJson
  //**************************************************************************
  /** Returns a JSON representation of this record
   */
    public JSONObject toJson(){
        JSONObject json = new JSONObject();

        HashMap<String, Integer> keys = new HashMap<>();
        for (Field field : fields){
            String key = field.getName();
            Integer x = keys.get(field.getName());
            if (x==null){
                x = 1;
            }
            else{
                x = x+1;
            }
            keys.put(key, x);

            if (x>1) key+="_"+x;
            json.set(key, field.getValue());
        }

        return json;
    }


}
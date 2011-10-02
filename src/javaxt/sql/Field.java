package javaxt.sql;

public class Field {

    protected int Count;
    protected String Name = null;
    protected String Type = null;
    protected Value Value = null;
    protected String Table = null;
    protected String Schema = null;
    protected String Class = null;
    protected boolean RequiresUpdate = false;
    
    
  /** Returns the name of the column for this field. */
    
    public String getName(){
        return Name;
    }
  
    
  /** Returns the name of the column for this field. */
    
    public Value getValue(){
        if (Value==null) Value = new Value(null);
        return Value;
    }

    public String getTable(){
        return Table;
    }

    public boolean isDirty(){
        return RequiresUpdate;
    }
    
    
}

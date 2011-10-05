package javaxt.webservices;
import org.w3c.dom.*;

//******************************************************************************
//**  Parameter Class
//******************************************************************************
/**
 *   Used to represent a parameter associated with a web method.
 *
 ******************************************************************************/

public class Parameter {

    protected String Name;
    protected String Type;
    protected String Value;
    protected String minOccurs = "0";
    protected String maxOccurs = "1";
    protected boolean IsNillable;
    //protected boolean IsComplex;

    protected Node ParentNode;
    protected NodeList ChildNodes;
    protected Parameter[] Children = null;

    protected Option[] Options = null;


    public String getName(){return Name;}
    public String getType(){return Type;}
    public String getValue(){
        return Value;
    }


    public void setValue(String Value){

      //Set Default Value
        if (Value==null) Value="";
        if (Value.equalsIgnoreCase("null")) Value=""; //this may not be such a good idea...

      //Update Value as Needed
        if (!isComplex() && Value.trim().length()>0){
            if (Type.equalsIgnoreCase("datetime")){
                try{
                    javaxt.utils.Date d = new javaxt.utils.Date(Value);
                    setValue(d.getDate());
                }
                catch(java.text.ParseException e){}
                Value = this.Value;
            }
            if (Type.equalsIgnoreCase("boolean")){
                if (Value.equalsIgnoreCase("true")) Value = "true";
                else Value = "false";
            }
        }

        this.Value = Value;

        //System.out.println(Name + ":" + this.Value);
    }

    public void setValue(byte[] bytes){
        setValue(javaxt.utils.Base64.encodeBytes(bytes));
    }

    public void setValue(java.util.Date ParameterValue){

      //2003-11-24T00:00:00.0000000-05:00
        java.text.SimpleDateFormat formatter =
             new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSZ");

        String d = formatter.format(ParameterValue).replace(" ", "T");

        String d1 = d.substring(0,d.length()-2);
        String d2 = d.substring(d.length()-2);
        d = d1 + ":" + d2;

        this.Value = d;
    }


    public int getMaxOccurs(){
        if (maxOccurs==null) return 1;
        maxOccurs = maxOccurs.trim();

        if (maxOccurs.equalsIgnoreCase("unbounded")){
            return 32767;
        }
        else if(maxOccurs.equals("")){
            return 1;
        }
        else{
            try{
                int i = cint(maxOccurs);
                if (i<0) return 0;
                else return i;
            }
            catch (Exception e){
                return 1;
            }
        }
    }

    public int getMinOccurs(){
        if (minOccurs==null) return 0;
        minOccurs = minOccurs.trim();
        try{
            int i = cint(minOccurs);
            if (i<0) return 0;
            else return i;
        }
        catch (Exception e){
            return 0;
        }
    }

    public boolean isRequired(){
        if (getMinOccurs()==0) return false;
        else return true;
    }

    public boolean isComplex(){
        //return IsComplex;
        
        if (this.getChildren()!=null){
            if (this.getChildren().length>0) return true;
        }

        if (Options!=null){
            return false;
        }

        return false;
    }

    public Parameter[] getChildren(){
        return Children;
    }

    public Option[] getOptions(){
        return Options;
    }

    public String toString(){
        if (isComplex()){
            return Name;
        }
        else{
           return Name + "=" + Value;
        }
    }

    private int cint(String str){return javaxt.utils.string.cint(str); }
}
package javaxt.webservices;
import org.w3c.dom.*;
import javaxt.xml.DOM;

//******************************************************************************
//**  Parameter Class
//******************************************************************************
/**
 *   Used to represent a parameter associated with a web method.
 *
 ******************************************************************************/

public class Parameter {

    private String Name;
    private String Type;
    private String Value;
    private String minOccurs = "0";
    private String maxOccurs = "1";
    protected boolean IsNillable;
    protected boolean IsAttribute;

    protected Node ParentNode;
    private NodeList ChildNodes;
    protected Parameter[] Children = null;
    private Option[] Options = null;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Instantiates this class using a "Parameter" node from an SSD.
   */
    protected Parameter(Node ParameterNode){
        NamedNodeMap attr = ParameterNode.getAttributes();
        Name = DOM.getAttributeValue(attr, "name");
        Type = DOM.getAttributeValue(attr, "type");
        IsAttribute = bool(DOM.getAttributeValue(attr, "isattribute"));
        IsNillable = bool(DOM.getAttributeValue(attr, "isnillable"));
        minOccurs = DOM.getAttributeValue(attr, "minOccurs");
        ChildNodes = ParameterNode.getChildNodes();
        ParentNode = ParameterNode.getParentNode();
        Options = getOptions(ParameterNode.getChildNodes());

        /*
        if (Parameter.Options!=null){
            Parameter.IsComplex = false;
        }
        */
    }

    
  //**************************************************************************
  //** getOptions
  //**************************************************************************
  /**  Used to retrieve an array of options from an SSD NodeList */

    private Option[] getOptions(NodeList optionNodes){

        java.util.ArrayList<Option> options = new java.util.ArrayList<Option>();
        for (Node optionNode : DOM.getNodes(optionNodes)){
            if (optionNode.getNodeName().equalsIgnoreCase("options")){
                for (Node childNode : DOM.getNodes(optionNode.getChildNodes())){
                    options.add(getOption(childNode));
                }
            }
        }

        if (options.isEmpty()){
            return null;
        }
        else{
            return options.toArray(new Option[options.size()]);
        }
    }


  //**************************************************************************
  //** getOption
  //**************************************************************************

    private Option getOption(Node node){
        NamedNodeMap attr = node.getAttributes();
        String value = DOM.getAttributeValue(attr, "value");
        String name = value;
        return new Option(name, value);
    }





    public String getName(){return Name;}
    public String getType(){return Type;}
    public String getValue(){
        return Value;
    }

    public void setValue(int value){
        setValue(value+"");
    }

    public void setValue(double value){
        setValue(value+"");
    }

    public void setValue(boolean value){
        setValue(value+"");
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

    public void setValue(java.util.Date date){

      //2003-11-24T00:00:00.0000000-05:00
        java.text.SimpleDateFormat formatter =
             new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSZ");

        String d = formatter.format(date).replace(" ", "T");

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

    protected NodeList getChildNodes(){
        return ChildNodes;
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


//    public void reset(){
//        reset(this);
//    }
//
//    private void reset(Parameter param){
//        param.Value = null;
//        if (Children!=null){
//            for (Parameter p : Children){
//                reset(p);
//            }
//        }
//    }


    private int cint(String str){
        return javaxt.utils.string.cint(str);
    }

    private boolean bool(String str){
        if (str.equalsIgnoreCase("true")) return true;
        else return false;
    }
}
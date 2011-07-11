package javaxt.webservices;
import org.w3c.dom.*;

//******************************************************************************
//**  Method Class - By peter.borissow
//******************************************************************************
/**
 *   Enter class description here
 *
 ******************************************************************************/

public class Method {


    protected String Name;
    protected String Description;
    //private String URL; //Do we still need this?
    //private String NameSpace; //Do we still need this?
    protected String SoapAction;
    protected String ResultsNode;
    //private Service Service;
    protected Parameter[] Parameters = null;
    protected NodeList ParameterXML = null;


    public String getName(){return Name;}
    public String getDescription(){return Description;}
    public String getSoapAction(){return SoapAction;}
    public String getResultsNodeName(){return ResultsNode;}
    public Parameters getParameters(){
        if (Parameters==null) return null;
        else {
            return new Parameters(Parameters);
        }
    }

    public boolean equals(String MethodName){
        if (Name.equalsIgnoreCase(MethodName)) return true;
        else return false;
    }

    public String toString(){
        return Name;
    }
}

package javaxt.webservices;
import org.w3c.dom.*;
import javaxt.xml.DOM;

//******************************************************************************
//**  Method Class
//******************************************************************************
/**
 *   Used to represent a web method.
 *
 ******************************************************************************/

public class Method {


    private String Name;
    private String Description;
    //private String URL;
    //private String NameSpace;
    private String SoapAction;
    private String ResultsNode;

    private NodeList Parameters = null;
    private NodeList Outputs = null;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /**  Used to retrieve a method from an SSD Method Node */

    protected Method(Node MethodNode){

        NamedNodeMap attr = MethodNode.getAttributes();
        Name = DOM.getAttributeValue(attr, "name");
        SoapAction = DOM.getAttributeValue(attr, "soapAction");
        ResultsNode = DOM.getAttributeValue(attr, "resultsNode");
        //Method.URL = Service.URL;
        //Method.NameSpace = Service.NameSpace;

        NodeList ChildNodes = MethodNode.getChildNodes();
        for (int j=0; j<ChildNodes.getLength(); j++ ) {
            if (ChildNodes.item(j).getNodeType()==1){
                String NodeName = ChildNodes.item(j).getNodeName();
                String NodeValue = ChildNodes.item(j).getTextContent();
                if (NodeName.toLowerCase().equals("description")){
                   Description = NodeValue;
                }
                if (NodeName.toLowerCase().equals("parameters")){
                    Parameters = ChildNodes.item(j).getChildNodes();
                }
                if (NodeName.toLowerCase().equals("outputs")){
                    Outputs = ChildNodes.item(j).getChildNodes();
                }
            }
        }

    }



    public String getName(){return Name;}
    public String getDescription(){return Description;}
    public String getSoapAction(){return SoapAction;}
    public String getResultsNodeName(){return ResultsNode;}


  //**************************************************************************
  //** getParameters
  //**************************************************************************
  /** Returns a list of input parameters associated with this method.
   */
    public Parameters getParameters(){

      //Note that we don't store parameters as a class variable. Instead, we
      //extract parameters from the SSD. This is important. Otherwise, the
      //param values get cached
        Parameter[] parameters = getParameters(Parameters, "parameter");
        if (parameters==null) return null;
        else return new Parameters(parameters);
    }


  //**************************************************************************
  //** getOutputs
  //**************************************************************************
  /** Returns a list of outputs returned by this method. There should be only
   *  one result. Use the getResultsNodeName() method to find the correct
   *  output.
   */
    public Outputs getOutputs(){
        Parameter[] parameters = getParameters(Outputs, "output");
        if (parameters==null) return null;
        return new Outputs(parameters, ResultsNode);
    }


  //**************************************************************************
  //** getParameters
  //**************************************************************************
  /**  Used to retrieve an array of parameters from an SSD NodeList
   */
    private Parameter[] getParameters(NodeList parameterNodes, String tagName){

        java.util.ArrayList<Parameter> parameters = new java.util.ArrayList<>();

        for (Node parameterNode : DOM.getNodes(parameterNodes)){
            if (parameterNode.getNodeName().equalsIgnoreCase(tagName)){
                try{
                    parameters.add(new Parameter(parameterNode));
                }
                catch(InstantiationException e){
                    e.printStackTrace();
                }
            }
        }

        if (parameters.isEmpty()){
            return null;
        }
        else{
            return parameters.toArray(new Parameter[parameters.size()]);
        }
    }



    public boolean equals(String MethodName){
        return Name.equalsIgnoreCase(MethodName);
    }

    public String toString(){
        return Name;
    }
}

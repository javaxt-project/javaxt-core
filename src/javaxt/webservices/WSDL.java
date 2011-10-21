package javaxt.webservices;
import javaxt.utils.string;
import org.w3c.dom.*;
import javaxt.xml.DOM;

/******************************************************************************
/**  WSDL Parser - By Peter Borissow
/*****************************************************************************/
/**  Used to parse a WSDL return information about the web services documented
 *   in the WSDL including service name and description, web methods, and input
 *   parameters.
 *
 <pre>
javaxt.webservices.WSDL wsdl = new javaxt.webservices.WSDL(url);
for (javaxt.webservices.Service service : wsdl.getServices()){
    System.out.println(service.getName());
    for (javaxt.webservices.Method method : service.getMethods()){
        System.out.println(" - " + method.getName());
        javaxt.webservices.Parameters parameters = method.getParameters();
        if (parameters!=null){
            for (javaxt.webservices.Parameter parameter : parameters.getArray()){
                System.out.println("   * " + parameter.getName());
            }
        }
    }
}
 </pre>
 * 
 *   Note that this parser works for most xml web services in production. That
 *   said, there are a couple limitations. Here's a short list of outstanding
 *   tasks:
 *   <ul>
 *   <ul>
 *   <li>Finish the Parameters.setValue() method to support arrays</li>
 *   <li>Verify that the namespace is identified and used properly (currently
 *   assume one targetNamespace per wsdl)</li>
 *   <li>Test whether parameters are identified properly</li>
 *   <li>Need to test against wsdls w/multiple services</li>
 *   </ul>
 *   </ul>
 *****************************************************************************/

public class WSDL {
        
    private Document wsdl;
    private Document ssd; //Simple Service Definition
    private String vbCrLf = "\r\n"; 
    private String ElementNameSpace = "";
    private java.util.HashMap<String, String> NameSpaces;
    private NodeList Schema;
    
    private class Port{
        public String Name;
        public String Binding;
        public String Address;
    }
    
    private class Binding{
        public String Operation;
        public String SoapAction;
        public String Style;
        
        public String Name;
        public String Type;
    }
    
    private class Message{ //PortType
        public String Input;
        public String Output;
        public String Documentation = null;
    }
    
    private class Element{
        public String Name;
        public String Type;
        public boolean IsNillable = false;
        public boolean IsComplex = false;
        public String minOccurs = "0";
        public String maxOccurs = "1";
        public boolean IsAttribute = false;

        private java.util.ArrayList<Object> children = new java.util.ArrayList<Object>();


        public Element(String Name, String Type){
            this.Name = Name;
            this.Type = stripNameSpace(Type);
            IsComplex = isElementComplex(Type);
        }

        public Element(Node node){

            String nodeName = stripNameSpace(node.getNodeName());
            NamedNodeMap attr = node.getAttributes();


            Name = getAttributeValue(attr, "name");
            Type = getAttributeValue(attr, "type");
            IsNillable = isElementNillable(getAttributeValue(attr, "nillable"));
            IsComplex = isElementComplex(Type);
            minOccurs = getAttributeValue(attr, "minOccurs");
            maxOccurs = getAttributeValue(attr, "maxOccurs");

            if (minOccurs.length()==0) minOccurs = "0";
            if (maxOccurs.length()==0) maxOccurs = "1";

            Type = stripNameSpace(Type);

            String elementRef = getAttributeValue(attr, "ref");
            if (elementRef.length()>0){
                Name = elementRef;
            }
            
            if (nodeName.equalsIgnoreCase("attribute")){
                IsAttribute = true;
                IsNillable = getAttributeValue(attr, "use").equalsIgnoreCase("optional");
            }
        }

        public void addElement(Element element){

          //Child elements should be unique.
            boolean addElement = true;
            java.util.Iterator it = children.iterator();
            while (it.hasNext()){
                Object child = it.next();
                if (child instanceof Element){
                    Element e = (Element) child;
                    if (e.Name.equals(element.Name) && e.Type.equals(element.Type)){
                        addElement = false;
                    }
                }
            }
            if (addElement) children.add(element);
        }

        public void addChoices(Choices choices){
            children.add(choices);
        }

        public void addOptions(Options options){
            children.add(options);
        }


        public String toString(){
            StringBuffer xml = new StringBuffer();
            xml.append("   <parameter " +
                               "name=\"" + Name + "\" " +
                               "type=\"" + Type + "\" " +
                               "minOccurs=\"" + minOccurs + "\" " +
                               "maxOccurs=\"" + maxOccurs + "\" " +
                               "isattribute=\"" + IsAttribute + "\" " +
                               "iscomplex=\"" + IsComplex + "\" " +
                               "isnillable=\"" + IsNillable + "\">" + vbCrLf);

            java.util.Iterator it = children.iterator();
            while (it.hasNext()){
                xml.append(it.next().toString());
            }

            xml.append("   </parameter>" + vbCrLf);
            return xml.toString();
        }
    }

    private class Options{
        private java.util.ArrayList<String> options = new java.util.ArrayList<String>();
        public Options(){}
        public void addOption(String option){
            options.add(option);
        }
        public String toString(){
            StringBuffer xml = new StringBuffer();
            if (!options.isEmpty()){
                xml.append("<options>");
                java.util.Iterator<String> it = options.iterator();
                while (it.hasNext()){
                    String option = it.next();
                    xml.append("    <option value=\"" + option + "\">" + option + "</option>" + vbCrLf);
                }
                xml.append("</options>");
            }
            return xml.toString();
        }
    }

    private class Choices{
        private java.util.ArrayList<Element> choices = new java.util.ArrayList<Element>();
        public Choices(){}
        public void addChoice(Element choice){
            choices.add(choice);
        }
        public String toString(){
            StringBuffer xml = new StringBuffer();
            if (!choices.isEmpty()){
                xml.append("<options>");
                java.util.Iterator<Element> it = choices.iterator();
                while (it.hasNext()){
                    xml.append("<option>");
                    xml.append(it.next().toString());
                    xml.append("</option>");
                }
                xml.append("</options>");
            }
            return xml.toString();
        }
    }
    
    

  //**************************************************************************
  //** Creates a new instance of wsdl
  //**************************************************************************    
  /** Instantiate wsdl parser using a url to a wsdl (java.net.url) */
    public WSDL(java.net.URL url){
        this(new javaxt.http.Request(url).getResponse().getXML());
    }
    
    public WSDL(String url){
        this(new javaxt.http.Request(url).getResponse().getXML());
    }
    
    public WSDL(java.net.URL url, String HttpProxyServer){
        this(new javaxt.http.Request(url).getResponse().getXML());
    }

    public WSDL(java.io.File xml) {
        this(new javaxt.io.File(xml).getXML());
    }

    public WSDL(Document xml) {
        wsdl = xml;
        NameSpaces = DOM.getNameSpaces(wsdl);
        ElementNameSpace = getElementNameSpace();
        parseWSDL();
    }
    

    
  //**************************************************************************
  //** getSSD
  //**************************************************************************
  /** Returns a Simple Service Description (SSD) - an xml document which
   *  outlines all the web methods and input parameters defined in
   *  the WSDL. This document is significantly easier to parse and understand
   *  than the original WSDL.
   */
    public Document getSSD(){
        return ssd;
    }
    
    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns a string representing an XML document containing a Simple
   *  Service Description (SSD). Please see the getSSD() for more information.
   */
    public String toString(){
        return DOM.getText(ssd);
    }


// <editor-fold defaultstate="collapsed" desc="Core WSDL Parser. Click on the + sign on the left to edit the code.">
    

    
    private NamedNodeMap getDefinitionAttributes(){
        NodeList Definitions = wsdl.getChildNodes();
        for (int i=0; i<Definitions.getLength(); i++ ) {
             if (Definitions.item(i).getNodeType() == 1){
                 if (contains(Definitions.item(i).getNodeName(), "definitions")) {
                     return Definitions.item(i).getAttributes();
                 }
             }
        }
        return null;        
    }
    
    
  //**************************************************************************
  //** getTargetNameSpace
  //**************************************************************************
    
    private String getTargetNameSpace(){
        NamedNodeMap attr = getDefinitionAttributes();
        return getAttributeValue(attr, "targetNamespace");
    }

    
  //**************************************************************************
  //** getElementNameSpace
  //**************************************************************************
    
    private String getElementNameSpace(){
        String elementNameSpace = "http://www.w3.org/2001/XMLSchema";
        NamedNodeMap attr = getDefinitionAttributes();      
        if (attr!=null){
            
            Node node;
            String nodeName;
            String nodeValue;

            for (int i=0; i < attr.getLength(); i++ ) {
                node = attr.item(i);
                nodeName = node.getNodeName().toLowerCase();
                nodeValue = node.getNodeValue();

                if (nodeValue.toLowerCase().equals(elementNameSpace.toLowerCase())) {
                    return stripNameSpace(nodeName);
                }

            }
        }
        return "";
    } 
    
    
  //**************************************************************************
  //** isElementComplex
  //**************************************************************************
    
    private boolean isElementComplex(String elementType){
        String elementNameSpace = "";
        if (elementType.contains(":")){
            elementNameSpace = elementType.substring(0, elementType.indexOf(":"));
        }
        return !elementNameSpace.equalsIgnoreCase(ElementNameSpace);
    }

    
  //**************************************************************************
  //** isElementNillable
  //**************************************************************************
    
    private boolean isElementNillable(String isnillable){
        if (isnillable.toLowerCase().equals("true")){
            return true;
        }
        else{
            return false;
        }        
    }    


  //**************************************************************************
  //** parseWSDL
  //**************************************************************************    
  /** Used to parse the WSDL and generate the SSD. */

    private void parseWSDL(){

        String SSD = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + vbCrLf +
              "<ssd>" + vbCrLf;


        String ServiceName = "";
        String ServiceDescription = "";
        
        NodeList Definitions, ChildNodes;
        NamedNodeMap attr;
        
        

        Port Port = null;
        Binding Binding = null;
        java.util.ArrayList<Binding> arrBindings = null;
        Element Element = null;
        java.util.ArrayList<Element> arrElements = null;
        

      //Loop Through Definitions and Get Services
      //definitions->service (name attribute)
        Definitions = getDefinitions();
        for (int i=0; i<Definitions.getLength(); i++ ) {         
            if (contains(Definitions.item(i).getNodeName(), "service")) {
 
              //Get Service Name
                attr = Definitions.item(i).getAttributes();
                ServiceName = getAttributeValue(attr, "name");

                
              //Get Service Description
                ChildNodes = Definitions.item(i).getChildNodes();
                for (int j=0; j<ChildNodes.getLength(); j++ ) {
                    if (contains(ChildNodes.item(j).getNodeName(), "documentation")) {
                        ServiceDescription = ChildNodes.item(j).getTextContent();
                    }
                }
                

              //Get Service Port
                Port = getPort(ChildNodes);

                
                if (Port!=null){
                    
                                      
                    SSD += " <service name=\"" + ServiceName + "\" url=\"" + Port.Address + "\" namespace=\"" + getTargetNameSpace() + "\">" + vbCrLf;
                    if (ServiceDescription!=null){
                    SSD += "  <description>" + ServiceDescription + "</description>" + vbCrLf;}
                    SSD += "  <methods>" + vbCrLf;
                    
                  //Get Bindings
                    arrBindings = getBindings(Port.Binding);
                    for (int j=0; j<arrBindings.size(); j++ ) {
                         Binding = arrBindings.get(j);

                         
                       //Get Soap Action
                         String SoapAction = Binding.SoapAction;
                         if (SoapAction!=null) SoapAction = " soapAction=\"" + SoapAction + "\"";
                         
                         
                         
                       //Get Messages (need to valid logic here!)
                         //Message Message = getMessages(Port.Name,Binding.Operation);
                         //if (Message==null) Message = getMessages(Port.Binding,Binding.Operation);
                         Message Message = getMessages(Binding.Type,Binding.Operation);
                         
                                      
                       //Get Response Element
                         String ResultsNode = "";
                         try{
                             arrElements = getElements(Message.Output);
                             for (int k=0; k<arrBindings.size(); k++ ) {
                                  Element = arrElements.get(k);
                                  ResultsNode = Element.Name;
                             }
                         }
                         catch(Exception e){
                             //System.out.println(e.toString());
                         }
                         
                         
                         SSD +="  <method name=\"" + Binding.Operation + "\"" + SoapAction + " resultsNode=\"" + ResultsNode + "\">" + vbCrLf;
                         if (Message.Documentation!=null){
                            SSD += "  <description>" + Message.Documentation + "</description>" + vbCrLf;
                         }
                         
                         SSD += "  <parameters>" + vbCrLf;
                         arrElements = getElements(Message.Input);
                         try{
                             for (int k=0; k<arrElements.size(); k++ ) {
                                 Element = arrElements.get(k);
                                 SSD += Element.toString();
                             }
                         }
                         catch(Exception e){
                             //System.out.println(e.toString());
                         }
                         SSD += "  </parameters>" + vbCrLf;


                         SSD +="  </method>" + vbCrLf;
                    }
                    
                  //Update SSD
                    SSD += "  </methods>" + vbCrLf;
                    SSD += " </service>" + vbCrLf;                                    
                }
            }
        }

        SSD += vbCrLf + "</ssd>";
        ssd = DOM.createDocument(SSD);

        knownTypes.clear();
    }  
    
    
  //**************************************************************************
  //** getPort
  //************************************************************************** 
    
    private Port getPort(NodeList Ports){
        
        Port Port = null;
        String PortName, PortBinding, PortAddress;
        boolean foundSoapPort = false;
        
        for (int j=0; j<Ports.getLength(); j++ ) {
            if (contains(Ports.item(j).getNodeName(), "port")) {

                //Get Service Binding
                NamedNodeMap attr = Ports.item(j).getAttributes(); 
                PortName = getAttributeValue(attr, "name");
                PortBinding = stripNameSpace(getAttributeValue(attr, "binding"));
                

                //Get Service Endpoint (url)
                PortAddress = "";
                NodeList Addresses = Ports.item(j).getChildNodes();                 
                for (int k=0; k<Addresses.getLength(); k++ ) {
                    String Address = Addresses.item(k).getNodeName();
                    if (contains(Address, "address") && !contains(Address,"http:") ) { //soap:address
                        attr = Addresses.item(k).getAttributes();
                        PortAddress = getAttributeValue(attr, "location"); 
                        foundSoapPort = true;
                    }
                }
                
                
                if (foundSoapPort){
                    Port = new Port();
                    Port.Name = PortName;
                    Port.Binding = PortBinding;
                    Port.Address = PortAddress;
                    return Port;
                }

            }
        }    
        
        return Port;
    }
    
  //**************************************************************************
  //** getBindings
  //************************************************************************** 
    
    private java.util.ArrayList<Binding> getBindings(String PortBinding){
        
        NodeList Definitions, ChildNodes;
        NamedNodeMap attr;
        
        String BindingName, BindingType, BindingStyle, BindingTransport;
        
        Binding Binding = null;
        java.util.ArrayList<Binding> arrBindings = null;
        int BindingCount = -1;
        
        //Loop through definitions
        Definitions = getDefinitions();
        //Definitions = Definitions.item(0).getChildNodes();
        for (int i=0; i<Definitions.getLength(); i++ ) {
            if (contains(Definitions.item(i).getNodeName(), "binding")) {
                
                //Get Binding Name
                attr = Definitions.item(i).getAttributes();
                BindingName = getAttributeValue(attr, "name");                
                BindingType = getAttributeValue(attr, "type");
                if (BindingName.equals(PortBinding)){
                    
                    
                    arrBindings = new java.util.ArrayList<Binding>();

                    //Get Binding Transport/Style      
                    BindingStyle = BindingTransport = "";
                    ChildNodes = Definitions.item(i).getChildNodes();
                    for (int j=0; j<ChildNodes.getLength(); j++ ) {
                        if (contains(ChildNodes.item(j).getNodeName(), "binding")) {
                            attr = ChildNodes.item(j).getAttributes();
                            BindingStyle = getAttributeValue(attr, "style");
                            BindingTransport = getAttributeValue(attr, "transport");
                        }
                    }

                    
                    //Get Operation Names/Soap Action
                    for (int j=0; j<ChildNodes.getLength(); j++ ) {
                        if (contains(ChildNodes.item(j).getNodeName(), "operation")) {
                            
                            Binding = new Binding();
                            BindingCount +=1;
                            
                            attr = ChildNodes.item(j).getAttributes();
                            Binding.Operation = getAttributeValue(attr, "name");
                            
                            NodeList Operations = ChildNodes.item(j).getChildNodes();
                            for (int k=0; k<Operations.getLength(); k++ ) {
                                 if (contains(Operations.item(k).getNodeName(), "operation")) {
                                     attr = Operations.item(k).getAttributes();
                                     Binding.SoapAction = getAttributeValue(attr, "soapaction");
                                     Binding.Style = BindingStyle; //getAttributeValue(attr, "style");
                                     
                                     Binding.Name = BindingName;
                                     Binding.Type = stripNameSpace(BindingType);
                                 }
                            }
                            

                            arrBindings.add(Binding);
                        }
                    }  
                    
                    
                    return arrBindings;
                    
                }

             
                
            }
        }        
        
        return null;
    }
    

  //**************************************************************************
  //** getMessages
  //************************************************************************** 
    
    private Message getMessages(String PortTypeName, String OperationName){
        
        //System.out.println();
        //System.out.println(PortTypeName);
        
        NodeList Definitions, PortTypes, Messages;
        NamedNodeMap attr;
        String portTypeName, operationName, messageName;        
        Message Message = null;
        
        
        //Loop through definitions
        Definitions = getDefinitions();
        //Definitions = Definitions.item(0).getChildNodes();
        for (int i=0; i<Definitions.getLength(); i++ ) {
            if (contains(Definitions.item(i).getNodeName(), "porttype")) {
                
                
                
                attr = Definitions.item(i).getAttributes();
                portTypeName = getAttributeValue(attr, "name"); 
                
                //System.out.println(" vs " + getAttributeValue(attr, "name"));

                if (portTypeName.equals(PortTypeName)){
                    
                    
                    String Documentation = "";

                    //Loop through PortTypes
                    PortTypes = Definitions.item(i).getChildNodes();
                    for (int j=0; j<PortTypes.getLength(); j++ ) {
                        
                        String NodeName = PortTypes.item(j).getNodeName();
                        
                        
                        if (NodeName.endsWith("documentation")) {
                            Documentation = DOM.getNodeValue(PortTypes.item(j));
                        }
                        
                        if (NodeName.endsWith("operation")) {
                            
                            attr = PortTypes.item(j).getAttributes();
                            operationName = getAttributeValue(attr, "name"); 
                            if (operationName.equals(OperationName)){
                                
                              //Instantiate Message Object
                                Message = new Message();
                                Message.Documentation = Documentation;
                                
                              //Loop through the Messages
                                Messages = PortTypes.item(j).getChildNodes();
                                for (int k=0; k<Messages.getLength(); k++ ) {
                                    if (Messages.item(k).getNodeType()==1){
                                    
                                        attr = Messages.item(k).getAttributes();
                                        messageName = stripNameSpace(getAttributeValue(attr, "message"));

                                        if (contains(Messages.item(k).getNodeName(), "input")) {
                                            Message.Input = messageName;
                                        }
                                        if (contains(Messages.item(k).getNodeName(), "output")) {
                                            Message.Output = messageName;
                                        } 
                                        if (contains(Messages.item(k).getNodeName(), "documentation")) {
                                            Documentation = DOM.getNodeValue(Messages.item(k));
                                            if (Documentation.length()>0){
                                                Message.Documentation = Documentation;
                                            }
                                        }  
                                    }
                                }
                                
                                return Message;
                            
                            }
                        }
                    }
                }

                 
            }
        }        
        
        return Message;
    }   
    
    
  //**************************************************************************
  //** getElements
  //************************************************************************** 
  /** Returns a list of elements (parameters) associated with a given message.
   *  definitions->message->part (element name attribute)
   */    
    private java.util.ArrayList<Element> getElements(String MessageName){

        NodeList Definitions, Messages;
        NamedNodeMap attr;
        
        String messageName, name, type, min, max;
        

        java.util.ArrayList<Element> elements = new java.util.ArrayList<Element>();

      //Loop through Definitions and Find Messages
        Definitions = getDefinitions();
        for (int i=0; i<Definitions.getLength(); i++ ) {
            if (contains(Definitions.item(i).getNodeName(), "message")) {
                
                attr = Definitions.item(i).getAttributes();
                messageName = getAttributeValue(attr, "name");
                if (messageName.equals(MessageName)){

                  

                  //Loop through Messages and Find Message Parts
                    Messages = Definitions.item(i).getChildNodes();
                    for (int j=0; j<Messages.getLength(); j++ ) {
                        if (contains(Messages.item(j).getNodeName(), "part")) { 
                            

                            attr = Messages.item(j).getAttributes();
                            type = getAttributeValue(attr, "type");
                            
                            if (type!=""){ //This is rare!
                                
                                
                                Element element = new Element(Messages.item(j));
                                elements.add(element);

                            }
                            else{      
                              
                                String element = stripNameSpace(getAttributeValue(attr, "element"));
                                elements = getElement(element);
                            }
                            
                        }
                    }
                    
                    return elements;
                }
            }
        }
        
        return null;
        
    }


  //**************************************************************************
  //** getDefinitions
  //**************************************************************************    
    
    private NodeList getDefinitions(){
        NodeList Definitions = wsdl.getChildNodes();
        if (Definitions!=null){
            for (int i=0; i<Definitions.getLength(); i++){
                 Node node = Definitions.item(i);
                 if (node.getNodeType() == 1){
                     if (node.getNodeName().endsWith("definitions")) {
                         return Definitions.item(i).getChildNodes();
                     }
                 }
            }
        }
        return null;
    }
    

  //**************************************************************************
  //** getTypes
  //**************************************************************************     
    
    private NodeList getTypes(){        
        NodeList Definitions = getDefinitions();
        if (Definitions!=null){
            for (int i=0; i<Definitions.getLength(); i++ ) {
                 Node node = Definitions.item(i);
                 if (node.getNodeType()==1){
                     if (node.getNodeName().endsWith("types")){
                         return node.getChildNodes();
                     }
                 }
            }
        }
        return null;
    }
    

    
  //**************************************************************************
  //** getShema
  //**************************************************************************  
    
    private NodeList getSchema(){
        if (Schema==null) addSchema(wsdl, false);
        return Schema;
    }


  //**************************************************************************
  //** addSchema
  //**************************************************************************
  /** Used to add an external XSD.
   *  @param followImports Flag used to indicate whether to download/parse XSD
   *  files referenced by the schema/import nodes.
   */
    public void addSchema(Document xsd, boolean followImports){

        if (xsd!=null){

            Node outerNode = DOM.getOuterNode(xsd);
            String outerNodeName = stripNameSpace(outerNode.getNodeName());

            NodeList schemaNodes = null;
            if (outerNodeName.equalsIgnoreCase("definitions")){
                NodeList childNodes = outerNode.getChildNodes();
                for (int i=0; i<childNodes.getLength(); i++){
                    Node node = childNodes.item(i);
                    if (stripNameSpace(node.getNodeName()).equalsIgnoreCase("types")){                        
                        NodeList types = node.getChildNodes();
                        for (int j=0; j<types.getLength(); j++){
                            Node typeNode = types.item(j);
                            if (stripNameSpace(typeNode.getNodeName()).equalsIgnoreCase("schema")){
                                schemaNodes = typeNode.getChildNodes();
                            }
                        }
                    }
                }
            }
            else if (outerNodeName.equalsIgnoreCase("schema")){
                schemaNodes = outerNode.getChildNodes();
            }
            else{
                return;
            }


            NameSpaces.putAll(DOM.getNameSpaces(xsd));
            StringBuffer auxSchemas = new StringBuffer();

            
          //Loop through Schemas and Get Imports            
            for (int i=0; i<schemaNodes.getLength(); i++ ) {
                Node node = getNode(schemaNodes.item(i), "import");
                if (node!=null){
                    NamedNodeMap attr = node.getAttributes();

                    if (followImports){
                        importSchemas(getAttributeValue(attr, "schemaLocation"), auxSchemas);
                    }

                    Node iSchema = node.getParentNode();
                    iSchema.removeChild(node);
                }
            }


            StringBuffer xml = new StringBuffer();
            xml.append("<?xml version=\"1.0\"?>");
            xml.append("<schemas");
            java.util.Iterator<String> it = NameSpaces.keySet().iterator();
            while (it.hasNext()){
                String ns = it.next();
                String url = (String) NameSpaces.get(ns);
                xml.append(" xmlns:");
                xml.append(ns);
                xml.append("=\"");
                xml.append(url);
                xml.append("\"");
            }
            xml.append(">");
            if (this.Schema!=null) xml.append(DOM.getText(this.Schema));
            xml.append(DOM.getText(schemaNodes));
            xml.append(auxSchemas);
            xml.append("</schemas>");

            boolean parseWSDL = this.Schema!=null;
            this.Schema = DOM.getOuterNode(DOM.createDocument(xml.toString())).getChildNodes();
            if (parseWSDL) parseWSDL();

        }
    }


  //**************************************************************************
  //** importSchemas
  //**************************************************************************
  /** Used to download and parse an XSD document. Used in conjunction with the
   *  addSchema method. Note that this is a recursive function.
   *
   *  @param schemaLocation URL to the XSD file.
   *  @param auxSchemas StringBuffer used to append new schema nodes.
   */
    private void importSchemas(String schemaLocation, StringBuffer auxSchemas){
        
        javaxt.http.Response response = new javaxt.http.Request(schemaLocation).getResponse();
        String txt = response.getText();

        Document doc = DOM.createDocument(txt);
        if (doc!=null){


            NameSpaces.putAll(DOM.getNameSpaces(doc));


            NodeList Schema = doc.getChildNodes();
            Schema = getChildNodes(Schema,"schema");
            for (int i=0; i<Schema.getLength(); i++ ) {
                Node node = getNode(Schema.item(i), "import");
                if (node!=null){
                    NamedNodeMap attr = node.getAttributes();
                    schemaLocation = getAttributeValue(attr, "schemaLocation");

                    importSchemas(schemaLocation, auxSchemas);

                    Node iSchema = node.getParentNode();
                    iSchema.removeChild(node);
                }
            }
            auxSchemas.append(DOM.getText(Schema));
        }
    }




  //**************************************************************************
  //** getElement
  //**************************************************************************
  /** Used to find an element
   *  definitions->types->schema->element (name attribute)
   */
    private java.util.ArrayList<Element> getElement(String ElementName){

      //Loop Through Schemas and Find Elements
        NodeList schemas = getSchema();
        for (int i=0; i<schemas.getLength(); i++ ) {

            Node elementNode = schemas.item(i);
            String elementName = DOM.getAttributeValue(elementNode, "name");
            String elementType = stripNameSpace(elementNode.getNodeName());

            if (elementName.equals(ElementName)) {

                if (elementType.equalsIgnoreCase("element")) {

                    java.util.ArrayList<Element> elements = new java.util.ArrayList<Element>();

                    
                    if (!DOM.hasChildren(elementNode)){ //Complex Type!

                        Element element = new Element(elementNode);
                        decomposeComplexType(stripNameSpace(element.Name), element);
                        elements.add(element);

                    }
                    else{ //Simple Type

                        Element element = new Element(elementNode);
                        decomposeComplexType(stripNameSpace(element.Name), element);

                        java.util.Iterator<Object> it = element.children.iterator();
                        while (it.hasNext()){
                            Object obj = it.next();
                            if (obj instanceof Element){
                                elements.add((Element) obj);
                            }
                            else{// ???
                            }
                        }
                    }

                    return elements;
                }
            }
        }

        return null;
    }

  //**************************************************************************
  //** knownTypes
  //**************************************************************************
  /** Hashmap of known Element Types. It is extremely important that you update
   *  this hashmap whenever you find a new element to prevent StackOverflows
   *  in the recursive decomposeComplexType method.
   */
    private java.util.HashMap<String, Element> knownTypes = new java.util.HashMap<String, Element>();


  //**************************************************************************
  //** decomposeComplexType (recursive function)
  //**************************************************************************    
    
    private void decomposeComplexType(String ElementName, Element parentElement){

        if (knownTypes.containsKey(ElementName)){
            Element Element = knownTypes.get(ElementName);
            parentElement.addElement(Element);
            return;
        }

        NodeList Schemas = getSchema(); 
        for (int i=0; i<Schemas.getLength(); i++ ) {

            Node node = Schemas.item(i);
            
            String typeName = DOM.getAttributeValue(node, "name");
            if (typeName.equals(ElementName)){

                this.parseComplexNode(node, ElementName, parentElement);

            }

        } //end loop     

    }

    

    private void parseComplexNode(Node node, String ElementName, Element parentElement){

        String nodeName = stripNameSpace(node.getNodeName().toLowerCase());

        if (nodeName.equals("element")){

            if (!DOM.hasChildren(node)){

                Element Element = new Element(node);
                if (Element.IsComplex){
                    decomposeComplexType(stripNameSpace(Element.Type), parentElement);
                }

            }
            else{

                NodeList complexTypes = node.getChildNodes();
                for (Node complexNode : DOM.getNodes(complexTypes)){
                    parseComplexNode(complexNode, ElementName, parentElement);
                }


            }
        }
        else if (nodeName.equals("complextype")){
            NodeList complexTypes = node.getChildNodes();
            for (Node complexNode : DOM.getNodes(complexTypes)){
                String complexType = stripNameSpace(complexNode.getNodeName());
                if (complexType.equalsIgnoreCase("sequence")) {
                    NodeList sequenceNodes = complexNode.getChildNodes();
                    for (Node sequenceNode : DOM.getNodes(sequenceNodes)){
                        if (stripNameSpace(sequenceNode.getNodeName()).equals("element")) {

                            Element Element = new Element(sequenceNode);
                            parentElement.addElement(Element);
                            knownTypes.put(ElementName, Element);

                            if (Element.IsComplex){
                                decomposeComplexType(stripNameSpace(Element.Type), Element);
                            }
                        }
                    }
                }
                if (complexType.equalsIgnoreCase("attribute")) {

                    Element Element = new Element(complexNode);
                    parentElement.addElement(Element);
                    knownTypes.put(ElementName, Element);

                    if (Element.IsComplex){
                        decomposeComplexType(stripNameSpace(Element.Type), Element);
                    }

                }
                if (complexType.equalsIgnoreCase("simpleContent")) {

                    NodeList childNodes = complexNode.getChildNodes();
                    for (Node childNode : DOM.getNodes(childNodes)){
                        if (stripNameSpace(childNode.getNodeName()).equalsIgnoreCase("extension")){

                            Element Element = new Element(ElementName, DOM.getAttributeValue(childNode, "base"));
                            if (Element.IsComplex){
                                //TODO
                            }


                            NodeList attributes = childNode.getChildNodes();
                            for (Node attribute : DOM.getNodes(attributes) ){
                                Element.addElement(new Element(attribute));
                            }

                            parentElement.addElement(Element);
                            knownTypes.put(ElementName, Element);

                        }
                    }
                }
                else if(complexType.equalsIgnoreCase("choice")) {

                    Choices choices = new Choices();
                    NodeList choiceNodes = complexNode.getChildNodes();
                    for (int k=0; k<choiceNodes.getLength(); k++){
                        Node choiceNode = choiceNodes.item(k);
                        if (stripNameSpace(choiceNode.getNodeName()).equalsIgnoreCase("element")){

                            Element Element = new Element(choiceNode);

                                choices.addChoice(Element);

                                if (Element.IsComplex){
                                    decomposeComplexType(stripNameSpace(Element.Type), Element);
                                }

                        }
                    }
                    parentElement.addChoices(choices);
                }
            }
        }
        else if (nodeName.equals("simpletype")){

            Options options = new Options();
            NodeList SimpleType = node.getChildNodes();
            for (int y=0; y<SimpleType.getLength(); y++ ) {
                if (stripNameSpace(SimpleType.item(y).getNodeName()).equals("restriction")){
                    NodeList Restriction = SimpleType.item(y).getChildNodes();
                    for (int z=0; z<Restriction.getLength(); z++ ) {
                        if (stripNameSpace(Restriction.item(z).getNodeName()).equals("enumeration")){
                            NamedNodeMap attr = Restriction.item(z).getAttributes();
                            String OptionValue = getAttributeValue(attr, "value");
                            if (OptionValue.length()>0){
                                options.addOption(OptionValue);
                            }
                        }
                    }
                }
            }

            parentElement.addOptions(options);

        }
        else{
            System.err.println("Unsupported element type: " + nodeName);
        }
    }
    
    
  // </editor-fold>
    
// <editor-fold defaultstate="collapsed" desc="Public Members. Click on the + sign on the left to edit the code.">


    
    
    

    
    
    

    
  //**************************************************************************
  //** getService
  //**************************************************************************
    
    public Service getService(String ServiceName){
        
        if (ServiceName==null) ServiceName = "";        
        ServiceName = ServiceName.trim();
        
        if (ServiceName.equals("")){
            return getService(0);
        }
        
        Service[] arrServices = getServices();
        if (arrServices!=null){
            for (int i=0; i<arrServices.length; i++){
                 if (arrServices[i].equals(ServiceName)){
                     return arrServices[i];
                 }
            }
        }
        return null;
    }
      
    
  //**************************************************************************
  //** getService
  //**************************************************************************
    
    public Service getService(int i){
        Service[] arrServices = getServices();
        if (arrServices!=null){
            if (i<arrServices.length){
                return arrServices[i];
            }
        }
        return null;
    }
    
    

  //**************************************************************************
  //** getMethod
  //**************************************************************************
    
    public Method getMethod(String ServiceName, String MethodName){
        Service Service = getService(ServiceName);
        if (Service!=null){
            return Service.getMethod(MethodName);
        }
        return null;
    }
    
  //**************************************************************************
  //** getMethod
  //**************************************************************************
    
    public Method getMethod(String MethodName){
        Service Service = getService(0);
        if (Service!=null){
            return Service.getMethod(MethodName);
        }
        return null;
    }
    
    

    
    
    
  //**************************************************************************
  //** getServices
  //**************************************************************************
  /**  Used to retrieve an array of Services from an SSD NodeList */
    
    public Service[] getServices(){
        
        java.util.ArrayList<Service> services = new java.util.ArrayList<Service>();
        
        for (Node serviceNode : DOM.getNodes(ssd.getElementsByTagName("service"))){
            Service service = getService(serviceNode);
            if (service!=null){
                services.add(service);
            }
        }
                
        if (services.isEmpty()){
            return null;
        }
        else{
            return services.toArray(new Service[services.size()]);
        }
    }
    
  //**************************************************************************
  //** getService -> NEW!
  //**************************************************************************
  /**  Used to retrieve a service from an SSD Service Node */
    
    private Service getService(Node ServiceNode){
        
        Service Service = null;
        
        if (ServiceNode.getNodeType()==1){
            NamedNodeMap attr = ServiceNode.getAttributes();

            Service = new Service();
            Service.Name = getAttributeValue(attr, "name");
            Service.NameSpace = getAttributeValue(attr, "namespace");
            Service.URL = getAttributeValue(attr, "url");

            NodeList ChildNodes = ServiceNode.getChildNodes();
            for (int j=0; j<ChildNodes.getLength(); j++ ) {
                if (ChildNodes.item(j).getNodeType()==1){
                    String NodeName = ChildNodes.item(j).getNodeName();
                    String NodeValue = ChildNodes.item(j).getTextContent();
                    if (NodeName.toLowerCase().equals("description")){
                        Service.Description = NodeValue;
                    }
                    if (NodeName.toLowerCase().equals("methods")){
                        //Service.Methods = ChildNodes.item(j).getChildNodes();
                        Service.Methods = getMethods(ChildNodes.item(j).getChildNodes());
                    }
                }
            }
        
        }
                    
        return Service;
    }
    
    
  //**************************************************************************
  //** getMethods
  //**************************************************************************
  /**  Used to retrieve an array of methods from an SSD NodeList */
    
    private Method[] getMethods(NodeList methodNodes){

        java.util.ArrayList<Method> methods = new java.util.ArrayList<Method>();
        
        for (Node methodNode : DOM.getNodes(methodNodes)){

            Method method = getMethod(methodNode);
            if (method!=null){
                methods.add(method);
            }
        }
        
        if (methods.isEmpty()){
            return null;
        }
        else{
            return methods.toArray(new Method[methods.size()]);
        }
    }
    
    
  //**************************************************************************
  //** getMethod
  //**************************************************************************
  /**  Used to retrieve a method from an SSD Method Node */
    
    private Method getMethod(Node MethodNode){
        
        Method Method = null;
        
        if (MethodNode.getNodeType()==1){
            NamedNodeMap attr = MethodNode.getAttributes();

            Method = new Method();
            Method.Name = getAttributeValue(attr, "name");  
            Method.SoapAction = getAttributeValue(attr, "soapAction");
            Method.ResultsNode = getAttributeValue(attr, "resultsNode");
            //Method.URL = Service.URL;
            //Method.NameSpace = Service.NameSpace;

            NodeList ChildNodes = MethodNode.getChildNodes();
            for (int j=0; j<ChildNodes.getLength(); j++ ) {
                if (ChildNodes.item(j).getNodeType()==1){
                    String NodeName = ChildNodes.item(j).getNodeName();
                    String NodeValue = ChildNodes.item(j).getTextContent();
                    if(NodeName.toLowerCase().equals("description")){
                       Method.Description = NodeValue;
                    }
                    if(NodeName.toLowerCase().equals("parameters")){
                         Method.ParameterXML = ChildNodes.item(j).getChildNodes();
                         Method.Parameters = getParameters(ChildNodes.item(j).getChildNodes());
                    }
                }
            }

            
        }
            
        return Method;
    }
    
    
    
  //**************************************************************************
  //** getParameters
  //**************************************************************************
  /**  Used to retrieve an array of parameters from an SSD NodeList */
    
    private Parameter[] getParameters(NodeList parameterNodes){
        
        java.util.ArrayList<Parameter> parameters = new java.util.ArrayList<Parameter>();

        for (Node parameterNode : DOM.getNodes(parameterNodes)){
            Parameter parameter = getParameter(parameterNode);
            parameters.add(parameter);

          //Need to verify recursion logic here!
            if (parameter.isComplex()){
                parameter.Children = getParameters(parameter.ChildNodes);
            }
        }
        
        if (parameters.isEmpty()){
            return null;
        }
        else{
            return parameters.toArray(new Parameter[parameters.size()]);
        }
    }
    
    

  //**************************************************************************
  //** getParameter
  //**************************************************************************
    
    private Parameter getParameter(Node ParameterNode){
        Parameter Parameter = new Parameter();
        NamedNodeMap attr = ParameterNode.getAttributes();
        Parameter.Name = getAttributeValue(attr, "name");
        Parameter.Type = getAttributeValue(attr, "type");
        Parameter.IsAttribute = bool(getAttributeValue(attr, "isattribute"));
        Parameter.IsNillable = bool(getAttributeValue(attr, "isnillable"));
        Parameter.minOccurs = getAttributeValue(attr, "minOccurs");
        Parameter.ChildNodes = ParameterNode.getChildNodes();
        Parameter.ParentNode = ParameterNode.getParentNode();
        Parameter.Options = getOptions(ParameterNode.getChildNodes());

        /*
        if (Parameter.Options!=null){
            Parameter.IsComplex = false;
        }
        */

        return Parameter;
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
        String value = getAttributeValue(attr, "value");
        String name = value;
        return new Option(name, value);
    }
    
    
    
    
  //**************************************************************************
  //** getListOfServices
  //**************************************************************************
    
    public String[] getListOfServices(){
        String[] arrServices = null;
        Service[] Services = getServices();
        if (Services!=null){
            arrServices = new String[Services.length];
            for (int i=0; i<Services.length; i++) { 
                 arrServices[i] = Services[i].getName();
            }
        }

        return arrServices;
    } 

  //**************************************************************************
  //** getListOfMethods
  //**************************************************************************
    
    public String[] getListOfMethods(String ServiceName){
        
        String[] arrMethods = null;
        Service Service = getService(ServiceName);
        Method[] Methods = Service.Methods;
        if (Methods!=null){
            arrMethods = new String[Methods.length];
            for (int i=0; i<Methods.length; i++) { 
                 arrMethods[i] = Methods[i].getName();
            }
        }

        return arrMethods;
    }     
    
    
// </editor-fold>


    private NodeList getChildNodes(NodeList ParentNodes, String NodeName){
        for (int i=0; i<ParentNodes.getLength(); i++ ) {
             Node node = ParentNodes.item(i);
             if (node.getNodeType() == 1){
                 //System.out.println(node.getNodeName());
                 if (node.getNodeName().endsWith(NodeName)) {
                     return node.getChildNodes();
                 }
             }
        }
        return null;
    }

    private Node getNode(Node node, String NodeName){
       if (node.getNodeType()==1) {
           if (node.getNodeName().endsWith(NodeName)){
               return node;
           }
       }
       return null;
    }

    
  //**************************************************************************
  //** getAttributeValue
  //**************************************************************************
    
    private String getAttributeValue(NamedNodeMap attrCollection, String attrName){
        return DOM.getAttributeValue(attrCollection,attrName);
    }    
    
    
  //**************************************************************************
  //** bool - convert String to boolean
  //**************************************************************************
    
    private boolean bool(String str){
        if (str.equalsIgnoreCase("true")) return true;
        else return false;
    }

    
  //**************************************************************************
  //** stripNameSpace
  //**************************************************************************
    
    private String stripNameSpace(String str){
        if (str.contains(":")) str = str.substring(str.lastIndexOf(":")+1);
        return str;
    }
    
  //**************************************************************************
  //** Legacy VB String Functions
  //**************************************************************************
    
    private boolean contains(String str, String ch){ return string.contains(str,ch,true); }

}

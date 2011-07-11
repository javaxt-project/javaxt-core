package javaxt.webservices;
import javaxt.utils.string;
import org.w3c.dom.*;
import javaxt.xml.DOM;

/******************************************************************************
/**  WSDL Parser - By Peter Borissow
/*****************************************************************************/
/**  Used to parse a WSDL file and generate a SSD (Simple Service Description).
 *   Once an SSD is generated, the class is used to can be used to extract
 *   portions of the WSDL (e.g. Method Names, etc) and can even be used to
 *   generate a SoapRequest.
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
        
    private Document XMLDoc;        
    private String SSD; //Simple Service Definition    
    private String vbCrLf = "\r\n"; 
    private String ElementNameSpace = "";
    private String temp = "";
    
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
        

        public Element(Node node){
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
            //System.out.println(Type);
            
            String elementRef = getAttributeValue(attr, "ref");
            if (elementRef.length()>0){
                Name = elementRef;
            }
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
        XMLDoc = xml;

        ElementNameSpace = getElementNameSpace();
        //getElements();
        //importSchemas();
        
        SSD = "";        
        generateSSD();
        SSD = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + vbCrLf + 
              "<ssd>" + vbCrLf + SSD + vbCrLf + "</ssd>";
        
        XMLDoc = DOM.createDocument(SSD);
    }
    
    

    
    
  //**************************************************************************
  //** getElements
  //**************************************************************************
/*    
    private void getElements(){
        
        Element[] Elements = null;
        
      //Find All Elements in the definitions/types/schema Section of the WSDL
        NodeList Schema = getSchema();
        Elements = getElements(Schema);
        
      //Find All Elements in the definitions/message/part/element Section of the WSDL (rare case)
        
        
      //Import All Imported Elements
        NodeList[] Schemas = getSchemas();
        if (Schemas!=null){
            for (int i=0; i<Schemas.length; i++){
                 getElements(Schemas[i]);
            }
        }
    }
    
    private Element[] getElements(NodeList Schema){
        Element[] Elements = null;
        if (Schema!=null){
            for (int i=0; i<Schema.getLength(); i++){
                 Node node = getNode(Schema.item(i),"element");
                 if (node!=null){
                     Element Element = new Element(node);
                     Elements = resizeArray(Element,Elements);
                 }
            }
        }
        return Elements;
    }
*/    
    
 
    
    //private NodeList[] Schemas = null;
    private StringBuffer auxSchemas;
    private java.util.HashMap NameSpaces;
    private void importSchemas(){
        
        //System.out.println("SOURCE = " + source);
        //javaxt.io.File File = new javaxt.io.File(source);
        
        auxSchemas = new StringBuffer();
        NameSpaces = new java.util.HashMap();
        
       //Loop through Schemas and Get Imports
         NodeList Schema = getSchema();         
         for (int i=0; i<Schema.getLength(); i++ ) {
              Node node = getNode(Schema.item(i), "import");
              if (node!=null){
                  NamedNodeMap attr = node.getAttributes();
                  String schemaLocation = getAttributeValue(attr, "schemaLocation");
                  //schemaLocation = File.MapPath(schemaLocation);
                  
                  //Schemas = getSchemas(schemaLocation, Schemas);
                  importSchemas(schemaLocation);
                  
                //Remove Import
                  Node iSchema = node.getParentNode();
                  iSchema.removeChild(node);
              }
         }
         
         auxSchemas.append(DOM.getText(Schema));
         //return Schemas;
/*         
         String Attributes = "";
         StringBuffer s = new StringBuffer();
         java.util.Iterator iter = NameSpaces.keySet().iterator();
         while(iter.hasNext()){
             String key = (String) iter.next();
             String value = (String) NameSpaces.get(key);
             Attributes += " " + key + "=\"" + value + "\"";
         }
         s.append("<z" + Attributes + ">" + auxSchemas.toString() + "</z>");

         
        File = new utils.File("C:\\Temp\\trash.xml");
        File.Save(s.toString());
*/        
         
    }
    
    private void importSchemas(String schemaLocation){
        //System.out.println("SOURCE = " + schemaLocation);
        //javaxt.io.File File = new javaxt.io.File(schemaLocation);
        Document doc = DOM.createDocument(schemaLocation);
        if (doc!=null){


            NameSpaces.putAll(DOM.getNameSpaces(doc));

            
            NodeList Schema = doc.getChildNodes();
            Schema = getChildNodes(Schema,"schema");
            //Schemas = resizeArray(auxSchemas, Schemas);
            for (int i=0; i<Schema.getLength(); i++ ) {
                 //System.out.println(Schema.item(i).getNodeName());
                 Node node = getNode(Schema.item(i), "import");
                 if (node!=null){
                     NamedNodeMap attr = node.getAttributes();
                     schemaLocation = getAttributeValue(attr, "schemaLocation");
                     //schemaLocation = File.MapPath(schemaLocation);

                     importSchemas(schemaLocation);

                     Node iSchema = node.getParentNode();
                     iSchema.removeChild(node);
                 }
            }
            
            auxSchemas.append(DOM.getText(Schema));
            
            //Schemas = resizeArray(Schema, Schemas);

        }
    }
    
/*    
    private NodeList[] getSchemas(){
        
        System.out.println("SOURCE = " + source);
        utils.File File = new utils.File(source);
        
        NodeList[] Schemas = null;
        
        
       //Loop through Schemas and Get Imports
         NodeList Schema = getSchema();         
         for (int i=0; i<Schema.getLength(); i++ ) {
              Node node = getNode(Schema.item(i), "import");
              if (node!=null){
                  NamedNodeMap attr = node.getAttributes();
                  String schemaLocation = getAttributeValue(attr, "schemaLocation");
                  schemaLocation = File.MapPath(schemaLocation);
                  
                  Schemas = getSchemas(schemaLocation, Schemas);
                  

                  
                  Node iSchema = node.getParentNode();
                  iSchema.removeChild(node);
              }
         }
         return Schemas;
         
    }
    
    private NodeList[] getSchemas(String schemaLocation, NodeList[] Schemas){
        
        System.out.println("SOURCE = " + schemaLocation);
        utils.File File = new utils.File(schemaLocation);
        Document doc = xml.getDocument(schemaLocation);
        if (doc!=null){

            NodeList Schema = doc.getChildNodes();
            Schema = getChildNodes(Schema,"schema");
            //Schemas = resizeArray(auxSchemas, Schemas);
            for (int i=0; i<Schema.getLength(); i++ ) {
                 //System.out.println(Schema.item(i).getNodeName());
                 Node node = getNode(Schema.item(i), "import");
                 if (node!=null){
                     NamedNodeMap attr = node.getAttributes();
                     schemaLocation = getAttributeValue(attr, "schemaLocation");
                     schemaLocation = File.MapPath(schemaLocation);

                     Schemas = getSchemas(schemaLocation, Schemas);

                     Node iSchema = node.getParentNode();
                     iSchema.removeChild(node);
                 }
            }
            
            Schemas = resizeArray(Schema, Schemas);

        }
                  
        return Schemas;
        
    }
    
*/
    
  //**************************************************************************
  //** getSSD
  //**************************************************************************
    
    public String getSSD(){
        //utils.File File = new utils.File("C:\\Temp\\trash.xml");
        //File.Save(SSD);
        return SSD;
    }
    
    
  //**************************************************************************
  //** toString
  //**************************************************************************
    
    public String toString(){
        return getSSD();
    }


// <editor-fold defaultstate="collapsed" desc="Core WSDL Parser. Click on the + sign on the left to edit the code.">
    

    
    private NamedNodeMap getDefinitionAttributes(){
        NodeList Definitions = XMLDoc.getChildNodes();
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
        String elementNameSpace = getNameSpace(elementType);
        if (elementNameSpace.toLowerCase().equals(ElementNameSpace.toLowerCase())){
            return false;
        }
        else{
            return true;
        }        
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
  //** generateSSD
  //**************************************************************************    
  
    private void generateSSD(){       
        //Extract services from WSDL:
        //definitions->service (name attribute)
        
        String ServiceName = "";
        String ServiceDescription = "";
        
        NodeList Definitions, ChildNodes;
        NamedNodeMap attr;
        
        

        Port Port = null;
        Binding Binding = null;
        Binding[] arrBindings = null;
        Element Element = null;
        Element[] arrElements = null; 
        

      //Loop Through Definitions and Get Services
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
                    
                    
                  //Update SSD
                    SSD += " <service name=\"" + ServiceName + "\" url=\"" + Port.Address + "\" namespace=\"" + getTargetNameSpace() + "\">" + vbCrLf;
                    if (ServiceDescription!=null){
                    SSD += "  <description>" + ServiceDescription + "</description>" + vbCrLf;}
                    SSD += "  <methods>" + vbCrLf;
                    
                  //Get Bindings
                    arrBindings = getBindings(Port.Binding);
                    for (int j=0; j<arrBindings.length; j++ ) {
                         Binding = arrBindings[j];

                         
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
                         arrElements = getMessageType(Message.Output);
                         for (int k=0; k<arrBindings.length; k++ ) {
                              Element = arrElements[k];
                              ResultsNode = Element.Name;
                         }  
                         
                         }catch(Exception e){
                             //System.out.println(e.toString());
                         }
                         
                         
                         SSD +="  <method name=\"" + Binding.Operation + "\"" + SoapAction + " resultsNode=\"" + ResultsNode + "\">" + vbCrLf;
                         if (Message.Documentation!=null){
                         SSD += "  <description>" + Message.Documentation + "</description>" + vbCrLf;}
                         SSD += "  <parameters>" + vbCrLf;
                         

                         
                       //Get Input Elements (Input Parameters)
                         try{
                         arrElements = getMessageType(Message.Input);
                         for (int k=0; k<arrElements.length; k++ ) {
                              Element = arrElements[k];
                              
                              
                                SSD += "   <parameter " + 
                                           "name=\"" + Element.Name + "\" " + 
                                           "type=\"" + Element.Type + "\" " + 
                                           "minOccurs=\"" + Element.minOccurs + "\" " + 
                                           "maxOccurs=\"" + Element.maxOccurs + "\" " + 
                                           "iscomplex=\"" + Element.IsComplex + "\" " + 
                                           "isnillable=\"" + Element.IsNillable + "\">" + vbCrLf;
                                try{
                                if (Element.IsComplex){
                                    temp = "";
                                    decomposeComplexType(Element.Type, null);
                                    SSD += temp;
                                }
                                }catch(Exception e){}
                                
                                SSD += "   </parameter>" + vbCrLf;

                         }
                         }catch(Exception e){
                             //System.out.println(e.toString());
                             StackTraceElement[] arr = e.getStackTrace();
                             for (int z=0; z<arr.length; z++){
                                  //System.out.println(arr[z]);
                             }
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
    
    private Binding[] getBindings(String PortBinding){
        
        NodeList Definitions, ChildNodes;
        NamedNodeMap attr;
        
        String BindingName, BindingType, BindingStyle, BindingTransport;
        
        Binding Binding = null;
        Binding[] arrBindings = null;
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
                    
                    
                    arrBindings = new Binding[0];

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
                            
                            arrBindings = (Binding[])resizeArray(arrBindings,BindingCount+1);
                            arrBindings[BindingCount] = Binding;
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
  //** getMessageType
  //************************************************************************** 
  /** Used to extract parameter names */
    
    private Element[] getMessageType(String MessageName){
        //definitions->message->part (element name attribute)
        
        NodeList Definitions, Messages;
        NamedNodeMap attr;
        
        String messageName, element, name, type, min, max;
        
        Element Element = null;
        Element[] arrElements = new Element[0];
        int ElementCount = -1;
    

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
                                
                                ElementCount +=1;
                                Element = new Element(Messages.item(j));
                                arrElements = (Element[])resizeArray(arrElements,ElementCount+1);
                                arrElements[ElementCount] = Element;

                            }
                            else{      
                                
                                element = stripNameSpace(getAttributeValue(attr, "element")); 
                                arrElements = getElementType(element);
                            }
                            
                        }
                    }
                    
                    return arrElements;
                }
            }
        }
        
        return null;
        
    }

    
  //**************************************************************************
  //** getElementType
  //************************************************************************** 
  /** Used to extract parameters */
    
    private Element[] getElementType(String ElementName){
        //definitions->types->schema->element (name attribute)
        
        Element[] arrElements = new Element[0];
        int ElementCount = -1;
        
      //Loop Through Schemas and Find Elements
        NodeList Schemas = getSchema();

                for (int k=0; k<Schemas.getLength(); k++ ) {

                  //Loop Through Elements
                    if (contains(Schemas.item(k).getNodeName(), "element")) {


                        //Compare method name to the the one we're searching for
                        NamedNodeMap attrElement = Schemas.item(k).getAttributes();
                        String elementName = getAttributeValue(attrElement, "name");
                        String elementType = getAttributeValue(attrElement, "type");
                        if (elementName.equals(ElementName)) {


                            if (!DOM.hasChildren(Schemas.item(k))){ //Complex Type!
                                String s = decomposeComplexType(stripNameSpace(elementType),null);
                                Document doc = DOM.createDocument("<parameters>" + s + "</parameters>");
                                NodeList Elements = doc.getElementsByTagName("parameter");
                                for (int x=0; x<Elements.getLength(); x++ ) {
                                     if (Elements.item(x).getNodeType()==1){
                                         
                                         ElementCount +=1;
                                         Element Element = new Element(Elements.item(x));
                                         arrElements = (Element[])resizeArray(arrElements,ElementCount+1);
                                         arrElements[ElementCount] = Element;
                                     }
                                }
                            }
                            else{

                                //Loop down to the second element
                                try{
                                    NodeList Elements = Schemas.item(k).getChildNodes();
                                    for (int x=0; x<Elements.getLength(); x++ ) { //Schemas?
                                        if (contains(Elements.item(x).getNodeName(), "complextype")) {
                                            NodeList ComplexType = Elements.item(x).getChildNodes();
                                            for (int y=0; y<ComplexType.getLength(); y++ ) { ////Schemas?
                                                if (contains(ComplexType.item(y).getNodeName(), "sequence")) {
                                                    NodeList Sequence = ComplexType.item(y).getChildNodes();
                                                    for (int z=0; z<Sequence.getLength(); z++ ) {
                                                        if (contains(Sequence.item(z).getNodeName(), "element")) {

                                                            ElementCount +=1;
                                                            Element Element = new Element(Sequence.item(z));
                                                            arrElements = (Element[])resizeArray(arrElements,ElementCount+1);
                                                            arrElements[ElementCount] = Element;

                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    }



                                }
                                catch(Exception e){
                                    //SSD+="<error>" + e.toString() + "</error>";
                                }

                            } //end if complex type
                            
                            
                            return arrElements;

                        }


                    }

                }
        
        return null;
    }
    
    
    
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
  //** getDefinitions
  //**************************************************************************    
    
    private NodeList getDefinitions(){
        NodeList Definitions = XMLDoc.getChildNodes();
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
  //** getShemas
  //**************************************************************************  
    
    private NodeList getSchema(){
        NodeList Types = getTypes();
        if (Types!=null){
            for (int i=0; i<Types.getLength(); i++){
                 Node node = Types.item(i);
                 if (node.getNodeType()==1){
                     if (node.getNodeName().endsWith("schema")){
                         return node.getChildNodes();
                     }
                 }
            }
        }
        return null;
    }
    


  //**************************************************************************
  //** decomposeComplexType (recursive function)
  //**************************************************************************    
    
    private String decomposeComplexType(String ElementName, NodeList Types){
        
        if (Types==null) Types=getTypes();
        NodeList Schemas = getSchema();
        for (int x=0; x<Schemas.getLength(); x++ ) {

            String nodeName = Schemas.item(x).getNodeName().toLowerCase();
            boolean isComplexType = false;
            boolean isSimpleType = false;

            if (nodeName.equals("complextype") || 
                nodeName.contains((CharSequence) ":complextype")){
                isComplexType = true;
            }

            if (nodeName.equals("simpletype") || 
                nodeName.contains((CharSequence) ":simpletype")){
                isSimpleType = true;
            }

            if (isComplexType || isSimpleType){

                NamedNodeMap attr = Schemas.item(x).getAttributes();
                String typeName = getAttributeValue(attr, "name"); 


                if (typeName.equals(ElementName)){


                    if (isComplexType){

                        NodeList ComplexType = Schemas.item(x).getChildNodes();
                        for (int y=0; y<ComplexType.getLength(); y++ ) {
                            if (ComplexType.item(y).getNodeName().endsWith("sequence")) {
                                NodeList Sequence = ComplexType.item(y).getChildNodes();
                                for (int z=0; z<Sequence.getLength(); z++ ) {
                                    if (Sequence.item(z).getNodeName().endsWith("element")) {

                                        Element Element = new Element(Sequence.item(z));
                                        String elementType = stripNameSpace(Element.Type);

                                        //Element.ComplexTypes = Elements;
                                        temp += "   <parameter " + 
                                                     "name=\"" + Element.Name + "\" " + 
                                                     "type=\"" + elementType + "\" " + 
                                                     "iscomplex=\"" + Element.IsComplex + "\" " + 
                                                     "isnillable=\"" + Element.IsNillable + "\">" + vbCrLf;

                                        if (Element.IsComplex){
                                            decomposeComplexType(elementType,Types);
                                        }

                                        temp += "   </parameter>" + vbCrLf;
                                    }
                                }
                            }
                        }

                    } //end if (isComplexType)


                    if (isSimpleType){
                        String Options = "";
                        NodeList SimpleType = Schemas.item(x).getChildNodes();
                        for (int y=0; y<SimpleType.getLength(); y++ ) {
                             nodeName = SimpleType.item(y).getNodeName();
                             if (nodeName.equals("restriction") || 
                                 nodeName.contains((CharSequence) ":restriction")){
                                 NodeList Restriction = SimpleType.item(y).getChildNodes();
                                 for (int z=0; z<Restriction.getLength(); z++ ) {
                                      nodeName = Restriction.item(z).getNodeName();
                                      if (nodeName.equals("enumeration") || 
                                          nodeName.contains((CharSequence) ":enumeration")){
                                          attr = Restriction.item(z).getAttributes();
                                          String OptionValue = getAttributeValue(attr, "value");
                                          if (OptionValue.length()>0){
                                              Options += "    <option value=\"" + OptionValue + "\">" + OptionValue + "</option>" + vbCrLf;
                                          }
                                      }
                                 }
                             }
                        }

                        if (Options.length()>0){
                            temp += "   <options>" + vbCrLf;
                            temp += Options;
                            temp += "   </options>" + vbCrLf;
                        }
                    }

                }
            }

        } //end loop     

                
        
        return temp;
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
  //** getServices -> NEW!
  //**************************************************************************
  /**  Used to retrieve an array of Services from an SSD NodeList */
    
    public Service[] getServices(){
        
        Service[] arrServices = new Service[0];
        
        NodeList Services = XMLDoc.getElementsByTagName("service"); 
        for (int i=0; i<Services.getLength(); i++){ 
             if (Services.item(i).getNodeType()==1){
                 Service Service = getService(Services.item(i));
                 
                 if (Service!=null){
                     int x = arrServices.length;
                     arrServices = (Service[])resizeArray(arrServices,x+1);
                     arrServices[x] = Service;
                 }
             }
        }
                
        if (arrServices.length == 0){
            return null;
        }
        else{
            return arrServices;
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
  //** getMethods -> NEW!
  //**************************************************************************
  /**  Used to retrieve an array of methods from an SSD NodeList */
    
    private Method[] getMethods(NodeList Methods){
        
        Method[] arrMethods = new Method[0];
        
        for (int i=0; i<Methods.getLength(); i++){ 
             if (Methods.item(i).getNodeType()==1){
                 Method method = getMethod(Methods.item(i));
                 
                 if (method!=null){
                     int x = arrMethods.length;
                     arrMethods = (Method[])resizeArray(arrMethods,x+1);
                     arrMethods[x] = method;
                 }
             }
        }
                
        if (arrMethods.length == 0){
            return null;
        }
        else{
            return arrMethods;
        }
    }
    
    
  //**************************************************************************
  //** getMethod -> NEW!
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
  //** getParameters -> NEW!
  //**************************************************************************
  /**  Used to retrieve an array of parameters from an SSD NodeList */
    
    private Parameter[] getParameters(NodeList Parameters){
        
        Parameter[] arrParameters = new Parameter[0];
       
        for (int i=0; i<Parameters.getLength(); i++){ 
             if (Parameters.item(i).getNodeType()==1){
                 Parameter Parameter = getParameter(Parameters.item(i));
                 

                 
                 int numParams = arrParameters.length;
                 arrParameters = (Parameter[])resizeArray(arrParameters,numParams+1);
                 arrParameters[numParams] = Parameter;
                 
               //Need to verify recursion logic here!
                 if (Parameter.isComplex()){
                     Parameter.Children = getParameters(Parameter.ChildNodes);
                 }
                 
             }
        }
        
        if (arrParameters.length == 0){
            return null;
        }
        else{
            return arrParameters;
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
        //Parameter.IsComplex = bool(getAttributeValue(attr, "iscomplex"));
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
    
    private Option[] getOptions(NodeList Options){
        
        Option[] arrOptions = new Option[0];
       
        for (int i=0; i<Options.getLength(); i++){ 
             if (Options.item(i).getNodeType()==1){
                 String NodeName = Options.item(i).getNodeName();
                 
                 if (NodeName.equalsIgnoreCase("options")){
                     NodeList ChildNodes = Options.item(i).getChildNodes();
                     for (int j=0; j<ChildNodes.getLength(); j++){ 
                          if (ChildNodes.item(j).getNodeType()==1){
                         
                             Option Option = getOption(ChildNodes.item(j));

                             int numOptions = arrOptions.length;
                             arrOptions = (Option[])resizeArray(arrOptions,numOptions+1);
                             arrOptions[numOptions] = Option;
                          }
                     }
                 
                 }
                 
             }
        }
        
        if (arrOptions.length == 0){
            return null;
        }
        else{            
            return arrOptions;
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
  //** getNameSpace
  //**************************************************************************
    
    private String getNameSpace(String str){
        return left(str, instr(str, ":")-1);
    }      
    
  //**************************************************************************
  //** stripNameSpace
  //**************************************************************************
    
    private String stripNameSpace(String str){
        return right(str, len(str) - instr(str, ":"));
    }   

    
    
  //**************************************************************************
  //** resizeArray
  //**************************************************************************
    
    private static Object resizeArray(Object oldArray, int newSize) {
       int oldSize = java.lang.reflect.Array.getLength(oldArray);
       Class elementType = oldArray.getClass().getComponentType();
       Object newArray = java.lang.reflect.Array.newInstance(
             elementType,newSize);
       int preserveLength = Math.min(oldSize,newSize);
       if (preserveLength > 0)
          System.arraycopy (oldArray,0,newArray,0,preserveLength);
       return newArray; 
    }    
    
    
  //**************************************************************************
  //** Legacy VB String Functions
  //**************************************************************************
    
    private int instr(String str, String ch){ return string.instr(str,ch); }
    private boolean contains(String str, String ch){ return string.contains(str,ch,true); }
    private int len(String str){ return string.len(str); }
    private String left(String str, int n){ return string.left(str,n); }
    private String right(String str, int n){ return string.right(str,n); }  
    
}

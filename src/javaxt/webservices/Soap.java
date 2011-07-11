package javaxt.webservices;
import java.util.HashMap;
import java.util.HashSet;
import org.w3c.dom.*;
import javaxt.xml.DOM;

//******************************************************************************
//**  SOAP Request
//*****************************************************************************/
/**
 *   Used to dynamically bind to web services. Example:
 *
 <pre>
String url = "http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL";
javaxt.webservices.WSDL wsdl = new javaxt.webservices.WSDL(url);
javaxt.webservices.Service service = wsdl.getServices()[0];
javaxt.webservices.Method method = service.getMethod("CountryName");
javaxt.webservices.Parameters parameters = method.getParameters();
parameters.setValue("sCountryISOCode", "US");

javaxt.webservices.Soap soap = new javaxt.webservices.Soap(wsdl);
try{
    String countryName = soap.execute(service, method, parameters).getText();
    System.out.println(countryName);
}
catch(Exception e){
    e.printStackTrace();
}
 </pre>
 *
 ******************************************************************************/

public class Soap {


    private Object wsdl;
    private String HttpProxy;
    private boolean useCache = false;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of Request. */

    public Soap(java.io.File wsdl) {
        this.wsdl = wsdl;
    }

    public Soap(java.net.URL wsdl) {
        this.wsdl = wsdl;
    }

    public Soap(javaxt.webservices.WSDL wsdl) {
        this.wsdl = wsdl;
    }


  //**************************************************************************
  //** Set Proxy
  //**************************************************************************

    public void setProxy(String proxyServer){
        this.HttpProxy = proxyServer;
    }


  //**************************************************************************
  //** getWSDL
  //**************************************************************************
  /**  Used to parse 
   */
    private javaxt.webservices.WSDL getWSDL(){
        if (wsdl instanceof java.net.URL){
            javaxt.http.Request request = new javaxt.http.Request((java.net.URL) wsdl);
            if (HttpProxy!=null) request.setProxy(HttpProxy);
            org.w3c.dom.Document xml = request.getResponse().getXML();
            wsdl = (javaxt.webservices.WSDL) new javaxt.webservices.WSDL(xml);
        }
        else if (wsdl instanceof java.io.File){
            org.w3c.dom.Document xml = new javaxt.io.File((java.io.File) wsdl).getXML();
            wsdl = (javaxt.webservices.WSDL) new javaxt.webservices.WSDL(xml);
        }
        return (javaxt.webservices.WSDL) wsdl;
    }




    private HashMap<String, HashSet<String>> RequestProperties = new HashMap<String, HashSet<String>>();


  //**************************************************************************
  //** setHeader
  //**************************************************************************
  /**  Used to set an http header in the http request.
   */
    public void setHeader(String key, String value){

        boolean foundProperty = false;
        java.util.Iterator<String> it = RequestProperties.keySet().iterator();
        while (it.hasNext()){
            String currKey = it.next();
            if (key.equalsIgnoreCase(currKey)){
                foundProperty = true;                
                HashSet<String> values = new HashSet<String>();
                values.add(value);
                RequestProperties.put(currKey, values);
                break;
            }
        }

        if (!foundProperty){
            HashSet<String> values = new HashSet<String>();
            values.add(value);
            RequestProperties.put(key, values);
        }
    }


  //**************************************************************************
  //** addHeader
  //**************************************************************************
  /**  Used to add an http header to the http request.
   */
    public void addHeader(String key, String value){
        if (key.equalsIgnoreCase("If-None-Match") || key.equalsIgnoreCase("If-Modified-Since") && value!=null){
            useCache = true;
        }

        boolean foundProperty = false;
        java.util.Iterator<String> it = RequestProperties.keySet().iterator();
        while (it.hasNext()){
            String currKey = it.next();
            if (key.equalsIgnoreCase(currKey)){
                foundProperty = true;
                HashSet<String> values = RequestProperties.get(currKey);
                if (values==null) values = new HashSet<String>();
                values.add(value);
                RequestProperties.put(currKey, values);
                break;
            }
        }

        if (!foundProperty){
            HashSet<String> values = new HashSet<String>();
            values.add(value);
            RequestProperties.put(key, values);
        }
    }



  //**************************************************************************
  //** Execute
  //**************************************************************************

    public SoapResponse execute(Service service, Method method, Parameters parameters){
        SoapRequest request = new SoapRequest(service, method, parameters);
        return request.getResponse(HttpProxy);
    }

  //**************************************************************************
  //** Execute
  //**************************************************************************

    public SoapResponse execute(String method, String[] parameters){

      //Update the method and service variables used to instantiate the SoapRequest
        javaxt.webservices.WSDL wsdl = getWSDL();
        Service service = wsdl.getService(0);
        Method Method = wsdl.getMethod(method);

        SoapRequest request = new SoapRequest(service, Method, parameters);
        return request.getResponse(HttpProxy);

    }





  //**************************************************************************
  //** SOAP Request
  //**************************************************************************

    private class SoapRequest{

        private javaxt.http.Request request;
        private String resultsNode;
        private String body;

        private SoapRequest(Service service, Method method, Object parameters){


            this.resultsNode = method.getResultsNodeName();



          //Create new http request
            request = new javaxt.http.Request(service.getURL());

            setHeader("Content-Type", "text/xml; charset=utf-8");
            setHeader("Accept", "text/html, text/xml, text/plain");


            java.util.Iterator<String> it = RequestProperties.keySet().iterator();
            while (it.hasNext()){
                String key = it.next();
                HashSet<String> values = RequestProperties.get(key);
                if (values!=null){
                    if (values.size()==1){
                        request.setHeader(key, values.iterator().next());
                    }
                    else{
                        java.util.Iterator<String> value = values.iterator();
                        while (value.hasNext()){
                            request.addHeader(key, value.next());
                        }
                    }
                }
            }




          //Add SoapAction to the header
            String action = method.getSoapAction();
            if (action.length()>0) request.addHeader("SOAPAction", action);


          //Create body
            StringBuffer body = new StringBuffer();
            body.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                  "<soap:Envelope " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                  "<soap:Body>");


          //Add method tag
            body.append("<" + method.getName() + " xmlns=\"" + service.getNameSpace() + "\">");


          //Insert parameters (inside the method node)
            if (parameters instanceof Parameters){
                body.append(parameters.toString());
            }
            else if (parameters instanceof String){
                body.append(parameters);
            }
            else if (parameters instanceof String[]){
                String[] values = (String[]) parameters;
                Parameter[] params = method.getParameters().getArray();
                if (params!=null){
                    for (int i=0; i<params.length; i++ ) {
                         String parameterName = params[i].getName();
                         String parameterValue = values[i];
                         body.append("<" + parameterName + "><![CDATA[" + parameterValue + "]]></" + parameterName + ">");
                    }
                }
            }

          //Close tags
            body.append("</" + method.getName() + ">");
            body.append("</soap:Body></soap:Envelope>");

            this.body = body.toString();

        }

        
        public SoapResponse getResponse(String proxyServer){
            if (proxyServer!=null) request.setProxy(proxyServer);
            //System.out.println(body);
            request.write(body);
            return new SoapResponse(request, resultsNode);
        }
    


    }


  //**************************************************************************
  //** SOAP Response
  //**************************************************************************

    public class SoapResponse{

        javaxt.http.Response response;
        private String resultsNode;

        public SoapResponse(javaxt.http.Request request, String resultsNode){
            this.response = request.getResponse();
            this.resultsNode = resultsNode;
        }

        public String getText() throws Exception {


            if (response.getStatus()==200 || response.getStatus()==202 || response.getStatus()==203){
                String ServiceResponse = response.getText("UTF-8");
                if (ServiceResponse!=null){

                    //Extract Response
                    if (ServiceResponse.substring(0,1).equals("<")){
                        try{
                            Document XMLDoc = DOM.createDocument(ServiceResponse);
                            NodeList Response = XMLDoc.getElementsByTagName(resultsNode);
                            if (Response!=null){

                              //Special Case: Probably Missing Namespace in Soap.resultsNode
                                if (Response.getLength()==0) {
                                    resultsNode = getResultsNode(ServiceResponse, resultsNode);
                                    Response = XMLDoc.getElementsByTagName(resultsNode);
                                }

                                return DOM.getNodeValue(Response.item(0));
                            }
                            else{
                                throw new Exception(
                                        "Failed to parse SOAP Response. " +
                                        "Could not find the " + resultsNode + " node. " +
                                        "Possibly due to a service exception:" +
                                        "\r\n" + ServiceResponse);
                            }
                        }
                        catch(Exception e){
                            e.printStackTrace();
                            throw new Exception("Failed to parse SOAP Response: \r\n" + ServiceResponse);
                        }
                    }
                    else{
                        throw new Exception("Invalid SOAP Response. Response does not appear to be xml:\r\n" + ServiceResponse);
                    }
                }
                else{
                    throw new Exception("Invalid SOAP Response.");
                }

               
            }
            else{
                throw new Exception("Invalid SOAP Response: " + response.getMessage() + " (" + response.getStatus() + ")");
            }


        }

        private String getResultsNode(String ServiceResponse, String resultsNode){
            resultsNode = ServiceResponse.substring(0,
                          ServiceResponse.toLowerCase().indexOf(resultsNode.toLowerCase()) + resultsNode.length());

            resultsNode = resultsNode.substring(resultsNode.lastIndexOf("<")+1);
            return resultsNode;
        }

        
    }


}
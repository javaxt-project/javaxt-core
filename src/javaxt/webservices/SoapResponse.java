package javaxt.webservices;
import org.w3c.dom.*;
import javaxt.xml.DOM;

//******************************************************************************
//**  SoapResponse Class
//******************************************************************************
/**
 *   Used to process SOAP responses
 *
 ******************************************************************************/

public class SoapResponse {

    //private javaxt.http.Response response;
    private String responseBody;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of SoapResponse. */

    protected SoapResponse(javaxt.http.Response response, String resultsNode) throws SoapException {


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

                            responseBody = DOM.getNodeValue(Response.item(0));
                        }
                        else{
                            throw new SoapException(
                                    "Failed to parse SOAP Response. " +
                                    "Could not find the " + resultsNode + " node, " +
                                    "possibly due to a service exception.", ServiceResponse);
                        }
                    }
                    catch(Exception e){
                        throw new SoapException("Failed to parse SOAP Response. " + e.getLocalizedMessage(), ServiceResponse);
                    }
                }
                else{
                    throw new SoapException("Invalid SOAP Response. Response does not appear to be xml.", ServiceResponse);
                }
            }
            else{
                throw new SoapException("Invalid SOAP Response.", ServiceResponse);
            }

        }
        else{
            throw new SoapException(response.getMessage() + " (" + response.getStatus() + ")", response.getErrorMessage());
        }


    }



    private String getResultsNode(String ServiceResponse, String resultsNode){
        resultsNode = ServiceResponse.substring(0,
                      ServiceResponse.toLowerCase().indexOf(resultsNode.toLowerCase()) + resultsNode.length());

        resultsNode = resultsNode.substring(resultsNode.lastIndexOf("<")+1);
        return resultsNode;
    }


    public String toString(){
        return responseBody;
    }

}
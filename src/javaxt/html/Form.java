package javaxt.html;
import java.util.*;

public class Form {

    private String name;
    private String method = "post";
    private String action = "";
    private List inputs = new LinkedList();

    

    public Form(String method, String action){
        this.method = method;
        this.action = action;
    }

    public void setName(String name){
        this.name = name;
    }


    public void addInput(String name, String value){
        inputs.add(new Input(name,value));
    }
        
    public void addInput(Input input){
        inputs.add(input);
    }
    
    public void setAction(String action){
        this.action = action;
    }

    public javaxt.http.Response submit(){

      //Instantiate HTTP request
        javaxt.http.Request request = new javaxt.http.Request(action);
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");

      //Convert the form inputs into a request body
        StringBuffer payload = new StringBuffer();
        for (int i=0; i<inputs.size(); i++){
            payload.append(inputs.get(i).toString());
            if (i<inputs.size()-1) payload.append("&");
        }

      //Send the http request
        request.write(payload.toString());

      //Return the response
        return request.getResponse();
        
    }
    
}

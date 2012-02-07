package javaxt.utils;
import java.util.HashMap;
import java.util.List;

//******************************************************************************
//**  URL Class - By Peter Borissow
//******************************************************************************
/**
 *   Used to parse urls, extract querystring parameters, etc. Partial 
 *   implementation of the java.net.URL class. Provides a querystring parser 
 *   that is not part of the java.net.URL class. Can be used to parse non-http
 *   URLs, including JDBC connection strings.
 *
 ******************************************************************************/

public class URL {
    
    private HashMap<String, List<String>> parameters;
    private String protocol;
    private String host;
    private Integer port;
    private String path;

    
  //**************************************************************************
  //** Overloaded Constructor
  //**************************************************************************
  /** Creates a new instance of URL using a java.net.URL */
    
    public URL(java.net.URL url){
        this(url.toString());
    }
    
    
  //**************************************************************************
  //** Constructor
  //************************************************************************** 
  /** Creates a new instance of URL using string representing a url. */  
    
    public URL(String url){

        url = url.trim();
        parameters = new HashMap<String, List<String>>();


        if (url.contains("://")){
            protocol = url.substring(0,url.indexOf("://"));
            url = url.substring(url.indexOf("://") + 3);
        }
        else{
            if (url.startsWith("jdbc")){
                protocol = url.substring(0, url.indexOf(";"));
                url = url.substring(url.indexOf(";")+1);
            }
        }


        if (url.contains(";")){
            url = url.substring(0, url.indexOf(";"));
        }


        if (url.contains("?")){
            String query = url.substring(url.indexOf("?")+1);
            url = url.substring(0, url.indexOf("?"));
            parameters = parseQueryString(query);
        }

        if (url.contains("/")){
            path = url.substring(url.indexOf("/"));
            url = url.substring(0, url.indexOf("/"));
        }

        if (url.contains(":")){
            try{
                port = Integer.valueOf(url.substring(url.indexOf(":")+1));
                url = url.substring(0, url.indexOf(":"));
            }
            catch(Exception e){}
        }

        host = url;
    }


  //**************************************************************************
  //** exists
  //**************************************************************************
  /** Used to test whether the url endpoint exists. Currently only supports 
   *  HTTP URLs.
   */
    public boolean exists(){
        try{
            java.net.URLConnection conn = new java.net.URL(this.toString()).openConnection();
            conn.setConnectTimeout(5000);
            conn.getInputStream();
            return true;
        }
        catch(Exception e){
            //System.err.println(e.toString());
            //System.err.println("URL not found: " + this.toString());
        }
        return false;

    }

    
  //**************************************************************************
  //** parseQueryString
  //************************************************************************** 
  /** Used to parse a url query string and create a list of name/value pairs.
   *  Note that the keys are all lowercase.
   */      
    public static HashMap<String, List<String>> parseQueryString(String query){


      //Create an empty hashmap
        HashMap<String, List<String>> parameters = new HashMap<String, List<String>>();
        if (query==null) return parameters;

      //Decode the querystring
        try{
            query = java.net.URLDecoder.decode(query, "UTF-8");
        }
        catch(Exception e){
          //Try to decode the string manually
            String find[] = new String[]{"%2C","%2F","%3A"};
            String replace[] = new String[]{",","/",":"};
            for (int i=0; i<find.length; i++){
                 query = query.replace(find[i],replace[i]);
            }
        }


      //Parse the querystring, one character at a time. Note that the tokenizer
      //implemented here is very inefficient. Need something better/faster.
        if (query.startsWith("&")) query = query.substring(1);
        query += "&";


        StringBuffer word = new StringBuffer();
        String c = "";

        for (int i=0; i<query.length(); i++){

             c = query.substring(i,i+1); 

             if (!c.equals("&")){
                 word.append(c); //word = word + c;
             }
             else{     
                 //System.out.println(word);

                 int x = word.indexOf("=");
                 if (x>=0){
                     String key = word.substring(0,x).toLowerCase();
                     String value = word.substring(x+1);
                     
                     List<String> values = parameters.get(key);
                     if (values==null) values = new java.util.LinkedList<String>();
                     values.add(value);
                     parameters.put(key, values);
                 }
                 else{
                     parameters.put(word.toString(), null);
                 }

                 word = new StringBuffer(); //word = "";
             }
        }

        return parameters;
    }


  //**************************************************************************
  //** setParameter
  //**************************************************************************
  /** Used to set or update a value for a given parameter. If append is true,
   *  the value will be added to other values for this key.
   */
    public void setParameter(String key, String value, boolean append){

        key = key.toLowerCase();
        if (append){
            List<String> values = parameters.get(key);
            java.util.Iterator<String> it = values.iterator();
            while(it.hasNext()){
                if (it.next().equalsIgnoreCase(value)){
                    append = false;
                    break;
                }
            }
            if (append) {
                values.add(value);
                parameters.put(key, values);
            }

        }
        else{
            if (value!=null){
                List<String> values = new java.util.LinkedList<String>();
                values.add(value);
                parameters.put(key, values);
            }
        }

    }


  //**************************************************************************
  //** setParameter
  //**************************************************************************
  /** Used to set or update a value for a given parameter. 
   */
    public void setParameter(String key, String value){
        setParameter(key, value, false);
    }


  //**************************************************************************
  //** getParameter
  //************************************************************************** 
  /** Used to retrieve the value of a specific variable supplied in the query 
   *  string.
   *
   *  @param key Query string parameter name. Performs a case insensitive
   *   search for the keyword.
   *
   *  @return Returns a comma delimited list of values associated with the
   *  given key. Returns a zero length string if the key is not found or if
   *  the value is null.
   */ 
    public String getParameter(String key){
        StringBuffer str = new StringBuffer();
        List<String> values = parameters.get(key.toLowerCase());
        if (values!=null){
            for (int i=0; i<values.size(); i++){
                str.append(values.get(i));
                if (i<values.size()-1) str.append(",");
            }
            return str.toString();
        }
        else{
            return "";
        }
    }

  //**************************************************************************
  //** getParameter
  //**************************************************************************
  /**  Used to retrieve the value of a specific variable supplied in the query
   *   string using multiple keywords.
   */
    public String getParameter(String[] keys){

        StringBuffer str = new StringBuffer();
        for (String key : keys){
            List<String> values = parameters.get(key.toLowerCase());
            if (values!=null){
                for (int i=0; i<values.size(); i++){
                    str.append(values.get(i) + ",");
                }
            }
        }

        String value = str.toString();
        if (value.endsWith(",")) value = value.substring(0, value.length()-1);
        return value;
    }


  //**************************************************************************
  //** getParameters
  //**************************************************************************
  /**  Used to retrieve the query string parameters
   */
    public HashMap<String, List<String>> getParameters(){
        return parameters;
    }


  //**************************************************************************
  //** removeParameter
  //**************************************************************************
  /**  Used to remove a parameter from the query string
   */
    public String removeParameter(String key){
        StringBuffer str = new StringBuffer();
        List<String> values = parameters.remove(key.toLowerCase());
        if (values!=null){
            for (int i=0; i<values.size(); i++){
                str.append(values.get(i));
                if (i<values.size()-1) str.append(",");
            }
            return str.toString();
        }
        else{
            return "";
        }
    }


  //**************************************************************************
  //** getHost
  //**************************************************************************
  /**  Used to return the hostname found in the URL
   */
    public String getHost(){
        return host;
    }

  //**************************************************************************
  //** getPort
  //**************************************************************************
  /**  Used to return the port found in the URL
   */
    public Integer getPort(){
        return port;
    }


  //**************************************************************************
  //** getProtocol
  //**************************************************************************
  /**  Used to return the protocol found in the URL
   */
    public String getProtocol(){
        return protocol;
    }


  //**************************************************************************
  //** getQueryString
  //**************************************************************************
  /**  Used to return the querystring in the URL
   */
    public String getQueryString(){

        StringBuffer str = new StringBuffer();
        java.util.HashSet<String> keys = getKeys();
        java.util.Iterator<String> it = keys.iterator();
        while (it.hasNext()){
            String key = it.next();
            String value = this.getParameter(key);
            if (value.length()==0){
                if (parameters.get(key.toLowerCase())==null){
                    value = null;
                }
            }
            str.append(key);
            if (value!=null){
                str.append("=" + value); 
            }
            if (it.hasNext()) str.append("&");
        }
        return str.toString();

    }


  //**************************************************************************
  //** setQueryString
  //**************************************************************************
  /**  Used to update the querystring in the URL
   */
    public void setQueryString(String query){
        if (query==null){
            parameters = new HashMap<String, List<String>>();
        }
        else{
            query = query.trim();
            if (query.startsWith("?")) query = query.substring(1).trim();
            if (query.length()>0){
                parameters = parseQueryString(query);
            }
        }
    }


  //**************************************************************************
  //** getKeys
  //**************************************************************************
  /**  Used to return a list of parameters/keys found in the querystring
   */
    public java.util.HashSet<String> getKeys(){
        java.util.HashSet<String> keys = new java.util.HashSet<String>();
        java.util.Iterator<String> it = parameters.keySet().iterator();
        while(it.hasNext()){
            keys.add(it.next());
        }
        return keys;
    }


  //**************************************************************************
  //** getPath
  //**************************************************************************
  /**  Used to return the path portion of the URL
   */
    public String getPath(){
        return path;
    }


  //**************************************************************************
  //** setPath
  //**************************************************************************
  /**  Used to update the path portion of the URL
   */
    public void setPath(String path){
        if (path!=null){
            path = path.trim();
            if (!path.startsWith("/")) path = "/" + path;
        }
        this.path = path;
    }


  //**************************************************************************
  //** toString
  //**************************************************************************
  /**  Returns the URL as a string. 
   */
    public String toString(){
            
      //Update Host
        String host = this.host;

        if (port!=null && port>0) host += ":" + port;

      //Update Path
        String path = "";
        if (getPath()!=null) path = getPath();

      //Update Query String
        String query = getQueryString();
        if (query.length()>0) query = "?" + query;

      //Assemble URL
        return protocol + "://" + host + path + query;
    }


  //**************************************************************************
  //** toURL
  //**************************************************************************
  /**  Returns a properly encoded URL for HTTP requests
   */
    public java.net.URL toURL(){
        java.net.URL url = null;
        try{

            Integer port = this.port;
            if (port==null){
                if (protocol.equalsIgnoreCase("http")) port = 80;
                else if (protocol.equalsIgnoreCase("https")) port = 443;
                else if (protocol.equalsIgnoreCase("ftp")) port = 23;
                else{
                    try{
                        port = new java.net.URL(protocol + "://" + host).getPort();
                    }
                    catch(Exception e){}
                }
            }
            url = new java.net.URI(protocol, null, host, port, path, null, null).toURL();


          //Encode and append QueryString as needed
            java.util.HashSet<String> keys = getKeys();
            java.util.Iterator<String> it = keys.iterator();
            if (it.hasNext()){
                StringBuffer str = new StringBuffer();
                while (it.hasNext()){
                    String key = it.next();
                    String value = this.getParameter(key);
                    if (value.length()==0){
                        if (parameters.get(key.toLowerCase())==null){
                            value = null;
                        }
                    }
                    str.append(java.net.URLEncoder.encode(key, "UTF-8"));
                    if (value!=null){
                        str.append("=" + java.net.URLEncoder.encode(value, "UTF-8"));
                    }
                    if (it.hasNext()) str.append("&");
                }
                url = new java.net.URL(url.toString() + "?" + str.toString());
            }
            
        }
        catch(Exception e){
            //e.printStackTrace();
        }
        return url;

    }
}
package javaxt.utils;
import java.util.*;

//******************************************************************************
//**  URL Class
//******************************************************************************
/**
 *   Used to parse urls, extract querystring parameters, etc. Partial
 *   implementation of the java.net.URL class. Provides a querystring parser
 *   that is not part of the java.net.URL class. Can be used to parse non-http
 *   URLs, including JDBC connection strings.
 *
 ******************************************************************************/

public class URL {

    private LinkedHashMap<String, List<String>> parameters;
    private LinkedHashMap<String, List<String>> extendedParameters;
    private String protocol;
    private String host;
    private Integer port;
    private String path;


  //**************************************************************************
  //** Constructor
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
        parameters = new LinkedHashMap<>();
        extendedParameters = new LinkedHashMap<>();


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



        if (url.contains("?")){
            String query = url.substring(url.indexOf("?")+1);
            url = url.substring(0, url.indexOf("?"));
            parameters = parseQueryString(query);
        }
        else{ //no query string, check for jdbc params

            int idx = url.indexOf(";");
            if (idx>-1){ //found jdbc delimiter
                extendedParameters = parseJDBCParams(url.substring(idx+1));
                url = url.substring(0, idx);
            }
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
   */
    public static LinkedHashMap<String, List<String>> parseQueryString(String query){


      //Create an empty hashmap
        LinkedHashMap<String, List<String>> parameters = new LinkedHashMap<>();
        if (query==null) return parameters;

        query = query.trim();
        if (query.length()==0) return parameters;


      //Parse the querystring, one character at a time
        if (query.startsWith("&")) query = query.substring(1);
        query += "&";


        StringBuffer word = new StringBuffer();
        for (int i=0; i<query.length(); i++){

            String c = query.substring(i,i+1);
            if (c.equals("&")){

                if (i+5<query.length() && query.substring(i,i+5).equals("&amp;")){
                    word.append(c);
                }
                else{
                    int x = word.indexOf("=");
                    if (x>=0){
                        String key = word.substring(0,x);
                        String value = decode(word.substring(x+1));

                        List<String> values = getParameter(key, parameters);
                        if (values==null) values = new LinkedList<>();
                        values.add(value);
                        setParameter(key, values, parameters);
                    }
                    else{
                        setParameter(word.toString(), null, parameters);
                    }

                    word = new StringBuffer();
                }
            }
            else{
                word.append(c);
            }
        }

        return parameters;
    }


  //**************************************************************************
  //** parseJDBCParams
  //**************************************************************************
  /** Used to parse a JDBC parameter strings and return a list of name/value
   *  pairs.
   */
    public static LinkedHashMap<String, List<String>> parseJDBCParams(String params){
        LinkedHashMap<String, List<String>> parameters = new LinkedHashMap<>();
        final String[] pairs = params.split(";");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = idx > 0 ? decode(pair.substring(0, idx)) : pair;
            String value = idx > 0 && pair.length() > idx + 1 ? decode(pair.substring(idx + 1)) : null;

            if (!parameters.containsKey(key)) parameters.put(key, new LinkedList<>());
            parameters.get(key).add(value);
        }
        return parameters;
    }


  //**************************************************************************
  //** decode
  //**************************************************************************
  /** Used to decode a URL encoded string
   */
    public static String decode(String str){
        try{

          //Replace unencoded "%" characters with "%25". The regex finds a "%"
          //but only those that are NOT followed by 2 hex characters (0-F),
          //then replaces with the ENCODED version of the % character "%25".
          //Credit: https://stackoverflow.com/a/18368345
            str = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25");


          //Decode the string with the URLDecoder
            if (str.contains("+")){
                StringBuilder out = new StringBuilder();
                while (str.contains("+")){
                    int idx = str.indexOf("+");
                    if (idx==0){
                        out.append("+");
                        str = str.substring(1);
                    }
                    else{
                        out.append(java.net.URLDecoder.decode(str.substring(0, idx), "UTF-8"));
                        str = str.substring(idx);
                    }
                }
                out.append(java.net.URLDecoder.decode(str, "UTF-8"));
                return out.toString();
            }
            else{
                return java.net.URLDecoder.decode(str, "UTF-8");
            }
        }
        catch(Exception e){
            return str;
        }
    }


  //**************************************************************************
  //** encode
  //**************************************************************************
  /** Used to URL encode a string
   */
    public static String encode(String str){
        try{
            if (str.contains(" ")){
                StringBuilder out = new StringBuilder();
                while (str.contains(" ")){
                    int idx = str.indexOf(" ");
                    if (idx==0){
                        out.append("%20");
                        str = str.substring(1);
                    }
                    else{
                        out.append(java.net.URLEncoder.encode(str.substring(0, idx), "UTF-8"));
                        str = str.substring(idx);
                    }
                }
                out.append(java.net.URLEncoder.encode(str, "UTF-8"));
                return out.toString();
            }
            else{
                return java.net.URLEncoder.encode(str, "UTF-8"); //.replaceAll("\\+", "%2B");
            }
        }
        catch(Exception e){
            return str;
        }
    }


  //**************************************************************************
  //** setParameter
  //**************************************************************************
  /** Used to set or update a value for a given parameter in the query string.
   *  If append is true, the value will be added to other values for this key.
   */
    public void setParameter(String key, String value, boolean append){

        if (value!=null) value = decode(value);

        key = key.toLowerCase();
        if (append){
            List<String> values = getParameter(key, parameters);
            Iterator<String> it = values.iterator();
            while(it.hasNext()){
                if (it.next().equalsIgnoreCase(value)){
                    append = false;
                    break;
                }
            }
            if (append) {
                values.add(value);
                setParameter(key, values, parameters);
            }

        }
        else{
            if (value!=null){
                List<String> values = new LinkedList<>();
                values.add(value);
                setParameter(key, values, parameters);
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
  /** Returns the value of a specific variable supplied in the query string.
   *
   *  @param key Query string parameter name. Performs a case insensitive
   *   search for the keyword.
   *
   *  @return Returns a comma delimited list of values associated with the
   *  given key. Returns a zero length string if the key is not found or if
   *  the value is null.
   */
    public String getParameter(String key){
        StringBuilder str = new StringBuilder();
        List<String> values = getParameter(key, parameters);
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
  /** Returns the value of a specific variable supplied in the query string.
   *  @param keys An array containing multiple possible parameter names.
   *  Performs a case insensitive search for each parameter name and returns
   *  the value for the first match.
   *
   *  @return Returns a comma delimited list of values associated with the
   *  given keys. Returns a zero length string if the key is not found or if
   *  the value is null.
   */
    public String getParameter(String[] keys){

        StringBuilder str = new StringBuilder();
        for (String key : keys){
            List<String> values = getParameter(key.toLowerCase(), parameters);
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
  /** Returns a list of parameters found in query string.
   */
    public LinkedHashMap<String, List<String>> getParameters(){
        return parameters;
    }


  //**************************************************************************
  //** getExtendedParameters
  //**************************************************************************
  /** Returns a list of extended parameters (e.g. jdbc params) that are not
   *  part of a standard url
   */
    public LinkedHashMap<String, List<String>> getExtendedParameters(){
        return extendedParameters;
    }


  //**************************************************************************
  //** removeParameter
  //**************************************************************************
  /** Used to remove a parameter from the query string
   */
    public String removeParameter(String key){
        StringBuilder str = new StringBuilder();
        List<String> values = removeParameter(key, parameters);
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


    public static List<String> getParameter(String key, HashMap<String, List<String>> parameters){
        List<String> values = parameters.get(key);
        if (values==null){
            Iterator<String> it = parameters.keySet().iterator();
            while (it.hasNext()){
                String s = it.next();
                if (s.equalsIgnoreCase(key)) return parameters.get(s);
            }
        }
        return values;
    };

    public static void setParameter(String key, List<String> values, HashMap<String, List<String>> parameters){
        Iterator<String> it = parameters.keySet().iterator();
        while (it.hasNext()){
            String s = it.next();
            if (s.equalsIgnoreCase(key)){
                parameters.put(key, values);
                return;
            }
        }
        parameters.put(key, values);
    }

    public static List<String> removeParameter(String key, HashMap<String, List<String>> parameters){
        List<String> values = parameters.remove(key);
        if (values==null){
            Iterator<String> it = parameters.keySet().iterator();
            while (it.hasNext()){
                String s = it.next();
                if (s.equalsIgnoreCase(key)) return parameters.remove(s);
            }
        }
        return values;
    };


  //**************************************************************************
  //** getHost
  //**************************************************************************
  /** Returns the host name or IP address found in the URL.
   */
    public String getHost(){
        return host;
    }


  //**************************************************************************
  //** setHost
  //**************************************************************************
  /** Used to update the host name or IP address found in the URL.
   */
    public void setHost(String host){
        if (host.contains(":")){
            port = Integer.valueOf(host.substring(host.indexOf(":")+1));
            host = host.substring(0, host.indexOf(":"));
        }
        this.host = host;
    }


  //**************************************************************************
  //** getPort
  //**************************************************************************
  /** Returns the server port found in the URL.
   */
    public Integer getPort(){
        return port;
    }


  //**************************************************************************
  //** setPort
  //**************************************************************************
  /** Used to update the port found in the URL.
   */
    public void setPort(int port){
        this.port = port;
    }


  //**************************************************************************
  //** setProtocol
  //**************************************************************************
  /** Used to update the protocol found in the URL.
   */
    public void setProtocol(String protocol){
        this.protocol = protocol;
    }


  //**************************************************************************
  //** getProtocol
  //**************************************************************************
  /** Returns the protocol found in the URL.
   */
    public String getProtocol(){
        return protocol;
    }


  //**************************************************************************
  //** getQueryString
  //**************************************************************************
  /** Returns the query string in the URL, or an empty string if none exists.
   */
    public String getQueryString(){

        StringBuilder str = new StringBuilder();
        HashSet<String> keys = getKeys();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()){
            String key = it.next();
            List<String> values = getParameter(key, parameters);


            if (values==null || values.isEmpty()){
                str.append(encode(key));
            }
            else{
                int x = 0;
                for (String value : values){
                    if (x>0) str.append("&");
                    str.append(encode(key));
                    if (value!=null){
                        if (value.trim().length()>0){
                            str.append("=");
                            boolean isEncoded = !decode(value).equals(value);
                            str.append(isEncoded ? value : encode(value));
                        }
                    }
                    x++;
                }
            }


            if (it.hasNext()) str.append("&");
        }
        return str.toString();
    }


  //**************************************************************************
  //** setQueryString
  //**************************************************************************
  /** Used to update the query string in the URL.
   */
    public void setQueryString(String query){
        if (query==null){
            parameters = new LinkedHashMap<>();
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
  /** Returns a list of parameter names found in the query string.
   */
    public HashSet<String> getKeys(){
        HashSet<String> keys = new HashSet<>();
        Iterator<String> it = parameters.keySet().iterator();
        while(it.hasNext()){
            keys.add(it.next());
        }
        return keys;
    }


  //**************************************************************************
  //** getPath
  //**************************************************************************
  /** Return the path portion of the URL, starting with a "/" character. The
   *  path does not include the query string. If no path is found, returns a
   *  null.
   */
    public String getPath(){
        return path;
    }


  //**************************************************************************
  //** setPath
  //**************************************************************************
  /** Used to update the path portion of the URL. If the supplied path starts
   *  with "./" or "../", only part of the path will be replaced. Otherwise,
   *  the entire path will be replaced.
   *  <p>
   *  When supplying a relative path (path starting with "./" or "../"), the
   *  url parser assumes that directories in the original path are terminated
   *  with a "/". For example:
   *  <pre>http://www.example.com/path/</pre>
   *  If a path is not terminated with a "/", the parser assumes that the last
   *  "/" separates a path from a file. Example:
   *  <pre>http://www.example.com/path/file.html</pre>
   *  For example, if the original url looks like this:
   *  <pre>http://www.example.com/path/</pre>
   *  If you provide a relative path like "../index.html", will yield this:
   *  <pre>http://www.example.com/index.html</pre>
   *  </p>
   *  Note that if the supplied path contains a query string,
   *  the original query string will be replaced with the new one.
   */
    public void setPath(String path){
        if (path==null){
            path = "";
        }
        else {
            path = path.trim();

            if (path.contains("?")){
                String query = path.substring(path.indexOf("?")+1);
                path = path.substring(0, path.indexOf("?"));
                parameters = parseQueryString(query);
            }

            if (path.contains(";")){ //found jdbc delimiter
                path = path.substring(0, path.indexOf(";"));
            }


            if (!path.startsWith("/")){

                if (path.startsWith("./") || path.startsWith("../")){

                    String RelPath = path;


                  //Remove "./" prefix in the RelPath
                    if (RelPath.length()>2){
                        if (RelPath.substring(0,2).equals("./")){
                            RelPath = RelPath.substring(2,RelPath.length());
                        }
                    }



                  //Build Path
                    String urlPath = "";
                    String newPath = "";
                    if (RelPath.substring(0,1).equals("/")){
                        newPath = RelPath;
                    }
                    else{

                        urlPath = "/";
                        String dir = "";
                        String orgPath = getPath();
                        if (orgPath==null) orgPath = "";
                        if (orgPath.length()>1 && !orgPath.endsWith("/")){
                            orgPath = orgPath.substring(0, orgPath.lastIndexOf("/"));
                        }
                        String[] arr = orgPath.split("/");
                        String[] arrRelPath = RelPath.split("/");
                        for (int i=0; i<=(arr.length-arrRelPath.length); i++){
                             dir = arr[i];
                             if (dir.length()>0){
                                 urlPath += dir + "/";
                             }
                        }



                      //This can be cleaned-up a bit...
                        if (RelPath.substring(0,1).equals("/")){
                            newPath = RelPath.substring(1,RelPath.length());
                        }
                        else if (RelPath.substring(0,2).equals("./")){
                            newPath = RelPath.substring(2,RelPath.length());
                        }
                        else if (RelPath.substring(0,3).equals("../")){
                            newPath = RelPath.replace("../", "");
                        }
                        else{
                            newPath = RelPath;
                        }
                    }

                    //System.out.println("urlPath: " + urlPath);
                    //System.out.println("newPath: " + newPath);

                    path = urlPath + newPath;


                }
                else{
                    path = "/" + path;
                }
            }

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
  /** Returns a properly encoded URL for HTTP requests
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
            String query = getQueryString();
            if (query.length()>0){
                url = new java.net.URL(url.toString() + "?" + query);
            }

        }
        catch(Exception e){
            //e.printStackTrace();
        }
        return url;

    }
}
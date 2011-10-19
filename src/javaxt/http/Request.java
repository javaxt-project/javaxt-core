package javaxt.http;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import javax.net.ssl.*;

//******************************************************************************
//**  Http Request
//******************************************************************************
/**
 *   Used to set up a connection to an http server. This class is used in
 *   conjunction with the HTTP Response class. Example:
 <pre>
    javaxt.http.Response response = new javaxt.http.Request(url).getResponse();
 </pre>
 *
 *   A slightly more complex example might look like this:
 <pre>
    javaxt.http.Request request = new javaxt.http.Request(url);
    request.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.10)");
    request.setHeader("Accept-Encoding", "gzip,deflate");
    java.io.InputStream inputStream = request.getResponse().getInputStream();
    new javaxt.io.File("/temp/image.jpg").write(inputStream);
    inputStream.close();
 </pre>
 *
 ******************************************************************************/

public class Request {

    protected URLConnection conn = null;
    private Proxy HttpProxy;

    private java.net.URL url;
    private boolean useCache = false;
    private int maxRedirects = 5;
    private String username;
    private String password;

    private java.util.Map<String, List<String>> requestHeaders = null;
    private HashMap<String, List<String>> RequestProperties = new HashMap<String, List<String>>();

  //Http response properties
    private java.util.Map headers = null;
    private String protocol;
    private String version;
    private int responseCode;
    private String message;


    private static TrustManager[] trustAllCerts = new TrustManager[]{
    new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(
            java.security.cert.X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(
            java.security.cert.X509Certificate[] certs, String authType) {
        }
    }
    };

    private static final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };



    private boolean validateCertificates = false;



  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** @param url URL endpoint
   */
    public Request(String url){
        this(url, null);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** @param url URL endpoint
   *  @param httpProxy Proxy server
   */
    public Request(String url, String httpProxy) {
        try{
            this.url = new java.net.URL(url);
            setProxy(httpProxy);
            initHeaders();
        }
        catch (Exception e) {
        }
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** @param url URL endpoint
   */
    public Request(java.net.URL url){
        this(url, null);
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** @param url URL endpoint
   *  @param httpProxy Proxy server
   */
    public Request(java.net.URL url, String httpProxy){
        this.url = url;
        setProxy(httpProxy);
        initHeaders();
    }


    private void initHeaders(){
        this.setUseCache(false);
        this.setHeader("Accept-Encoding", "gzip,deflate");
        this.setHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        this.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.10)");
    }




  //**************************************************************************
  //** getURL
  //**************************************************************************
  /**  Used to return the URL used to instantiate this class.
   */
    public java.net.URL getURL(){
        return url;
    }


  //**************************************************************************
  //** getResponse
  //**************************************************************************
  /**  Used to return the response from the server.
   */
    public Response getResponse(){
        if (conn==null) conn = getConnection(false);
        return new Response(this);
    }

  //**************************************************************************
  //** setUseCache
  //**************************************************************************
  /**  Sets the header associated with cache-control. If true, the protocol is
   *   allowed to use caching whenever it can. If false, the protocol must
   *   always try to get a fresh copy of the object. By default, the useCache
   *   variable is set to false.
   */
    public void setUseCache(boolean useCache){
        this.useCache = useCache;
    }


  //**************************************************************************
  //** validateSSLCertificates
  //**************************************************************************
  /** Used to enable/disable certificate validation for HTTPS Connections.
   *  Note that this is set to false by default.
   */
    public void validateSSLCertificates(boolean validateCertificates){
        this.validateCertificates = validateCertificates;
    }


  //**************************************************************************
  //** setMaxRedirects
  //**************************************************************************
  /** Sets the maximum number of redirects to follow. By default, this number
   *  is set to 5.
   */
    public void setNumRedirects(int maxRedirects){
        this.maxRedirects = maxRedirects;
    }



    public void setCredentials(String username, String password){
        this.username = username;
        this.password = password;
    }
    
    public void setUserName(String username){
        this.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }

    private String getCredentials() throws Exception {
        if (username==null || password==null) return null;
        else
            return javaxt.utils.Base64.encodeBytes(
                (username + ":" + password).getBytes("UTF-8"));
    }


  //**************************************************************************
  //** write
  //**************************************************************************
  /**  Used to open an HTTP connection to the URL and POST data to the server.
   *   @param payload InputStream containing the body of the HTTP request.
   */
    public void write(InputStream payload) {

        //this.setHeader("Connection", "close");
        if (conn==null) conn = getConnection(false);
        conn = connect(true);
        OutputStream output = null;

        try{
            output = conn.getOutputStream();
            byte[] buf = new byte[8192]; //8KB
            int i = 0;
            while((i=payload.read(buf))!=-1) {
              output.write(buf, 0, i);
            }

        }
        catch (Exception e){}
        finally {
            try { if (output != null) output.close(); }
            catch (Exception e){}
        }

        parseResponse(conn);

    }



  //**************************************************************************
  //** write
  //**************************************************************************
  /**  Used to open an HTTP connection to the URL and POST data to the server.
   *   @param payload String containing the body of the HTTP request.
   */
    public void write(String payload) {
        if (conn==null) conn = getConnection(false);
        conn = connect(true);

        try{
            conn.getOutputStream().write(payload.getBytes());            
        }
        catch(Exception e){
            //e.printStackTrace();
        }

        parseResponse(conn);
    }



  //**************************************************************************
  //** setHeader
  //**************************************************************************
  /**  Used to set a Request Property in the HTTP header (e.g. "User-Agent").
   */
    public void setHeader(String key, String value){

        boolean foundProperty = false;
        java.util.Iterator<String> it = RequestProperties.keySet().iterator();
        while (it.hasNext()){
            String currKey = it.next();
            if (key.equalsIgnoreCase(currKey)){
                foundProperty = true;
                List<String> values = new ArrayList<String>();
                values.add(value);
                RequestProperties.put(currKey, values);
                break;
            }
        }

        if (!foundProperty){
            List<String> values = new ArrayList<String>();
            values.add(value);
            RequestProperties.put(key, values);
        }
    }


  //**************************************************************************
  //** addHeader
  //**************************************************************************
  /**  Used to add a Request Property to the HTTP header (e.g. "User-Agent").
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
                List<String> values = RequestProperties.get(currKey);
                if (values==null) values = new ArrayList<String>();
                values.add(value);
                RequestProperties.put(currKey, values);
                break;
            }
        }

        if (!foundProperty){
            List<String> values = new ArrayList<String>();
            values.add(value);
            RequestProperties.put(key, values);
        }
    }


  //**************************************************************************
  //** connect
  //**************************************************************************
  /**  Used to create a URLConnection.
   */
    private URLConnection connect(boolean doOutput){
        try {


          //Disable Certificate Validation for HTTPS Connections
            if (url.getProtocol().equalsIgnoreCase("https") && validateCertificates==false){
                try {
                    //SSLContext sc = SSLContext.getInstance("SSL");
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                } catch (Exception e) {
                }
            }


          //Encode whitespaces and other illegal chars
            url = new javaxt.utils.URL(url).toURL();
            

          //Open connection
            URLConnection conn;
            if (HttpProxy==null || isLocalHost(url.getHost())){
                conn = url.openConnection();
            }
            else{
                conn = url.openConnection(HttpProxy);
            }

            if (url.getProtocol().equalsIgnoreCase("https") && validateCertificates==false){
            	HttpsURLConnection con = (HttpsURLConnection)conn;
        	con.setHostnameVerifier(DO_NOT_VERIFY);
            }



            conn.setUseCaches(useCache);
            if (doOutput) conn.setDoOutput(true);

            String credentials = getCredentials();
            if (credentials!=null) conn.setRequestProperty ("Authorization", "Basic " + credentials);


            java.util.Iterator<String> it = RequestProperties.keySet().iterator();
            while (it.hasNext()){
                String key = it.next();
                List<String> values = RequestProperties.get(key);
                if (values!=null){
                    if (values.size()==1){
                        conn.setRequestProperty(key, values.iterator().next());
                    }
                    else{
                        java.util.Iterator<String> value = values.iterator();
                        while (value.hasNext()){
                            conn.addRequestProperty(key, value.next());
                        }
                    }
                }
            }


            return conn;

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }





  //**************************************************************************
  //** getConnection
  //**************************************************************************
  /**  Used to open a connection to a url/host.
   */
    private URLConnection getConnection(boolean doOutput){


        conn = null;
        URLConnection conn = this.connect(doOutput);
        if (conn!=null){
	    requestHeaders = conn.getRequestProperties();
            parseResponse(conn);
            if ((responseCode>=300 && responseCode<400) && maxRedirects>0){
                int numRedirects = 0;
                while (responseCode>=300 && responseCode<400){

                    if (useCache && responseCode==304) break;

                    try{
                        this.url = new java.net.URL(getResponseHeader("Location"));
                        conn = this.connect(doOutput);
                        parseResponse(conn);
                        numRedirects++;
                        if (numRedirects>maxRedirects) break;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }

        return conn;
    }



  //**************************************************************************
  //** parseResponse
  //**************************************************************************
  /** Used to parse the first line from the http response */

    private void parseResponse(URLConnection conn){


        protocol = "";
        version = "";
        responseCode = -1;
        message = "";


        //requestHeaders = conn.getRequestProperties();


        headers = conn.getHeaderFields();
        if (!headers.isEmpty()){

            List status = (List)headers.get(null);
            if (status!=null){

                java.util.StringTokenizer st = new java.util.StringTokenizer( (String)(status).get(0) );
                if (st.hasMoreTokens()) protocol = st.nextToken().trim().toUpperCase();
                if (protocol.contains("/")) {
                    String temp = protocol;
                    protocol = temp.substring(0,temp.indexOf("/"));
                    version = temp.substring(temp.indexOf("/")+1);
                }
                else{
                    protocol = "HTTP";
                    version = "1.1";
                }

                if (st.hasMoreTokens()) responseCode = javaxt.utils.string.toInt(st.nextToken().trim());
                if (st.hasMoreTokens()){
                    message = "";
                    while (st.hasMoreTokens()){
                        message += st.nextToken() + " ";
                    }
                    message = message.trim();
                }
            }
        }

    }




  //**************************************************************************
  //** getExpiration
  //**************************************************************************
  /** Returns the time when the document should be considered expired.
   *  The time will be zero if the document always needs to be revalidated.
   *  It will be <code>null</code> if no expiration time is specified.
   */
    private Long getExpiration(URLConnection connection, long baseTime) {

        DateFormat PATTERN_RFC1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"); //, Locale.US

        String cacheControl = connection.getHeaderField("Cache-Control");
        if (cacheControl != null) {
            java.util.StringTokenizer tok = new java.util.StringTokenizer(cacheControl, ",");
            while(tok.hasMoreTokens()) {
                String token = tok.nextToken().trim().toLowerCase();
                if ("must-revalidate".equals(token)) {
                    return new Long(0);
                }
                else if (token.startsWith("max-age")) {
                    int eqIdx = token.indexOf('=');
                    if (eqIdx != -1) {
                        String value = token.substring(eqIdx+1).trim();
                        int seconds;
                        try {
                            seconds = Integer.parseInt(value);
                            return new Long(baseTime + seconds * 1000);
                        }
                        catch(NumberFormatException nfe) {
                            System.err.println("getExpiration(): Bad Cache-Control max-age value: " + value);
                            // ignore
                        }
                    }
                }
            }
        }

        String expires = connection.getHeaderField("Expires");
        if (expires != null) {
            try {
                synchronized(PATTERN_RFC1123) {
                    java.util.Date expDate = PATTERN_RFC1123.parse(expires);
                    return new Long(expDate.getTime());
                }
            }
            catch(java.text.ParseException pe) {
                int seconds;
                try {
                    seconds = Integer.parseInt(expires);
                    return new Long(baseTime + seconds * 1000);
                }
                catch(NumberFormatException nfe) {
                    System.err.println("getExpiration(): Bad Expires header value: " + expires);
                }
            }
        }
        return null;
    }



  //**************************************************************************
  //** getResponseCode
  //**************************************************************************
  /** Returns the HTTP status code extracted from the first line in the
   *  response.
   */
    protected int getResponseCode(){
        return responseCode;
    }


  //**************************************************************************
  //** getResponseMessage
  //**************************************************************************
  /** Returns the message extracted from the first line in the response.
   */
    protected String getResponseMessage(){
        return message;
    }


    protected java.util.Map<String, List<String>> getResponseHeaders(){
        return headers;
    }

    public java.util.Map<String, List<String>> getRequestHeaders(){
        if (requestHeaders!=null) return requestHeaders;
        else{
            return RequestProperties;
        }
    }


    protected String[] getResponseHeaders(String headerName){

      //Iterate through the headers and find the matching header
        java.util.List values = new java.util.LinkedList();
        java.util.Iterator it = headers.keySet().iterator();
        while(it.hasNext()){
            String key = (String) it.next();
            if (key!=null){
                if (key.equalsIgnoreCase(headerName)){
                    values = (java.util.List) headers.get(key);
                }
            }
        }

      //Convert the list into a string array
        String[] arr = new String[values.size()];
        for (int i=0; i<values.size(); i++){
            arr[i] = (String) values.get(i);
        }
        return arr;

    }

    protected String getResponseHeader(String headerName){
        String[] arr = getResponseHeaders(headerName);
        if (arr.length>0) return getResponseHeaders(headerName)[0];
        return null;
    }



  //**************************************************************************
  //** setProxy
  //**************************************************************************
  /** Used to set the http proxy.
   */
    public Proxy setProxy(String proxyHost, int proxyPort){
        //if (isProxyAvailable(proxyHost,proxyPort)==true) {
            SocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
            HttpProxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
            return HttpProxy;
        //}
    }


  //**************************************************************************
  //** setProxy
  //**************************************************************************
  /** Used to set the http proxy.
   */
    public Proxy setProxy(String httpProxy){
        if (httpProxy!=null)
        if (httpProxy.length()>0){
            String[] arr = httpProxy.split(":");
            String httpHost = arr[0];
            int httpPort = 80;
            if (arr.length>0){
                httpPort = Integer.valueOf(arr[1]).intValue();
            }
            return setProxy(httpHost,httpPort);
        }
         return null;
    }

  //**************************************************************************
  //** setProxy
  //**************************************************************************
  /** Used to set the http proxy as needed.
   */
    public void setProxy(Proxy httpProxy){
        HttpProxy = httpProxy;
    }


  //**************************************************************************
  //** isProxyAvailable
  //**************************************************************************
  /** Used to check whether a proxy server is online/accessible.
   */
    public boolean isProxyAvailable(String proxyHost, int proxyPort){

        try {
           InetAddress address = InetAddress.getByName(proxyHost);
           System.out.println("Name: " + address.getHostName());
           System.out.println("Addr: " + address.getHostAddress());
           System.out.println("Reach: " + address.isReachable(3000));
           return true;
        }
        catch (UnknownHostException e) {}
        catch (IOException e) {}
        return false;
    }


  //**************************************************************************
  //** isLocalHost
  //**************************************************************************
  /** Used to determine whether to use the proxy server. doesn't account for
   *  the local machine name.
   */
    private boolean isLocalHost(String host){
        host = host.toLowerCase();
        if (host.equals("localhost") || host.equals("127.0.0.1")){
            return true;
        }
        else{
            return false;
        }
    }



  //**************************************************************************
  //** toString
  //**************************************************************************
  /**  Returns the request headers sent to the server.
   */
    public String toString(){


        StringBuffer out = new StringBuffer();
        //System.out.println("Request Header");
        //System.out.println("------------------------------------------------");

        java.util.Map<String,List<String>> requestHeaders = getRequestHeaders();
        if (requestHeaders!=null){
            java.util.Iterator it = requestHeaders.keySet().iterator();
            while(it.hasNext()){
                String key = (String) it.next();
                if (key!=null){
                    java.util.List list = (java.util.List) requestHeaders.get(key);
                    for (int i=0; i<list.size(); i++){
                        String value = list.get(i).toString();
                        out.append(key + ": " + value + "\r\n");
                    }
                }
                else{
                    out.append(requestHeaders.get(key) + "\r\n");
                }
            }
        }

        out.append("\r\n");
        return out.toString();
    }

}
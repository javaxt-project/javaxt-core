package javaxt.http;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.zip.*;
import java.io.*;

//******************************************************************************
//**  Http Response
//******************************************************************************
/**
 *   Used to process the response from an HTTP server.
 *
 ******************************************************************************/

public class Response {
    
    private URLConnection conn;
    private Request request;


    protected Response(Request request){
        this.request = request;
        this.conn = request.conn;
        //this.RequestProperties = conn.getRequestProperties();
    }


  //**************************************************************************
  //** getURL
  //**************************************************************************
  /** Returns the url used to connect to the server. Note that this URL may
   *  differ from the one used to instantiate the Request object. This only
   *  occurs when the server returns a redirect code and the maximum number of
   *  redirects is greater than 0. See Request.setNumRedirects().
   */
    public java.net.URL getURL(){
        return request.getURL();
    }


  //**************************************************************************
  //** getHeaders
  //**************************************************************************
  /** Returns key/value map representing all the HTTP headers returned from
   *  the server.
   */
    public java.util.Map getHeaders(){
        return request.getResponseHeaders();
    }


  //**************************************************************************
  //** getHeaders
  //**************************************************************************
  /** Returns an array of values associated with a given key found in the HTTP
   *  headers returned from the server.
   */
    public String[] getHeaders(String headerName){
        return request.getResponseHeaders(headerName);
    }


  //**************************************************************************
  //** getHeader
  //**************************************************************************
  /** Returns the value of a given key in the HTTP header.
   */
    public String getHeader(String headerName){
        return request.getResponseHeader(headerName);
    }


  //**************************************************************************
  //** getStatus
  //**************************************************************************
  /** Returns the HTTP status code extracted from the first line in the
   *  response header.
   */
    public int getStatus(){
        return request.getResponseCode();
    }


  //**************************************************************************
  //** getMessage
  //**************************************************************************
  /** Returns the message extracted from the first line in the response header.
   */
    public String getMessage(){
        return request.getResponseMessage();
    }


  //**************************************************************************
  //** getInputStream
  //**************************************************************************
  /**  Returns the raw InputStream from the server.
   */
    public InputStream getInputStream(){
        try{
            return conn.getInputStream();
        }
        catch(Exception e){
            return null;
        }
    }

    
  //**************************************************************************
  //** getText
  //**************************************************************************
  /**  Used read through the entire response stream and cast it to a string.
   *   The string is encoded using ISO-8859-1 character encoding.
   */
    public String getText(){
        try{
            return getText("ISO-8859-1"); 
        }
        catch(Exception e){}
        try{
            return getBytes(true).toString();
        }
        catch(Exception e){}
        return null;
    }
    
    
  //**************************************************************************
  //** getText
  //**************************************************************************
  /**  Used read through the entire response stream and cast it to a string.
   *   WARNING: This method will never throw an error.
   *
   *   @param charsetName Name of the character encoding used to read the file.
   *   Examples include UTF-8 and ISO-8859-1
   */    
    public String getText(String charsetName){
        try{
            return getBytes(true).toString(charsetName);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

  //**************************************************************************
  //** getXML
  //**************************************************************************
  /**  Used read through the entire response stream and converts it to an xml
   *   DOM document.
   */
    public org.w3c.dom.Document getXML(){
        return javaxt.xml.DOM.createDocument(new ByteArrayInputStream(getBytes(true).toByteArray()));
    }

  //**************************************************************************
  //** getImage
  //**************************************************************************
  /**  Used read through the entire response stream and returns an Image.
   */
    public javaxt.io.Image getImage(){
        return new javaxt.io.Image(getBytes(true).toByteArray());
    }
    

  //**************************************************************************
  //** getBytes
  //**************************************************************************
  /**  Used read through the entire response stream and returns a byte array
   *   (ByteArrayOutputStream).
   */
    public ByteArrayOutputStream getBytes(){                
        return getBytes(false);
    }


  //**************************************************************************
  //** getBytes
  //**************************************************************************
  /** Used read through the entire response stream and returns a byte array
   *  (ByteArrayOutputStream).
   *  @param deflate Option to decompress a gzip encoded response
   */
    public ByteArrayOutputStream getBytes(boolean deflate){

        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        String encoding = this.getHeader("Content-Encoding");
        if (deflate && encoding!=null){
            if (encoding.equalsIgnoreCase("gzip")){

                GZIPInputStream gzipInputStream = null;
                byte[] buf = new byte[1024];
                int len;

                try{
                    gzipInputStream = new GZIPInputStream(this.getInputStream());
                    while ((len = gzipInputStream.read(buf)) > 0) {
                        bas.write(buf, 0, len);
                    }
                }
                catch(Exception e){
                    //e.printStackTrace();
                }

                try { gzipInputStream.close(); } catch (Exception e){}
                try { bas.close(); } catch (Exception e){}


                return bas;

            }
            else{
                System.err.println("Unsupported encoding:  " + encoding);
            }
        }
        else{

            InputStream inputStream = null;
            byte[] buf = new byte[1024];
            int len=0;

            try{
                inputStream = conn.getInputStream();
                while((len=inputStream.read(buf,0,1024))>-1) {
                    bas.write(buf,0,len);
                }
            }
            catch(Exception e){
                //e.printStackTrace();
            }


            try { inputStream.close(); } catch (Exception e){}
            try { bas.close(); } catch (Exception e){}

            return bas;


        }


        return null;
    }


  //**************************************************************************
  //** getErrorStream
  //**************************************************************************
  /** Returns the error output stream returned from the server (if available).
   *  Example:
   <pre>
    int status = response.getStatus();
    if (status>=400 && status<500){
        try{
            java.io.InputStream errorStream = response.getErrorStream();
            java.io.BufferedReader buf = new java.io.BufferedReader (
                                new java.io.InputStreamReader(errorStream));

            try {
                String line;
                while  ((line = buf.readLine())!= null) {
                    System.out.println(line);
                }
            }
            catch (java.io.IOException e) {}

            errorStream.close();
            buf.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
   </pre>
   */
    public InputStream getErrorStream(){
        try{
            return ((HttpURLConnection)conn).getErrorStream();
        }
        catch(Exception e){
            return null;
        }        
    }




  //**************************************************************************
  //** toString
  //**************************************************************************
  /**  Returns the response headers returned from the server. Use the getText()
   *   method to get response body as a String.
   */
    public String toString(){

        java.util.Map headers = request.getResponseHeaders();

        StringBuffer out = new StringBuffer();
        java.util.Iterator it = headers.keySet().iterator();
        while(it.hasNext()){
            String key = (String) it.next();
            if (key!=null){
                java.util.List<String> list = (java.util.List<String>) headers.get(key);
                for (int i=0; i<list.size(); i++){
                    out.append(key + ": " + list.get(i) + "\r\n");
                }
            }
            else{
                out.append(headers.get(key) + "\r\n");
            }
        }
        
        out.append("\r\n");
        return out.toString();
    }
 

}

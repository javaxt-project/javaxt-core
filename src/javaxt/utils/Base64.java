package javaxt.utils;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

//******************************************************************************
//**  Base64
//******************************************************************************
/**
 *   Used to encode and decode Base64 data.
 *
 ******************************************************************************/

public class Base64 {
    
    private static final int bufferSize = 1024*1024; //1MB
    private static final Class[] ByteArray = new Class[] {byte[].class};
    private static Class<?> cls;
    private static class JDK {
        private static int majorVersion;
        private static int minorVersion;
        static {
            String[] arr = System.getProperty("java.version").split("\\.");
            majorVersion = Integer.valueOf(arr[0]);
            minorVersion = Integer.valueOf(arr[1]);
            try{
                if (JDK.majorVersion==1 && JDK.minorVersion<8){
                    cls = Class.forName("javax.xml.bind.DatatypeConverter");
                }
                else{
                    cls = Class.forName("java.util.Base64");
                }
            }
            catch(Throwable e){}
        }
    }
    
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Private constructor prevents users from instantiating this class. All 
   *  methods are static and can be called directly.
   */
    private Base64(){}

    
  //**************************************************************************
  //** encode
  //**************************************************************************
  /** Used to encode a given string to a Base64 encoded string.
   */
    public static String encode(String str){
        try{
            return encode(str.getBytes("UTF-8"));
        }
        catch(java.io.UnsupportedEncodingException e){
            return encode(str.getBytes());
        }
    }


  //**************************************************************************
  //** encode
  //**************************************************************************
  /** Used to encode a given byte array to a Base64 encoded string.
   */
    public static String encode(byte[] b){
        try{
            if (JDK.majorVersion==1 && JDK.minorVersion<8){
                try{
                    Class<?> cls = Class.forName("sun.misc.BASE64Encoder");
                    Object obj = cls.newInstance();
                    return (String) cls.getMethod("encode", ByteArray).invoke(obj, b);
                }
                catch(Exception e){
                    //e.printStackTrace();
                    return (String) cls.getMethod("printBase64Binary", ByteArray).invoke(null, b);
                }
            }
            else{
                Object encoder = cls.getMethod("getEncoder").invoke(null, null);
                return (String) encoder.getClass().getMethod("encodeToString", ByteArray).invoke(encoder, b);
            }
        }
        catch(Exception e){
            return null;
        }
    }

    
  //**************************************************************************
  //** encode
  //**************************************************************************
  /** Used to encode a given InputStream.
   */
    public static InputStream encode(InputStream is){
        try{
            if (JDK.majorVersion==1 && JDK.minorVersion<8){
                return null;
            }
            else{
                ByteArrayOutputStream bas = new java.io.ByteArrayOutputStream();              
                Object encoder = cls.getMethod("getEncoder").invoke(null, null);
                OutputStream os = (OutputStream) encoder.getClass().getMethod("wrap", OutputStream.class).invoke(encoder, bas);
                copy(is, bas);
                return getInputStream((ByteArrayOutputStream) os);
            }
        }
        catch(Exception e){
            return null;
        }
    }
    
    
  //**************************************************************************
  //** decode
  //**************************************************************************
  /** Used to decode a given Base64 encoded string to a byte array.
   */
    public static byte[] decode(String str){
        try{
            if (JDK.majorVersion==1 && JDK.minorVersion<8){
                try{
                    Class<?> cls = Class.forName("sun.misc.BASE64Decoder");
                    Object obj = cls.newInstance();
                    return (byte[]) cls.getMethod("decodeBuffer", String.class).invoke(obj, str);
                }
                catch(Throwable e){
                    //e.printStackTrace();
                    return (byte[]) cls.getMethod("parseBase64Binary", String.class).invoke(null, str);
                }
            }
            else{
                Object decoder = cls.getMethod("getDecoder").invoke(null, null);
                return (byte[]) decoder.getClass().getMethod("decode", String.class).invoke(decoder, str);
            }
        }
        catch(Exception e){
            return null;
        }
    }
    
    
  //**************************************************************************
  //** decode
  //**************************************************************************
  /** Used to decode a given Base64 encoded InputStream.
   */
    public static InputStream decode(InputStream is){
        try{
            if (JDK.majorVersion==1 && JDK.minorVersion<8){

                try{
                    Class<?> cls = Class.forName("sun.misc.BASE64Decoder");
                    Object obj = cls.newInstance();
                    ByteArrayOutputStream bas = new java.io.ByteArrayOutputStream();
                    cls.getMethod("encode", new Class[]{InputStream.class, OutputStream.class}).invoke(obj, is, bas);
                    return getInputStream(bas);
                }
                catch(Throwable e){
                    
                  //Options are limited
                    ByteArrayOutputStream bas = new ByteArrayOutputStream();
                    int x;
                    byte[] b = new byte[bufferSize];
                    while ((x = is.read(b, 0, b.length)) != -1) {
                        bas.write(b, 0, x);
                    }

                    bas.flush();
                    byte[] byteArray = decode(new String(bas.toByteArray()));
                    return new java.io.ByteArrayInputStream(byteArray);
                }
            }
            else{
                Object decoder = cls.getMethod("getDecoder").invoke(null, null);
                return (InputStream) decoder.getClass().getMethod("wrap", InputStream.class).invoke(decoder, is);
            }
        }
        catch(Exception e){
            return null;
        }
    }

    
  //**************************************************************************
  //** getInputStream
  //**************************************************************************
  /** Convert the OutputStream to an InputStream
   *  https://stackoverflow.com/a/23874232/
   */
    private static InputStream getInputStream(final ByteArrayOutputStream bas) throws java.io.IOException {
        java.io.PipedInputStream in = new java.io.PipedInputStream();
        final java.io.PipedOutputStream out = new java.io.PipedOutputStream(in);
        new Thread(new Runnable() {
            public void run() {
                try {
                    
                    bas.writeTo(out);
                } 
                catch (java.io.IOException e) {
                }
            }
        }).start();
        return in;
    }


  //**************************************************************************
  //** copy
  //**************************************************************************
  /** Reads all bytes from an input stream and writes them to an output stream.
   */
    private static void copy(InputStream source, OutputStream sink) throws java.io.IOException {
        byte[] buf = new byte[bufferSize];
        int x;
        while ((x = source.read(buf)) > 0) {
            sink.write(buf, 0, x);
        }
    }
}
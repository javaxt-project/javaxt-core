package javaxt.utils;

//******************************************************************************
//**  Java Class
//******************************************************************************
/**
 *   Used obtain the version of Java running this library.
 *
 ******************************************************************************/

public class Java {

    /** Static variable used to store the output of getJavaVersion() */
    public static final int version = getJavaVersion();


  //**************************************************************************
  //** getVersion
  //**************************************************************************
  /** Returns the version number of the JVM. Before Java 9, Java releases had
   *  a "1." prefix (e.g. "1.8"). This method will return the major release
   *  without the "1." prefix (e.g. 5, 6, 7, 8, 9, 10, ..., 17, etc). Returns
   *  a -1 if the version could not be determined for whatever reason.
   */
    public static int getVersion() {
        return version;
    }


    private static int getJavaVersion(){
        int version;
        try{
            version = getVersion(System.getProperty("java.version"));
        }
        catch (Throwable t){
            try{
                version = getVersion(
                    java.lang.Runtime.class.getMethod("version").invoke(null).toString());
                
                //version = getVersion(java.lang.Runtime.version().toString());
            }
            catch (Throwable t2){
                version = -1;
            }
        }
        return version;
    }



    private static int getVersion(String version) throws Exception {
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        }
        else {
            int dot = version.indexOf(".");
            if (dot != -1) { version = version.substring(0, dot); }
        }
        return Integer.parseInt(version);
    }

}
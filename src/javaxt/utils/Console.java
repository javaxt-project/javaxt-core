package javaxt.utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.lang.reflect.Array;

//******************************************************************************
//**  Console
//******************************************************************************
/**
 *   Various command line utilities used to print debug messages and collect
 *   user inputs.
 *
 ******************************************************************************/

public class Console {

    private static final String indent = "                       "; // 25 spaces
    private static final DecimalFormat df = new DecimalFormat("#.##");

  /** Static instance of the this class that can be called directly via a
   *  static import. Example:
   <pre>
    import static javaxt.utils.Console.console;
    public class Test {
        public Test(){
            console.log("Hello!");
        }
    }
   </pre>
   */
    public static Console console = new Console();


  //**************************************************************************
  //** log
  //**************************************************************************
  /** Prints a message to the standard output stream, along with the class
   *  name and line number where the log() function is called. This function
   *  is intended to help debug applications and is inspired by the JavaScript
   *  console.log() function.
   *  @param any Accepts any Java object (e.g. string, number, null, classes,
   *  arrays, etc). You can pass in multiple objects separated by a comma.
   *  Example:
   <pre>
       console.log("Hello");
       console.log("Date: ", new Date());
       console.log(1>0, 20/5, new int[]{1,2,3});
   </pre>
   */
    public void log(Object... any){


      //Get source
        String source = getSource();
        source = "[" + source + "]";
        if (source.length()<indent.length()){
            source += indent.substring(0, indent.length() - source.length());
        }
        else{
            source += " ";
        }


      //Get string representation of the object
        StringBuilder out = new StringBuilder(source);
        if (any!=null){
            int n = 0;
            for (Object obj : any){
                String str = null;
                if (obj!=null){
                    if (obj.getClass().isArray()){
                        StringBuilder sb = new StringBuilder("[");
                        for (int i=0; i<Array.getLength(obj); i++) {
                            Object o = Array.get(obj, i);
                            String s = format(o);
                            if (i>0) sb.append(",");
                            sb.append(s);
                        }
                        sb.append("]");
                        str = sb.toString();
                    }
                    else{
                        str = format(obj);
                    }
                }
                if (n>0) out.append(" ");
                out.append(str);
                n++;
            }
        }
        else{
            out.append(any);
        }

        System.out.println(out);
    }


  //**************************************************************************
  //** format
  //**************************************************************************
    private String format(Object obj){
        String str = null;
        if (obj!=null){
            if (obj instanceof Double){
                df.setMaximumFractionDigits(8);
                str = df.format((Double) obj);
            }
            else{
                str = obj.toString();
            }
        }
        return str;
    }


  //**************************************************************************
  //** getSource
  //**************************************************************************
  /** Returns the class name and line number that was used to call the log()
   *  method.
   */
    private String getSource(){

      //Create an exception and get the stack trace
        Exception e = new Exception();
        StackTraceElement[] stackTrace = e.getStackTrace();


      //Find first element in the stack trace that is not an instance of this class
        StackTraceElement target = null;
        boolean foundConsole = false;
        for (int i=1; i<stackTrace.length; i++){
            StackTraceElement el = stackTrace[i];
            try{
                Class c = Class.forName(el.getClassName());
                if (c.isAssignableFrom(this.getClass())){
                    foundConsole = true;
                }
                else{
                    if (foundConsole){
                        target = stackTrace[i];
                        break;
                    }
                }
            }
            catch(Exception ex){
            }
        }

        if (target==null) return "";


      //Parse target classname and append line number
        String className = target.getClassName();
        int idx = className.lastIndexOf(".");
        if (idx>0) className = className.substring(idx+1);

        idx = className.indexOf("$");
        if (idx>0) className = className.substring(0, idx);

        return className + ":" + target.getLineNumber();
    }


  //**************************************************************************
  //** getInput
  //**************************************************************************
  /** Used to prompt a user for an input
   */
    public static String getInput(String prompt){
        String input = null;
        System.out.print(prompt);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            input = br.readLine();
        }
        catch (IOException e) {
            System.out.println("Failed to read input");
            //System.exit(1);
        }
        return input;
    }


  //**************************************************************************
  //** getUserName
  //**************************************************************************
  /** Used to prompt a user for a username
   */
    public static String getUserName(String prompt){
        return getInput(prompt);
    }


  //**************************************************************************
  //** getPassword
  //**************************************************************************
  /** Used to prompt a user for a password. The password is hidden as is it
   *  entered.
   */
    public static String getPassword(String prompt) {

        String password = "";
        ConsoleEraser consoleEraser = new ConsoleEraser();
        System.out.print(prompt);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        consoleEraser.start();
        try {
            password = in.readLine();
        }
        catch (IOException e){
            System.out.println("Error trying to read your password!");
            //System.exit(1);
        }

        consoleEraser.halt();
        System.out.print("\b");

        return password;
    }


  //**************************************************************************
  //** ConsoleEraser Class
  //**************************************************************************
    private static class ConsoleEraser extends Thread {
        private boolean running = true;
        public void run() {
            while (running) {
                System.out.print("\b ");
                try {
                    Thread.currentThread().sleep(1);
                }
                catch(InterruptedException e) {
                    break;
                }
            }
        }
        public synchronized void halt() {
            running = false;
        }
    }


  //**************************************************************************
  //** parseArgs
  //**************************************************************************
  /** Converts command line inputs into key/value pairs. Assumes keys start
   *  with a "-" character (e.g. "-version" or "--version") followed by a
   *  value (or nothing at all).
   */
    public static java.util.HashMap<String, String> parseArgs(String[] args){
        java.util.HashMap<String, String> map = new java.util.HashMap<String, String>();
        for (int i=0; i<args.length; i++){
            String key = args[i];
            if (key.startsWith("-")){
                if (i<args.length-1){
                    String nextArg = args[i+1];
                    if (nextArg.startsWith("-")){
                        map.put(key, null);
                    }
                    else{
                        i++;
                        map.put(key, nextArg);
                    }
                }
                else{
                    map.put(key, null);
                }
            }
        }
        return map;
    }


  //**************************************************************************
  //** main
  //**************************************************************************
  /** Used to print the JavaXT version number when this jar is called from the
   *  command line. Example:
   <pre>
    java -jar javaxt-core.jar
    JavaXT: 1.11.3
   </pre>
   *  Under the hood, this simple command line application uses the
   *  getVersion() method in the javaxt.io.Jar class.
   */
    public static void main(String[] args) {
        javaxt.io.Jar jar = new javaxt.io.Jar(javaxt.io.Jar.class);
        System.out.println("JavaXT: " + jar.getVersion());
    }

}
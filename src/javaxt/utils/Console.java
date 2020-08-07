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
 *   Used to print messages to the standard output stream.
 *
 ******************************************************************************/

public class Console {

    private static final String indent = "                       "; // 25 spaces
    private static final DecimalFormat df = new DecimalFormat("#.##");
    public static Console console = new Console();


  //**************************************************************************
  //** log
  //**************************************************************************
  /** Prints a message to the standard output stream. Accepts strings, nulls,
   *  and all other Java objects.
   */
    public void log(Object obj){
        l(obj);
    }


  //**************************************************************************
  //** log
  //**************************************************************************
  /** Prints a message to the standard output stream. Accepts strings, nulls,
   *  and all other Java objects.
   */
    public void log(Object... obj){
        l(obj);
    }

    
  //**************************************************************************
  //** log
  //**************************************************************************
    private void l(Object... any){

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
  /** Converts command line inputs into key/value pairs.
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
}
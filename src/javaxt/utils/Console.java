package javaxt.utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

//******************************************************************************
//**  Console
//******************************************************************************
/**
 *   Used to print messages to the standard output stream.
 *
 ******************************************************************************/

public class Console {

    private String me = this.getClass().getName();
    private static final String indent = "                       "; // 25 spaces
    private static final DecimalFormat df = new DecimalFormat("#.##");


  //**************************************************************************
  //** log
  //**************************************************************************
  /** Prints a message to the standard output stream. Accepts strings, nulls,
   *  and all other Java objects.
   */
    public void log(Object obj){

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
        String str = null;
        if (obj!=null){
            if (obj.getClass().isArray()){
                Object[] arr = (Object[]) obj;
                StringBuilder sb = new StringBuilder("[");
                for (int i=0; i<arr.length; i++){
                    String s = format(arr[i]);
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

        System.out.println(source + str);
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

      //Create an exception
        Exception e = new Exception();

      //Find first element in the stack trace that doesn't belong to this class
        for (StackTraceElement el : e.getStackTrace()){
            if (!el.getClassName().equals(me)){
                String className = el.getClassName();
                int idx = className.lastIndexOf(".");
                if (idx>0) className = className.substring(idx+1);

                idx = className.indexOf("$");
                if (idx>0) className = className.substring(0, idx);

                return className + ":" + el.getLineNumber();
            }
        }

        return "";
    }


  //**************************************************************************
  //** getUserName
  //**************************************************************************
    public static String getUserName(String prompt){
        String username = null;
        System.out.print(prompt);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            username = br.readLine();
        }
        catch (IOException e) {
            System.out.println("Error trying to read your name!");
            //System.exit(1);
        }
        return username;
    }


  //**************************************************************************
  //** getPassword
  //**************************************************************************
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
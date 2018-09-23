package javaxt.utils;

//******************************************************************************
//**  Console
//******************************************************************************
/**
 *   Used to print messages to the standard output stream.
 *
 ******************************************************************************/

public class Console {
    
    private String me = this.getClass().getName();
    String indent = "                       "; // 25 spaces
    
    
  //**************************************************************************
  //** log
  //**************************************************************************
  /** Prints a message to the standard output stream. Accepts strings, nulls, 
   *  and all other Java objects.
   */
    public void log(Object obj){
        String source = getSource();
        source = "[" + source + "]";
        source += indent.substring(0, indent.length() - source.length());
        String str = null;
        if (obj!=null) str = obj.toString();
        System.out.println(source + str);
    }
    
    
  //**************************************************************************
  //** getSource
  //**************************************************************************
  /** Returns the class name and line number that was used to call the log() 
   *  method.
   */
    private String getSource(){
        try{
            int x = 1/0; //intentionally throw exception
        }
        catch(Exception e){

          //Find first element in the stack trace that doesn't belong to this class
            for (StackTraceElement el : e.getStackTrace()){
                if (!el.getClassName().equals(me)){
                    String className = el.getClassName();
                    int idx = className.lastIndexOf(".");
                    if (idx>0) className = className.substring(idx+1);

                    return className + ":" + el.getLineNumber();
                }
            }
        }
        return "";
    }
}
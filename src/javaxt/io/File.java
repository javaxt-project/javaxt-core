package javaxt.io;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.io.*;

//******************************************************************************
//**  File Class - By Peter Borissow
//******************************************************************************
/**
 *   Used to represent a single file on a file system. In many ways, this class 
 *   is an extension of the java.io.File class. However, unlike the java.io.File 
 *   class, this object provides functions that are relevant and specific to
 *   files (not directories). 
 *
 ******************************************************************************/

public class File implements Comparable {
    
    private java.io.File File; 
    public final String PathSeparator = System.getProperty("file.separator");
    public final String LineSeperator = System.getProperty("line.separator");
    
    
  //**************************************************************************
  //** Instantiate File
  //************************************************************************** 
  /** Creates a new File instance by converting the given pathname string into 
   *  an abstract pathname. 
   */  
    
    public File(String Path) {
        if (Path.startsWith("\"") && Path.endsWith("\"")){
            Path = Path.substring(1,Path.length()-1);
        }
        File = new java.io.File(Path);
        
    }
    
    public File(java.io.File File) {
        this.File = File;
    }
    
  /** Creates a new File instance from a parent abstract pathname and a child 
   *  pathname string. 
   */
    public File(java.io.File Parent, String Child){
        this.File = new java.io.File(Parent, Child);
    }
    
    public File(Directory Parent, String Child){
        this.File = new java.io.File(Parent.toFile(), Child);
    }
        
    public File(String Parent, String Child){
        this.File = new java.io.File(new Directory(Parent).toFile(), Child);
    }   
    
    
  //**************************************************************************
  //** Get File Name
  //************************************************************************** 
  /**  Returns the name of the file, excluding the path. */
    
    public String getName(){
        if (File!=null) return File.getName();
        else return "";
    }
    
    
  //**************************************************************************
  //** Get File Name
  //************************************************************************** 
  /**  Returns the name of the file, excluding the path. 
   *
   *   @param IncludeFileExtension If true, includes the file extension. 
   *   Otherwise, will return the file name without the extension.
   */
    
    public String getName(boolean IncludeFileExtension){
        String FileName = getName();
        if (!IncludeFileExtension){
            String FileExt = getExtension();
            if (FileExt.length()>0){
                FileName = FileName.substring(0, 
                           FileName.length() - (FileExt.length()+1));
            }
        }
        return FileName;
    }
    
    
  //**************************************************************************
  //** Get File Path
  //**************************************************************************  
  /**  Used to retrieve the path to the file, excluding the file name. Appends 
   *   a file separator to the end of the string. 
   */
    
    public String getPath(){
        if (File!=null) {
            String path = ""; //File.getParentFile().toString();
            try{
               path = File.getParentFile().getCanonicalPath().toString();
            }
            catch(Exception e){
               path = File.getParentFile().toString();
            }
            if (!path.endsWith(PathSeparator)) path += PathSeparator;
            return path;
        }
        else return "";
    }
    
    
  //**************************************************************************
  //** Get Directory
  //************************************************************************** 
  /**  Returns the file's parent directory. Same as getParentDirectory() 
   *   @deprecated Use the getParentDirectory() method instead.
   */
    @Deprecated
    public Directory getDirectory(){
        return getParentDirectory();
    }

  //**************************************************************************
  //** Get Parent Directory
  //**************************************************************************
  /**  Returns the file's parent directory. */

    public Directory getParentDirectory(){
        if (File!=null) return new Directory(getPath());
        else return null;
    }

    
  //**************************************************************************
  //** Get File
  //************************************************************************** 
  /**  Returns the java.io.File representation of this object. 
   *   @deprecated Use the toFile() method instead.
   */
    @Deprecated
    public java.io.File getFile(){
        return File;
    }
    
  //**************************************************************************
  //** toFile
  //**************************************************************************
  /**  Returns the java.io.File representation of this object. */

    public java.io.File toFile(){
        return File;
    }

    
  //**************************************************************************
  //** Get File Extension
  //**************************************************************************  
  /**  Returns the file's extension, excluding the last dot/period 
   *   (e.g. "C:\image.jpg" will return "jpg").
   */
    public String getExtension(){
        String FileName = getName();
        if (FileName.contains((CharSequence) ".")){
            return FileName.substring(FileName.lastIndexOf(".")+1,FileName.length());
        }
        else{
            return "";
        }
    }
    
    
  //**************************************************************************
  //** Get File Size
  //**************************************************************************
  /**  Returns the size of the file, in bytes. */
    
    public long getSize(){
        if (File!=null) return File.length();
        else return 0;
    }
    
  //**************************************************************************
  //** Get File Date
  //**************************************************************************
  /**  Returns the date of the file (when it was last modified). */
    
    public java.util.Date getDate(){
        if (File!=null) return new javaxt.utils.Date(File.lastModified()).getDate();
        else return null;
    }
    
  //**************************************************************************
  //** Exists
  //**************************************************************************
  /**  Used to determine whether a file exists. */
    
    public boolean exists(){ 
        if (File!=null) return File.exists();
        else return false;
    }
    
    
  //**************************************************************************
  //** isHidden
  //**************************************************************************
  /** Used to check whether the file is hidden. */
    
    public boolean isHidden(){
        if (File!=null) return File.isHidden();
        else return false;
    }
    
  //**************************************************************************
  //** isReadOnly
  //**************************************************************************
  /** Used to check whether the file has read permissions. */

    public boolean isReadOnly(){
        if (File!=null) return !File.canWrite();
        else return true;
    }
    
  //**************************************************************************
  //** isExecutable
  //**************************************************************************
  /** Used to check whether the file has execute permissions. Note that this
   *  method is not supported by JDK 1.5 or lower. Instead, the method will
   *  return false.
   */
    public boolean isExecutable(){
        if (File!=null){

            //return File.canExecute(); //<--incompatable with JDK 1.5

            String[] arr = System.getProperty("java.version").split("\\.");
            if (Integer.valueOf(arr[0]).intValue()==1 && Integer.valueOf(arr[1]).intValue()<6) return false;
            else{
                try{
                    return (Boolean) File.getClass().getMethod("canExecute").invoke(File, null);
                }
                catch(Exception e){
                    return false;
                }
            }
        }
        else return false;
    }
    
    
  //**************************************************************************
  //** Delete File
  //**************************************************************************
  /**  Used to delete the file. Warning: this operation is irrecoverable. */
    
    public boolean delete(){ 
        if (File!=null) return File.delete();
        else return false;
    }



  //**************************************************************************
  //** setBufferSize
  //**************************************************************************
  /** Used to set the size of the buffer used to read/write bytes. The default
   *  is 1MB (1,048,576 bytes)
   */
    public void setBufferSize(int numBytes){
        bufferSize = numBytes;
    }
    
    private int bufferSize = 1024*1024; //1MB
    
  //**************************************************************************
  //** Move File
  //**************************************************************************
  /**  Used to move the file to a different directory. */
    
    public javaxt.io.File moveTo(Directory Destination){
        java.io.File Dir = Destination.toFile();
        Dir.mkdirs();
        javaxt.io.File newFile = new javaxt.io.File(Dir, File.getName());
        File.renameTo(newFile.toFile());
        File = newFile.toFile();
        return newFile;
    }
    
    
  //**************************************************************************
  //** Copy File
  //**************************************************************************
  /**  Used to create a copy of this file. */
    
    public boolean copyTo(Directory Destination, boolean Overwrite){
        File Output = new File(Destination, File.getName());
        return copyTo(Output,Overwrite);
    }
    
    
  //**************************************************************************
  //** Copy File
  //**************************************************************************
  /**  Used to create a copy of this file. */
    
    public boolean copyTo(javaxt.io.File Destination, boolean Overwrite){
        
      //Validate Input/Output
        System.out.println(Destination);
        if (File.exists()==false) return false;
        if (Destination.exists() && Overwrite==false) return false;
        if (File.equals(Destination.toFile())) return false;
        
      //Create New Path
        Destination.getParentDirectory().create();
        
      //Copy File
        try{
            

            FileInputStream input  = new FileInputStream(File);
            FileOutputStream output = new FileOutputStream(Destination.toFile());
            final ReadableByteChannel inputChannel = Channels.newChannel(input);
            final WritableByteChannel outputChannel = Channels.newChannel(output);
            final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(bufferSize);

            while (inputChannel.read(buffer) != -1) {
                buffer.flip();
                outputChannel.write(buffer);
                buffer.compact();

            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                outputChannel.write(buffer);
            }

            inputChannel.close();
            outputChannel.close();

            Destination.toFile().setLastModified(File.lastModified());
            return true;
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    


    
  //**************************************************************************
  //** Rename File
  //**************************************************************************
  /**  Used to rename a file - existing File Name is relaced with input 
   *   FileName. Note that this method is NOT equivalent to the java.io.File
   *   "renameTo" method.
   *   @param FileName The new file name (including the file extension).
   */
    public javaxt.io.File rename(String FileName){
        if (FileName!=null){
            FileName = FileName.trim();
            if (FileName.length()>0){
                if (File!=null) {
                    javaxt.io.File newFile = new javaxt.io.File(getPath() + FileName);
                    File.renameTo(newFile.toFile());
                    File = newFile.toFile();
                    return newFile;
                }
            }
        }
        return this;
    }



  //**************************************************************************
  //** getBufferedWriter
  //**************************************************************************
  /**  Used to instantiate a BufferedWriter for this file.
   */
    public BufferedWriter getBufferedWriter(String charsetName){

        try{
            File.getParentFile().mkdirs();
            if (charsetName==null){
                return new BufferedWriter( new FileWriter(File) );
            }
            else{
                return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(File), charsetName));
            }
        }
        catch (Exception e){
            return null;
        }        
    }

    
  //**************************************************************************
  //** getBufferedReader
  //**************************************************************************
  /**  Used to extract the contents of the file into a BufferedReader.
   *   <pre>
   *   BufferedReader br = file.getBufferedReader("UTF-8");
   *   String strLine;
   *   while ((strLine = br.readLine()) != null){
   *      System.out.println (strLine);
   *   }
   *   </pre>
   *   WARNING: This method will never throw an error.
   * 
   *   @return BufferedReader
   */
    public BufferedReader getBufferedReader(){

        if (File.exists()){
            try{

              //NOTE: FileReader always assumes default encoding is OK!
                return new BufferedReader( new FileReader(File) ); 

            }
            catch (Exception e){
            }

        }
        return null;
    }
    

  //**************************************************************************
  //** getBufferedReader
  //**************************************************************************
  /**  Used to extract the contents of the file into a BufferedReader.
   *   <pre>
   *   BufferedReader br = file.getBufferedReader("UTF-8");
   *   String strLine;
   *   while ((strLine = br.readLine()) != null){
   *      System.out.println (strLine);
   *   }
   *   </pre>
   *   WARNING: This method will never throw an error.
   *
   *   @param charsetName Name of the character encoding used to read the file.
   *   Examples include UTF-8 and ISO-8859-1
   */
    public BufferedReader getBufferedReader(String charsetName){
        if (File.exists()){
            try{

                return new java.io.BufferedReader(new java.io.InputStreamReader(this.getInputStream(),charsetName));

            }
            catch (Exception e){
            }

        }
        return null;
    }
    
    
    
  //**************************************************************************
  //** getBufferedImage
  //**************************************************************************
    
    public java.awt.image.BufferedImage getBufferedImage(){
        
        if (File.exists()){
            try{
                return javax.imageio.ImageIO.read(File);
            }
            catch(Exception e){}
        }
        
        return null;
    }
    
    
  //**************************************************************************
  //** getImage
  //**************************************************************************
  /** Used to open the file and read the contents into an image.
   */
    public Image getImage(){
        if (File.exists()){
            return new Image(File);
        }
        return null;
    }

    
  //**************************************************************************
  //** getText
  //**************************************************************************
  /** Used to open the file and read the contents into a string.
   */
    public String getText(){
        try{
            return getText("UTF-8");
        }
        catch(Exception e){}
        try{
            return getBytes().toString();
        }
        catch(Exception e){}
        return "";
    }
    
    
  //**************************************************************************
  //** getText
  //**************************************************************************
  /**  Used to extract the contents of the file as a String. 
   *   WARNING: This method will never throw an error.
   *
   *   @param charsetName Name of the character encoding used to read the file.
   *   Examples include UTF-8 and ISO-8859-1
   */
    
    public String getText(String charsetName){
        try{
           return getBytes().toString(charsetName);
        }
        catch(Exception e){}
        return "";
    }


  //**************************************************************************
  //** getXML
  //**************************************************************************
  /** Returns an XML DOM Document (org.w3c.dom.Document) */
    
    public org.w3c.dom.Document getXML(){
        try{
            return javaxt.xml.DOM.createDocument(getInputStream());
        }
        catch(Exception e){
            return null;
        }
    }
    
    
  //**************************************************************************
  //** getBytes
  //**************************************************************************
    
    public ByteArrayOutputStream getBytes(){
        
        if (File.exists()){
            try{
                FileInputStream InputStream = new FileInputStream(File);
                ByteArrayOutputStream bas = new ByteArrayOutputStream();
                byte[] b = new byte[bufferSize];
                int x=0;
                while((x=InputStream.read(b,0,bufferSize))>-1) {
                    bas.write(b,0,x);
                }
                bas.close();
                InputStream.close();
                return bas;
            }
            catch(Exception e){}
        }
        
        return null;
    }
    
    

    public void write(ByteArrayOutputStream bas){
        write(bas.toByteArray());
    }


    public void write(byte[] bytes){
        if (File!=null){                
            FileOutputStream output = null;
            try {
                File.getParentFile().mkdirs();
                output = new FileOutputStream(File);
                output.write(bytes);
            }
            catch (Exception e){}
            finally {
                try { if (output != null) output.close(); }
                catch (Exception e){}
            }
        }
    }
    
    
    public void write(InputStream input){
        if (File!=null){                
            FileOutputStream output = null;
            try {
                File.getParentFile().mkdirs();
                output = new FileOutputStream(File);
                byte[] buf = new byte[bufferSize];
                int i = 0;
                while((i=input.read(buf))!=-1) {
                  output.write(buf, 0, i);
                }              
                
            }
            catch (Exception e){}
            finally {
                try { if (output != null) output.close(); }
                catch (Exception e){}
            }
        }
    }
    

  //**************************************************************************
  //** Write Text
  //**************************************************************************
    
    public void write(String Text){
        write(Text, null);
    }

    
  //**************************************************************************
  //** Write Text
  //**************************************************************************
  /**  Used to write text to a file.
   *   WARNING: This method will never throw an error.
   *
   *   @param charsetName Name of the character encoding used to read the file.
   *   Examples include UTF-8 and ISO-8859-1
   */
    
    public void write(String Text, String charsetName){
        if (File!=null){                
            Writer output = null;
            try {
                File.getParentFile().mkdirs();
                
                if (charsetName==null){
                    output = new BufferedWriter( new FileWriter(File) );
                }
                else{
                    output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(File), charsetName));
                }
                
                output.write( Text );
            }
            catch (Exception e){e.printStackTrace();}
            finally {
                try { if (output != null) output.close(); }
                catch (Exception e){}
            }
        }
    }


  //**************************************************************************
  //** Write XML
  //**************************************************************************
  /**  Used to write an XML DOM Document to a file. 
   */
    public void write(org.w3c.dom.Document xml){
        write(javaxt.xml.DOM.getText(xml), xml.getXmlEncoding());
    }


  //**************************************************************************
  //** Write Text
  //**************************************************************************
    
    public void write(String[] Content){
        
        if (File!=null){
        
            Writer output = null;
            try {
                File.getParentFile().mkdirs();
                output = new BufferedWriter( new FileWriter(File) );
                for (int i=0; i<Content.length-1; i++){
                     output.write( Content[i] + LineSeperator);
                }
                output.write( Content[Content.length-1]);
            }
            catch (Exception e){}
            finally {
                try { if (output != null) output.close(); }
                catch (Exception e){}
            }

        }
    }
    
    
  //**************************************************************************
  //** Write Image
  //**************************************************************************
  /** Used to write an image to a file. */
    
    public void write(java.awt.image.BufferedImage Image){
              
        if (File!=null){      
            try{
                
                File.getParentFile().mkdirs();
                java.awt.image.RenderedImage rendImage = Image;
                javax.imageio.ImageIO.write(rendImage,getExtension(),File);
            }
            catch (Exception e){
                //System.out.println(e.toString());
            }
        }
    }

    
    
    
  //**************************************************************************
  //** MapPath
  //**************************************************************************
    
    public String MapPath(String RelPath){
        
      //Update currDir
        String currDir = getPath();
        currDir = currDir.replace("\\","/");
        if (!currDir.endsWith("/")){
            currDir+="/";
        }


        RelPath = RelPath.replace("\\","/");
        
        String[] arrRelPath = RelPath.split("/");
        String[] arrAbsPath = currDir.split("/");
        
        
        
        int x = -1;
        RelPath = "";
        String Dir = "";
        for (int i=0; i<arrRelPath.length; i++) {
            Dir = arrRelPath[i];
            if (Dir.equals("..")){
                x = x + 1;               
            }
            else if (Dir.equals(".")){
                //do nothing?
            }
            else{
                RelPath = RelPath + "\\" + arrRelPath[i];
            }                      
            
        }
                
        
        //x = x + 1 'because currDir has a "\" at the end of it
        Dir = "";
        int ubound = 0;
        for (int i=0; i<arrAbsPath.length-(x+1); i++){ //because currDir has a "\" at the end of it
            Dir = Dir + arrAbsPath[i] + "\\";
        }

        
        //trim off last "\"
        //Dir = left(Dir, len(Dir) - 1);
        Dir = Dir.substring(0,Dir.length()-1);
        
        //replace any leftover "/" characters
        Dir = Dir + RelPath.replace("/", "\\");
        
        
        Dir = Dir.replace((CharSequence) "\\", (CharSequence) PathSeparator);
        
        return Dir;
    }
    
    
    
  //**************************************************************************
  //** getInputStream
  //**************************************************************************
  /**  Returns a new FileInputStream Object */
    
    public FileInputStream getInputStream() throws IOException{
        return new FileInputStream(this.File);
    }
    
  //**************************************************************************
  //** getInputStream
  //**************************************************************************
  /**  Returns a new FileOutputStream Object */
    
    public FileOutputStream getOutputStream() throws IOException{
        return new FileOutputStream(this.File);
    }
    
    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /**  Returns the full file path (including the file name) */
    
    public String toString(){
        return File.toString();
    }
 

    @Override
    public int hashCode(){
        return this.File.hashCode();
    }

    //@Override
    public int compareTo(Object obj){
        if (obj==null) return -1;
        else return -obj.toString().compareTo(getPath());
    }


  //**************************************************************************
  //** equals
  //**************************************************************************
  
    public boolean equals(Object obj){
        
        if (obj instanceof javaxt.io.File){
            return File.equals(((javaxt.io.File) obj).getFile());
        }
        else if (obj instanceof java.io.File){
            if (((java.io.File) obj).isFile()) 
                return File.equals(obj);
        }
        return false;
    }
    
    
    
  //**************************************************************************
  //** IsValidPath -- NOT USED!
  //**************************************************************************
  /**  Checks whether PathToFile is a valid */
    
    private boolean isValidPath(String PathToFile){
        
        if (PathToFile==null) return false;
        if (PathToFile.length()<1) return false;
  
        if (File.isDirectory()) return false;
        
        String FileName = File.getName();
        if (FileName.length()<1) return false;
        if (FileName.length()>260) return false;
        
        PathToFile = File.toString();
        PathToFile = PathToFile.replace((CharSequence) "\\", "/");
        String[] Path = PathToFile.split("/");                
        String[] arr = new String[]{ "/", "?", "<", ">", "\\", ":", "*", "|", "\"" };
        
        for (int i=0; i<Path.length; i++){
             for (int j=0; j<arr.length; j++){
                  if (arr[j].equals(":") && i==0 & Path[i].length()==2){ //&& File.pathSeparator.equals(":")
                    //skip check b/c we've got something like "C:\" in the path                      
                  }
                  else{
                      if (Path[i].contains((CharSequence) arr[j])) return false;
                  }
             }
        }
        

        return true;

    }
    
    
    
  //**************************************************************************
  //** getContentType
  //**************************************************************************
  /**  Returns the mime type associated with the file extension. This method 
   *   only covers the most common/popular mime types. The returned mime type
   *   is NOT authoritative.
   */
    
    public String getContentType(){
        
      //TEXT 
        if (this.extensionEquals("css")) return "text/css";
        if (this.extensionEquals("dtd")) return "text/plain";
        if (this.extensionEquals("htm,html")) return "text/html";
        if (this.extensionEquals("java")) return "text/plain";
        if (this.extensionEquals("js")) return "text/javascript";
        if (this.extensionEquals("txt")) return "text/plain";
        
      //IMAGE
        if (this.extensionEquals("bmp")) return "image/bmp";
        if (this.extensionEquals("gif")) return "image/gif";
        if (this.extensionEquals("jp2,j2c,j2k,jpx")) return "image/jp2";
        if (this.extensionEquals("jpg,jpe,jpeg,jfif,pjpeg,pjp")) return "image/jpeg";
        if (this.extensionEquals("png")) return "image/png";
        if (this.extensionEquals("psd")) return "image/x-photoshop";
        if (this.extensionEquals("rgb")) return "image/x-rgb";
        if (this.extensionEquals("tif,tiff")) return "image/tiff";
        if (this.extensionEquals("xbm")) return "image/x-xbitmap";
        if (this.extensionEquals("xpm")) return "image/x-xpixmap";
        if (this.extensionEquals("ico")) return "image/vnd.microsoft.icon";
        
      //MICROSOFT OFFICE APPLICATIONS
        if (this.extensionEquals("doc,dot")) return "application/msword";
        if (this.extensionEquals("xls,xlw,xla,xlc,xlm,xlt,xll")) return "application/vnd.ms-excel";
        if (this.extensionEquals("ppt,pps,pot")) return "application/vnd.ms-powerpoint";
        if (this.extensionEquals("mdb")) return "application/x-msaccess";
        if (this.extensionEquals("mpp")) return "application/vnd.ms-project";
        if (this.extensionEquals("pub")) return "application/x-mspublisher";
        if (this.extensionEquals("wmz")) return "application/x-ms-wmz";
        if (this.extensionEquals("wmd")) return "application/x-ms-wmd";
        
      //OTHER APPLICATIONS
        if (this.extensionEquals("ai,eps,ps")) return "application/postscript";
        if (this.extensionEquals("gz")) return "application/x-gzip";
        if (this.extensionEquals("pdf")) return "application/pdf";
        if (this.extensionEquals("xml")) return "application/xml"; //return "text/xml";
        if (this.extensionEquals("z")) return "application/x-compress";
        if (this.extensionEquals("zip")) return "application/zip";
        
      //AUDIO
        if (this.extensionEquals("mid,midi")) return "audio/x-midi";
        if (this.extensionEquals("mp1,mp2,mp3,mpa,mpega")) return "audio/x-mpeg";
        if (this.extensionEquals("ra,ram")) return "audio/x-pn-realaudio";
        if (this.extensionEquals("wav")) return "audio/x-wav";
        if (this.extensionEquals("wma")) return "audio/x-ms-wma";
        if (this.extensionEquals("wax")) return "audio/x-ms-wax";
        if (this.extensionEquals("wmv")) return "audio/x-ms-wmv";
        
      //VIDEO
        if (this.extensionEquals("asf,asx")) return "video/x-ms-asf";
        if (this.extensionEquals("avi")) return "video/msvideo";
        if (this.extensionEquals("mov")) return "video/quicktime";
        if (this.extensionEquals("mpe,mpeg,mpg")) return "video/mpeg";
        if (this.extensionEquals("mpv2")) return "video/mpeg2";
        if (this.extensionEquals("qt,mov,moov")) return "video/quicktime";
        
        if (this.extensionEquals("wvx")) return "video/x-ms-wvx";
        if (this.extensionEquals("wm")) return "video/x-ms-wm";
        if (this.extensionEquals("wmx")) return "video/x-ms-wmx"; 

        
      //DEFAULT
        return "application/octet-stream";
    }
    
    
  //**************************************************************************
  //** extensionEquals
  //**************************************************************************
  /**  Used by the getContentType to compare file extensions.
   *   @param FileExtension Comma Seperated List Of File Extensions 
   */
    
    private boolean extensionEquals(String FileExtension){
        String ext = this.getExtension();
        String[] arr = FileExtension.split(",");
        for (int i=0; i<arr.length; i++){
             String str = arr[i].trim();
             if (str.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }
    
}

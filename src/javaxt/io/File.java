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
    private static final boolean isWindows = Directory.isWindows;
    
    
  //**************************************************************************
  //** Constructor
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


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Instantiates this class using a java.io.File. Please use the Directory
   *  class for directories. Example:
   * <pre>if (file.isDirectory()) new Directory(file);</pre>
   */        
    public File(java.io.File File) {
        if (File.isDirectory()){} //throw an error or set this.File=null?
        this.File = File;
    }

    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new File instance from a parent abstract pathname and a child 
   *  pathname string. 
   */
    public File(java.io.File Parent, String Child){
        this(new java.io.File(Parent, Child));
    }
    
    public File(Directory Parent, String Child){
        this(new java.io.File(Parent.toFile(), Child));
    }
        
    public File(String Parent, String Child){
        this(new java.io.File(new Directory(Parent).toFile(), Child));
    }


  //**************************************************************************
  //** Protected Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a Directory. This constructor
   *  was added to support methods such as isLink(), getLink(), and
   *  getFileAttributes() in the Directory class.
   */
    protected File(Directory directory){
        this.File = directory.toFile();
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
  /**  Returns the file's parent directory. Same as getParentDirectory() */

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
   *   (e.g. "C:\image.jpg" will return "jpg"). Returns a zero-length string
   *   if there is no extension.
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
  //** isLink
  //**************************************************************************
  /**  Used to determine whether the file is actually a link to another file.
   *   Returns true for symbolic links, Windows junctions, and Windows
   *   shortcuts.
   */
    public boolean isLink(){

        if (File == null || !File.exists()) {
            return false;
        }


        if (this.getExtension().equalsIgnoreCase("lnk")){
            return (new LnkParser(this).getFile()!=null);
        }
        else{
            if (isWindows){
                java.util.HashSet<String> flags = this.getFlags();
                if (flags!=null)  return (flags.contains("REPARSE_POINT"));
            }
            else{
                try{
                    return !File.getCanonicalFile().equals(File.getAbsoluteFile());
                }
                catch(Exception e){
                }
            }
        }
        return false;
    }


  //**************************************************************************
  //** getLink
  //**************************************************************************
  /**  Returns the target of a symbolic link, Windows junction, or Windows
   *   shortcut.
   */
    public java.io.File getLink(){

        if (File == null || !File.exists()) {
            return null;
        }

        if (this.getExtension().equalsIgnoreCase("lnk")){
            return new LnkParser(this).getFile();
        }
        else{
            if (isWindows){
                try{
                    if (loadDLL()){
                        java.io.File link = new java.io.File(GetTarget(File.toString()));
                        if (link.exists()) return link;
                    }
                }
                catch(Exception e){
                    //e.printStackTrace();
                }
            }
            else{
                try{
                    if (!File.getCanonicalFile().equals(File.getAbsoluteFile())){
                        return File.getCanonicalFile(); //this needs to be tested...
                    }
                }
                catch(Exception e){
                }
            }
        }
        return null;
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
  /**  Used to rename a file - existing File Name is replaced with input
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
            return File.equals(((javaxt.io.File) obj).toFile());
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
   *   @param FileExtension Comma Separated List Of File Extensions
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


  //**************************************************************************
  //** getCreationTime
  //**************************************************************************
  /** Returns a timestamp of when the file was first created. Returns a null
   *  if the timestamp is not available. Note that this attribute is currently
   *  only available on Windows XP or later.
   */
    public java.util.Date getCreationTime(){
        try{
            return getFileAttributes().getCreationTime();
        }
        catch(Exception e){
            return null;
        }
    }

  //**************************************************************************
  //** getLastAccessTime
  //**************************************************************************
  /** Returns a timestamp of when the file was last accessed. Returns a null
   *  if the timestamp is not available. Note that this attribute is currently
   *  only available on Windows XP or later.
   */
    public java.util.Date getLastAccessTime(){
        try{
            return getFileAttributes().getLastAccessTime();
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }


  //**************************************************************************
  //** getLastWriteTime
  //**************************************************************************
  /** Returns a timestamp of when the file was last written to. Returns a null
   *  if the timestamp is not available. Note that this attribute is currently
   *  only available on Windows XP or later.
   */
    public java.util.Date getLastWriteTime(){
        try{
            return getFileAttributes().getLastWriteTime();
        }
        catch(Exception e){
            return null;
        }
    }


  //**************************************************************************
  //** getFlags
  //**************************************************************************
  /** Returns keywords representing file attributes. Returns a null if the
   *  attributes are not available. Note that this attribute is currently only
   *  available on Windows XP or later.
   */
    public java.util.HashSet<String> getFlags(){
        try{
            return getFileAttributes().getFlags();
        }
        catch(Exception e){
            return null;
        }
    }


  //**************************************************************************
  //** getFileAttributes
  //**************************************************************************
  /** Returns extended file attributes such as when the file was first created
   *  and when it was last accessed. Note that the getLastAccessTime(),
   *  getLastAccessTime(), and getLastWriteTime() in the File class all call
   *  this method. With each method, a call is made to the underlying file
   *  system to instantiate the FileAttributes class. It is therefore more
   *  efficient to call the getFileAttributes() method once, than to make
   *  separate calls to getLastAccessTime(), getLastAccessTime(), and
   *  getLastWriteTime().
   */
    public FileAttributes getFileAttributes() throws Exception{
        return new FileAttributes(this);
    }


  //**************************************************************************
  //** loadDLL
  //**************************************************************************
  /** Used to load the javaxt-core.dll. Returns a boolean to indicate load
   *  status. Note that the dll is only loaded once per JVM so it should be
   *  safe to call this method multiple times.
   */
    protected static synchronized boolean loadDLL(){

      //Try to load the dll as needed. Update the
        if (isWindows){

            if (dllLoaded==null){ //haven't tried to load the dll yet...
                String jvmPlatform = System.getProperty("os.arch");
                String dllName = null;
                if (jvmPlatform.equalsIgnoreCase("x86")){
                    dllName = "javaxt-core.dll";
                }
                else if(jvmPlatform.equalsIgnoreCase("amd64")){
                    dllName = "javaxt-core64.dll";
                }
                else{
                    dllLoaded = false;
                    return dllLoaded;
                }


              //Find the appropriate dll
                Jar jar = new Jar(Jar.class);
                Jar.Entry entry = jar.getEntry(null, dllName);
                java.io.File dll = entry.getFile();

              //Extract the dll next to the jar file (if necessary)
                if (dll==null){
                    dll = new java.io.File(jar.getFile().getParentFile(), dllName);
                    if (dll.exists()==false){
                        entry.extractFile(dll);
                    }
                }


                try{
                    System.load(dll.toString());
                    dllLoaded = true;
                    return dllLoaded;

                }
                catch(Exception e){
                    e.printStackTrace();

                  //Don't update the static variable to give users a chance
                  //to fix the load error.
                    return false;
                }

            }
            else{
                return dllLoaded;
            }
            
        }
        else{//not windows...
            return false;
        }
    }


    

    /** Used to track load status. Null = no load attempted, True = successfully
     * loaded the dll, False = failed to load dll (don't try again). Do not try
     * to modify the value directly. Use the loadDLL() method instead.*/
    private static Boolean dllLoaded;


    /** Simple date formatter for extended file attributes. */
    private static final java.text.SimpleDateFormat
        ftFormatter = new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS");

    /** JNI entry point to retrieve file attributes. */
    private static native long[] GetFileAttributesEx(String lpPathName) throws Exception;


    /** JNI entry point to retrieve file attributes. */
    private static native String GetTarget(String lpPathName) throws Exception;

//******************************************************************************
//**  FileAttributes Class
//******************************************************************************
/**
 *  Used to encapsulate extended file attributes. Currently only supports
 *  WIN32_FILE_ATTRIBUTE_DATA
 *
 <pre>
    typedef struct _WIN32_FILE_ATTRIBUTE_DATA {
      DWORD dwFileAttributes;
      FILETIME ftCreationTime;
      FILETIME ftLastAccessTime;
      FILETIME ftLastWriteTime;
      DWORD nFileSizeHigh;
      DWORD nFileSizeLow;
    } WIN32_FILE_ATTRIBUTE_DATA;
 </pre>
 *
 ******************************************************************************/

public class FileAttributes {

    private long dwFileAttributes;
    private java.util.Date ftCreationTime;
    private java.util.Date ftLastAccessTime;
    private java.util.Date ftLastWriteTime;
    private long nFileSizeHigh;
    private long nFileSizeLow;
    private java.util.HashSet<String> flags;

    private FileAttributes(File file) throws Exception {

        if (file.exists() && loadDLL()){

            long[] attributes = GetFileAttributesEx(file.toString());

            dwFileAttributes = attributes[0];
            ftCreationTime = ftFormatter.parse(attributes[1]+"");
            ftLastAccessTime = ftFormatter.parse(attributes[2]+"");
            ftLastWriteTime = ftFormatter.parse(attributes[3]+"");
            nFileSizeHigh = attributes[4];
            nFileSizeLow = attributes[5];

            flags = new java.util.HashSet<String>();
            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_READONLY) == FILE_ATTRIBUTE_READONLY)
            flags.add("READONLY");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_HIDDEN) == FILE_ATTRIBUTE_HIDDEN)
            flags.add("HIDDEN");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_SYSTEM) == FILE_ATTRIBUTE_SYSTEM)
            flags.add("SYSTEM");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_DIRECTORY) == FILE_ATTRIBUTE_DIRECTORY)
            flags.add("DIRECTORY");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_ARCHIVE) == FILE_ATTRIBUTE_ARCHIVE)
            flags.add("ARCHIVE");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_DEVICE) == FILE_ATTRIBUTE_DEVICE)
            flags.add("DEVICE");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_NORMAL) == FILE_ATTRIBUTE_NORMAL)
            flags.add("NORMAL");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_TEMPORARY) == FILE_ATTRIBUTE_TEMPORARY)
            flags.add("TEMPORARY");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_SPARSE_FILE) == FILE_ATTRIBUTE_SPARSE_FILE)
            flags.add("SPARSE_FILE");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_REPARSE_POINT) == FILE_ATTRIBUTE_REPARSE_POINT)
            flags.add("REPARSE_POINT");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_COMPRESSED) == FILE_ATTRIBUTE_COMPRESSED)
            flags.add("COMPRESSED");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_OFFLINE) == FILE_ATTRIBUTE_OFFLINE)
            flags.add("OFFLINE");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_NOT_CONTENT_INDEXED) == FILE_ATTRIBUTE_NOT_CONTENT_INDEXED)
            flags.add("NOT_CONTENT_INDEXED");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_ENCRYPTED) == FILE_ATTRIBUTE_ENCRYPTED)
            flags.add("ENCRYPTED");

            if (bitand(dwFileAttributes, FILE_ATTRIBUTE_VIRTUAL) == FILE_ATTRIBUTE_VIRTUAL)
            flags.add("VIRTUAL");
        }
        else{
            if (!file.exists()) throw new Exception("File not found.");
            if (!isWindows) throw new Exception("FileAttributes are only available on Windows.");
            if (!loadDLL() && isWindows) throw new Exception("Failed to load FileAttributes.dll.");
        }
        
    }

    public java.util.Date getCreationTime(){
        return ftCreationTime;
    }
    public java.util.Date getLastAccessTime(){
        return ftLastAccessTime;
    }
    public java.util.Date getLastWriteTime(){
        return ftLastWriteTime;
    }

    public java.util.HashSet<String> getFlags(){
        return flags;
    }

  /** A file that is read-only. Applications can read the file, but cannot
   *  write to it or delete it. This attribute is not honored on directories.
   *  For more information, see You cannot view or change the Read-only or the
   *  System attributes of folders in Windows Server 2003, in Windows XP, in
   *  Windows Vista or in Windows 7.
   */
    private static final int FILE_ATTRIBUTE_READONLY  = 1;


    private static final int FILE_ATTRIBUTE_HIDDEN    = 2;   //The file or directory is hidden. It is not included in an ordinary directory listing.

    private static final int FILE_ATTRIBUTE_SYSTEM    = 4;   //A file or directory that the operating system uses a part of, or uses exclusively.

    private static final int FILE_ATTRIBUTE_DIRECTORY = 16;  //The handle that identifies a directory.

    private static final int FILE_ATTRIBUTE_ARCHIVE   = 32;  //A file or directory that is an archive file or directory.
                                                //Applications typically use this attribute to mark files for backup or removal.

    private static final int FILE_ATTRIBUTE_DEVICE    = 64;  //This value is reserved for system use.

  /** A file that does not have other attributes set. This attribute is valid
   *  only when used alone.
   */
    private static final int FILE_ATTRIBUTE_NORMAL    = 128;

  /** A file that is being used for temporary storage. File systems avoid
   *  writing data back to mass storage if sufficient cache memory is available,
   *  because typically, an application deletes a temporary file after the
   *  handle is closed. In that scenario, the system can entirely avoid writing
   *  the data. Otherwise, the data is written after the handle is closed.
   */
    private static final int FILE_ATTRIBUTE_TEMPORARY = 256;

    private static final int FILE_ATTRIBUTE_SPARSE_FILE  = 512; //A file that is a sparse file.


  /** A file or directory that has an associated reparse point, or a file that
   *  is a symbolic link.
   */
    private static final int FILE_ATTRIBUTE_REPARSE_POINT = 1024; 

    private static final int FILE_ATTRIBUTE_COMPRESSED = 2048; //A file or directory that is compressed. For a file, all
                                                  //of the data in the file is compressed. For a directory,
                                                  //compression is the default for newly created files and subdirectories.

    private static final int FILE_ATTRIBUTE_OFFLINE  = 4096;   //The data of a file is not available immediately. This attribute
                                                  //indicates that the file data is physically moved to offline storage.
                                                  //This attribute is used by Remote Storage, which is the hierarchical
                                                  //storage management software. Applications should not arbitrarily change
                                                  //this attribute.

    private static final int FILE_ATTRIBUTE_NOT_CONTENT_INDEXED = 8192; //The file or directory is not to be indexed by the content indexing service.

    private static final int FILE_ATTRIBUTE_ENCRYPTED = 16384; //A file or directory that is encrypted. For a file, all data streams in
                                                  //the file are encrypted. For a directory, encryption is the default for
                                                  //newly created files and subdirectories.

    private static final int FILE_ATTRIBUTE_VIRTUAL = 65536;  //This value is reserved for system use.




    private long bitand (long Number1, long Number2){
        try {
            return Number1 & Number2;
        }
        catch (Exception e) {
            return -1;
        }
    }


}// End FileAttributes Class


//******************************************************************************
//**  LnkParser Class
//******************************************************************************
/**
 *   Class used to parse a windows lnk files (aka shortcuts). It is based on a
 *   document called "The Windows Shortcut File Format as reverse-engineered by
 *   Jesse Hager jesseha...@iname.com Document Version 1.0." and code by
 *   Dan Andrews dan.and...@home.com.
 *
 *   That document may be found at "http://www.wotsit.org/" and the original
 *   code can be found here:
 *   "http://groups.google.com/group/comp.lang.java.help/browse_thread/thread/a2e147b07d5480a2/"
 *
 ******************************************************************************/

public class LnkParser {

    private java.io.File file;

    public LnkParser(String lnk) {
        this(new javaxt.io.File(lnk));
    }

    public LnkParser(java.io.File lnk) {
        this(new javaxt.io.File(lnk));
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Instantiates the class by parsing the Windows shortcut (lnk) file.
   *  @param fName fileName or full path name to the shortcut file
   */
    public LnkParser(javaxt.io.File lnk) {

        if (!lnk.getExtension().equalsIgnoreCase("lnk") || !lnk.exists()) return;

        try {
          BufferedInputStream in = new BufferedInputStream(lnk.getInputStream());
          int ch = -1;

        //First 4 bytes is the letter L
          byte b[] = new byte[4];
          ch = in.read(b);
          //System.out.println(new String(b));


        //GUID
          b = new byte[16];
          ch = in.read(b);



        //Flags
          b = new byte[4];
          ch = in.read(b);
          //parseFlags(b);



        //File Attributes
          b = new byte[4];
          ch = in.read(b);
          //parseFileAttributes(b);



        //Creation time, Modification time, Last access time
          b = new byte[8*3];
          ch = in.read(b);


        //The length of the target file.
          b = new byte[4];
          ch = in.read(b);

          //System.out.println(java.nio.ByteBuffer.wrap(b).getInt());

        //Icon Number
          b = new byte[4];
          ch = in.read(b);


        //ShowWnd
          b = new byte[4];
          ch = in.read(b);


        //HotKey
          b = new byte[4];
          ch = in.read(b);


        //Reserved
          b = new byte[8];
          ch = in.read(b);



        //Get length of the The Shell Item Id List.
          int lenShellItemList = in.read();



        //Skip the Shell Item Id List and Jump to the File Location Info
          in.read(new byte[lenShellItemList]);
          in.read();



        //Skip first 2 entries in the File Location Table
          b = new byte[8];
          in.read(b);


        //Volume flags
          b = new byte[4];
          in.read(b);


        //Offset of local volume info
          b = new byte[4];
          in.read(b);



        //Offset of base pathname on local system
          b = new byte[4];
          in.read(b);
          int offsetBasePathName = in.read();
          //System.out.println("offsetBasePathName: " + offsetBasePathName);


          if (ch < 0) return;



        //Offset of network volume info
          for (int i=0; i<4; i++){
            ch = in.read();
            offsetBasePathName--;
          }
          int offsetNetworkVolumeInfo = ch;
          //System.out.println("offsetNetworkVolumeInfo: " + offsetNetworkVolumeInfo);



        //Offset of remaining pathname
          for (int i=0; i<4; i++){
            ch = in.read();
            offsetBasePathName--;
          }
          int offLocal = ch;



          //System.out.println("offLocal: " + offLocal);
          if (offLocal < 0) return;


          byte loc[];
          int index;

        //Get Base Path Name
          String BasePathName = null;
          if (offsetBasePathName>0){
              for (int i = 0; i < offsetBasePathName; i++) {
                ch = in.read();
                offLocal--;
              }
              loc = new byte[256];
              index = 0;
              loc[index++] = (byte)ch;
              while ( (ch = in.read()) != 0) {
                loc[index++] = (byte)ch;
                offLocal--;
              }
              BasePathName = new String(loc);
              BasePathName = BasePathName.trim();

          }



        //Get local pathname
          for (int i = 0; i < offLocal-1; i++) {
            ch = in.read();
          }
          loc = new byte[256];
          index = 0;
          loc[index++] = (byte)ch;
          while ( (ch = in.read()) != 0) {
            loc[index++] = (byte)ch;
          }
          String local = new String(loc);
          local = local.trim();
          //System.out.println("LocalPathName: " + local);



          if (BasePathName!=null){
              this.file = new java.io.File(BasePathName, local);
          }
          else{
              this.file = new java.io.File(local);
          }

          if (!this.file.exists()) this.file = null;

        }
        catch (IOException e) {
          e.printStackTrace();
        }
    }

    public java.io.File getFile(){
        return file;
    }

    public String toString(){
        return file.toString();
    }



  /**
   * Reports with good probability that this is a link
   * @returns true if it is likley a link
   *//*
    public boolean isLink() {
        if (file == null)
            return false;

        // check for valid "drive letter and :\"
        String drives = "abcdefghijklmnopqrstuvwxyz";
        String drive = local.substring(0,1).toLowerCase();
        if (drives.indexOf(drive) < 0) {
          //if (debug) System.out.println("not a drive");
          drives = null;
          drive = null;
          return false;
        }
        drives = null;
        drive = null;
        if (! local.substring(1,3).equals(":" + File.separator)) {
          //if (debug) System.out.println("Not found :\\\" ");
          return false;
        }

        // check for any invalid characters
        String winInvalids[] = {"/", "*", "?", "\"", "<", ">", "|"};
        for (int i = 0; i < winInvalids.length; i++) {
          if (getFullPath().indexOf(winInvalids[i]) >= 0) {
            return false;
          }
        }
        winInvalids = null;

        // check for funny ascii values
        char chars[] = getFullPath().toCharArray();
        for (int i = 0; i < chars.length; i++) {
          if ( (chars[i] < 32) || (chars[i] > 126) ) {
            chars = null;
            return false;
          }
        }
        chars = null;
        return true;
    }
*/


  //**************************************************************************
  //** getBit
  //**************************************************************************
  /** Used to return a bit from a byte array
   */
    private int getBit(byte[] data, int pos) {
        int posByte = pos/8;
        int posBit = pos%8;
        byte valByte = data[posByte];
        int valInt = valByte>>(8-(posBit+1)) & 0x0001;
        return valInt;
    }



  //**************************************************************************
  //** parseFlags
  //**************************************************************************
  /** Used to parse the Flags in the header
   */
    private void parseFlags(byte[] data){
        for (int i=0; i<data.length*8; i++){
            int val = getBit(data, i);


            switch (i) {
                case 0:{
                    if (val==1) System.out.println("The shell item id list is present.");
                    else System.out.println("The shell item id list is absent.");
                    break;
                }
                case 1:{
                    if (val==1) System.out.println("Points to a file or directory.");
                    else System.out.println("Points to something else.");
                    break;
                }
                case 2:{
                    if (val==1) System.out.println("Has a description string.");
                    else System.out.println("No description string.");
                    break;
                }
                case 3:{
                    if (val==1) System.out.println("Has a relative path string.");
                    else System.out.println("No relative path.");
                    break;
                }
                case 4:{
                    if (val==1) System.out.println("Has a working directory.");
                    else System.out.println("No working directory.");
                    break;
                }
                case 5:{
                    if (val==1) System.out.println("Has command line arguments.");
                    else System.out.println("No command line arguments.");
                    break;
                }
                case 6:{
                    if (val==1) System.out.println("Has a custom icon.");
                    else System.out.println("Has the default icon.");
                    break;
                }
                default:{
                    break;//System.out.println(val);
                }
            }

        }
    }

  //**************************************************************************
  //** parseFileAttributes
  //**************************************************************************
  /** Used to parse the Flags in the header
   */
    private void parseFileAttributes(byte[] data){
        for (int i=0; i<data.length*8; i++){
            boolean val = (getBit(data, i) != 0);


            switch (i) {
                case 0 :{
                    System.out.println("Target is read only. " + val);
                    break;
                }
                case 1 :{
                    System.out.println("Target is hidden. " + val);
                    break;
                }
                case 2 :{
                    System.out.println("Target is a system file. " + val);
                    break;
                }
                case 3 :{
                    System.out.println("Target is a volume label. " + val);
                    break;
                }
                case 4 :{
                    System.out.println("Target is a directory. " + val);
                    break;
                }
                case 5 :{
                    System.out.println("Target has been modified since last backup. " + val);
                    break;
                }
                case 6 :{
                    System.out.println("Target is encrypted (NTFS EFS) " + val);
                    break;
                }
                case 7 :{
                    System.out.println("Target is Normal? " + val);
                    break;
                }
                case 8 :{
                    System.out.println("Target is temporary. " + val);
                    break;
                }
                case 9 :{
                    System.out.println("Target is a sparse file. " + val);
                    break;
                }
                case 10 :{
                    System.out.println("Target has reparse point data. " + val);
                    break;
                }
                case 11 :{
                    System.out.println("Target is compressed. " + val);
                    break;
                }
                case 12 :{
                    System.out.println("Target is offline. " + val);
                    break;
                }
                default:{
                    break;//System.out.println(val);
                }
            }

        }
    }

}//End LnkParser Inner Class
}//End File Class

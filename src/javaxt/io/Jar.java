package javaxt.io;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;

//Imports for dealing with ZIP files
import java.net.URI;
import java.util.zip.*;

//******************************************************************************
//**  Jar Class - By Peter Borissow
//******************************************************************************
/**
 *   Used to find entries in a jar file associated with a given class or 
 *   package. Note that jar files are unzipped when deployed on an app server.
 *   This class is designed to handle both zipped and unzipped jar files.
 *
 *   The original motivation behind this class was to support a requirement to 
 *   extract and update config files stored in Java packages. For console apps, 
 *   the config file is stored in the jar (zip) file. For web apps, chances are 
 *   that the package has been un-zipped and the config file is laying around
 *   on disk. This class was designed to support both use cases.
 *
 ******************************************************************************/

public class Jar {
    
    private java.io.File file;
    private java.lang.Package Package;
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /**  Creates a new instance of Jar */
    
    public Jar(java.lang.Object object){
        this(object.getClass());
    }
    
    public Jar(java.lang.Class Class) {
        this(Class.getPackage());
    }
    
    public Jar(java.lang.Package Package) {

        this.Package = Package;
        String path = Package.getName().replace((CharSequence)".",(CharSequence)"/");
        String url = this.getClass().getClassLoader().getResource(path).toString();
        url = url.replace((CharSequence)" ",(CharSequence)"%20");
        try{
            java.net.URI uri = new java.net.URI(url);
            if (uri.getPath()==null){
                path = uri.toString();        
                if (path.startsWith("jar:file:")){

                  //Update Path and Define Zipped File
                    path = path.substring(path.indexOf("file:/"));
                    path = path.substring(0,path.toLowerCase().indexOf(".jar")+4);
                    
                    if (path.startsWith("file://")){ //UNC Path
                        path = "C:/" + path.substring(path.indexOf("file:/")+7);
                        path = "/" + new URI(path).getPath();
                    }
                    else{
                        path = new URI(path).getPath();
                    }
                    this.file = new java.io.File(path);                    
                }
            }
            else{
                this.file = new java.io.File(uri);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        
    }

    public Jar(java.io.File file){
        this.file = file;
    }
    
    
  //**************************************************************************
  //** getFile
  //**************************************************************************
  /**  Returns a java.io.File representation of the jar file or directory where 
   *   the jar file has been extracted.
   */
    
    public java.io.File getFile(){
        return file;
    }


  //**************************************************************************
  //** getManifest
  //**************************************************************************
  /**  Returns the Manifest file found in the "META-INF" directory. The
   *   Manifest file contains metadata for the jar file including version
   *   numbers, vendor name, etc. You can loop through properties in the
   *   Manifest like this:
   <pre>
    java.io.File file = new java.io.File("/Drivers/h2/h2-1.3.162.jar");
    java.util.jar.JarFile jar = new javaxt.io.Jar(file);
    java.util.jar.Manifest manifest = jar.getManifest();

    System.out.println("\r\nMain Attributes:\r\n--------------------------");
    printAttributes(manifest.getMainAttributes());


    System.out.println("\r\nOther Attributes:\r\n--------------------------");
    java.util.Map&lt;String, java.util.jar.Attributes&gt; entries = manifest.getEntries();
    java.util.Iterator&lt;String&gt; it = entries.keySet().iterator();
    while (it.hasNext()){
        String key = it.next();
        printAttributes(entries.get(key));
        System.out.println();
    }

    jar.close();

    private static void printAttributes(java.util.jar.Attributes attributes){
        java.util.Iterator it = attributes.keySet().iterator();
        while (it.hasNext()){
            java.util.jar.Attributes.Name key = (java.util.jar.Attributes.Name) it.next();
            Object value = attributes.get(key);
            System.out.println(key + ":  " + value);
        }
    }
   </pre>
   */
    public java.util.jar.Manifest getManifest(){
        try{
            Entry entry = this.getEntry("META-INF", "MANIFEST.MF");
            if (entry!=null) {
                ByteArrayInputStream is = new ByteArrayInputStream(entry.getBytes());
                java.util.jar.Manifest manifest = new java.util.jar.Manifest(is);
                is.close();
                return manifest;
            }
        }
        catch(Exception e){
        }
        return null;
    }

    
  //**************************************************************************
  //** getVersion
  //**************************************************************************
  /** Returns the version number of the jar file, if available. Two different
   *  strategies are used to find the version number. First strategy is to
   *  parse the jar file manifest and return the value of the
   *  "Implementation-Version" or "Bundle-Version", whichever is found first.
   *  If no version information is found in the manifest, an attempt is made
   *  to parse the file name. Returns a null is no version information is
   *  available.
   */
    public String getVersion(){

        java.util.jar.Attributes attributes = getManifest().getMainAttributes();
        if (attributes!=null){
            java.util.Iterator it = attributes.keySet().iterator();
            while (it.hasNext()){
                java.util.jar.Attributes.Name key = (java.util.jar.Attributes.Name) it.next();
                String keyword = key.toString();
                if (keyword.equals("Implementation-Version") || keyword.equals("Bundle-Version")){
                    return (String) attributes.get(key);
                }
            }
        }

        String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
        if (fileName.contains(".")){
            String majorVersion = fileName.substring(0, fileName.indexOf("."));
            int delimiter = majorVersion.lastIndexOf("-");
            if (majorVersion.indexOf("_")>delimiter) delimiter = majorVersion.indexOf("_");
            majorVersion = majorVersion.substring(delimiter+1, fileName.indexOf("."));
            String minorVersion = fileName.substring(fileName.indexOf("."));
            return majorVersion + minorVersion;
        }

        return null;
    }
    
    
  //**************************************************************************
  //** getEntries
  //**************************************************************************
  /**  Used to return a list of all the entries found in the jar file.
   */    
    public Entry[] getEntries(){
        java.util.ArrayList<Entry> entries = new java.util.ArrayList<Entry>();
        try{
            
            if (file.isDirectory()){
                Directory dir = new Directory(file);
                java.util.List items = dir.getChildren(true);
                for (int i=0; i<items.size(); i++){
                     Object item = items.get(i);
                     if (item instanceof File){
                         entries.add(new Entry(((File) item).toFile()));
                     }
                }
            }
            else{
                ZipInputStream in = new ZipInputStream(new FileInputStream(file));
                ZipEntry zipEntry = null;
                while((zipEntry = in.getNextEntry())!=null){
                    entries.add(new Entry(zipEntry));
                }
                in.close();
            }
        }
        catch(Exception e){
        }
        return entries.toArray(new Entry[entries.size()]);
    }
    
    
  //**************************************************************************
  //** getEntry
  //**************************************************************************
  /**  Used to retrieve a single entry from the jar file. */
    
    public Entry getEntry(String Entry){
        return getEntry(Package.getName(),Entry);
    }
    
    
  //**************************************************************************
  //** getEntry
  //**************************************************************************
  /**  Used to retrieve a single entry from the jar file. 
   *  @param Package Name of the package or directory in the jar file 
   *  (e.g. "javaxt.io"). Null values and zero length strings default to the
   *  the root directory. 
   *  @param Entry Name of the class/file found in the given package  
   *  (e.g. "Jar.class").
   */
    public Entry getEntry(String Package, String Entry){
        
        try{
            
            if (file.isDirectory()){
                return new Entry(new java.io.File(file, Entry));
            }
            else{
            
              //Update package name and entry
                if (Package!=null){
                    Package = Package.trim();
                    if (Package.length()==0) Package = null;
                }
                if (Package!=null){
                    if (Package.contains(".")) Package = Package.replace(".","/");
                    Entry = Package + "/" + Entry;
                }


              //Find entry in the jar file
                ZipInputStream in = new ZipInputStream(new FileInputStream(file));
                ZipEntry zipEntry = null;
                while((zipEntry = in.getNextEntry())!=null){
                    if (zipEntry.getName().equalsIgnoreCase(Entry)){
                        //System.out.println(zipEntry.getName() + " <--");
                        Entry entry = new Entry(zipEntry);
                        in.close();
                        return entry;
                    }
                }
                in.close();
            }
        }
        catch(Exception e){
            //e.printStackTrace();
        }
        
        return null;
    }

    
  //**************************************************************************
  //** getEntries
  //**************************************************************************
  /**  Used to retrieve a single entry from the jar file. */
    
    public Entry getEntry(java.lang.Class Class) {
        String ClassName = Class.getName();
        String PackageName = Class.getPackage().getName();
        ClassName = ClassName.substring(PackageName.length()+1);
        return getEntry(PackageName,ClassName+".class");
    }
    
    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /**  Returns the path to the jar file. */
    
    public String toString(){
        return file.toString();
    }
    
    
  //**************************************************************************
  //** JAR Entry Class
  //**************************************************************************
  /** Used to represent a single entry in a jar file. */  
    
    public class Entry{
        private ZipEntry zipEntry = null;
        private java.io.File fileEntry = null;
        
      /** Constructor for zipped jar files. */
        private Entry(ZipEntry zipEntry){
            this.zipEntry = zipEntry;
        }
        
      /** Constructor for unzipped jar files. */  
        private Entry(java.io.File fileEntry){
            this.fileEntry = fileEntry;
        }
        
        
        public java.io.File getFile(){
            return fileEntry;
        }


        public byte[] getBytes(){
            try{
                ZipFile zip = new ZipFile(file);
                if (fileEntry==null){
                    
                    java.io.DataInputStream is = new java.io.DataInputStream(zip.getInputStream(zipEntry));

                    int bufferSize = 1024;
                    ByteArrayOutputStream bas = new ByteArrayOutputStream();
                    byte[] b = new byte[bufferSize];
                    int x=0;
                    while((x=is.read(b,0,bufferSize))>-1) {
                        bas.write(b,0,x);
                    }
                    bas.close();

                    zip.close();
                    return bas.toByteArray();
                }
                else{
                    byte[] b = new byte[(int)fileEntry.length()];
                    java.io.DataInputStream is = new java.io.DataInputStream(new FileInputStream(fileEntry));
                    is.readFully(b, 0, b.length);
                    is.close();
                    zip.close();
                    return b;
                }
            }
            catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }


      /** Used to extract the zip entry to a file. */
        public void extractFile(java.io.File destination){
            try{
                if (fileEntry==null){
                    FileOutputStream out = new FileOutputStream(destination);
                    ZipInputStream in = new ZipInputStream(new FileInputStream(file));
                    ZipEntry zipEntry = null;
                    while((zipEntry = in.getNextEntry())!=null){
                        if (zipEntry.getName().equals(this.zipEntry.getName())){

                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            break;
                        }
                    }
                    in.close();
                    out.close();
                }
                else{
                    
                  //Simply copy the file to the destination
                    if (destination.isFile()){
                        new File(fileEntry).copyTo(new File(destination),false);
                    }
                    else{
                        new File(fileEntry).copyTo(new Directory(destination),false);
                    }
                }
            }
            catch(Exception e){
                
            }
        }
        
        
        public void setText(String text){
            try{
                if (fileEntry==null){
                    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                    ByteArrayInputStream byteInput = new ByteArrayInputStream(text.getBytes());
                    ZipOutputStream zipOutput = new ZipOutputStream(byteOutput);
                    ZipInputStream zipInput = new ZipInputStream(new FileInputStream(file));
                    
                    
                    ZipEntry zipEntry = null;
                    while((zipEntry = zipInput.getNextEntry())!=null){
                        
                        if (zipEntry.getName().equals(this.zipEntry.getName())){
                            
                          //Write Updated Config File
                            zipOutput.putNextEntry(new ZipEntry(this.zipEntry.getName()));
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = byteInput.read(buf)) > 0) {
                                zipOutput.write(buf, 0, len);
                            }
                            byteInput.close();
                        }
                        else{
                            zipOutput.putNextEntry(zipEntry);
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = zipInput.read(buf)) > 0) {
                                zipOutput.write(buf, 0, len);
                            }
                        }
                        
                        zipInput.closeEntry();
                        zipOutput.closeEntry();
                    }
                    
                    zipInput.close();
                    zipOutput.close();


                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(byteOutput.toByteArray());
                    fos.close();
                    
                    byteOutput.close();
                    
                }
                else{
                    new File(fileEntry).write(text);
                }
            }
            catch(Exception e){
            }
        }
        
      /** Used to extract the contents to a string. */  
        public String getText(){
            try{
                if (fileEntry==null){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ZipInputStream in = new ZipInputStream(new FileInputStream(file));
                    ZipEntry zipEntry = null;
                    while((zipEntry = in.getNextEntry())!=null){
                        if (zipEntry.getName().equals(this.zipEntry.getName())){

                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            break;
                        }
                    }
                    in.close();
                    return out.toString();
                }
                else{
                    return new File(fileEntry).getText();
                }
            }
            catch(Exception e){
                return null;
            }

        }
        
        public String toString(){
            return getText();
        }
    }
}

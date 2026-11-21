package javaxt.io;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

//Imports for dealing with ZIP files
import java.net.URI;
import java.util.zip.*;

//******************************************************************************
//**  Jar Class
//******************************************************************************
/**
 *   Used to find entries in a jar file associated with a given class or
 *   package.
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
  /** Creates a new instance of this class using an instance of another class.
   */
    public Jar(java.lang.Object object){
        this(object.getClass());
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a Class.
   */
    public Jar(java.lang.Class Class) {
        this.Package = Class.getPackage();
        String path = Class.getName();
        if (path!=null) path = path.replace((CharSequence)".",(CharSequence)"/") + ".class";
        java.util.ArrayList<java.net.URL> urls = getResource(path, Class.getClassLoader());
        if (urls.size()==1){
            file = getFile(urls.get(0));
        }
        /*
        else if (urls.size()>1){ //Should never happen!

            for (java.net.URL url : urls){

                java.net.JarURLConnection urlcon = (java.net.JarURLConnection) (url.openConnection());
                java.util.jar.JarFile jar = urlcon.getJarFile();
                java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    //System.out.println(entry);
                }
            }
        }
        */
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a Package. This method is
   *  somewhat unreliable if there are multiple jar files that contain the
   *  same package name. Suggest using the getJars() method instead.
   */
    public Jar(java.lang.Package Package) {
        this.Package = Package;
        String path = Package.getName().replace((CharSequence)".",(CharSequence)"/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (java.net.URL url : getResource(path, classLoader)){
            file = getFile(url);
            if (file!=null) break;
        }
    }


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a path to a jar file or
   *  directory.
   */
    public Jar(java.io.File file){
        this.file = file;
    }


  //**************************************************************************
  //** getJars
  //**************************************************************************
  /** Returns an array of files or directories associated with a given Package.
   *  This method should be used instead of new Jar(java.lang.Package).
   */
    public static Jar[] getJars(java.lang.Package Package){
        return getJars(Package.getName());
    }

    public static Jar[] getJars(String packageName){
        java.util.ArrayList<Jar> jars = new java.util.ArrayList<Jar>();
        String path = packageName.replace((CharSequence)".",(CharSequence)"/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (java.net.URL url : getResource(path, classLoader)){
            java.io.File file = getFile(url);
            if (file!=null) jars.add(new Jar(file));
        }
        return jars.toArray(new Jar[jars.size()]);
    }

  //**************************************************************************
  //** getFile
  //**************************************************************************
  /** Returns a java.io.File representation of the jar file or directory where
   *  the jar file has been extracted.
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
    java.util.Map<String, java.util.jar.Attributes> entries = manifest.getEntries();
    java.util.Iterator<String> it = entries.keySet().iterator();
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
        Entry entry = this.getEntry("META-INF", "MANIFEST.MF");
        if (entry==null) return null;
        try (java.io.InputStream is = entry.getInputStream()){
            if (is==null) return null;
            return new java.util.jar.Manifest(is);
        }
        catch(Exception e){
            return null;
        }
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
        final java.util.ArrayList<Entry> entries = new java.util.ArrayList<>();
        java.io.File root = getRoot();
        if (root==null) return new Entry[0];

        if (root.isDirectory()){

          //All the files under the root directory
            for (Object item : new Directory(root).getChildren(true)){
                if (item instanceof File) entries.add(new Entry(((File) item).toFile()));
            }
        }
        else{

          //All the entries in the jar file. ZipFile reads the jar's central
          //directory, so entry sizes and checksums are known (unlike when
          //scanning the file with a ZipInputStream).
            try (ZipFile zip = new ZipFile(root)){
                java.util.Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()){
                    entries.add(new Entry(en.nextElement()));
                }
            }
            catch(Exception e){
            }
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
        if (Entry==null) return null;


      //Update package name and entry
        if (Package!=null){
            Package = Package.trim();
            if (Package.length()==0) Package = null;
        }
        if (Package!=null){
            if (Package.contains(".")) Package = Package.replace(".","/");
            Entry = Package + "/" + Entry;
        }


      //Classes in a directory: the entry is a file under the root of the
      //classpath (it may not exist yet, e.g. a config file to create, see
      //Entry.setText)
        java.io.File root = getRoot();
        if (root==null) return null;
        if (root.isDirectory()){
            return new Entry(new java.io.File(root, Entry));
        }


      //Find entry in the jar file (names are compared case-insensitively)
        try (ZipFile zip = new ZipFile(root)){
            ZipEntry zipEntry = zip.getEntry(Entry);
            if (zipEntry!=null) return new Entry(zipEntry);

            java.util.Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()){
                zipEntry = en.nextElement();
                if (zipEntry.getName().equalsIgnoreCase(Entry)){
                    return new Entry(zipEntry);
                }
            }
        }
        catch(Exception e){
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
  //** getClasses
  //**************************************************************************
  /** Returns all the classes in the jar file. Returns an empty array if the
   *  jar file has not been loaded or if there are no classes in the file.
   */
    public Class[] getClasses(){
        java.util.ArrayList<Class> classes = new java.util.ArrayList<Class>();
        java.net.URLClassLoader child = null;
        for (Jar.Entry entry : getEntries()){
            String name = entry.getName();
            if (name.endsWith(".class")){
                name = name.substring(0, name.length()-6).replace("/", ".");
                try{
                    classes.add(Class.forName(name));
                }
                catch(Exception e){
                    try{
                        if (child==null){

                          //Note: this class loader is never closed, on
                          //purpose. Classes it loads need it to stay open.
                            child = new java.net.URLClassLoader(
                                new java.net.URL[] {file.toURI().toURL()},
                                this.getClass().getClassLoader()
                            );
                        }
                        classes.add(Class.forName(name, true, child));
                    }
                    catch(Exception e2){
                        //e2.printStackTrace();
                    }
                }
            }
        }
        return classes.toArray(new Class[classes.size()]);
    }


  //**************************************************************************
  //** getRoot
  //**************************************************************************
  /** Returns the jar file, or the root of the classpath directory if the
   *  classes aren't in a jar (e.g. unit tests, or an IDE running classes
   *  from a build directory).
   */
    public java.io.File getRoot(){
        if (file==null) return null;

      //Jar file
        if (file.isFile() && !file.getName().toLowerCase().endsWith(".class")) return file;

      //Directory
        java.io.File dir = file.isDirectory() ? file : file.getParentFile();
        if (Package==null || dir==null) return dir;

      //If we're still here, walk up the package folders
        String[] folders = Package.getName().split("\\.");
        java.io.File root = dir;
        for (int i=folders.length-1; i>-1; i--){
            if (root==null || !root.getName().equals(folders[i])) return dir;
            root = root.getParentFile();
        }
        return root==null ? dir : root;
    }


  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns the path to the jar file.
   */
    public String toString(){
        return file.toString();
    }


  //**************************************************************************
  //** getResource
  //**************************************************************************
  /** Returns the URL associated with a given path in the jar file.
   *  @param cl ClassLoader used to find the given path/resource. If the
   *  ClassLoader is null or fails to find the requested path, an alternate
   *  ClassLoader is used (e.g. ClassLoader's Parent, ContextClassLoader,
   *  SystemClassLoader).
   */
    private static java.util.ArrayList<java.net.URL> getResource(String path, ClassLoader cl){

        java.util.ArrayList<java.net.URL> arr = new java.util.ArrayList<java.net.URL>();
        try{

            int x = 0;
            while (true){

                if (cl==null){
                    cl = Thread.currentThread().getContextClassLoader();
                    if (cl==null){
                        cl = java.lang.ClassLoader.getSystemClassLoader();
                        if (cl==null){
                            break; //Throw Exception?
                        }
                    }
                }


                java.util.Enumeration<java.net.URL> en = cl.getResources(path);
                if (en.hasMoreElements()){
                    while (en.hasMoreElements()){
                        java.net.URL url = en.nextElement();
                        arr.add(url);
                    }

                    break;
                }
                else{
                    if (cl.getParent()!=null && cl.getParent().equals(cl)){
                        break;
                    }
                    cl = cl.getParent();
                }



                x++;
                if (x==50) break;
            }

            /*
            String debug = "";
            debug += "Path: " + path + "\r\n";
            debug += "ClassLoader: " + cl + "\r\n";
            for (java.net.URL url : arr){
                debug += "Resource: " + url + "\r\n";
            }
            System.out.println(debug);
            */

        }
        catch(Exception e){
            //e.printStackTrace();
        }

        return arr;
    }


    private static java.io.File getFile(java.net.URL url){
        if (url!=null)
        try{
            java.net.URI uri = new java.net.URI(url.toString().replace((CharSequence)" ",(CharSequence)"%20"));
            if (uri.getPath()==null){
                String path = uri.toString();


              //Special case for nexted jar files (e.g. spring boot)
                if (path.startsWith("jar:nested:")){
                    path = "jar:file:" + path.substring("jar:nested:".length());
                }


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
                    return new java.io.File(path);
                }
                else if (path.startsWith("jar:http")){
                    path = path.substring(path.indexOf("http"));
                    path = path.substring(0,path.toLowerCase().indexOf(".jar")+4);
                }
            }
            else{
                return new java.io.File(uri);
            }
        }
        catch(Exception e){
            //e.printStackTrace();
        }

        return null;
    }


  //**************************************************************************
  //** readAll
  //**************************************************************************
  /** Returns the bytes in a stream
   */
    private static byte[] readAll(java.io.InputStream is) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > -1) out.write(buf, 0, n);
        return out.toByteArray();
    }


  //**************************************************************************
  //** replace
  //**************************************************************************
  /** Replaces a file with another one (e.g. a temp file written next to it),
   *  atomically if the file system supports it.
   */
    private static void replace(java.io.File src, java.io.File dest) throws java.io.IOException {
        try{
            java.nio.file.Files.move(src.toPath(), dest.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        }
        catch(java.nio.file.AtomicMoveNotSupportedException e){
            java.nio.file.Files.move(src.toPath(), dest.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }


  //**************************************************************************
  //** JAR Entry Class
  //**************************************************************************
  /** Used to represent an entry in a jar/war file. The jar file might be
   *  zipped or unpacked by a web server.
   */
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

        public String getName(){
            if (fileEntry==null) return zipEntry.getName();
            else return fileEntry.getName();
        }

        public java.util.Date getDate(){
            if (fileEntry==null) return new java.util.Date(zipEntry.getTime());
            else return new java.util.Date(fileEntry.lastModified());
        }


      /** Returns a long value representing a cyclic redundancy check
       * (CRC-32 checksum) of the uncompressed entry data, or -1 if not known.
       */
        public long checksum(){
            if (fileEntry==null) return zipEntry.getCrc();
            if (!fileEntry.isFile()) return -1;
            return new File(fileEntry).checksum();
        }


        public java.io.File getFile(){
            return fileEntry;
        }

        public long getSize(){
            if (fileEntry==null){
                return zipEntry.getSize();
            }
            else{
                return fileEntry.length();
            }
        }


      /** Returns a stream to read the entry, or null if the entry doesn't
       *  exist (e.g. it was removed from the jar file, or a file that hasn't
       *  been created yet).
       */
        public java.io.InputStream getInputStream(){
            try{
                if (fileEntry==null){
                    final ZipFile zip = new ZipFile(file);
                    try{
                        ZipEntry entry = zip.getEntry(zipEntry.getName());
                        java.io.InputStream is = (entry==null) ? null : zip.getInputStream(entry);
                        if (is==null){
                            zip.close();
                            return null;
                        }
                        return new java.io.FilterInputStream(is){
                            public void close() throws java.io.IOException {
                                try{ super.close(); }
                                finally{ zip.close(); }
                            }
                        };
                    }
                    catch(Exception e){
                        zip.close();
                        throw e;
                    }
                }
                else{
                    if (!fileEntry.isFile()) return null;
                    return new File(fileEntry).getInputStream();
                }
            }
            catch(Exception e){
                return null;
            }
        }


      /** Returns the content of the entry, or null if the entry doesn't exist
       *  or can't be read.
       */
        public byte[] getBytes(){
            if (fileEntry!=null){
                if (!fileEntry.isFile()) return null;
                ByteArrayOutputStream bytes = new File(fileEntry).getBytes();
                return bytes==null ? null : bytes.toByteArray();
            }
            try (java.io.InputStream is = getInputStream()){
                if (is==null) return null;
                return readAll(is);
            }
            catch(Exception e){
                return null;
            }
        }


      /** Used to extract the entry to a file. If the destination is an
       *  existing directory, the entry is extracted into it (with the same
       *  name). The file is written next to the destination first, then moved
       *  into place, so an error never leaves a partial file. Nothing is
       *  written if the entry doesn't exist.
       */
        public void extractFile(java.io.File destination){
            if (destination.isDirectory()){
                destination = new java.io.File(destination, new java.io.File(getName()).getName());
            }

          //Folder: copy the file with javaxt.io.File (keeps the date)
            if (fileEntry!=null){
                if (!fileEntry.isFile()) return; //missing entry: don't create a file
                java.io.File tmp = null;
                try{
                    java.io.File dir = destination.getAbsoluteFile().getParentFile();
                    if (dir!=null) dir.mkdirs();
                    tmp = java.io.File.createTempFile("jar-", ".tmp", dir);
                    if (new File(fileEntry).copyTo(new File(tmp), true)){
                        replace(tmp, destination);
                        tmp = null;
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                finally{
                    if (tmp!=null) tmp.delete();
                }
                return;
            }

          //Jar file: stream the entry to a temp file, then replace
            java.io.File tmp = null;
            try (java.io.InputStream is = getInputStream()){
                if (is==null) return; //missing entry: don't create a file

                java.io.File dir = destination.getAbsoluteFile().getParentFile();
                if (dir!=null) dir.mkdirs();
                tmp = java.io.File.createTempFile("jar-", ".tmp", dir);
                java.nio.file.Files.copy(is, tmp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                replace(tmp, destination);
                tmp = null;
            }
            catch(Exception e){
                e.printStackTrace();
            }
            finally{
                if (tmp!=null) tmp.delete();
            }
        }


      /** Used to update/replace an entry with new text. For jar files, a new
       *  jar is written next to the original, then replaces it, so an error
       *  never leaves a partial jar. Note that a jar file can't be replaced
       *  while it's in use.
       */
        public void setText(String text){
            if (text==null) return;
            byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);


          //Directory
            if (fileEntry!=null){
                new File(fileEntry).write(bytes);
                return;
            }


          //Jar file: copy the jar to a temp file, with the new content for
          //this entry, then replace the original
            java.io.File tmp = null;
            try{
                tmp = java.io.File.createTempFile("jar-", ".tmp", file.getAbsoluteFile().getParentFile());
                boolean found = false;
                try (ZipFile zip = new ZipFile(file);
                     ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmp))){

                    java.util.Enumeration<? extends ZipEntry> en = zip.entries();
                    while (en.hasMoreElements()){
                        ZipEntry entry = en.nextElement();

                        ZipEntry copy = new ZipEntry(entry.getName());
                        copy.setTime(entry.getTime());
                        if (entry.getComment()!=null) copy.setComment(entry.getComment());
                        if (entry.getExtra()!=null) copy.setExtra(entry.getExtra());
                        out.putNextEntry(copy);

                        if (entry.getName().equals(zipEntry.getName())){
                            out.write(bytes);
                            found = true;
                        }
                        else if (!entry.isDirectory()){
                            try (java.io.InputStream is = zip.getInputStream(entry)){
                                byte[] buf = new byte[8192];
                                int n;
                                while ((n = is.read(buf)) > -1) out.write(buf, 0, n);
                            }
                        }
                        out.closeEntry();
                    }
                }

                if (found){
                    replace(tmp, file);
                    tmp = null;
                }
            }
            catch(Exception e){
            }
            finally{
                if (tmp!=null) tmp.delete();
            }
        }


      /** Used to extract the contents to a string. */
        public String getText(){
            return getText("UTF-8");
        }


      /** Used to extract the contents to a string. Returns null if the
       *  extraction failed.
       *  @param charsetName Name of the character encoding used to read the
       *  file. Examples include UTF-8 and ISO-8859-1
       */
        public String getText(String charsetName){
            byte[] bytes = getBytes();
            if (bytes==null) return null;
            try{
                return new String(bytes, charsetName);
            }
            catch(Exception e){
                return null;
            }
        }


        public String toString(){
            return getName();
        }
    }
}

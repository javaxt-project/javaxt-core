package javaxt.ntfs;
import java.io.*;

//******************************************************************************
//**  LnkParse Class
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

}
package javaxt.html;
import java.util.ArrayList;

//******************************************************************************
//**  HTML Parser
//******************************************************************************
/**
 *   Used to parse HTML documents and fragments and find DOM elements
 *
 ******************************************************************************/

public class Parser {

    private String html;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Parser(String html){
        this.html = html;
    }


  //**************************************************************************
  //** getHTML
  //**************************************************************************
    public String getHTML(){
        return html;
    }


  //**************************************************************************
  //** setHTML
  //**************************************************************************
  /** Used to reset the "scope" of the parser
   */
    public void setHTML(String html){
        this.html = html;
    }


  //**************************************************************************
  //** getElementByID
  //**************************************************************************
  /** Returns an HTML Element with a given id. Returns null if the element was
   *  not found.
   */
    public Element getElementByID(String id){
        return getElementByAttributes(null, "id", id);
    }


  //**************************************************************************
  //** getElementByTagName
  //**************************************************************************
  /** Returns an array of HTML Elements found in the HTML document with given
   *  tag name.
   */
    public Element[] getElementsByTagName(String tagName){
        String orgHTML = html;
        ArrayList<Element> elements = new ArrayList<Element>();
        Element e = getElementByTagName(tagName);
        while (e!=null){
            elements.add(e);
            String outerHTML = e.getOuterHTML();
            int idx = html.indexOf(outerHTML);
            String a = html.substring(0, idx);
            String b = html.substring(idx+outerHTML.length());
            html = a + b;
            e = getElementByTagName(tagName);
        }

        html = orgHTML;
        return elements.toArray(new Element[elements.size()]);
    }


  //**************************************************************************
  //** getElements
  //**************************************************************************
  /** Returns an array of top-level HTML Elements found in the HTML document
   */
    public Element[] getElements(){
        ArrayList<Element> elements = getElements(html);
        return elements.toArray(new Element[elements.size()]);
    }


  //**************************************************************************
  //** getElementByTagName
  //**************************************************************************
  /** Returns an array of HTML Elements found in the HTML document with given
   *  tag name, attribute, and attribute value (e.g. "div", "class", "hdr2").
   */
    public Element[] getElements(String tagName, String attributeName, String attributeValue){
        String orgHTML = html;
        ArrayList<Element> elements = new ArrayList<Element>();
        Element e = getElementByAttributes(tagName, attributeName, attributeValue);
        while (e!=null){
            elements.add(e);
            String outerHTML = e.getOuterHTML();
            int idx = html.indexOf(outerHTML);
            String a = html.substring(0, idx);
            String b = html.substring(idx+outerHTML.length());
            html = a + b;
            e = getElementByAttributes(tagName, attributeName, attributeValue);
        }

        html = orgHTML;
        return elements.toArray(new Element[elements.size()]);
    }


  //**************************************************************************
  //** getElementByTagName
  //**************************************************************************
  /** Returns the first HTML Element found in the HTML document with given tag
   *  name. Returns null if an element was not found.
   */
    public Element getElementByTagName(String tagName){
        return getElementByAttributes(tagName, null, null);
    }


  //**************************************************************************
  //** getElementByAttributes
  //**************************************************************************
  /** Returns the first HTML Element found in the HTML document with given tag
   *  name and attribute. Returns null if an element was not found.
   */
    public Element getElementByAttributes(String tagName, String attributeName, String attributeValue){
        Element[] elements = getElements();
        for (Element element : elements){
            if (hasAttributes(tagName, attributeName, attributeValue, element)){
                return element;
            }
        }

        for (Element element : elements){
            String n = element.getName();
            if (n!=null){
                if (!n.equalsIgnoreCase("script")){
                    Element e = getElementByAttributes(tagName, attributeName, attributeValue, element);
                    if (e!=null) return e;
                }
            }
        }

        return null;
    }

    private Element getElementByAttributes(String tagName, String attributeName, String attributeValue, Element element){
        Element[] elements = element.getChildNodes();
        for (Element e : elements){
            if (hasAttributes(tagName, attributeName, attributeValue, e)){
                return e;
            }
        }

        for (Element e : elements){
            String n = element.getName();
            if (n!=null){
                if (!n.equalsIgnoreCase("script")){
                    Element el = getElementByAttributes(tagName, attributeName, attributeValue, e);
                    if (el!=null) return el;
                }
            }
        }

        return null;
    }


  //**************************************************************************
  //** hasAttributes
  //**************************************************************************
  /** Returns true if the given element matches the tagName, attributeName,
   *  and attributeValue
   */
    private boolean hasAttributes(String tagName, String attributeName, String attributeValue, Element element){

        String name = element.getName();
        if (name==null) return false;

        if (name.equalsIgnoreCase(tagName) || tagName==null){

            if (attributeName==null) return true;

            String val = element.getAttribute(attributeName);
            if (val==null){
                if (attributeValue==null) return true;
            }
            else{
                if (val.equals(attributeValue)){
                    return true;
                }
            }
        }
        return false;
    }


  //**************************************************************************
  //** getImageLinks
  //**************************************************************************
  /** Returns a list of links to images. The links may include relative paths.
   *  Use the getAbsolutePath method to resolve the relative paths to a fully
   *  qualified url.
   */
    public String[] getImageLinks(){
        ArrayList<String> links = new ArrayList<String>();
        for (Element img : getElementsByTagName("img")){
            String src = img.getAttribute("src");
            if (src.length()>0) links.add(src);
        }
        return links.toArray(new String[links.size()]);
    }


  //**************************************************************************
  //** stripHTMLTags
  //**************************************************************************
  /** Used to remove any html tags from a block of text
   */
    public static String stripHTMLTags(String html){

        String s = html + " ";
        String c = "";
        boolean concat = false;
        String tag = "";

        for (int i = 0; i < s.length(); i++){

             c = s.substring(i,i+1);

             if (c.equals("<")){
                 concat = true;
             }


             if (concat==true){
                 tag += c;
             }


             if (c.equals(">") && concat==true){
                 concat = false;

                 html = html.replace(tag,"");

               //Clear tag variable for the next pass
                 tag = "";
             }

        }

        //html = html.replaceAll("\\s+"," ");

        return html.replace("&nbsp;", " ").trim();
    }


  //**************************************************************************
  //** MapPath
  //**************************************************************************
  /** Returns a fully qualified URL for a given path. Returns null if the
   *  function fails to resolve the path.
   *  @param relPath Relative path to a file (e.g. "../images/header.jpg")
   *  @param url URL that is sourcing the relPath (e.g. "http://acme.com/about/")
   *  @return Using the examples cited in the 2 parameters, return a URL
   *  "http://acme.com/images/header.jpg"
   */
    public static String MapPath(String relPath, java.net.URL url){

      //Check if relPath is a fully qualified URL. If so, return the relPath.
        try{
            new java.net.URL(relPath);
            return relPath;
        }
        catch(Exception e){}


      //Remove "./" prefix in the relPath
        if (relPath.length()>2){
            if (relPath.substring(0,2).equals("./")){
                relPath = relPath.substring(2,relPath.length());
            }
        }


        String[] arrRelPath = relPath.split("/");
        try{
            String urlBase = url.getProtocol() + "://" + url.getHost();
            int port = url.getPort();
            if (port>0 && port!=80) urlBase+= ":" + url.getPort();



          //Build Path
            String urlPath = "";
            String newPath;
            if (relPath.substring(0,1).equals("/")){
                newPath = relPath;
            }
            else{

                urlPath = "/";
                String[] arr = url.getPath().split("/");
                for (int i=0; i<=(arr.length-arrRelPath.length); i++){
                     String dir = arr[i];
                     if (dir.length()>0){

                         urlPath += dir + "/";
                     }
                }


              //This can be cleaned-up a bit...
                if (relPath.substring(0,1).equals("/")){
                    newPath = relPath.substring(1,relPath.length());
                }
                else if (relPath.substring(0,2).equals("./")){
                    newPath = relPath.substring(2,relPath.length());
                }
                else if (relPath.substring(0,3).equals("../")){
                    newPath = relPath.replace("../", "");
                }
                else{
                    newPath = relPath;
                }
            }

            return urlBase + urlPath + newPath;
        }
        catch(Exception e){}
        return null;
    }


  //**************************************************************************
  //** getAbsolutePath
  //**************************************************************************
  /** Returns a fully qualified URL for a given path. See MapPath() method for
   *  more information.
   *  @deprecated Use MapPath()
   */
    public static String getAbsolutePath(String relPath, String url){
        try{
            return MapPath(relPath, new java.net.URL(url));
        }
        catch(Exception e){}
        return null;
    }


  //**************************************************************************
  //** getElements
  //**************************************************************************
  /** Returns top-level nodes in a given HTML string
   */
    private static ArrayList<Element> getElements(String s){

        ArrayList<Element> elements = new ArrayList<Element>();
        if (s==null) return elements;


        int start = -1;


        int len = s.length();
        for (int i=0; i<len; i++){
            char c = s.charAt(i);


            if (isBlank(c) || i==len-1){
                if (start>-1){
                    String str = s.substring(start, i);
//                    console.log("str:",str);


                    int idx = str.indexOf("<");
                    if (idx>-1){
                        int outerStart = start+idx;
                        int outerEnd;

                        String tag = s.substring(outerStart+1, i);


                        int a = tag.indexOf("/>");
                        int b = tag.indexOf(">");

                        if (a==-1 && b==-1){
                            outerEnd = findEndTag(tag, i, s);
                        }
                        else{
                            if (a<b && a>-1){
                                outerEnd = outerStart+1+a+2;
                            }
                            else{
                                tag = tag.substring(0, b);
                                outerEnd = findEndTag(tag, outerStart+1+b+1, s);
                            }
                        }


                        if (outerEnd==-1){ //unclosed tag like <link ...>
                            outerEnd = findGT(outerStart, s);
                        }


                        if (outerEnd>-1){

                            String outerHTML = s.substring(outerStart, outerEnd);
//                            System.out.println("---------------------");
//                            System.out.println(outerHTML+"|");
//                            System.out.println("---------------------");


                            try{
                                elements.add(new Element(outerHTML));
                            }
                            catch(Exception e){}


                            i = outerEnd;
                        }
//                        System.out.println();

                    }


                    start = -1;
                }

            }
            else{
                if (start==-1) start = i;

                if (c=='"'){
                    String quote = readQuote(s, i, c);
                    i += (quote.length()-1);
                }
            }

        }

        return elements;
    }


  //**************************************************************************
  //** findEndTag
  //**************************************************************************
  /** Returns the position of an end tag corresponding to the given tagName
   */
    private static int findEndTag(String tagName, int x, String s){


        if (tagName.startsWith("!")) tagName = tagName.substring(1);
//        console.log("tagName:",tagName);

        int numTags = 1;


        int start = -1;
        int len = s.length();
        for (int i=x; i<len; i++){
            char c = s.charAt(i);


            if (isBlank(c) || i==len-1){

                if (start>-1){
                    String str = s.substring(start, i);
                    //console.log("test:",str);


                    int a1 = str.indexOf("/>");
                    int b1 = str.indexOf("<");



                    if (a1<b1 && a1>-1){ //found "/>"

                    }
                    else{ //found "<"

                        String tag = s.substring(start+b1+1, i);
                        if (tag.startsWith("!")) tag = tag.substring(1);
                        boolean foundMatch = false;

                        int a = tag.indexOf("/>");
                        int b = tag.indexOf(">");

//console.log("tag:", tag, a, b);


                        if (a==-1 && b==-1){ //tag doesn't end with a ">" (e.g. "<link ..." or "</link ...")
                            if (tag.equals("/" + tagName)){
                                foundMatch = true;
                                numTags--;
                            }
                            else if (tag.equals(tagName)){
                                foundMatch = true;
                                numTags++;
                            }
                        }
                        else{
                            if (a<b && a>-1){ //found "/>"
                                tag = tag.substring(0, a);


                                if (tag.equals(tagName)){ //found self enclosed tag without whitespaces (e.g. "<link/>")
                                    foundMatch = true;
                                    numTags--;
                                }
                                else{
                                    String t = s.substring(start+b1, start+b1+1);
                                    if (!t.equals("<")){
//                                        console.log("Go back and find start of /> tag");
//                                        tag = findStartTag(start+b1, s);
//                                        if (tag.equals(tagName)){
//                                            foundMatch = true;
//                                            numTags--;
//                                        }
                                    }
                                }
                            }
                            else{
                                tag = tag.substring(0, b);


                                if (tag.equals("/" + tagName)){
                                    tag = tag.substring(1);
                                    foundMatch = true;
                                    numTags--;
                                }
                                else if (tag.equals(tagName)){
                                    foundMatch = true;

                                    if (tagName.equals("--")){
                                        numTags--;
                                    }
                                    else{
                                        numTags++;
                                    }
                                }

                            }
                        }



//                        console.log("tag2:", tag, numTags, foundMatch);



                        if (foundMatch && numTags==0){
                            if (i==len-1) return i+1;
                            return i;
                        }

                    }

                }
                start = -1;
            }
            else{
                if (start==-1) start = i;

                if (c=='"'){
                    String quote = readQuote(s, i, c);
                    i += (quote.length()-1);
                }
            }
        }

        return -1;
    }


  //**************************************************************************
  //** findGT
  //**************************************************************************
  /** Returns the position of the first ">" character that is not a comment
   *  or inside a quote
   */
    protected static int findGT(int x, String s){

        int numComments = 0;

        int start = -1;
        int len = s.length();
        for (int i=x; i<len; i++){
            char c = s.charAt(i);


            if (isBlank(c) || i==len-1){

                if (start>-1){
                    int end = i;
                    String str = s.substring(start, end);
                    //console.log("test:",str, numComments);



                    int a = str.indexOf("-->");
                    int b = str.indexOf("<!--");



                    if (a==-1 && b==-1){ //did not find a "<!--" or "-->"

                        int idx = str.indexOf(">");
                        if (idx>-1 && numComments==0) return start+idx+1;

                    }
                    else if (a>-1 && b>-1){ //contains both "<!--" and "-->"

                        if (a<b){ //found "-->"
                            numComments--;
                            i = start+a+3;
                            start = i;
                            continue;
                        }
                        else{ //found "<!--"
                            numComments++;
                            i = start+b+4;
                            start = i;
                            continue;
                        }

                    }
                    else{

                        if (a>-1 && a>b){ //found "-->"
                            numComments--;
                            i = start+a+3;
                            start = i;
                            continue;
                        }

                        if (b>-1 && b>a){ //found "<!--"
                            numComments++;
                            i = start+b+4;
                            start = i;
                            continue;
                        }
                    }

                }
                start = -1;
            }
            else{
                if (start==-1) start = i;

                if (c=='"'){
                    String quote = readQuote(s, i, c);
                    i += (quote.length()-1);
                }
            }
        }

        return -1;
    }


  //**************************************************************************
  //** isBlank
  //**************************************************************************
  /** Returns true if the given char is a white space, tab or return
   */
    protected static boolean isBlank(char c){
        if (c==' ') return true;
        if (c=='\r') return true;
        if (c=='\n') return true;
        if (c=='\t') return true;
        return false;
    }


  //**************************************************************************
  //** readQuote
  //**************************************************************************
  /** Returns a string encapsulated by either a single or double quote,
   *  starting at a given index
   */
    private static String readQuote(String s, int i, char t){

        StringBuilder str = new StringBuilder();
        str.append(s.charAt(i));
        boolean escaped = false;
        for (int x=i+1; x<s.length(); x++){
            char q = s.charAt(x);
            str.append(q);

            if (q==t){
                if (!escaped){
                    break;
                }
                else{
                    escaped = false;
                }
            }
            else if (q=='\\'){
                escaped = !escaped;
            }
            else{
                escaped = false;
            }
        }

        return str.toString();
    }
}
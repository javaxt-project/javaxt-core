package javaxt.html;
import java.util.ArrayList;
import static javaxt.utils.Console.console;

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


        boolean insideComment = false;
        boolean insideQuote = false;


        int start = 0;

        int len = s.length();
        for (int i=0; i<len; i++){
            char c = s.charAt(i);
            if (c=='<'){

                if (!insideComment && !insideQuote){
                    if (i+3<len-1){
                        String str = s.substring(i, i+4);
                        if (str.equals("<!--")){
                            insideComment = true;
                            i += 3;
                            continue;
                        }
                    }
                }


                if (!insideComment && !insideQuote){
                    start = i;
                }

            }
            else if (c=='>'){

                if (insideComment && !insideQuote){
                    String str = s.substring(i-2, i+1);
                    if (str.equals("-->")){
                        insideComment = false;
                        continue;
                    }
                }

                if (!insideComment && !insideQuote){


                    int end = -1;
                    char p = s.charAt(i-1);
                    if (p=='/'){
                        end = i+1;
                    }
                    else{
                        try{
                            String str = s.substring(start, i+1);
                            Element el = new Element(str);
                            end = findEndTag(el.getName(), i+1, s);
                            if (end==-1){
                                end = i+1;
                            }
                            else{
                                end++;
                            }
                        }
                        catch(Exception e){} //shouldn't happen
                    }


                    if (end>-1){
                        String str = s.substring(start, end);
                        try{
                            Element el = new Element(str);
                            elements.add(el);
                        }
                        catch(Exception e){} //shouldn't happen

                        i = end;
                        start = i;
                    }
                    else{
                        //console.log("error finding end tag!");
                    }

                }
            }
            else if (c=='"'){
                if (!insideComment){
                    insideQuote = !insideQuote;
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

        int numTags = 1;
        int start = -1;

        boolean insideComment = false;
        boolean insideQuote = false;
        boolean insideClosingTag = false;
        ArrayList<Object[]> tags = new ArrayList<Object[]>();

        int len = s.length();
        for (int i=x; i<len; i++){
            char c = s.charAt(i);
            if (c=='<'){

                if (!insideComment && !insideQuote){
                    if (i+3<len-1){
                        String str = s.substring(i, i+4);
                        if (str.equals("<!--")){
                            insideComment = true;
                            i += 3;
                            continue;
                        }
                    }
                }


                if (!insideComment && !insideQuote){

                    if (i+1<len-1){
                        char n = s.charAt(i+1);
                        if (n=='/'){
                            insideClosingTag = true;
                        }
                    }


                    start = i;
                }

            }
            else if (c=='>'){

                if (insideComment && !insideQuote){
                    String str = s.substring(i-2, i+1);
                    if (str.equals("-->")){
                        insideComment = false;
                        continue;
                    }
                }

                if (!insideComment && !insideQuote){


                  //Get tag name
                    String currTagName = "";
                    try{
                        String str;
                        if (insideClosingTag){
                            str = "<" + s.substring(start+2, i+1);
                        }
                        else{
                            str = s.substring(start, i+1);
                        }
                        Element el = new Element(str);
                        currTagName = el.getName();
                        tags.add(new Object[]{currTagName, insideClosingTag, start, i});
                    }
                    catch(Exception e){} //shouldn't happen



                  //Compare current tag to the target tag. If there's a match,
                  //update the numTags
                    if (currTagName.equals(tagName)){
                        char p = s.charAt(i-1);
                        if (p=='/'){
                            //self enclosing tag, don't update the numTags
                        }
                        else{
                            String t = s.substring(start, i+1);

                            if (insideClosingTag){
                                numTags--;
                            }
                            else{
                                numTags++;
                            }
                        }
                    }


                  //If numTags is 0, we have found the end!
                    if (numTags==0) return i;



                  //Update insideClosingTag variable as needed
                    if (insideClosingTag){
                        insideClosingTag = false;
                    }

                }
            }
            else if (c=='"'){
                if (!insideComment){
                    insideQuote = !insideQuote;
                }
            }
        }



      //Special case for tags like this: <div><div id="1"></div>
      //In Chrome and Firefox, this translates to: <div><div id="1"></div></div>
      //In this case, we want to return the position of the end of: </div>
        if (!tags.isEmpty()){
            Object[] nextTag = tags.get(0);
            String nextTagName = (String) nextTag[0];
            boolean isClosingTag = (Boolean) nextTag[1];
            if (nextTagName.equals(tagName) && !isClosingTag){

                Object[] lastTag = null;
                for (int i=1; i<tags.size(); i++){
                    Object[] tag = tags.get(i);
                    String name = (String) tag[0];
                    if (name.equals(tagName)){
                        lastTag = tag;
                    }
                    else {
                        break;
                    }
                }

                if (lastTag!=null){
                    String lastTagName = (String) lastTag[0];
                    isClosingTag = (Boolean) lastTag[1];
                    if (lastTagName.equals(tagName) && isClosingTag){
                        return (Integer) lastTag[3];
                    }
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

        boolean insideComment = false;
        boolean insideQuote = false;

        int len = s.length();
        for (int i=x; i<len; i++){
            char c = s.charAt(i);
            if (c=='<'){

                if (!insideComment && !insideQuote){
                    if (i+3<len-1){
                        String str = s.substring(i, i+4);
                        if (str.equals("<!--")){
                            insideComment = true;
                            i += 3;
                        }
                    }
                }
            }
            else if (c=='>'){

                if (insideComment && !insideQuote){
                    String str = s.substring(i-2, i+1);
                    if (str.equals("-->")){
                        insideComment = false;
                        continue;
                    }
                }

                if (!insideComment && !insideQuote){
                    return i;
                }
            }
            else if (c=='"'){
                if (!insideComment){
                    insideQuote = !insideQuote;
                }
            }
        }

        return -1;
    }
}
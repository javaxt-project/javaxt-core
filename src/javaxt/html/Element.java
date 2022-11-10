package javaxt.html;
import javaxt.xml.DOM;

//******************************************************************************
//**  HTML Element
//******************************************************************************
/**
 *   Used to represent a DOM element in an HTML document.
 *
 ******************************************************************************/

public class Element {

    private String nodeName;
    private String attributes;
    private String innerHTML;
    private String outerHTML;
    private Parser parser;
    private boolean isClosed;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** @param outerHTML HTML used to define a tag (e.g. <div id="1">test</div>)
   */
    public Element(String outerHTML) throws Exception {
        this.outerHTML = outerHTML;


      //Get position of the first whitespace character
        int ws = -1;
        for (int z=0; z<outerHTML.length(); z++){
            char t = outerHTML.charAt(z);
            if (isWhitespace(t)){
                ws = z+1;
                break;
            }
        }


      //Get position of the first ">" character
        int gt = Parser.findGT(0, outerHTML+" ");
        if (gt==-1) throw new Exception("Invalid or unsupported html"); //includes html comments
        gt++;


      //Set nodeName and attributes
        int endNode;
        if (ws==-1){
            endNode = gt-1;
            attributes = "";
        }
        else{
            if (gt<ws){
                endNode = gt-1;
                attributes = "";
            }
            else{
                endNode = ws;
                attributes = outerHTML.substring(endNode, gt-1).trim();
                if (attributes.endsWith("/")) attributes = attributes.substring(0, attributes.length()-1).trim();
            }

        }
        nodeName = outerHTML.substring(1, endNode);
        if (nodeName.endsWith("/")) nodeName = nodeName.substring(0, nodeName.length()-1);
        nodeName = nodeName.trim();



      //Set innerHTML
        innerHTML = outerHTML.substring(gt);
        if (innerHTML.trim().length()>0){ //has inner html

            int idx = innerHTML.lastIndexOf("</" + nodeName);
            if (idx>-1){
                innerHTML = innerHTML.substring(0, idx);
                isClosed = true;
            }
            else{
                isClosed = false;
            }
        }
        else{ //no inner html

          //Check if the tag is self enclosing (e.g. "<hr/>")
            char c = outerHTML.charAt(gt-2); //character immediately before ">"
            isClosed = c=='/';
        }
    }


  //**************************************************************************
  //** getName
  //**************************************************************************
  /** Returns the node name associated with the element
   */
    public String getName(){
        return nodeName;
    }


  //**************************************************************************
  //** getAttributes
  //**************************************************************************
  /** Returns the element attributes
   *  @return Anything that appears after the tag/node name
   */
    public String getAttributes(){
        return attributes;
    }


  //**************************************************************************
  //** isClosed
  //**************************************************************************
  /** Returns true if the element has a closing tag or is self closing
   */
    public boolean isClosed(){
        return isClosed;
    }


  //**************************************************************************
  //** getInnerHTML
  //**************************************************************************
  /** Returns the HTML content (inner HTML) of an element as a String.
   */
    public String getInnerHTML(){
        return innerHTML;
    }


  //**************************************************************************
  //** getOuterHTML
  //**************************************************************************
  /** Returns the HTML used to define this element (tag and attributes), as
   *  well as the HTML content (inner HTML). You can use this String to remove
   *  or replace elements from the original HTML document.
   */
    public String getOuterHTML(){
        return outerHTML;
    }


  //**************************************************************************
  //** getInnerText
  //**************************************************************************
  /** Removes all HTML tags and attributes inside this element, leaving the
   *  raw rendered text.
   */
    public String getInnerText(){
        return Parser.stripHTMLTags(innerHTML);
    }


  //**************************************************************************
  //** getAttribute
  //**************************************************************************
  /** Returns the value for a given attribute. If no match is found, returns
   *  an empty string.
   */
    public String getAttribute(String attributeName){
        return _getAttributeValue(attributeName);
    }


  //**************************************************************************
  //** getElementByID
  //**************************************************************************
  /** Returns an HTML Element with given a id. Returns null if the element was
   *  not found.
   */
    public Element getElementByID(String id){
        return getElementByAttributes(null, "id", id);
    }


  //**************************************************************************
  //** getElementByTagName
  //**************************************************************************
  /** Returns an array of HTML Elements with given tag name.
   */
    public Element[] getElementsByTagName(String tagName){
        return getParser().getElementsByTagName(tagName);
    }


  //**************************************************************************
  //** getElementByTagName
  //**************************************************************************
  /** Returns the first HTML Element with given tag name. Returns null if an
   *  element was not found.
   */
    public Element getElementByTagName(String tagName){
        return getElementByAttributes(tagName, null, null);
    }


  //**************************************************************************
  //** getElements
  //**************************************************************************
    public Element[] getChildNodes(){
        return getParser().getElements();
    }


  //**************************************************************************
  //** getElements
  //**************************************************************************
  /** Returns an array of HTML Elements with given tag name, attribute, and
   *  attribute value (e.g. "div", "class", "panel-header").
   */
    public Element[] getElements(String tagName, String attributeName, String attributeValue){
        return getParser().getElements(tagName, attributeName, attributeValue);
    }


  //**************************************************************************
  //** getElementByAttributes
  //**************************************************************************
  /** Returns the first HTML Element with given tag name and attribute. Returns
   *  null if an element was not found.
   */
    public Element getElementByAttributes(String tagName, String attributeName, String attributeValue){
        return getParser().getElementByAttributes(tagName, attributeName, attributeValue);
    }


  //**************************************************************************
  //** getImageLinks
  //**************************************************************************
  /** Returns a list of links to images. The links may include relative paths.
   *  Use the Parser.getAbsolutePath() method to resolve the relative paths to
   *  a fully qualified url.
   */
    public String[] getImageLinks(){
        return getParser().getImageLinks();
    }


  //**************************************************************************
  //** getParser
  //**************************************************************************
    private Parser getParser(){
        if (parser==null) parser = new Parser(innerHTML);
        return parser;
    }


  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns the outer HTML of this element. See getOuterHTML() for more
   *  information.
   */
    public String toString(){
        return outerHTML;
    }


    /** @deprecated Use getInnerText() */
    public String stripHTMLTags(){
        return getInnerText();
    }

    /** @deprecated Use getAttribute() */
    public String getAttributeValue(String attributeName){
        return getAttribute(attributeName);
    }


  //**************************************************************************
  //** getAttributeValue
  //**************************************************************************
  /** Returns the value for a given attribute. If no match is found, returns
   *  an empty string.
   */
    private String _getAttributeValue(String attributeName){
        String tag = (nodeName + " " + attributes).trim();
        try{
            org.w3c.dom.Document XMLDoc = DOM.createDocument("<" + tag + "/>");
            org.w3c.dom.NamedNodeMap attr = XMLDoc.getFirstChild().getAttributes();
            return DOM.getAttributeValue(attr,attributeName);
        }
        catch(Exception e){
            try{
               return _getAttributeValue2(tag, attributeName);
            }
            catch(Exception ex){
               return "";
            }
        }
    }


    private String _getAttributeValue2(String tag, String attributeName){

        tag = tag.trim();

        if (!tag.contains(" ")) return tag;

        String orgTag = tag;
        tag = tag.substring(tag.indexOf(" "), tag.length()).trim();


        String tagName = orgTag + " ";
        tagName = tagName.substring(0, tagName.indexOf(" "));



      //compress spaces
        String newTag = "";
        tag += " ";
        for (int i=0; i<tag.length()-1; i++){
             char ch = tag.charAt(i);
             if (ch==' ' && tag.charAt(i+1)==' '){
             }
             else{
                 newTag += ch;
             }
        }

        newTag = newTag.replace("= ", "=");
        newTag = newTag.replace(" =", "=");

        //System.out.println("newTag = " + newTag);System.out.println();

        newTag = " " + newTag + " ";
        //attributeName = attributeName.toLowerCase();
        for (int i=0; i<newTag.length(); i++){
             char ch = newTag.charAt(i);


             if (ch == '='){

                 String tmp = newTag.substring(0, i);
                 //System.out.println(tmp);

                 String AttrName = tmp.substring(tmp.lastIndexOf(" "), tmp.length()).trim();
                 String AttrValue = "";

                 //System.out.println("AttrName = " + AttrName);
                 if (AttrName.equalsIgnoreCase(attributeName)){

                     tmp = newTag.substring(i+1, newTag.length()).trim() + " ";

                     if (newTag.charAt(i+1)=='"'){
                         tmp = tmp.substring(1, tmp.length());
                         AttrValue = tmp.substring(0, tmp.indexOf("\""));
                     }
                     else if (newTag.charAt(i+1)=='\''){
                         tmp = tmp.substring(1, tmp.length());
                         AttrValue = tmp.substring(0, tmp.indexOf("'"));
                     }
                     else{
                         AttrValue = tmp.substring(0, tmp.indexOf(" "));
                     }

                     return AttrValue;

                 }

             }
        }
        return "";
    }


  //**************************************************************************
  //** isWhitespace
  //**************************************************************************
  /** Returns true if the given char is a white space, tab or return
   */
    private static boolean isWhitespace(char c){
        if (c==' ') return true;
        if (c=='\r') return true;
        if (c=='\n') return true;
        if (c=='\t') return true;
        return false;
    }
}
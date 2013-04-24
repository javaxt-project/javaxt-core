package javaxt.html;
import javaxt.xml.DOM;

public class Element {

    private String tagName;
    //private String html;

    protected String innerHTML;
    protected String outerHTML;
    protected boolean isStartTag = true;

    private String tag;
    private String[] arr;


  //**************************************************************************
  //** Constructor
  //**************************************************************************

    public Element(String html){
        //this.html = html;
        
        tag = html;
        if (tag.indexOf("</")==0){
            isStartTag = false;
        }
        tag = tag.replace("</","");
        tag = tag.replace("<","");
        tag = tag.replace("/>","");
        tag = tag.replace(">","");
        //tag = AddQuotes(tag);
        tag = tag.trim();

        arr = tag.split(" ");

        tagName = arr[0];
    }

  //**************************************************************************
  //** getName
  //**************************************************************************
  /** Returns the element tag/node name. */

    public String getName(){
        return tagName;
    }
    
    public String getInnerHTML(){
        return innerHTML;
    }


    public String getOuterHTML(){
        return outerHTML;
    }


  //**************************************************************************
  //** getAttributeValue
  //**************************************************************************
  /** Returns the value for a given attribute. If no match is found, returns
   *  an empty string.
   */
    public String getAttributeValue(String attributeName){
        try{
            org.w3c.dom.Document XMLDoc = DOM.createDocument("<" + tag + "/>");
            org.w3c.dom.NamedNodeMap attr = XMLDoc.getFirstChild().getAttributes();
            return DOM.getAttributeValue(attr,attributeName);
        }
        catch(Exception e){
            try{
               return getAttributeValue2(attributeName);
            }
            catch(Exception ex){
               return "";
            }
        }

    }




    private String getAttributeValue2(String attributeName){

        tag = tag.trim();

        if (tag.contains((CharSequence) " ")==false){
            return tag;
        }

        String orgTag = tag;
        tag = tag.substring(tag.indexOf(" "), tag.length()).trim();


        String tagName = orgTag + " ";
        tagName = tagName.substring(0, tagName.indexOf(" "));

/*
        if (tagName.equalsIgnoreCase("img")){
            System.out.println("IMGTAG = " + tag);
        }
        else{
            return "";
        }

*/


      //compress spaces
        String newTag = "";
        tag += " ";
        boolean skipChar = false;
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


    public String toString(){
        return outerHTML;
    }
}
package javaxt.html;
import javaxt.xml.DOM;

//******************************************************************************
//**  HTML Parser
//******************************************************************************
/**
 *   A simple html parser used to extract blocks of html from a document.
 *
 ******************************************************************************/

public class Parser {
    

    private String HTML;
    
    
  //**************************************************************************
  //** Creates a new instance of the html parser
  //**************************************************************************
    
    public Parser(String html){
        HTML = html;
    }
    
  //**************************************************************************
  //** setHTML
  //**************************************************************************
  /**  Used to reset the "scope" of the parser */
    
    public void setHTML(String html){
        HTML = html;
    }
    
  //**************************************************************************
  //** Element Class
  //**************************************************************************
  /**  Structure used to store basic element information */
    
    public class Element{
        public String Name;
        public String Html;
        public String innerHTML;
        public String outerHTML;


    }
    
    public String getAbsolutePath(String RelPath, String url){
        
      //Check whether RelPath is actually a relative
        try{
            java.net.URL URL = new java.net.URL(RelPath);
            return RelPath;
        }
        catch(Exception e){}
        
        
        
      //Remove "./" prefix in the RelPath
        if (RelPath.length()>2){
            if (RelPath.substring(0,2).equals("./")){
                RelPath = RelPath.substring(2,RelPath.length());
            }
        }
        
        
        String[] arrRelPath = RelPath.split("/");

        try{
            java.net.URL URL = new java.net.URL(url);
            String urlBase = URL.getProtocol() + "://" + URL.getHost();
            
            //System.out.println(url);
            //System.out.println(URL.getPath());
            //System.out.print(urlBase);
            

          //Build Path
            String urlPath = "";
            String newPath = "";
            if (RelPath.substring(0,1).equals("/")){
                newPath = RelPath;
            }
            else{
            
                urlPath = "/";
                String dir = "";
                String[] arr = URL.getPath().split("/");
                for (int i=0; i<=(arr.length-arrRelPath.length); i++){
                     dir = arr[i];
                     if (dir.length()>0){

                         urlPath += dir + "/";
                     }
                }
                //System.out.println(urlPath);
                
                
              //This can be cleaned-up a bit...
                if (RelPath.substring(0,1).equals("/")){
                    newPath = RelPath.substring(1,RelPath.length());
                }
                else if (RelPath.substring(0,2).equals("./")){
                    newPath = RelPath.substring(2,RelPath.length());
                }
                else if (RelPath.substring(0,3).equals("../")){
                    newPath = Replace(RelPath,"../","");
                }
                else{
                    newPath = RelPath;
                }
            }

            

            return urlBase + urlPath + newPath;
            
        
        }
        catch(Exception e){}
        return null;
    }
    
  //**************************************************************************
  //** Replace
  //**************************************************************************
    
    private String Replace(String str, String target, String replacement){
        CharSequence Target = (CharSequence) target;
        CharSequence Replacement = (CharSequence) replacement;
        return str.replace(Target,Replacement);
    }
    
  //**************************************************************************
  //** getElementByAttributes
  //**************************************************************************
  /**  Used used to extract an html "element" from a larger html document */
    
    public Element getElementByAttributes(String tagName, String attributeName, String attributeValue){
        
        String s = HTML + " ";
        String c = "";
         
        boolean findEndTag = false;
        boolean concat = false;
        int absStart = 0;
        int absEnd = 0;
        int numStartTags = 0;
        int numEndTags = 0;
         
         
        int outerStart = 0;
        int outerEnd = 0;

         
         
        String tag = "";
        Tag Tag = null;
        Element Element = new Element();
                 
        for (int i = 0; i < s.length(); i++){
              
             c = s.substring(i,i+1); 
              
              
             if (c.equals("<")){       
                 concat = true;
                 absEnd = i;
             }
              
              
             if (concat==true){
                 tag += c;
             } 
              
              
             if (c.equals(">") && concat==true){    
                 concat = false;
                  
                  
              //Find Start Tag and Compare it to the client's inputs
                 Tag = new Tag(tag);
                 if (Tag.Name.equalsIgnoreCase(tagName) && Tag.isStartTag){
                      if (Tag.getAttributeValue(attributeName).equalsIgnoreCase(attributeValue)){
                          
                          absStart = i+1;
                          Element.Name = Tag.Name;
                          Element.Html = tag;
                          outerStart = absStart - tag.length();
                          findEndTag = true;
                      }

                 }
                  
                 
               //Find End Tag
                 if (findEndTag){
                  
                      if (Tag.Name.equalsIgnoreCase(tagName)){
                          if (Tag.isStartTag == true) numStartTags +=1;
                          if (Tag.isStartTag == false) numEndTags +=1;
                      }
                      
                      if (numEndTags>=numStartTags){ 
                          Element.innerHTML = HTML.substring(absStart,absEnd);
                          outerEnd = i+1;
                          Element.outerHTML = HTML.substring(outerStart,outerEnd);
                          return Element;
                      }
                 }
                 
                 
               //Clear tag variable for the next pass
                 tag = "";
                  
             }
                    
              
        }
        return null;
    }


    
    public Element[] getElementsByTagName(String tagName){
        String orgHTML = HTML;
        java.util.Vector elements = new java.util.Vector();
        Element e = getElementByTagName(tagName);
        while (e!=null){
            elements.add(e);
            HTML = HTML.replace(e.outerHTML, "");
            e = getElementByTagName(tagName);
        }


        HTML = orgHTML;
        Element[] arr = new Element[elements.size()];
        for (int i=0; i<arr.length; i++){
            arr[i] = (Element) elements.get(i);
        }
        return arr;
    }
    
    
  //**************************************************************************
  //** getElementByTagName
  //**************************************************************************
  /**  Used used to extract an html "element" from a larger html document */
    
    public Element getElementByTagName(String tagName){
        String s = HTML + " ";
        String c = "";
         
        boolean findEndTag = false;
        boolean concat = false;
        int absStart = 0;
        int absEnd = 0;
        int numStartTags = 0;
        int numEndTags = 0;
         
         
        int outerStart = 0;
        int outerEnd = 0;

         
         
        String tag = "";
        Tag Tag = null;
        Element Element = new Element();
                 
        for (int i = 0; i < s.length(); i++){
              
             c = s.substring(i,i+1); 
              
              
             if (c.equals("<")){       
                 concat = true;
                 absEnd = i;
             }
              
              
             if (concat==true){
                 tag += c;
             } 
              
              
             if (c.equals(">") && concat==true){    
                 concat = false;
                  
                  
              //Find Start Tag and Compare it to the client's inputs
                 Tag = new Tag(tag);
                 if (Tag.Name.equalsIgnoreCase(tagName) && Tag.isStartTag){
                          
                     absStart = i+1;
                     Element.Name = Tag.Name;
                     Element.Html = tag;
                     outerStart = absStart - tag.length();
                     findEndTag = true;

                 }
                  
                 
               //Find End Tag
                 if (findEndTag){
                  
                      if (Tag.Name.equalsIgnoreCase(tagName)){
                          if (Tag.isStartTag == true) numStartTags +=1;
                          if (Tag.isStartTag == false) numEndTags +=1;
                      }
                      
                      if (numEndTags>=numStartTags){ 
                          Element.innerHTML = HTML.substring(absStart,absEnd);
                          outerEnd = i+1;
                          Element.outerHTML = HTML.substring(outerStart,outerEnd);
                          return Element;
                      }
                 }
                 
                 
               //Clear tag variable for the next pass
                 tag = "";
                  
             }
                    
              
        }
        return null;
    }
    
    
  //**************************************************************************
  //** getElementByID
  //**************************************************************************
  /**  Used used to extract an html "element" from a larger html document */
    
    public Element getElementByID(String id){
        String s = HTML + " ";
        String c = "";
         
        boolean findEndTag = false;
        boolean concat = false;
        int absStart = 0;
        int absEnd = 0;
        int numStartTags = 0;
        int numEndTags = 0;
         
         
        int outerStart = 0;
        int outerEnd = 0;

         
         
        String tag = "";
        Tag Tag = null;
        Element Element = new Element();
                 
        for (int i = 0; i < s.length(); i++){
              
             c = s.substring(i,i+1); 
              
              
             if (c.equals("<")){       
                 concat = true;
                 absEnd = i;
             }
              
              
             if (concat==true){
                 tag += c;
             } 
              
              
             if (c.equals(">") && concat==true){    
                 concat = false;
                  
                  
              //Find Start Tag and Compare it to the client's inputs
                 Tag = new Tag(tag);
                  if (Tag.getAttributeValue("id").equalsIgnoreCase(id)){
                     
                      absStart = i+1;
                      Element.Name = Tag.Name;
                      Element.Html = tag;
                      outerStart = absStart - tag.length();
                      findEndTag = true;
                  }
                  
                 
               //Find End Tag
                 if (findEndTag){
                  
                      if (Tag.Name.equalsIgnoreCase(Element.Name)){
                          if (Tag.isStartTag == true) numStartTags +=1;
                          if (Tag.isStartTag == false) numEndTags +=1;
                      }
                      
                      if (numEndTags>=numStartTags){ 
                          Element.innerHTML = HTML.substring(absStart,absEnd);
                          outerEnd = i+1;
                          Element.outerHTML = HTML.substring(outerStart,outerEnd);
                          return Element;
                      }
                 }
                 
                 
               //Clear tag variable for the next pass
                 tag = "";
                  
             }
                    
              
        }
        return null;
    }
    
    
    
  //**************************************************************************
  //** getImageLink
  //**************************************************************************
  /**  Used used to extract an image link from a block of html */
    
    public String[] getImageLinks(String html){
        
        String s = html + " ";
        String c = "";
         
        boolean concat = false;
        
        String tag = "";
        Tag Tag = null;
        
        String link = "";
        String[] links = new String[0];
        int numLinks = 0;

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
                  
                  
                 Tag = new Tag(tag);
                 if (Tag.Name.equalsIgnoreCase("img")){

                      //System.out.println(Tag.Name + " " + Tag.getAttributeValue("src"));
                      //System.out.println(tag + " " + Tag.isStartTag + " " + i);   
                     
                     numLinks += 1;
                     link = Tag.getAttributeValue("src");

                     links = (String[])resizeArray(links,numLinks);
                     links[numLinks-1] = link;

                 }
                   
                  
                 tag = "";
                  
             }
                    
              
        }
        
        if (numLinks>0){
            return links;
        }
        else{
            return null;
        }
    }
    
    
   
  //**************************************************************************
  //** stripHTMLTags
  //**************************************************************************
  /**  Used used to remove any html tags from a block of text */
    
    public String stripHTMLTags(String html){
        
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
        return html.trim();
    }
    
    
    
    private class Tag{
        public String Name;
        public boolean isStartTag = true;
               
        private String tag;
        private String[] arr;
        
        public Tag(String tag){
            
            if (tag.indexOf("</")==0){
                isStartTag = false;
            }
            
            tag = tag.replace("</","");
            tag = tag.replace("<","");
            tag = tag.replace("/>","");
            tag = tag.replace(">","");
            //tag = AddQuotes(tag);
            this.tag = tag.trim();
            this.arr = this.tag.split(" ");
            this.Name = arr[0];
            
        }
    
        


        
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
                   return "" ;
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
        

    }
    
    

    


    

    
    
    private static Object resizeArray(Object oldArray, int newSize) {
       int oldSize = java.lang.reflect.Array.getLength(oldArray);
       Class elementType = oldArray.getClass().getComponentType();
       Object newArray = java.lang.reflect.Array.newInstance(
             elementType,newSize);
       int preserveLength = Math.min(oldSize,newSize);
       if (preserveLength > 0)
          System.arraycopy (oldArray,0,newArray,0,preserveLength);
       return newArray; 
    }
    
}

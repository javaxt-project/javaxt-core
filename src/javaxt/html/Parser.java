package javaxt.html;

//******************************************************************************
//**  HTML Parser
//******************************************************************************
/**
 *   A simple html parser used to extract blocks of html from a document.
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
  //** setHTML
  //**************************************************************************
  /** Used to reset the "scope" of the parser */
    
    public void setHTML(String html){
        this.html = html;
    }


    public String getAbsolutePath(String RelPath, String url){
        
      //Check whether RelPath is actually a relative
        try{
            new java.net.URL(RelPath);
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
                    newPath = RelPath.replace("../", "");
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
  //** getElementByAttributes
  //**************************************************************************
  /** Returns the first HTML Element found in the HTML document with given tag
   *  name and attribute. Returns null if an element was not found.
   */
    public Element getElementByAttributes(String tagName, String attributeName, String attributeValue){
        
        String s = html + " ";
        String c = "";
        
        boolean concat = false;
        int absStart = 0;
        int absEnd = 0;
        int numStartTags = 0;
        int numEndTags = 0;
         
         
        int outerStart = 0;
        int outerEnd = 0;

         
         
        String tag = "";
        Element Element = null;

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
                Element Tag = new Element(tag);


                if (Element==null){
                    if (Tag.getName().equalsIgnoreCase(tagName) && Tag.isStartTag){
                        if (Tag.getAttributeValue(attributeName).equalsIgnoreCase(attributeValue)){

                            absStart = i+1;
                            Element = Tag;
                            outerStart = absStart - tag.length();
                         }
                    }
                }               
                else { //Find End Tag

                    if (Tag.getName().equalsIgnoreCase(tagName)){
                        if (Tag.isStartTag == true) numStartTags +=1;
                        if (Tag.isStartTag == false) numEndTags +=1;
                    }
                      
                    if (numEndTags>=numStartTags){ 
                        Element.innerHTML = html.substring(absStart,absEnd);
                        outerEnd = i+1;
                        Element.outerHTML = html.substring(outerStart,outerEnd);
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
  //** getElementByTagName
  //**************************************************************************
  /** Returns an array of HTML Elements found in the HTML document with given
   *  tag name.
   */
    public Element[] getElementsByTagName(String tagName){
        String orgHTML = html;
        java.util.ArrayList<Element> elements = new java.util.ArrayList<Element>();
        Element e = getElementByTagName(tagName);
        while (e!=null){
            elements.add(e);
            html = html.replace(e.outerHTML, "");
            e = getElementByTagName(tagName);
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
        String s = html + " ";
        String c = "";
         
        boolean concat = false;
        int absStart = 0;
        int absEnd = 0;
        int numStartTags = 0;
        int numEndTags = 0;
         
        int outerStart = 0;
        int outerEnd = 0;

        String tag = "";
        Element Element = null;
                 
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
                Element Tag = new Element(tag);

                if (Element==null){
                    if (Tag.getName().equalsIgnoreCase(tagName) && Tag.isStartTag){

                        absStart = i+1;
                        Element = Tag;
                        outerStart = absStart - tag.length();
                    }
                }
                else { //Find End Tag
                  
                    if (Tag.getName().equalsIgnoreCase(tagName)){
                        if (Tag.isStartTag == true) numStartTags +=1;
                        if (Tag.isStartTag == false) numEndTags +=1;
                    }
                      
                    if (numEndTags>=numStartTags){ 
                        Element.innerHTML = html.substring(absStart,absEnd);
                        outerEnd = i+1;
                        Element.outerHTML = html.substring(outerStart,outerEnd);
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
  /**  Used to extract an html "element" from a larger html document */
    
    public Element getElementByID(String id){
        String s = html + " ";
        String c = "";
         

        boolean concat = false;
        int absStart = 0;
        int absEnd = 0;
        int numStartTags = 0;
        int numEndTags = 0;
         
         
        int outerStart = 0;
        int outerEnd = 0;

         
        String tag = "";
        Element Element = null;
                 
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
                Element Tag = new Element(tag);

                if (Element==null){
                    if (Tag.getAttributeValue("id").equalsIgnoreCase(id)){

                        absStart = i+1;
                        Element = Tag;
                        outerStart = absStart - tag.length();
                    }
                }
                else { //Find End Tag
                  
                      if (Tag.getName().equalsIgnoreCase(Element.getName())){
                          if (Tag.isStartTag == true) numStartTags +=1;
                          if (Tag.isStartTag == false) numEndTags +=1;
                      }
                      
                      if (numEndTags>=numStartTags){ 
                          Element.innerHTML = html.substring(absStart,absEnd);
                          outerEnd = i+1;
                          Element.outerHTML = html.substring(outerStart,outerEnd);
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
  /**  Used to extract an image link from a block of html */
    
    public String[] getImageLinks(String html){
        
        String s = html + " ";
        String c = "";
        boolean concat = false;
        String tag = "";
        java.util.ArrayList<String> links = new java.util.ArrayList<String>();

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
                  
                  
                Element e = new Element(tag);
                if (e.getName().equalsIgnoreCase("img")){
                    links.add(e.getAttributeValue("src"));
                }

                tag = "";
            }
        }
        
        if (links.size()>0){
            return links.toArray(new String[links.size()]);
        }
        else{
            return null;
        }
    }

   
  //**************************************************************************
  //** stripHTMLTags
  //**************************************************************************
  /**  Used to remove any html tags from a block of text */
    
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
}
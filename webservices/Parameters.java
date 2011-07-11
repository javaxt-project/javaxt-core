package javaxt.webservices;
import org.w3c.dom.*;

//******************************************************************************
//**  Parameters Class - By peter.borissow
//******************************************************************************
/**
 *   A convienance class used to represent multiple parameters.
 *
 ******************************************************************************/

public class Parameters {

    private String vbCrLf = "\r\n";

    private Parameter[] Parameters = null;
    private StringBuffer xml = null;
    private StringBuffer html = null;

    public Parameters(Parameter[] Parameters){
        this.Parameters = Parameters;
    }

    public Parameter[] getArray(){
        return Parameters;
    }

    
    public int getLength(){
        return Parameters.length;
    }



    /** Used to set a parameter value. Use "/" character to seperate nodes */
    public void setValue(String parameterName, String parameterValue){
        if (Parameters==null) return;
        Parameter parameter = getParameter(parameterName);
        if (parameter!=null){
            if (parameter.getValue()==null){
                parameter.setValue(parameterValue);
            }
            else{
                if (parameter.getMaxOccurs()>1){
                    //TODO: Create new instance of current parameter in parent and set value
                    //getParent(Parameter);
                }
            }
        }
    }

    public void setValue(String parameterName, byte[] bytes){
        setValue(parameterName, javaxt.utils.Base64.encodeBytes(bytes));
    }

    public void setValue(String ParameterName, java.util.Date ParameterValue){

      //2003-11-24T00:00:00.0000000-05:00
        java.text.SimpleDateFormat formatter =
             new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSZ");

        String d = formatter.format(ParameterValue).replace(" ", "T");

        String d1 = d.substring(0,d.length()-2);
        String d2 = d.substring(d.length()-2);
        d = d1 + ":" + d2;

        setValue(ParameterName, d);

    }


    public String getValue(String ParameterName){
        Parameter p = getParameter(ParameterName);
        if (p!=null){
            return p.getValue();
        }
        return null;
    }


    public Parameter getParameter(String ParameterName){
        if (Parameters==null) return null;
        else return getParameter(Parameters, ParameterName);
    }

    public Parameter getParameter(int i){
        return Parameters[i];
    }


    private Parameter getParameter(Parameter[] Parameters, String ParameterName){
        String A, B = "";
        if (ParameterName.contains((CharSequence) "/")){
            String[] arr = ParameterName.split("/");
            A = arr[0];
            B = ParameterName.substring(A.length() + 1);
            //System.out.println(A + " vs " + B);
            ParameterName = A;
        }
        for (int i=0; i<Parameters.length; i++){
             if (Parameters[i].getName().equalsIgnoreCase(ParameterName)){
                 if (B.length()==0){
                     //System.out.println("Return " + Parameters[i].getName());
                     return Parameters[i];
                 }
                 else{
                     //System.out.println("getParameter");
                     return getParameter(Parameters[i].getChildren(), B);
                 }
             }
        }
        return null;
    }




    public String toString(){
        if (Parameters!=null){
            xml = new StringBuffer();

            
            getParameters(Parameters);
            
            return xml.toString();
        }
        else{
            return "";
        }
    }


    private void getParameters(Parameter[] Parameters){
        if (Parameters!=null){
            for (int i=0; i<Parameters.length; i++){
                 Parameter Parameter = Parameters[i];
                 getParameter(Parameter);
            }
        }
    }

    private void getParameter(Parameter parameter){
        String ParameterName = parameter.getName();
        xml.append("<" + ParameterName + ">");

        if (parameter.isComplex()){
            getParameters(parameter.getChildren());
        }
        else{
            String ParameterValue = parameter.getValue();
            if (ParameterValue==null) ParameterValue = "";

            /*
            String[] find = new String[]{"<",">","&","'","\""};
            String[] replace = new String[]{"&lt;","&gt;","&amp;","&apos;","&quot;"};
            for (int i=0; i<find.length; i++){
                 ParameterValue = ParameterValue.replace((CharSequence)find[i], (CharSequence)replace[i]);
            }
            */

            if (ParameterValue.trim().length()>0){
                if (parameter.Type.equalsIgnoreCase("base64Binary")){
                    xml.append(ParameterValue);
                }
                else{
                    xml.append("<![CDATA[" + ParameterValue + "]]>");
                }
            }
        }
        xml.append("</" + ParameterName + ">");
    }


// <editor-fold defaultstate="collapsed" desc="Convert Parameters to HTML/Web Form. Click on the + sign on the left to edit the code.">


        /** Used to generate html form inputs */
        public String toHTML(){

           String buttons =
           "<div><br>" +
           "<input type=\"submit\" value=\"Invoke\" name=\"Invoke\" class=\"button\">&nbsp;&nbsp;" +
           "<input type=\"reset\" value=\"Reset\" name=\"Reset\" class=\"button\">" +
           "</div>" + vbCrLf;

            if (Parameters==null){
                return buttons;
            }
            else{
                html = new StringBuffer();
                html.append(vbCrLf);
                html.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">" + vbCrLf);
                addRows(Parameters);
                html.append("<tr><td colspan=\"2\"></td><td align=\"center\">" + buttons + "</td><td></td></tr>"  + vbCrLf);
                html.append("</table>" + vbCrLf);

                return html.toString();
            }


        }

        private void addRows(Parameter[] Parameters){
            if (Parameters!=null){
                for (int i=0; i<Parameters.length; i++){
                     Parameter Parameter = Parameters[i];
                     addRow(Parameter);
                }
            }
        }

        private void addRow(Parameter Parameter){

               String ParameterName = Parameter.getName();
               String ParameterType = Parameter.getType();
               String ParameterValue = Parameter.getValue();

               boolean isRequired = Parameter.isRequired();
               boolean isComplex = Parameter.isComplex();

               Option[] Options = Parameter.getOptions();

               String InputText = "";
               String InputName = "";
               String InputValue = "";
               String InputHTML = "";

             //Set Input Text
               if (isRequired){
                   InputText = ParameterName + "<span style=\"color:#FF0000;\">*</span>";
               }
               else{
                   InputText = ParameterName;
               }

             //Set Input Name
               InputName = getParentName(Parameter.ParentNode) + ParameterName;

             //Set Input HTML
               if (ParameterType.equalsIgnoreCase("String")){
                   String type = "text";
                   if (ParameterName.toLowerCase().contains((CharSequence) "password")){
                       type = "password";
                   }
                   InputHTML = "<input style=\"width:275px;\" type=\"" + type + "\" size=\"40\" name=\"" + InputName + "\">";
               }
               else if (ParameterType.equalsIgnoreCase("Boolean")){
                   InputHTML =
                   "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">" +
                   "<tr>" +
                     "<td><input type=\"radio\" value=\"TRUE\" checked name=\"" + InputName + "\"></td>" +
                     "<td>TRUE</td>" +
                     "<td>&nbsp;&nbsp; </td>" +
                     "<td><input type=\"radio\" value=\"FALSE\" checked name=\"" + InputName + "\"></td>" +
                     "<td>FALSE</td>" +
                   "</tr>" +
                   "</table>";
               }
               else if (ParameterType.equalsIgnoreCase("base64Binary")){
                   InputHTML = "<input style=\"width:275px;\" type=\"file\" size=\"40\" name=\"" + InputName + "\">";
               }
               else{
                   InputHTML = "<input style=\"width:275px;\" type=\"text\" size=\"40\" name=\"" + InputName + "\">";
               }

               if (isComplex){
                   InputHTML = "";
               }

               if (Options!=null){
                   InputHTML = "<select style=\"width:275px;\" name=\"" + InputName + "\">";
                   for (int i=0; i<Options.length; i++){
                        Option Option = Options[i];
                        InputHTML += "<option value=\"" + Option.getValue() + "\">" + Option.getName() + "</option>";
                   }
                   InputHTML += "</select>";
               }

               html.append("<tr>" + vbCrLf);
               html.append("<td></td>" + vbCrLf); //spacer or plus/minus sign
               html.append("<td valign=\"top\">" + InputText + "</td>" + vbCrLf);
               html.append("<td valign=\"top\" style=\"padding-left:5px;padding-right:5px;\">" + InputHTML + "</td>" + vbCrLf);
               html.append("<td valign=\"top\" class=\"smgrytxt\"><i>" + ParameterType + "</i></td>" + vbCrLf);
               html.append("</tr>" + vbCrLf);


            if (Parameter.isComplex()){
                addRows(Parameter.getChildren());
            }
        }

        private String getParentName(Node ParameterNode){
            String ret = "";
            while (ParameterNode!=null){
                if (ParameterNode.getNodeType()==1){
                    if (ParameterNode.getNodeName().equals("parameter")){
                        NamedNodeMap attr = ParameterNode.getAttributes();
                        String ParameterName = javaxt.xml.DOM.getAttributeValue(attr,"name"); //getAttributeValue(attr, "name");
                        ret = ParameterName + "/" + ret;
                    }
                }
                ParameterNode = ParameterNode.getParentNode();
            }
            return ret;
        }
// </editor-fold>



}




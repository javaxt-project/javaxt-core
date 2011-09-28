package javaxt.webservices;

//******************************************************************************
//**  Service Class
//******************************************************************************
/**
 *   Used to represent a web service.
 *
 ******************************************************************************/

public class Service {

    protected String Name = "";
    protected String Description = "";
    protected String URL = "";
    protected String NameSpace;
    protected Method[] Methods = null;

    public String getName(){return Name;}
    public String getDescription(){return Description;}
    public String getURL(){return URL;}
    public String getNameSpace(){return NameSpace;}
    public Method[] getMethods(){return Methods;}

  //Equals
    public boolean equals(String ServiceName){
        if (Name.equalsIgnoreCase(ServiceName)) return true;
        else return false;
    }

  //Get Method
    public Method getMethod(String MethodName){

        if (MethodName==null) MethodName = "";
        else MethodName = MethodName.trim();

        if (MethodName.equals("")){
            return getMethod(0);
        }
        else{
            for (int i=0; i<Methods.length; i++){
                 if (Methods[i].equals(MethodName)){
                     return Methods[i];
                 }
            }
        }
        return null;
    }

  //Get Method
    public Method getMethod(int i){
        if (Methods==null){
            return getMethod(0);
        }
        else{
            if (i<Methods.length){
                return Methods[i];
            }
        }
        return null;
    }

    public void setURL(String URL){
        this.URL = URL;
    }

}

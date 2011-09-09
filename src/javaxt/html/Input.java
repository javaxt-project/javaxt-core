package javaxt.html;


//******************************************************************************
//**  HTML Input
//******************************************************************************
/**
 *   Used to represent a simple html form input. Used in conjunction with the
 *   Form class.
 *
 ******************************************************************************/

public class Input {
    
    private String name;
    private String value;
    public Input(String name, String value){
        this.name = name;
        this.value = value;
    }

    public String toString(){
        return name + "=" + value;
    }

}

package javaxt.html;

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

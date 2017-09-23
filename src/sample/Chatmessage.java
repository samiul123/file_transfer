package sample;

import java.io.Serializable;

public class Chatmessage implements Serializable{
    protected static final long serialVersionUID = 1L;
    static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2;
    private int type;
    private String message;
    Chatmessage(int type, String message){
        this.type = type;
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}

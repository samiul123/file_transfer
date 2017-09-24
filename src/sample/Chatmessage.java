package sample;

import java.io.Serializable;

public class Chatmessage implements Serializable{
    protected static final long serialVersionUID = 1L;
    static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2;
    private int type;
    private String message;
    private String recipient;
    Chatmessage(int type, String message, String recipient){
        this.type = type;
        this.message = message;
        this.recipient = recipient;
    }

    public String getRecipient() {
        return recipient;
    }

    public int getType() {
        return type;

    }

    public String getMessage() {
        return message;
    }
}

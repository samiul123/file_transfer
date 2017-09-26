package sample;

import java.io.Serializable;

public class Chatmessage implements Serializable{
    protected static final long serialVersionUID = 1L;
    static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, FILE = 3;
    private int type;
    private String message;
    private String recipient;
    private String fileName;
    //private Integer fileId = 0;
    Chatmessage(int type, String message, String recipient,String fileName){
        this.type = type;
        this.message = message;
        this.recipient = recipient;
        this.fileName = fileName;
        //++fileId;

    }

    /*public int getFileId() {
        return fileId;
    }*/

    public String getFileName() {
        return fileName;
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

package sample;

import java.io.Serializable;

public class Chatmessage implements Serializable{
    protected static final long serialVersionUID = 1L;
    static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, FILE = 3,CLIENTSERVERONLY = 4,CONFIRM = 5;
    private int type;
    private String message;
    private String recipient;
    private String fileName;
    private String fileSize;
    private String serverMessage;
    public  int fileId;
    Chatmessage(int type, String message, String recipient,String fileName,String fileSize,String serverMessage){
        this.type = type;
        this.message = message;
        this.recipient = recipient;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.serverMessage = serverMessage;

    }

    public String getServerMessage() {
        return serverMessage;
    }

    public String getFileSize() {
        return fileSize;
    }

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

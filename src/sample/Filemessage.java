package sample;

import java.io.Serializable;

public class Filemessage implements Serializable{
    protected static final long serialVersionUID = 1L;
    static final int LOGOUT = 2, FILE = 3,CLIENTSERVERONLY = 4,CONFIRM = 5,ONLINE = 1;

    private int type;
    private String recipient;
    private String fileName;
    private String fileSize;
    private String serverMessage;
    public  int fileId;
    Filemessage(int type,String recipient,String fileName,String fileSize,String serverMessage){
        this.type = type;
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

}

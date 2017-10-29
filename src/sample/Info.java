package sample;

import java.io.File;

/**
 * Created by samiu on 26-Sep-17.
 */
public class Info {
    private String recipient;
    private String sender;
    private File fileName;
    private Integer fileId;

    Info(String sender, String recipient, File fileName,Integer fileId){
        this.sender = sender;
        this.recipient = recipient;
        this.fileName = fileName;
        this.fileId = fileId;
    }

    public String getSender() {
        return sender;
    }

    public Integer getFileId() {
        return fileId;
    }

    public String getRecipient() {
        return recipient;
    }

    public File getFileName() {
        return fileName;
    }
}

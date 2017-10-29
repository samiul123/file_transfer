package sample;

import java.io.File;

/**
 * Created by samiu on 28-Sep-17.
 */
public class Pending {
    private String sender;
    private String recipient;
    private File file;
    Pending(String sender,String recipient, File file){
        this.sender = sender;
        this.recipient = recipient;
        this.file = file;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public File getFile() {
        return file;
    }
}

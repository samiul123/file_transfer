package sample;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import static sample.Client.sOutput;


public class Server {
    public static int uniqueID = 0;
    public static ArrayList<ClientThread> clientLists;
    public static ArrayList<Info> infoObjects;
    public static int idRun = 1;
    private ServerGui sg;
    private SimpleDateFormat sdf;
    private int port;
    private boolean keepGoing;
    public static int buffer_size = 100;
    public static int chunkNumber = 100;
    public static int remainingChunkNumber;
    public static String acknowledgement = "Server has downloaded 1 chunk\n";
    public static String serverSig = "FROM SERVER: You logged out.File transmission cancelled\n" +
                                           "Server discarding the file\n";
    public static ArrayList<Pending> pendingList;
    public static int pendingIndicator = 0;
    public static int logOut = 0;
    public Server(int port, ServerGui sg){
        this.sg = sg;
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        clientLists = new ArrayList<ClientThread>();
        infoObjects = new ArrayList<>();
        pendingList = new ArrayList<>();
    }
    public void start(){
        keepGoing = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (keepGoing){
                display("Server waiting for Clients on port " + port + "\n");
                Socket socket = serverSocket.accept();
                if(!keepGoing){
                    break;
                }
                ClientThread t = new ClientThread(socket);
                display(t.username + " just connected");
                clientLists.add(t);
                System.out.println(clientLists.size());
                for(int i = 0; i < pendingList.size(); i++){
                    Pending pending = pendingList.get(i);
                    if(pending.getRecipient().equals(t.username)){
                        pendingIndicator = 1;
                        t.writeMessage(pending.getSender() + " wants to send you a file. " +
                                "Please write 'yes' or 'no' and click 'confirm' button below\n");
                        break;
                    }
                }
                t.start();
            }
            try{
                serverSocket.close();
                for(int i = 0; i < clientLists.size(); i++){
                    ClientThread tc = clientLists.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }catch (IOException ioE){

                    }
                }
            }catch (Exception e){
                display("Exception closing the server and clients: " + e);
            }
        }catch (IOException e){
            String msg = sdf.format(new Date()) + "Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    protected void stop(){
        keepGoing = false;
        try {
            new Socket("localhost",port);
        }catch (IOException e){

        }
    }
    private void display(String msg){
        String time = sdf.format(new Date()) + " " + msg;
        sg.appendEvent(time + "\n");
    }

    private synchronized boolean broadcast(String msg,String recipient){
        String time = sdf.format(new Date());
        String message = time + " " + msg + "\n";
        //int found = 0;

        for (int i = clientLists.size(); --i>= 0;){
            ClientThread ct = clientLists.get(i);
            System.out.println("broadcast: " + ct.username);
            if(ct.username.equals(recipient)){
                if(!ct.writeMessage(message)){
                    clientLists.remove(i);
                    display("Disconnected client " + ct.username + "removed from list");
                    return false;
                }
                else {
                    sg.appendChat(message);
                    return true;
                }
            }
        }
        return false;
    }
    private synchronized void broadcastServer(String msg,String recipient){
        String time = sdf.format(new Date());
        String message = time + " " + msg + "\n";


        for (int i = clientLists.size(); --i>= 0;){
            ClientThread ct = clientLists.get(i);
            System.out.println("broadcast: " + ct.username);
            if(ct.username.equals(recipient)){
                if(!ct.writeMessage(message)){
                    clientLists.remove(i);
                    display("Disconnected client " + ct.username + "removed from list");
                }
            }

        }

    }

    private synchronized void sendFile(String sender,String recipient,String fileN) throws IOException, ClassNotFoundException {
        String time = sdf.format(new Date());
        String message = time + " sending file to " + recipient + "\n";
        sg.appendEvent(message);

        for (int i = clientLists.size(); --i>= 0;){
            ClientThread ct = clientLists.get(i);
            if(ct.username.equals(recipient)){
                System.out.println("recipient: " + recipient);
                if(!ct.writeFile(sender,recipient,new File(fileN))){
                    clientLists.remove(i);
                    display("Disconnected client " + ct.username + "removed from list");
                }
            }
        }
        sg.appendEvent(time +" file sent to " + recipient + "\n");

    }

    synchronized void remove(int id){
        for(int i = 0; i < clientLists.size(); i++){
            ClientThread ct = clientLists.get(i);
            if(ct.id == id){
                clientLists.remove(i);
            }
        }
    }
    class ClientThread extends Thread{
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        FileOutputStream fOut;
        int id;
        String username;
        Chatmessage cm;
        String date;
        File f;
        FileInputStream fInput;
        ClientThread(Socket socket){
            id = ++uniqueID;
            this.socket = socket;
            System.out.println("unique id: " + id);
            System.out.println("Thread trying to create Object I/O stream");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String)sInput.readObject();
                f = new File("D:/Idea_projects/" +
                "file_transfer/server_storage/received" + id + ".txt");
                fOut = new FileOutputStream(f);
            }catch (IOException e){
                display("Exception creating new I/O stream");
            }catch (ClassNotFoundException e){

            }
            date = new Date().toString() + "\n";
        }
        public void run(){
            boolean keepgoing = true;
            while (keepgoing){
                try {
                    cm = (Chatmessage) sInput.readObject();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                int type = cm.getType();
                if(type == Chatmessage.FILE || type == Chatmessage.CLIENTSERVERONLY || type == Chatmessage.CONFIRM){
                    Info info;
                    if(type == Chatmessage.CLIENTSERVERONLY){
                        cm.fileId = idRun;
                        idRun++;
                        int found = 0;
                        String recipient = cm.getRecipient();
                        int fileId = cm.fileId;
                        System.out.println("File id: " + fileId);
                        String fileName = cm.getFileName();
                        String filseSize = cm.getFileSize();
                        Server.remainingChunkNumber = Server.chunkNumber;
                        System.out.println("remaining chunk: " + Server.remainingChunkNumber);
                        System.out.println("chunk number: " + Server.chunkNumber);
                        Double requiredChunk = Math.ceil(Double.valueOf(filseSize)/(double) buffer_size);
                        for(int i = 0; i < clientLists.size(); i++){
                            ClientThread ct = clientLists.get(i);
                            if(ct.username.equals(recipient)){
                                found = 1;
                                break;
                            }
                        }
                        if(found == 1){
                            info = new Info(username,recipient,fileName,fileId);
                            infoObjects.add(info);
                            if(requiredChunk <= (double) chunkNumber){
                                writeMessage(sdf.format(new Date()) +
                                        " FROM SERVER:Server can store " + requiredChunk + " chunks\n" +
                                        "Please write 'Yes' or 'No'\n");
                            }
                            else {
                                writeMessage(sdf.format(new Date()) +
                                        " FROM SERVER:Server can not store " + requiredChunk + " chunks\n");
                            }
                        }
                        else{
                            writeMessage(sdf.format(new Date()) + " " +
                                    recipient + " not connected\n");
                        }
                    }
                    else if(type == Chatmessage.FILE){
                        String fromClient;
                        String fileSize;
                        //FlagClass lockObject;
                        System.out.println("Current thread: "+ Thread.currentThread().getName()
                        + " " + Thread.currentThread().getId());
                        try {
                            fromClient = cm.getServerMessage();
                            fileSize = cm.getFileSize();
                            if(fromClient.equals("yes")){
                                if(saveFile(fileSize)){
                                    System.out.println("info objects size: " + infoObjects.size());
                                    for(int i = 0; i < infoObjects.size(); i++){
                                        Info info1 = infoObjects.get(i);
                                        System.out.println("IdRun: " + idRun);
                                        System.out.println("getFieldId: " + info1.getFileId());
                                        if(info1.getFileId() == idRun - 1){
                                            System.out.println("in File");
                                            broadcastServer(info1.getSender() + " wants to send you a file "+
                                                    "Please write 'yes' or 'no' and click" +
                                                    " 'confirm' button below\n",info1.getRecipient());
                                        }
                                    }
                                }
                            }
                            else{
                                close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        String fromReceiver;
                        fromReceiver = cm.getServerMessage();
                        System.out.println(fromReceiver);
                        if(fromReceiver.equals("yes")){
                            System.out.println(idRun);
                            System.out.println(infoObjects.size());
                            if(pendingIndicator == 0){
                                for(int i = 0; i < infoObjects.size(); i++){
                                    Info info1 = infoObjects.get(i);
                                    System.out.println(info1.getRecipient());
                                    System.out.println(idRun);
                                    if(info1.getFileId() == idRun - 1){
                                        //boolean found;
                                        System.out.println(idRun);
                                        try {
                                            System.out.println(info1.getRecipient());
                                            sendFile(info1.getSender(),info1.getRecipient(),info1.getFileName());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (ClassNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                            else{
                                for(int i = 0; i < pendingList.size(); i++){
                                    Pending pending = pendingList.get(i);
                                    if(pending.getRecipient().equals(username)){
                                        try {
                                            sendFile(pending.getSender(), pending.getRecipient(),
                                                    pending.getFile().getName());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (ClassNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                        }
                        else {
                            close();
                        }
                    }
                }
                if(type == Chatmessage.MESSAGE){
                    String recipient = cm.getRecipient();
                    String message = cm.getMessage();
                    System.out.println("Message");
                    boolean found = broadcast(username + ": " + message, recipient);
                    if(found == false){
                        writeMessage(recipient + " not connected\n");
                    }

                }
                if(type == Chatmessage.LOGOUT){
                    display(username + " disconnected with a LOGOUT message\n");
                    keepgoing = false;
                }
                if(type == Chatmessage.WHOISIN){
                    writeMessage("List of the users connected at " + sdf.format(new Date()) + "\n");
                    for (int i = 0; i < clientLists.size(); i++){
                        ClientThread ct = clientLists.get(i);
                        writeMessage((i + 1) + ": " + ct.username + " since " + ct.date);
                    }
                }
            }
            remove(id);
            close();
        }
        private void close(){
            try{
                if(sOutput != null)sOutput.close();
                if(sInput != null)sInput.close();
                if(socket != null)socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        synchronized boolean writeMessage(String msg){
            if(!socket.isConnected()){
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            } catch (IOException e) {
                display("Error sending message to " + username);
                e.printStackTrace();
            }
            return true;
        }

        synchronized boolean writeFile(String sender,String recipient,File fileN){
            if(!socket.isConnected()){
                close();
                return false;
            }
            try {
                System.out.println("Content writing");
                fInput = new FileInputStream(fileN);
                byte[] buffer = new byte[buffer_size];
                Integer bytesRead = 0;
                sOutput.writeObject(fileN.length());
                while ((bytesRead = fInput.read(buffer)) > 0){
                    System.out.println(logOut);
                    System.out.println("sInput available: " + sInput.available());
                    if (sInput.available() > 0) {
                        Object o = sInput.readObject();
                        if(o instanceof Chatmessage){
                            if(((Chatmessage) o).getType() == Chatmessage.LOGOUT){
                                Pending pending = new Pending(sender,recipient,fileN);
                                pendingList.add(pending);
                                return false;
                            }

                        }
                    } else {
                        sOutput.writeObject(bytesRead);
                        sOutput.writeObject(Arrays.copyOf(buffer, buffer.length));
                        Thread.sleep(1000);
                    }

                }
                System.out.println("content written");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        }
        private boolean saveFile(String fileSize) throws IOException, ClassNotFoundException {
            System.out.println("in save file\n");
            byte[] buffer = new byte[buffer_size];
            Integer bytesRead = 0;
            Object o;
            Integer totalByteRead = 0;
            int interrupt = 0;
            display("Server downloading file from " + username + "\n");
            do {
                System.out.println("in loop\n");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                o = sInput.readObject();
                if(o instanceof Chatmessage){
                    if(((Chatmessage) o).getType() == Chatmessage.LOGOUT){
                        interrupt = 1;
                        writeMessage(serverSig);
                        fOut.close();
                        f.delete();
                        break;
                    }
                }
                System.out.println("1");
                System.out.println(o.getClass());
                if (!(o instanceof Integer)) {
                    display("Something is wrong 1");
                    return false;
                }
                bytesRead = (Integer)o;
                totalByteRead += bytesRead;
                System.out.println("Bytes read: " + bytesRead);
                Server.chunkNumber -= (bytesRead/Server.buffer_size);
                System.out.println("2");
                o = sInput.readObject();
                if(o instanceof Chatmessage){
                    if(((Chatmessage) o).getType() == Chatmessage.LOGOUT){
                        interrupt = 1;
                        writeMessage(serverSig);
                        fOut.close();
                        f.delete();
                        break;
                    }
                }
                System.out.println("3");
                if (!(o instanceof byte[])) {
                    display("Something is wrong 2");
                    return false;
                }
                buffer = (byte[])o;
                System.out.println("4");
                fOut.write(buffer, 0, bytesRead);
                System.out.println("5");
                sOutput.writeObject(acknowledgement);
                /*while (true){
                    o = sInput.readObject();
                    if(o instanceof String){
                        if(o.equals("yes")){
                            break;
                        }
                    }
                }*/

            } while (bytesRead == buffer_size);
            System.out.println("total byte read: " + totalByteRead);
            if(interrupt == 0){
                if(totalByteRead.equals(Integer.valueOf(fileSize))){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    display("Received file from " + username + "\n");
                    writeMessage("FROM SERVER: Download complete\n");
                    return true;
                }
                else{
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    writeMessage("FROM SERVER: File size does not match the initial fileSize.Server is deleteing " +
                            "the file\n");
                    fOut.close();
                    f.delete();
                    return false;
                }
            }
            else {
                return false;
            }
        }
    }
}

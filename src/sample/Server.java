package sample;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.concurrent.atomic.AtomicInteger;

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


    //public static ArrayList<Integer> loggedIn = new ArrayList<>();


    public Server(int port, ServerGui sg){
        this.sg = sg;
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        clientLists = new ArrayList<ClientThread>();
        infoObjects = new ArrayList<>();
        //repeat = 0;
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

    private synchronized void broadcast(String msg,String recipient){
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
                else sg.appendChat(message);
            }

        }

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

    private synchronized void sendFile(String recipient,String fileN) throws IOException, ClassNotFoundException {
        String time = sdf.format(new Date());
        String message = time + " sending file to " + recipient + "\n";
        sg.appendEvent(message);

        for (int i = clientLists.size(); --i>= 0;){
            ClientThread ct = clientLists.get(i);
            if(ct.username.equals(recipient)){
                System.out.println("recipient: " + recipient);
                if(!ct.writeFile(new File(fileN))){
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
    /*public static void main(String[] args){
        int portNumber = 1500;
        Server server = new Server(portNumber);
        server.start();
    }*/

    class ClientThread extends Thread{
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        FileOutputStream fOut;
        int id;
        String username;
        Chatmessage cm;
        String date;
        //ArrayList<Info> infoObjects;

        ClientThread(Socket socket){
            id = ++uniqueID;
            this.socket = socket;
            System.out.println("unique id: " + id);
            System.out.println("Thread trying to create Object I/O stream");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String)sInput.readObject();
                fOut = new FileOutputStream(new File("D:/Idea_projects/file_transfer-master/" +
                        "file_transfer-master/server_storage/received" + id + ".txt"));
                //infoObjects = new ArrayList<>();
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
                            info = new Info(recipient,fileName,fileId);
                            infoObjects.add(info);
                            if(requiredChunk <= (double) chunkNumber){
                                writeMessage("FROM SERVER:Server can store " + requiredChunk + " chunks\n" +
                                        "Please write 'Yes' or 'No'\n");
                            }
                            else {
                                writeMessage("FROM SERVER:Server can not store " + requiredChunk + " chunks\n");
                            }

                        }
                        else{
                            writeMessage(recipient + " not connected\n");
                        }

                        //System.out.println(infoObjects);

                    }
                    else if(type == Chatmessage.FILE){
                        String fromClient;
                        try {
                            fromClient = cm.getServerMessage();
                            if(fromClient.equals("yes")){
                                saveFile();
                                System.out.println("info objects size: " + infoObjects.size());
                                for(int i = 0; i < infoObjects.size(); i++){
                                    Info info1 = infoObjects.get(i);
                                    System.out.println("IdRun: " + idRun);
                                    System.out.println("getFieldId: " + info1.getFileId());
                                    if(info1.getFileId() == idRun - 1){
                                        //sendFile(info1.getRecipient(),info1.getFileName());
                                        System.out.println("in File");
                                        broadcastServer("Please write 'yes' or 'no' and click" +
                                                " 'confirm' button below\n",info1.getRecipient());
                                    }
                                }
                                //sendFile(recipient, fileName);
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
                            for(int i = 0; i < infoObjects.size(); i++){
                                Info info1 = infoObjects.get(i);
                                System.out.println(info1.getRecipient());
                                System.out.println(idRun);
                                if(info1.getFileId() == idRun - 1){

                                    System.out.println(idRun);
                                    try {
                                        System.out.println(info1.getRecipient());
                                        sendFile(info1.getRecipient(),info1.getFileName());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    //broadcast(info1.getRecipient(),info1.getFileName());
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
                    broadcast(username + ": " + message, recipient);
                }
                if(type == Chatmessage.LOGOUT){
                    display(username + "disconnected with a LOGOUT message");
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
        private boolean writeMessage(String msg){
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

        private boolean writeFile(File fileN){
            if(!socket.isConnected()){
                close();
                return false;
            }
            try {
                //sOutput.writeObject(fileId);
                //sOutput.writeObject(fileN.getName());
                System.out.println("Content writing");
                byte[] content = Files.readAllBytes(fileN.toPath());
                sOutput.writeObject(content);
                System.out.println("content written");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        private boolean saveFile() throws IOException, ClassNotFoundException {
            System.out.println("in save file\n");
            byte[] buffer = new byte[buffer_size];
            Integer bytesRead = 0;
            Object o;
            /*o = sInput.readObject();
            if(o instanceof Integer){
                System.out.println("file id: " + o.toString());
            }*/
            do {
                System.out.println("in loop\n");
                //System.out.println(sInput.readObject());

                o = sInput.readObject();
                System.out.println("1");
                System.out.println(o.getClass());
                if (!(o instanceof Integer)) {
                    display("Something is wrong 1");
                    return false;
                }

                bytesRead = (Integer)o;

                Server.chunkNumber -= (bytesRead/Server.buffer_size);

                System.out.println("2");

                o = sInput.readObject();
                System.out.println("3");

                if (!(o instanceof byte[])) {
                    display("Something is wrong 2");
                    return false;
                }

                buffer = (byte[])o;
                System.out.println("4");

                // 3. Write data to output file.
                fOut.write(buffer, 0, bytesRead);
                System.out.println("5");

                //writeMessage();
            } while (bytesRead == buffer_size);

            display("Received file from " + username + "\n");
            return true;
        }

    }
}

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
    private ServerGui sg;
    private SimpleDateFormat sdf;
    private int port;
    private boolean keepGoing;
    public static int buffer_size = 100;


    //public static ArrayList<Integer> loggedIn = new ArrayList<>();


    public Server(int port, ServerGui sg){
        this.sg = sg;
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        clientLists = new ArrayList<ClientThread>();
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
            if(ct.username.equals(recipient)){
                if(!ct.writeMessage(message)){
                    clientLists.remove(i);
                    display("Disconnected client " + ct.username + "removed from list");
                }
                else sg.appendChat(message);
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
        sg.appendEvent("File sent to " + recipient);

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
                String recipient = cm.getRecipient();
                String message = cm.getMessage();
                String fileName = cm.getFileName();
                switch (cm.getType()){
                    case Chatmessage.FILE:
                        try {
                            saveFile();
                            //sendFile(recipient);
                            sendFile(recipient, fileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        System.out.println("1");
                        break;
                    case Chatmessage.MESSAGE:
                        broadcast(username + ": " + message, recipient);
                        break;
                    case Chatmessage.LOGOUT:
                        display(username + "disconnected with a LOGOUT message");
                        keepgoing = false;
                        break;
                    case Chatmessage.WHOISIN:
                        writeMessage("List of the users connected at " + sdf.format(new Date()) + "\n");
                        for (int i = 0; i < clientLists.size(); i++){
                            ClientThread ct = clientLists.get(i);
                            writeMessage((i + 1) + ": " + ct.username + " since " + ct.date);
                        }
                        break;
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
                    //return false;
                }

                bytesRead = (Integer)o;

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
            } while (bytesRead == buffer_size);

            display("Received file from " + username + "\n");
            return true;
        }

    }
}

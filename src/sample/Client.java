package sample;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;


public class Client {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private FileInputStream fInput;
    private Socket socket;
    //private DataOutputStream dOut;

    private static ClientGUI cg;
    private String server;
    private String username;
    private int port;

    Client(String server, int port, String username, ClientGUI cg){
        this.server = server;
        this.port = port;
        this.username = username;
        this.cg = cg;
    }

    public boolean start(){
        try {
            System.out.println(9);
            socket = new Socket(server,port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        System.out.println(10);
        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort() + "\n" +
                "Username: " + username;
        display(msg);

        try{
            System.out.println(11);
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }catch (IOException sIO){
            display("Exception creating new I/O streams: " + sIO);
            return false;
        }
        new ListenFromServer().start();
        try{
            sOutput.writeObject(username);
        } catch (IOException e) {
            display("Exception logIN: " + e);
            disconnect();
            e.printStackTrace();
            return false;
        }
    return true;
    }
    public static void display(String msg){
        cg.append(msg + "\n");
    }
    void sendMessage(Chatmessage msg){
        try{
            File file = new File(msg.getFileName());
            sOutput.writeObject(msg);
            fInput = new FileInputStream(file);
            byte [] buffer = new byte[Server.buffer_size];
            Integer bytesRead = 0;
            display(username + " sending file to server\n");
            while ((bytesRead = fInput.read(buffer)) > 0) {
                sOutput.writeObject(bytesRead);
                sOutput.writeObject(Arrays.copyOf(buffer, buffer.length));
            }
            display("File sent\n");
        } catch (IOException e) {
            display("Exception writing to server");
            e.printStackTrace();
        }
    }
    private void disconnect(){
        try {
            if(sInput != null)sInput.close();
            if(sOutput != null)sOutput.close();
            if(socket != null)socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(cg != null){
            cg.connectionFailed();
        }
    }
    /*public static void main(String[] args){
        int portNumber = 1500;
        String serverAddress = "localhost";
        String username = "Anonymous";

        Client client = new Client(serverAddress, portNumber, username);


    }*/
    class ListenFromServer extends Thread{
        public void run(){
            while (true){
                try{
                    String msg = (String)sInput.readObject();
                    cg.append(msg);
                    System.out.println(msg);
                } catch (IOException e) {
                    display("Server has closed the connection: " + e);
                    e.printStackTrace();
                    break;

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}

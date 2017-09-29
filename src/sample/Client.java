package sample;

import com.sun.scenario.effect.impl.prism.ps.PPSBlend_ADDPeer;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static sample.Client.*;
import static sample.Server.*;

public class Client {
    public static ObjectInputStream sInput;
    public static ObjectOutputStream sOutput;
    public static FileInputStream fInput;
    private Socket socket;
    public static ClientGUI cg;
    private String server;
    public static String username;
    private int port;
    public static SimpleDateFormat sdf = new SimpleDateFormat();
    public static int serverSignal = 0;
    public Chatmessage msg;
    public static FileOutputStream fOut;
    Client(String server, int port, String username, ClientGUI cg){
        this.server = server;
        this.port = port;
        this.username = username;
        this.cg = cg;
        this.msg = null;
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
            System.out.println(12);
            sOutput = new ObjectOutputStream(socket.getOutputStream());
            System.out.println(13);
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
        cg.append(sdf.format(new Date()) + ": " + msg + "\n");
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
}
class ListenFromServer extends Thread{
    public void run(){
        while (true){
            try{
                byte[] buffer = new byte[buffer_size];
                Integer bytesRead = 0;
                Object o = sInput.readObject();
                System.out.println("byte instance");

                if(o instanceof String){
                    if(o.equals(serverSig)){
                        Client.serverSignal = 1;
                    }
                    cg.append(o.toString());
                }
                else{
                    fOut = new FileOutputStream(new File("rtc.txt"));
                    Long length = (Long)o;
                    int i = 0;
                    do{
                        Thread.sleep(1000);
                        if(i == length){
                            break;
                        }
                        System.out.println("received");
                        o = sInput.readObject();
                        if(!(o instanceof Integer)){
                            System.out.println("not found Integer");
                        }
                        cg.append(sdf.format(new Date()) + ": " + "Received 1 chunk\n");
                        bytesRead = (Integer)o;
                        o = sInput.readObject();
                        if(!(o instanceof byte[])){
                            System.out.println("not found byte");
                        }
                        buffer = (byte[])o;
                        i += bytesRead;
                        fOut.write(buffer, 0, bytesRead);
                    }while (bytesRead == buffer_size);
                    System.out.println("received file");
                    cg.append(sdf.format(new Date()) + ": " + "Received file ..\n");
                }

            } catch (IOException e) {
                display("Server has closed the connection: " + e);
                e.printStackTrace();
                break;

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
class WriteToServer extends Thread {
    private Chatmessage chatmessage;
    WriteToServer(Chatmessage chatmessage) {
        this.chatmessage = chatmessage;
    }

    public void run() {
        try {
            sOutput.writeObject(chatmessage);
            File file = new File(chatmessage.getFileName());
            fInput = new FileInputStream(file);
            byte[] buffer = new byte[buffer_size];
            Integer bytesRead = 0;
            display(username + " sending file to server\n");

            while ((bytesRead = fInput.read(buffer)) > 0) {
                if(serverSignal != 1){
                    sOutput.writeObject(bytesRead);
                    sOutput.writeObject(Arrays.copyOf(buffer, buffer.length));
                    Thread.sleep(1000);
                    System.out.println("1st chunk sent");
                }
                else {
                    break;
                }
            }
            if(serverSignal != 1){
                Thread.sleep(1000);
                display("File sent\n");
            }
            else {
                display("File transmission cancelled\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

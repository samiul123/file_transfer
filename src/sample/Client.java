package sample;

import com.sun.scenario.effect.impl.prism.ps.PPSBlend_ADDPeer;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static sample.Client.*;
import static sample.Server.*;

public class Client {
    public static ObjectInputStream sInput;
    public static ObjectOutputStream sOutput;
    public static FileInputStream fInput;
    private Socket socket;
    public static ClientGUI cg;
    public static final byte framKind_Ack = 0;
    public static final int frameKind_Data = 1;
    private String server;
    public static String username;
    private int port;
    public static SimpleDateFormat sdf = new SimpleDateFormat();
    public static int serverSignal = 0;
    public Filemessage msg;
    public static FileOutputStream fOut;
    public static String serverAcknow;
    public static int sendTime;
    public static int receiveTime;
    public static String fileName = "";
    public int loggedIn = 0;
    public static String timeOutMsg = "Timed out.Cancel transmission\n";
    public static String clientcheckSum ="";

    Client(String server, int port, String username, ClientGUI cg) {
        this.server = server;
        this.port = port;
        this.username = username;
        this.cg = cg;
        this.msg = null;
    }

    public String getUserName() {
        return username;
    }

    public boolean start() {
        try {
            System.out.println(9);
            socket = new Socket(server, port);
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
        try {
            System.out.println(11);
            sInput = new ObjectInputStream(socket.getInputStream());
            System.out.println(12);
            sOutput = new ObjectOutputStream(socket.getOutputStream());
            System.out.println(13);
            serverAcknow = "";
        } catch (IOException sIO) {
            display("Exception creating new I/O streams: " + sIO);
            return false;
        }
        new ListenFromServer().start();
        try {
            sOutput.writeObject(username);
        } catch (IOException e) {
            display("Exception logIN: " + e);
            disconnect();
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void display(String msg) {
        cg.append(sdf.format(new Date()) + ": " + msg + "\n");
    }

    private void disconnect() {
        try {
            if (sInput != null) sInput.close();
            if (sOutput != null) sOutput.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (cg != null) {
            cg.connectionFailed();
        }
    }
    public StringBuilder doDeStuff(String data){
        int dataLength = data.length();
        int counter = 0;
        byte[] buffer = new byte[buffer_size];

        StringBuilder mainData = new StringBuilder();
        StringBuilder out = new StringBuilder();
        String toFile;
        for(int i = 8; i < dataLength - 8; i++){
            mainData.append(data.charAt(i));
        }
        for(int i = 0; i < mainData.length(); i++){
            if(mainData.charAt(i) == '1'){
                counter++;
                out.append(mainData.charAt(i));
            }else{
                out.append(mainData.charAt(i));
                counter = 0;
            }
            if(counter == 5){
                if((i+2)!=mainData.length())
                    out.append(mainData.charAt(i+2));
                else
                    out.append('1');
                i=i+2;
                counter = 1;
            }
        }
        System.out.println("DeStuffed message: " + out);
        return out;
    }
    class ListenFromServer extends Thread {
        public void run() {
            while (true) {
                try {
                    byte[] buffer = new byte[buffer_size];
                    Integer bytesRead = 0;
                    System.out.println("client waiting");
                    Object o = sInput.readObject();
                    System.out.println("byte instance");


                    if (o instanceof String) {
                        if (o.equals(serverSig)) {
                            Client.serverSignal = 1;
                        } else if (doDeStuff((String) o).substring(0,8).equals("00000000")) {
                            System.out.println("receive time: " + receiveTime);
                            Client.serverAcknow = Server.acknowledgement;
                            System.out.println("Client server acknowListen: " + Client.serverAcknow);
                            receiveTime = Calendar.getInstance().get(Calendar.MILLISECOND);
                            cg.append(Server.acknowledgement);
                            continue;
                        }
                        else if(o.equals(Server.checkSumMismatch)){
                            clientcheckSum = Server.checkSumMismatch;
                            cg.append(clientcheckSum);
                            continue;
                        }
                        cg.append(o.toString());
                    } else if (o instanceof File) {
                        File file = (File) o;
                        Client.fileName = file.getName();
                    } else {
                        fOut = new FileOutputStream(new File("receiver_" + Client.fileName));
                        Long length = (Long) o;
                        int i = 0;
                        do {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (i == length) {
                                break;
                            }
                            System.out.println("received");
                            o = sInput.readObject();
                            if (!(o instanceof Integer)) {
                                System.out.println("not found Integer");
                            }
                            cg.append(sdf.format(new Date()) + ": " + "Received 1 chunk\n");
                            bytesRead = (Integer) o;
                            o = sInput.readObject();
                            if (!(o instanceof byte[])) {
                                System.out.println("not found byte");
                            }
                            buffer = (byte[]) o;
                            i += bytesRead;
                            fOut.write(buffer, 0, bytesRead);
                        } while (bytesRead == buffer_size);
                        System.out.println("received file");
                        cg.append(sdf.format(new Date()) + ": " + "Received file ..\n");
                    }

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
class WriteToServer extends Thread {
    private Filemessage filemessage;

    WriteToServer(Filemessage filemessage) {
        this.filemessage = filemessage;
    }

    public void run() {
        try {
            sOutput.writeObject(filemessage);
            File file = new File(filemessage.getFileName());
            fInput = new FileInputStream(file);
            byte[] buffer = new byte[buffer_size];

            //String deStuffed;
            byte checkSum = 0;
            Integer bytesRead = 0;

            //ArrayList<String> toSend;
            display(username + " sending file to server\n");
            int i = 0;
            byte seq = 0;
            byte ackNo = 0;
            while ((bytesRead = fInput.read(buffer)) > 0) {
                //toSend = new StringBuilder();
                if(serverSignal != 1){
                    Thread.sleep(1000);
                    StringBuilder toSend = new StringBuilder();
                    byte[] buffer1 = new byte[buffer_size];
                    i++;
                    seq++;
                    ackNo++;
                    sOutput.writeObject(bytesRead);
                    sOutput.writeObject(Arrays.copyOf(buffer, buffer.length));
                    //building frame
                    //1st frame kind
                    String frame_kind = ("0000000" + Integer.toBinaryString(0xFF & frameKind_Data)).replaceAll(".*(.{8})$", "$1");
                    toSend.append(frame_kind);
                    System.out.println("FRAME_KIND: " + frame_kind);
                    //2nd sequence number
                    String seqNo =("0000000" + Integer.toBinaryString(0xFF & seq)).replaceAll(".*(.{8})$", "$1");
                    toSend.append(seqNo);
                    System.out.println("SEQ_NO: " + seqNo);

                    //3rd acknowledge number
                    String ack = ("0000000" + Integer.toBinaryString(0xFF & ackNo)).replaceAll(".*(.{8})$", "$1");
                    toSend.append(ack);
                    System.out.println("ACK_NO: " + ack);

                    //4th payload
                    for(int k = 0; k < buffer.length; k++){
                        String s =("0000000" + Integer.toBinaryString(0xFF & buffer[k])).replaceAll(".*(.{8})$", "$1");
                        toSend.append(s);
                    }
                    System.out.println("PAYLOAD: " + String.valueOf(toSend).substring(24));

                    //5th checksum on payload
                    checkSum = hasCheckSum(buffer);
                    String checkSumStr = ("0000000" + Integer.toBinaryString(0xFF & checkSum)).replaceAll(".*(.{8})$", "$1");
                    toSend.append(checkSumStr);
                    System.out.println("CHECKSUM: " + checkSumStr);

                    //ReTransmission reTransmission = new ReTransmission(frame_kind,seqNo,ack,checkSumStr,buffer);
                    //toBeReTransmitted.add(reTransmission);
                    //System.out.println("Before bit stuffing: " + String.valueOf(toSend));
                    //frame built

                    String toBetransferredwithBitStuffed = doBitStuff(toSend);
                    //System.out.println("After bit stuffing: " + String.valueOf(toBetransferredwithBitStuffed));

                    sOutput.writeObject(toBetransferredwithBitStuffed);
                    //System.out.println("1 chunk sent");
                    sendTime = Calendar.getInstance().get(Calendar.MILLISECOND);
                    System.out.println("Send time: " + sendTime);
                    while (true){
                        //System.out.println("Client server acknow: " + Client.serverAcknow);
                        if(Client.serverAcknow.equals(Server.acknowledgement)){
                            //System.out.println("12");
                            if(receiveTime - sendTime <= 30000){
                                //System.out.println("13");
                                while (true){
                                    //System.out.println("Client GUI: " + ClientGUI.continueD);
                                    if(serverSignal != 1){
                                        //System.out.println("14");
                                        sOutput.writeObject("yes");
                                        break;
                                    }
                                }
                            }else{
                                //if time out ,sending again
                                sOutput.writeObject(timeOutMsg);
                                sOutput.writeObject(bytesRead);
                                sOutput.writeObject(Arrays.copyOf(buffer, buffer.length));
                                sOutput.writeObject(doBitStuff(toSend));
                            }
                            break;
                        }
                        else if(clientcheckSum.equals(Server.checkSumMismatch)){
                            sOutput.writeObject(bytesRead);
                            sOutput.writeObject(Arrays.copyOf(buffer, buffer.length));
                            sOutput.writeObject(doBitStuff(toSend));
                            break;
                        }
                    }
                }
                if(receiveTime - sendTime > 30000){
                    break;
                }
                if(serverSignal == 1) {
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
    public String doBitStuff(StringBuilder data){

        String wrapper = "01111110";
        int counter = 0;
        StringBuilder res = new StringBuilder();
        StringBuilder dataToBin = new StringBuilder();
        //convert 1st buffer into binary string
        /*for(int i = 0; i < data.length; i++){
            dataToBin.append(Integer.toBinaryString(data[i]));
        }*/
        System.out.println("Before bit stuffing: " + data);
        System.out.println("Stuffing chunk");
        for(int i = 0; i < data.length(); i++){
            if(data.charAt(i) == '1'){
                counter++;
                res.append(data.charAt(i));
            }
            else{
                res.append(data.charAt(i));
                counter = 0;
            }
            if(counter == 5){
                res.append('0');
                counter = 0;
            }
        }
        String in = wrapper + res + wrapper;
        System.out.println("After bit Stuffing: " + in);
        return in;
    }
    public byte hasCheckSum(byte[] data){
        byte xorChecksum = 0;
        for(int i = 0; i < data.length; i++){
            xorChecksum ^= data[i];
        }
        //display("CheckSum: " + xorChecksum);
        return xorChecksum;
    }

}

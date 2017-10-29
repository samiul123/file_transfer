package sample;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static sample.Client.frameKind_Data;
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
    public static String alreadyLoggedInString = "User with same name is already logged in\n" +
            "Try with another username\n";
    public static int alreadyLoggedIn = 0;
    public static int receiverLogOut = 0;
    public static int interrupt = 0;
    public static int timeOut = 0;
    public static int sizeMismatch = 0;
    public static String checkSumMismatch = "CheckSum did not match\n";
    //Random rand;
    public Server(int port, ServerGui sg){
        this.sg = sg;
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        clientLists = new ArrayList<>();
        infoObjects = new ArrayList<>();
        pendingList = new ArrayList<>();
    }
    public void start(){
        keepGoing = true;
        try {

            ServerSocket serverSocket = new ServerSocket(port);
            while (keepGoing){
                int found = 0;
                display("Server waiting for Clients on port " + port + "\n");
                Socket socket = serverSocket.accept();
                if(!keepGoing){
                    break;
                }
                ClientThread t = new ClientThread(socket);
                System.out.println("new user: " + t.username);
                System.out.println("Client list Size: " + clientLists.size());
                if(clientLists.size() >= 1){
                    for(int i = 0; i < clientLists.size(); i++){
                        ClientThread ct = clientLists.get(i);
                        if(ct.username.equals(t.username)){
                            System.out.println("Already logged IN");
                            alreadyLoggedIn = 1;
                            System.out.println("found paise");
                            found = 1;
                            t.writeMessage(alreadyLoggedInString);
                            t = null;
                            break;
                        }
                    }
                    if(found == 0){
                        System.out.println("found pay nai");
                        alreadyLoggedIn = 0;
                    }
                }
                if(alreadyLoggedIn != 1){
                    clientLists.add(t);
                    System.out.println("after add Client list size: " + clientLists.size());
                    display(t.username + " just connected");
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
            }
            if(alreadyLoggedIn != 1){
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

    private synchronized void sendFile(String sender,String recipient,File fileN) throws IOException, ClassNotFoundException {
        String time = sdf.format(new Date());
        String message = time + " sending file to " + recipient + "\n";
        sg.appendEvent(message);

        for (int i = clientLists.size(); --i>= 0;){
            ClientThread ct = clientLists.get(i);
            if(ct.username.equals(recipient)){
                System.out.println("recipient: " + recipient);
                if(!ct.writeFile(sender,recipient,fileN)){
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
        PrintStream ps;
        int id;
        String username;
        Filemessage fm;
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
                    fm = (Filemessage) sInput.readObject();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                int type = fm.getType();
                if(type == Filemessage.FILE || type == Filemessage.CLIENTSERVERONLY || type == Filemessage.CONFIRM){
                    Info info;
                    if(type == Filemessage.CLIENTSERVERONLY){
                        fm.fileId = idRun;
                        idRun++;
                        int found = 0;
                        String recipient = fm.getRecipient();
                        int fileId = fm.fileId;
                        System.out.println("File id: " + fileId);
                        String fileName = fm.getFileName();
                        String filseSize = fm.getFileSize();
                        f = new File("Server_" + fileName);
                        try {
                            fOut = new FileOutputStream(f);
                            //ps = new PrintStream(fOut);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        Server.remainingChunkNumber = Server.chunkNumber;
                        System.out.println("remaining chunk: " + Server.remainingChunkNumber);
                        System.out.println("chunk number: " + Server.chunkNumber);
                        for(int i = 0; i < clientLists.size(); i++){
                            ClientThread ct = clientLists.get(i);
                            if(ct.username.equals(recipient)){
                                found = 1;
                                break;
                            }
                        }
                        if(found == 1){
                            Double requiredChunk = Math.ceil(Double.valueOf(filseSize)/(double) buffer_size);
                            info = new Info(username,recipient,f,fileId);
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
                    else if(type == Filemessage.FILE){
                        String fromClient;
                        String fileSize;
                        System.out.println("Current thread: "+ Thread.currentThread().getName()
                        + " " + Thread.currentThread().getId());
                        try {
                            fromClient = fm.getServerMessage();
                            fileSize = fm.getFileSize();
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
                                else{
                                    System.out.println("Interrupt: " + interrupt);
                                    if(interrupt == 1){
                                        display(username + " disconnected with a LOGOUT message\n");
                                        keepgoing = false;
                                    }
                                    else if(timeOut == 1){
                                        display("Timed out while downloading\n");
                                    }
                                    else if(sizeMismatch == 1){
                                        display("File size did not match\n");
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
                        fromReceiver = fm.getServerMessage();
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
                                                    pending.getFile());
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
                if(type == Filemessage.LOGOUT){
                    display(username + " disconnected with a LOGOUT message\n");
                    keepgoing = false;
                }
                if(type == Filemessage.ONLINE){
                    writeMessage("Online users: " + sdf.format(new Date()) + "\n");
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
                int successful = 0;
                System.out.println("Content writing");
                fInput = new FileInputStream(fileN);
                byte[] buffer = new byte[buffer_size];
                Integer bytesRead = 0;
                sOutput.writeObject(fileN);
                sOutput.writeObject(fileN.length());
                while ((bytesRead = fInput.read(buffer)) > 0){
                    System.out.println(logOut);
                    System.out.println("receiver logOut: " + Server.receiverLogOut);
                    if (receiverLogOut == 1) {
                        Object o = sInput.readObject();
                        if(o instanceof Filemessage){
                            if(((Filemessage) o).getType() == Filemessage.LOGOUT){
                                Pending pending = new Pending(sender,recipient,fileN);
                                pendingList.add(pending);
                                return false;
                            }
                        }
                    }
                    else {
                        successful = 1;
                        sOutput.writeObject(bytesRead);
                        sOutput.writeObject(Arrays.copyOf(buffer, buffer.length));
                        Thread.sleep(1000);
                    }
                }
                if(successful == 1){
                    System.out.println("File successfully written");
                    fInput.close();
                    fileN.delete();
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
        //ArrayList<Integer> integers = new ArrayList<>();
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
        public Integer hasCheckSum(byte[] data){
            Integer xorChecksum = 0;
            for(int i = 0; i < data.length; i++){
                xorChecksum ^= data[i];
            }
            display("ServerCheckSum: " + xorChecksum);
            return xorChecksum;
        }
        byte[] fromBinary( String s )
        {
            int sLen = s.length();
            byte[] toReturn = new byte[(sLen + Byte.SIZE - 1) / Byte.SIZE];
            char c;
            for( int i = 0; i < sLen; i++ )
                if( (c = s.charAt(i)) == '1' )
                    toReturn[i / Byte.SIZE] = (byte) (toReturn[i / Byte.SIZE] | (0x80 >>> (i % Byte.SIZE)));
                else if ( c != '0' )
                    throw new IllegalArgumentException();
            return toReturn;
        }
        public String doBitStuff(StringBuilder data){

            String wrapper = "01111110";
            int counter = 0;
            StringBuilder res = new StringBuilder();
            StringBuilder dataToBin = new StringBuilder();
            //convert 1st buffer into binary string
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
        private boolean saveFile(String fileSize) throws IOException, ClassNotFoundException {
            System.out.println("in save file\n");
            byte[] buffer = new byte[buffer_size];
            byte[] buffer1 = new byte[buffer_size];
            StringBuilder severDestuffed = new StringBuilder();
            Integer bytesRead = 0;
            Object o;
            Integer totalByteRead = 0;
            String checksum = "";
            byte serverSeq = 0;
            byte serverAck = 0;
            //BigInteger bi;
            Integer serverCheckSum = 0;
            String servercheckSumStr ="";
            display("Server downloading file from " + username + "\n");
            do {
                System.out.println("in loop\n");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                o = sInput.readObject();
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

                System.out.println("3");
                if (!(o instanceof byte[])) {
                    display("Something is wrong 2");
                    return false;
                }
                buffer = (byte[])o;
                serverCheckSum = hasCheckSum(buffer);
                servercheckSumStr = ("0000000" + Integer.toBinaryString(0xFF & serverCheckSum)).replaceAll(".*(.{8})$", "$1");
                System.out.println("4");

                timeOut = 0;
                o = sInput.readObject();
                if(!(o instanceof String)){
                    display("Something is wrong 3");
                    return false;
                }
                display("Server received from sender: " + o);

                severDestuffed = doDeStuff((String) o);
                display("Server deStuffed: " + severDestuffed);
                String frame_kind = String.valueOf(severDestuffed).substring(0,8);
                String seq_no = String.valueOf(severDestuffed).substring(8,16);
                String ack_no = String.valueOf(severDestuffed).substring(16,24);
                String payload = String.valueOf(severDestuffed).substring(24,severDestuffed.length() - 8);
                checksum = String.valueOf(severDestuffed).substring(severDestuffed.length() - 8,severDestuffed.length());
                if(servercheckSumStr.equals(checksum)){
                    display("CheckSums matched");
                    buffer1 = fromBinary(String.valueOf(payload));
                    //fOut.write(buffer1, 0, bytesRead);
                    fOut.write(buffer1);
                    System.out.println("5");
                    //building server frame
                    StringBuilder serverFrame = new StringBuilder();
                    serverSeq++;
                    serverAck++;
                    //1st frame_kind
                    String server_frame_kind = ("0000000" + Integer.toBinaryString(0xFF & Client.framKind_Ack)).replaceAll(".*(.{8})$", "$1");
                    System.out.println("Server frame kind: " + server_frame_kind);
                    serverFrame.append(server_frame_kind);
                    //2nd seq_no
                    String server_seq_no = ("0000000" + Integer.toBinaryString(0xFF & serverSeq)).replaceAll(".*(.{8})$", "$1");
                    System.out.println("Server seq_no: " + server_seq_no);
                    serverFrame.append(server_seq_no);
                    //3rd ack no
                    String server_ack_no = ("0000000" + Integer.toBinaryString(0xFF & serverAck)).replaceAll(".*(.{8})$", "$1");
                    System.out.println("Server ack_no: " + server_ack_no);
                    serverFrame.append(server_ack_no);
                    //4th payload
                    String server_payload = ("0000000" + Integer.toBinaryString(0xFF & 0)).replaceAll(".*(.{8})$", "$1");
                    System.out.println("Server payLoad: " + server_payload);
                    serverFrame.append(server_payload);
                    //5th checksum
                    String server_checkSum = ("0000000" + Integer.toBinaryString(0xFF & 0)).replaceAll(".*(.{8})$", "$1");
                    System.out.println("Server checkSum: " + server_checkSum);
                    serverFrame.append(server_checkSum);
                    String serverFrameBitStuffed = doBitStuff(serverFrame);
                    sOutput.writeObject(serverFrameBitStuffed);

                    while (true){
                        System.out.println("Server waiting");
                        o = sInput.readObject();
                        if(o instanceof String){
                            System.out.println("no");
                            if(o.equals("yes")){
                                System.out.println("yes");
                                break;
                            }
                            else if(o.equals(Client.timeOutMsg)){
                                System.out.println("Client timeOut message: " + Client.timeOutMsg);
                                timeOut = 1;
                                break;
                            }
                        }
                        else if(o instanceof Filemessage){
                            if(((Filemessage) o).getType() == Filemessage.LOGOUT){
                                interrupt = 1;
                                display("File downloading cancelled\n");
                                writeMessage(serverSig);
                                fOut.close();
                                f.delete();
                                break;
                            }
                        }
                    }
                }
                else{
                    sOutput.writeObject(checkSumMismatch);
                }
                //buffer1 = fromBinary(String.valueOf(payload));
                //fOut.write(buffer1, 0, bytesRead);
                //fOut.write(buffer1);
                //System.out.println("5");
                //sOutput.writeObject(acknowledgement);

                if(interrupt == 1 || totalByteRead.equals(Integer.valueOf(fileSize))){
                    break;
                }
            } while (bytesRead == buffer_size);
            System.out.println("total byte read: " + totalByteRead);
            if(timeOut == 1){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                writeMessage("FROM SERVER: Server received the 'TIME OUT MESSAGE'.Server is deleting the chunks\n");
                fOut.close();
                f.delete();
                return false;
            }
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
                    sizeMismatch = 1;
                    writeMessage("FROM SERVER: File size does not match the initial fileSize.Server is deleting " +
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

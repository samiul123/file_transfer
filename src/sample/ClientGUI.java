package sample;

import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class ClientGUI extends ServerGui implements EventHandler{

    private static final long serialVersionUID = 1L;
    private Label label;
    private TextField tf;
    private TextField serverAddress, portAddress;
    private Button login, logout, whoIsIn, send;
    private TextArea ta;
    private boolean connected;
    private Client client;
    private int defaultPort = 1500;
    private String defaultHost = "localhost";
    Scene scene;
    boolean found = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Client");

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(8);
        gridPane.setHgap(10);

        HBox hBox = new HBox();
        GridPane.setConstraints(hBox,0,0);

        //label = new Label("Server Address");
        serverAddress = new TextField();
        serverAddress.setText(defaultHost);
        portAddress = new TextField();
        portAddress.setText(String.valueOf(defaultPort));
        hBox.getChildren().addAll(new Label("Server Address: "), serverAddress, new Label("Port: "), portAddress);

        label = new Label("Enter username: ");
        GridPane.setConstraints(label, 0, 1);

        tf = new TextField();
        tf.setPromptText("UserName");
        GridPane.setConstraints(tf, 0, 2);
        ta = new TextArea();
        GridPane.setConstraints(ta, 0, 3);


        HBox hBox1 = new HBox();
        login = new Button("Log In");
        login.addEventHandler(MouseEvent.MOUSE_CLICKED,this);

        logout = new Button("Log out");
        logout.addEventHandler(MouseEvent.MOUSE_CLICKED,this);
        logout.setDisable(true);

        whoIsIn = new Button("Who is in");
        whoIsIn.addEventHandler(MouseEvent.MOUSE_CLICKED,this);
        whoIsIn.setDisable(true);

        send = new Button("Send");
        send.addEventHandler(MouseEvent.MOUSE_CLICKED,this);
        send.setDisable(true);
        hBox1.getChildren().addAll(login, logout, whoIsIn,send);
        GridPane.setConstraints(hBox1, 0,4);

        gridPane.getChildren().addAll(hBox,label,tf,ta,hBox1);
        scene = new Scene(gridPane,500, 400);

        primaryStage.setScene(scene);
        primaryStage.show();

    }
    void append(String str){
        ta.appendText(str);
    }

    void connectionFailed(){
        login.setDisable(false);
        logout.setDisable(false);
        whoIsIn.setDisable(false);
        send.setDisable(false);
        label.setText("Enter username");
        tf.setPromptText("Username");
        portAddress.setText(""+defaultPort);
        serverAddress.setText(""+defaultHost);
        serverAddress.setEditable(false);
        portAddress.setEditable(false);
        tf.removeEventHandler(InputEvent.ANY,this);
        connected = false;
    }
    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void handle(Event event) {
        Object o = event.getSource();
        if(o == logout){
            client.sendMessage(new Chatmessage(Chatmessage.LOGOUT,""));
        }
        if(o == whoIsIn){
            client.sendMessage(new Chatmessage(Chatmessage.WHOISIN,""));
        }
        if(connected && o == send){
            client.sendMessage(new Chatmessage(Chatmessage.MESSAGE,tf.getText()));
            tf.setText("");
        }
        if(o == login){
            String username = tf.getText().trim();
            //System.out.p
            if(Server.repeat == 1){
                System.out.println(1);
                if(tf.getText().trim().length() == 0){
                    System.out.println(2);
                    return;
                }
                String server = serverAddress.getText().trim();
                System.out.println(3);
                if(server.length() == 0){
                    System.out.println(4);
                    return;
                }
                String portNumber = portAddress.getText().trim();
                System.out.println(5);
                if(portNumber.length() == 0){
                    System.out.println(6);
                    return;
                }
                int port = 0;
                try{
                    System.out.println(7);
                    port = Integer.parseInt(portNumber);
                }catch (Exception e){
                    return;
                }
                client = new Client(server,port,username,this);
                System.out.println(8);
                if(!client.start()){
                    System.out.println("client start");
                    return;
                }
                tf.setText("");
                label.setText("Enter your message below: ");
                tf.setPromptText("message");
                connected = true;
                login.setDisable(true);
                logout.setDisable(false);
                whoIsIn.setDisable(false);
                send.setDisable(false);
                serverAddress.setEditable(false);
                portAddress.setEditable(false);
            }
            else{
                System.out.println(Server.repeat);
                for(int i = 0; i < Server.clientLists.size(); i++){
                    Server.ClientThread ct = Server.clientLists.get(i);
                    if(ct.username.equals(username)){
                        Client.display(ct.username + " already logged in");
                        tf.setText("");
                        found = true;
                    }
                }
                if(found){
                    login.setDisable(false);
                    logout.setDisable(true);
                    whoIsIn.setDisable(true);
                    send.setDisable(true);
                    serverAddress.setEditable(true);
                    portAddress.setEditable(true);

                }
                else{
                    System.out.println(1);
                    if(tf.getText().trim().length() == 0){
                        System.out.println(2);
                        return;
                    }
                    String server = serverAddress.getText().trim();
                    System.out.println(3);
                    if(server.length() == 0){
                        System.out.println(4);
                        return;
                    }
                    String portNumber = portAddress.getText().trim();
                    System.out.println(5);
                    if(portNumber.length() == 0){
                        System.out.println(6);
                        return;
                    }
                    int port = 0;
                    try{
                        System.out.println(7);
                        port = Integer.parseInt(portNumber);
                    }catch (Exception e){
                        return;
                    }
                    client = new Client(server,port,username,this);
                    System.out.println(8);
                    if(!client.start()){
                        System.out.println("client start");
                        return;
                    }
                    tf.setText("");
                    label.setText("Enter your message below: ");
                    tf.setPromptText("message");
                    connected = true;
                    login.setDisable(true);
                    logout.setDisable(false);
                    whoIsIn.setDisable(false);
                    send.setDisable(false);
                    serverAddress.setEditable(false);
                    portAddress.setEditable(false);

                }

            }

            //tf.addEventHandler(InputEvent.ANY,this);
        }
    }
}

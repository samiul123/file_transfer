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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClientGUI extends ServerGui implements EventHandler{

    private static final long serialVersionUID = 1L;
    private Label label;
    private TextField tf,recipient,fileText;
    private TextField serverAddress, portAddress;
    private Button login, logout, whoIsIn, sendMessage, sendFile;
    private TextArea ta;
    private boolean connected;
    private Client client;
    private int defaultPort = 1500;
    private String defaultHost = "localhost";
    Scene scene;
    int fileName;

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

        HBox hBoxUser = new HBox();
        VBox vBoxUser = new VBox();
        VBox vBoxRecipient = new VBox();
        label = new Label("Enter username: ");
        //GridPane.setConstraints(label, 0, 1);

        tf = new TextField();
        tf.setPromptText("UserName");
        //GridPane.setConstraints(tf, 0, 2);
        ta = new TextArea();
        GridPane.setConstraints(ta, 0, 2);

        vBoxUser.getChildren().addAll(label, tf);

        Label label1 = new Label("Enter recipient name: ");
        recipient = new TextField();
        recipient.setPromptText("Recipient name");
        vBoxRecipient.getChildren().addAll(label1, recipient);

        fileText = new TextField();
        fileText.setPromptText("File name");
        Label fileLabel = new Label("Enter file name:");

        VBox fileVBox = new VBox();
        fileVBox.getChildren().addAll(fileLabel, fileText);
        hBoxUser.getChildren().addAll(vBoxUser, vBoxRecipient, fileVBox);
        GridPane.setConstraints(hBoxUser, 0, 1);

        HBox hBox1 = new HBox();
        login = new Button("Log In");
        login.addEventHandler(MouseEvent.MOUSE_CLICKED,this);

        logout = new Button("Log out");
        logout.addEventHandler(MouseEvent.MOUSE_CLICKED,this);
        logout.setDisable(true);

        whoIsIn = new Button("Who is in");
        whoIsIn.addEventHandler(MouseEvent.MOUSE_CLICKED,this);
        whoIsIn.setDisable(true);

        sendMessage = new Button("Send message");
        sendMessage.addEventHandler(MouseEvent.MOUSE_CLICKED,this);
        sendMessage.setDisable(true);

        sendFile = new Button("Send file");
        sendFile.addEventFilter(MouseEvent.MOUSE_CLICKED,this);
        sendFile.setDisable(true);

        hBox1.getChildren().addAll(login, logout, whoIsIn,sendMessage, sendFile);
        GridPane.setConstraints(hBox1, 0,3);

        gridPane.getChildren().addAll(hBox, hBoxUser,ta,hBox1);
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
        sendMessage.setDisable(false);
        sendFile.setDisable(false);
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
            client.sendMessage(new Chatmessage(Chatmessage.LOGOUT,"","",""));
        }
        if(o == whoIsIn){
            client.sendMessage(new Chatmessage(Chatmessage.WHOISIN,"","",""));
        }
        if(connected && o == sendMessage){
            client.sendMessage(new Chatmessage(Chatmessage.MESSAGE,tf.getText(),recipient.getText(),""));
            tf.setText("");
        }
        if(connected && o == sendFile){
            client.sendMessage(new Chatmessage(Chatmessage.FILE,"",recipient.getText(),fileText.getText()));
            fileText.setText("");
            recipient.setText("");
        }
        if(o == login){
            String username = tf.getText().trim();
            //System.out.p

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
                sendMessage.setDisable(false);
                sendFile.setDisable(false);
                serverAddress.setEditable(false);
                portAddress.setEditable(false);
            }
        //tf.addEventHandler(InputEvent.ANY,this);
    }
}

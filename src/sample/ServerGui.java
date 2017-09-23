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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.WindowListener;

public class ServerGui extends Application implements EventHandler {
    private static final long serialVersionUID = 1L;
    private Button stopStart;
    private TextArea chat, event;
    private TextField tPortNumber;
    public Server server;
    //private Server[] serverArray;
    private Server newServ;
    Stage window;
    Scene scene;


    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;
        window.setTitle("File Transfer Server");
        server = null;
        //serverArray = new Server[120];

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(8);
        gridPane.setHgap(10);

        HBox hBox = new HBox();
        GridPane.setConstraints(hBox,0,0);

        Label label = new Label("Port Number:");
        tPortNumber = new TextField();
        tPortNumber.setText(""+1500);

        stopStart = new Button("Start");
        stopStart.setOnAction(e->startStopServer());
        hBox.getChildren().addAll(label, tPortNumber,stopStart);

        chat = new TextArea();
        GridPane.setConstraints(chat, 0, 1);

        event = new TextArea();
        GridPane.setConstraints(event, 0, 2);

        gridPane.getChildren().addAll(hBox, chat, event);

        scene = new Scene(gridPane,400, 400);

        window.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,this);

        window.setScene(scene);
        window.show();

    }
    public void startStopServer(){
        if(server != null){
            server.stop();
            server = null;
            tPortNumber.setEditable(true);
            stopStart.setText("Start");
        }
        else{
            int port = 1700;
            /*int[] portArray = new int[120];
            for(int i = 0; i < 120; i++){
                portArray[i] = port;
                port++;
            }*/
            try {
                port = Integer.parseInt(tPortNumber.getText().trim());
            }catch (Exception e){
                event.appendText("Invalid port number");
            }
            /*for(int i = 0; i < 120; i++){
                serverArray[i] = new Server(portArray[i],this);

            }*/
            server = new Server(port,this);
            //setNewServ(server);
            new ServerRunning().start();
            stopStart.setText("Stop");
            tPortNumber.setEditable(false);
        }

    }
    void appendChat(String str){
        chat.appendText(str);
    }
    void appendEvent(String str){
        event.appendText(str);
    }

    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void handle(Event event) {
        Object o = event.getSource();
        if(o == window && server != null)server.stop();
    }

    class ServerRunning extends Thread{
        public void run(){
            server.start();
            stopStart.setText("Start");
            //System.out.println("start");
            tPortNumber.setEditable(true);
            event.appendText("Server crashed\n");
            server = null;
        }
    }

}

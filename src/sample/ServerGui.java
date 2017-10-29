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
    private TextArea event;
    private TextField tPortNumber;
    public Server server;
    private Server newServ;
    //public static int clientListSize = 0;
    Stage window;
    Scene scene;


    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;
        window.setTitle("File Transfer Server");
        server = null;
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


        event = new TextArea();
        event.setMinHeight(350);
        GridPane.setConstraints(event, 0, 1);

        gridPane.getChildren().addAll(hBox, event);

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
            try {
                port = Integer.parseInt(tPortNumber.getText().trim());
            }catch (Exception e){
                event.appendText("Invalid port number");
            }
            server = new Server(port,this);
            new ServerRunning().start();
            stopStart.setText("Stop");
            tPortNumber.setEditable(false);
        }

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
            tPortNumber.setEditable(true);
            event.appendText("Server crashed\n");
            server = null;
        }
    }

}

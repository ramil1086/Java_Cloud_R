package io;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {
    @FXML
    public Button buttonSignIn;
    @FXML
    public Button buttonRegister;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public Label loginResultLabel;

    private static Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    public static void setUser(String user) {
        AuthController.user = user;
    }

    private static String user;

    public static String getUser() {return user;}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            socket = new Socket("localhost", 4444);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openCloud() throws Exception {
        Cloud cloud = new Cloud();
        cloud.start(new Stage());
        Auth.getStage().close();
    }

//    авторизует пользователя
    public void signIn(ActionEvent actionEvent) throws Exception {
        user = loginField.getText().trim();
        String password = passwordField.getText().trim();
        dos.writeUTF("auth");
        dos.writeUTF(user + " " + password);
        String authResult = dis.readUTF();
        if (authResult.equals("authOK")) {
            openCloud();
//            dos.close();
//            dis.close();
        } else loginResultLabel.setText("Incorrect Login or Password");

    }

// репистрирует нового пользователя
    public void register(ActionEvent actionEvent) throws Exception {
        Reg reg = new Reg();
        reg.start(new Stage());
    }

    public static Socket getSocket() {
        return socket;
    }
}

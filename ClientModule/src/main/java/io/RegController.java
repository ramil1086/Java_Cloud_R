package io;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class RegController implements Initializable {
    @FXML
    public Button buttonRegister;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public PasswordField passwordConfirmField;
    @FXML
    public Label registerResultLabel;

    private DataOutputStream dos;
    private DataInputStream dis;
    private String user;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = AuthController.getSocket();
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());

        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void register(ActionEvent actionEvent) throws Exception {
        user = loginField.getText().trim();
        System.out.println("Got Login =" + user);
        String password = passwordField.getText().trim();
        System.out.println("Got password = " + password);
        String password2 = passwordConfirmField.getText().trim();

        System.out.println("Got password2 = " + password2);
        if (!password.equals(password2)) {
            registerResultLabel.setText("Check password");
            return;
        }
        dos.writeUTF("reg");
        dos.writeUTF(user + " " + password);
        System.out.println("sent to server user and pass");
        String authResult = dis.readUTF();
        System.out.println("server answered " + authResult);
        if (authResult.equals("registerOK")) {
            AuthController.setUser(user);
            Auth.getStage().hide();
            openCloud();
        } else registerResultLabel.setText("Choose another Login");
    }

    private void openCloud() throws Exception {
        Cloud cloud = new Cloud();
        cloud.start(new Stage());
        Reg.getStage().close();
        Auth.getStage().close();
    }
}

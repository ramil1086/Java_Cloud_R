package io;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CloudController implements Initializable {

    public Button buttonUploadFiles;
    public Button buttonSendFiles;
    public TextArea textArea;
    public TextField cmdTextField;
    public ScrollBar scrollBar;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private OutputStream outputStream;
    private final FileChooser fileChooser = new FileChooser();
    List<File> files;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = new Socket("localhost", 4444);
            outputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            //получаем ответ сервера и выводим в textArea
            Thread readThread = new Thread(()-> {
                try {
                    while (true) {
                        textArea.appendText(dataInputStream.readUTF());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // выбираем файлы для загрузки на сервер
    public void uploadFiles(ActionEvent actionEvent) {
        textArea.clear();
        files = fileChooser.showOpenMultipleDialog(Cloud.getStage());
        for (File f : files) {
            textArea.appendText(f.getAbsolutePath() + "\n");
        }
    }

    // отправляем файлы на сервер
    public void sendFiles(ActionEvent actionEvent) throws IOException {
        for (File f : files) {
            String fileName = f.getName();
            InputStream in = new FileInputStream(f.getAbsolutePath());
            int b = 0;
            byte[] buffer = new byte[8192];
            dataOutputStream.writeUTF(fileName);
            dataOutputStream.writeLong(f.length());
            while ((b = in.read(buffer)) != -1) {
                outputStream.write(buffer, 0, b);
            }
            outputStream.flush();
            in.close();
        }
    }

    public void sendCmd(ActionEvent actionEvent) throws IOException {
        textArea.clear();
        dataOutputStream.writeUTF("cmd\\" + cmdTextField.getText());
        cmdTextField.clear();

    }
}
package io;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class CloudController implements Initializable {
    @FXML
    public Button buttonUploadFiles;
    @FXML
    public ListView<String> listView;
    @FXML
    public Button buttonDelete;
    @FXML
    public Button buttonDownloadFiles;
    @FXML
    public Label userLabel;
    @FXML
    public Label processLabel;

    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private OutputStream outputStream;
    private InputStream inputStream;
    private final FileChooser fileChooser = new FileChooser();
    private List<File> files;
    private ObservableList<String> selected;
    private Path dir;
    private MultipleSelectionModel<String> msm;
    private String user;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = AuthController.getSocket();
            user = AuthController.getUser();
            dir = Paths.get("clientDir\\" + user);
            Files.createDirectories(dir);
            userLabel.setText(user);
            outputStream = new DataOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());


            //получаем ответ сервера и выводим в listView
            Thread readThread = new Thread(() -> {
                try {
                    while (true) {
                        StringBuilder msg = new StringBuilder(dataInputStream.readUTF());
                        if (!msg.toString().startsWith("file\\")) {
                            listView.getItems().add(msg.toString());
                        } else downloadFilesFromServer(msg);
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

// организация мультиселекта
        msm = listView.getSelectionModel();
        msm.setSelectionMode(SelectionMode.MULTIPLE);
        msm.selectedItemProperty().addListener((observable, oldValue, newValue) -> selected = msm.getSelectedItems());
    }

    // выбирает файлы и загружает на сервер
    public void uploadFiles(ActionEvent actionEvent) throws IOException {
        files = fileChooser.showOpenMultipleDialog(Cloud.getStage());
        if (files != null) {
            listView.getItems().clear();
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
            dataOutputStream.writeUTF("cmd\\ls");
            processLabel.setText("Uploaded to Server");
        }
    }

    // удаляет файлы с сервера
    public void deleteFiles(ActionEvent actionEvent) throws IOException, InterruptedException {
        selected = msm.getSelectedItems();
        for (String s : selected) {
            dataOutputStream.writeUTF("cmd\\del" + s);
        }
        listView.getItems().clear();
        dataOutputStream.writeUTF("cmd\\ls");
        processLabel.setText("Deleted from Server");
    }

    // отправляет команду на скачивание файлов с сервера
    public void cmdDownloadFiles(ActionEvent actionEvent) throws IOException {
        selected = msm.getSelectedItems();
        for (String s : selected) {
            dataOutputStream.writeUTF("cmd\\dwnld" + s);
        }

        processLabel.setText("Downloaded from Server");
    }

    // принимает файлы с сервера
    public void downloadFilesFromServer(StringBuilder msg) throws IOException {
        String fileName = msg.delete(0, 5).toString();
        OutputStream fileOutputStream = new FileOutputStream(dir + "\\" + fileName);
        long fileSize = dataInputStream.readLong();
        byte[] buffer = new byte[8192];
        int b = 0;
        while (fileSize > 0 && (b = inputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
            fileOutputStream.write(buffer, 0, b);
            fileSize -= b;
        }
        fileOutputStream.close();
    }
}
package io;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class CloudController implements Initializable {

    public Button buttonUploadFiles;
    public ListView<String> listView;
    public Button buttonDelete;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private OutputStream outputStream;
    private InputStream inputStream;
    private final FileChooser fileChooser = new FileChooser();
    private List<File> files;
    private ObservableList<String> selected;
    private String clientDir = "clientDir\\user2";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket = new Socket("localhost", 4444);
            outputStream = new DataOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());

            //получаем ответ сервера и выводим в textArea
            Thread readThread = new Thread(()-> {
                try {
                    while (true) {

                        StringBuilder msg = new StringBuilder(dataInputStream.readUTF());
                        if (!msg.toString().startsWith("file\\")) {
                            listView.getItems().add(msg.toString());
                        } else {
                            String fileName = msg.delete(0, 5).toString();
                            System.out.println("Got fileName " + fileName);
                            System.out.println(Paths.get(clientDir+"\\" + fileName).toAbsolutePath());
                            OutputStream fileOutputStream = new FileOutputStream(clientDir + "\\" + fileName);
                            System.out.println("Created OutputStream");
                            long fileSize = dataInputStream.readLong();
                            System.out.println("Got fileSize");
                            byte[] buffer = new byte[8192];
                            int b = 0;
                            while (fileSize > 0 && (b = inputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                                fileOutputStream.write(buffer, 0, b);
                                fileSize -= b;
                            }
                            System.out.println("GotFileBytes");
                            fileOutputStream.close();
                        }
                    }



                } catch (Exception e) {
//                    e.printStackTrace();
                }
            });
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
//            e.printStackTrace();
        }
        MultipleSelectionModel<String> msm = listView.getSelectionModel();
        msm.setSelectionMode(SelectionMode.MULTIPLE);
        msm.selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                selected = msm.getSelectedItems();
            }
        });
    }

    // выбираем файлы для загрузки на сервер
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
        }
    }

    public void deleteFiles(ActionEvent actionEvent) throws IOException, InterruptedException {

        for (String s : selected) {
            System.out.println("Command to delete " + s);
            dataOutputStream.writeUTF("cmd\\del"+s);
        }
        listView.getItems().clear();
        dataOutputStream.writeUTF("cmd\\ls");
    }

    public void downloadFiles(ActionEvent actionEvent) throws IOException {
        for (String s : selected) {
            System.out.println("downloading " + s);
            dataOutputStream.writeUTF("cmd\\dwnld" + s);
        }
    }

}
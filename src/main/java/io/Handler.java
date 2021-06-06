package io;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Handler implements Runnable, Closeable {

    private final Socket socket;
    private String user = "serverDir\\user2";
    private Path dir;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream();
             DataInputStream dataInputStream = new DataInputStream(inputStream);
             DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {

            dir = Paths.get(user);
            System.out.println("Client = " + user);
            lsCommand(dataOutputStream);

            while (true) {
                StringBuilder message = new StringBuilder(dataInputStream.readUTF());
                System.out.println("Command from client " + message.toString());
                if (message.toString().startsWith("cmd\\del")) {
                    delCommand(message);
                } else if (message.toString().equals("cmd\\ls")) lsCommand(dataOutputStream);
                else if (message.toString().startsWith("cmd\\dwnld")) sendFiles(message, dataOutputStream,outputStream);
                else {
                    String fileName = message.toString();
                    OutputStream fileOutputStream = new FileOutputStream(dir.toString() + "\\" + fileName);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFiles(StringBuilder message, DataOutputStream dataOutputStream, OutputStream outputStream) throws IOException {
        System.out.println("start sending");
        String fileName = message.delete(0,9).toString();
        System.out.println("FileName " + fileName);
        File file = new File(dir.toString()+"\\"+fileName);
        System.out.println("Created File " + file.getAbsolutePath());
        InputStream in = new FileInputStream(file.getAbsolutePath());
        System.out.println("Created InputStream");
        int b = 0;
        byte[] buffer = new byte[8192];
        dataOutputStream.writeUTF("file\\" + fileName);
        System.out.println("Sent FileName");
        dataOutputStream.writeLong(file.length());
        System.out.println("Sent FileLength");
        while ((b = in.read(buffer)) != -1) {
            outputStream.write(buffer, 0, b);
        }
        System.out.println("Sent FileBytes");
        outputStream.flush();
        System.out.println("Flushed");
        in.close();
        System.out.println("InputStream Closed");
    }


    public void close() throws IOException {
        socket.close();
    }

//    public void answerCommand(DataInputStream dis,DataOutputStream dos) throws IOException {
//        String msg = dis.readUTF();
//        if (msg.equals("ls")) lsCommand(dos);
//        else if (msg.equals("del")) delCommand(dis);
//
//    }

    private void lsCommand(DataOutputStream dos) throws IOException {
        Files.walk(dir).forEach(x -> {
            try {
                if (!Files.isDirectory(x)) {
                dos.writeUTF(x.getFileName().toString());}
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        System.out.println("SRV: walkdir completed\n");

    }

    private void delCommand(StringBuilder message) throws IOException {
        String fileName = message.delete(0,7).toString();
        System.out.println("filename to delete " + fileName);
        Path path = Paths.get(dir.toString() + "\\" + fileName);
        System.out.println("path " + path.toString());
        if (Files.exists(path)) {
            Files.delete(path);
            System.out.println("File Deleted");
        }
    }



}

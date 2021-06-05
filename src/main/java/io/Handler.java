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

    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    private final Socket socket;
    private String user;
    private Path dir;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream inputStream = socket.getInputStream();
             DataInputStream dataInputStream = new DataInputStream(inputStream);
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            //получаем файлы от клиента
            StringBuilder userDir = new StringBuilder(dataInputStream.readUTF());
            userDir.delete(0, 4);
            user = userDir.toString();
            dir = Paths.get(userDir.toString());
            System.out.println("Client = " + user);
            if (!Files.exists(dir)) {
                Files.createDirectory(dir);
                dataOutputStream.writeUTF("Created dir " + dir.toString() + "\n");
            }
            dataOutputStream.writeUTF("Hello " + user);
            dataOutputStream.writeUTF("\nEnter \"help\" to see commands\n");
            while (true) {
                StringBuilder message = new StringBuilder(dataInputStream.readUTF());
                if (message.toString().startsWith("cmd\\")) {
                    answerCommand(message, dataOutputStream);
                } else {
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
                    dataOutputStream.writeUTF(String.format("%s added to server cloud\n", fileName));
                }
            }
        } catch (Exception e) {
//            e.printStackTrace();
            log.error("exception = ", e);
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    public void answerCommand(StringBuilder command, DataOutputStream dos) throws IOException {
        System.out.println("SRV: " + dir + " " + command + "\n");

        if (command.toString().startsWith("cmd\\ls")) {
            lsCommand(dos);
        } else if (command.toString().startsWith("cmd\\del ")) {
            delCommand(command, dos);
        } else if (command.toString().startsWith("cmd\\mkdir ")) {
            mkdirCommand(command, dos);
        } else if (command.toString().startsWith("cmd\\cd")) {
            cdCommand(command, dos);
        } else if (command.toString().equals("cmd\\help")) {
            helpCommand(dos);
        }
        command = new StringBuilder();
    }

    private void helpCommand(DataOutputStream dos) throws IOException {
        dos.writeUTF(
                "ls - shows all files and dirs\n" +
                    "cd - shows current directory\n" +
                    "cd [fullpath] - changes dir, ex: cd user1\\folder1\\somefolder\n" +
                    "mkdir [folder] - creates folder in current directory, ex: mkdir testdir\n" +
                    "del [filename] - deletes file, ex: del file1.docx\n"
        );
    }

    private void cdCommand(StringBuilder command, DataOutputStream dos) throws IOException {
        command.delete(0, 4);
        if (command.toString().equals("cd")) {
            dos.writeUTF("Current Dir : " + dir.toString());
        } else {
            command.delete(0, 3);
            Path path = Paths.get(command.toString());
            if (path.toString().startsWith(user) && Files.isDirectory(path) && Files.exists(path)) {
                dir = path;
                dos.writeUTF("Dir changed to : " + dir.toString());
            } else dos.writeUTF("No such directory. Please create");
        }
    }

    private void mkdirCommand(StringBuilder command, DataOutputStream dos) throws IOException {
        command.delete(0, 10);
        Path path = Paths.get(dir + "\\" + command.toString());
        if (!Files.exists(path)) {
            Files.createDirectory(path);
            dos.writeUTF("Created : " + path.toString());
        } else dos.writeUTF("Directory already exists : " + command.toString());

    }

    private void lsCommand(DataOutputStream dos) throws IOException {
        Files.walk(dir).forEach(x -> {
            try {
                dos.writeUTF(x.toString());
                dos.writeUTF("\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        System.out.println("SRV: walkdir completed\n");

    }

    private void delCommand(StringBuilder command, DataOutputStream dos) throws IOException {
        command.delete(0, 8);
        Path path = Paths.get(dir + "\\" + command.toString());

        if (Files.exists(path)) {
            Files.delete(path);
            dos.writeUTF("deleted " + command.toString());
            System.out.println("SRV: deleted " + command.toString());
        } else dos.writeUTF("No such file : " + command.toString());

    }


}
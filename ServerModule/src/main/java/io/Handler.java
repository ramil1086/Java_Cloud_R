package io;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class Handler implements Runnable, Closeable {

    private final Socket socket;
    private String user;
    private Path dir;
    private boolean isAuthorized = false;
    private boolean hasUser;
    private static final String url = "jdbc:mysql://localhost:3306/users?useUnicode=true&serverTimezone=UTC&useSSL=false";
    private static final String loginMySQL = "cloud_server1";
    private static final String passwordMySQL = "cloud_server1";
    private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream();
             DataInputStream dataInputStream = new DataInputStream(inputStream);
             DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {
            while (!isAuthorized) {
                String msg = dataInputStream.readUTF();
                System.out.println(msg);
                if (msg.equals("reg")) registerNewUser(dataInputStream, dataOutputStream);
            else if (msg.equals("auth")) checkLoginAndPassword(dataInputStream, dataOutputStream);
            }
            dir = Paths.get("serverDir\\" + user);
            Files.createDirectories(dir);
            System.out.println(String.format("%s : %s", user, "Authorized"));
            lsCommand(dataOutputStream);

            while (true) {
                StringBuilder message = new StringBuilder(dataInputStream.readUTF());
                System.out.println(String.format("%s : %s %s", user, "Command", message));

                if (message.toString().startsWith("cmd\\del")) delCommand(message);
                else if (message.toString().equals("cmd\\ls")) lsCommand(dataOutputStream);
                else if (message.toString().startsWith("cmd\\dwnld")) sendFiles(message, dataOutputStream, outputStream);
                else downloadFiles(message, dataInputStream, inputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // отправляет файлы Клиенту
    private void sendFiles(StringBuilder message, DataOutputStream dataOutputStream, OutputStream outputStream) throws IOException {
        String fileName = message.delete(0, 9).toString();
        File file = new File(dir.toString() + "\\" + fileName);
        InputStream in = new FileInputStream(file.getAbsolutePath());
        int b = 0;
        byte[] buffer = new byte[8192];
        dataOutputStream.writeUTF("file\\" + fileName);
        dataOutputStream.writeLong(file.length());
        while ((b = in.read(buffer)) != -1) {
            outputStream.write(buffer, 0, b);
        }
        outputStream.flush();
        System.out.println(String.format("%s : %s %s to %s", user, "Sent file", fileName, user));
        in.close();
    }


    public void close() throws IOException {
        socket.close();
    }

    // направляет Клиенту список файлов на Сервере
    private void lsCommand(DataOutputStream dos) throws IOException {
        Files.walk(dir).forEach(x -> {
            try {
                if (!Files.isDirectory(x)) {
                    dos.writeUTF(x.getFileName().toString());
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        System.out.println(String.format("%s : %s", user, "Walkdir completed"));
    }

    // удаляет файлы с сервера
    private void delCommand(StringBuilder message) throws IOException {
        String fileName = message.delete(0, 7).toString();
        Path path = Paths.get(dir.toString() + "\\" + fileName);
        if (Files.exists(path)) {
            Files.delete(path);
            System.out.println(String.format("%s : %s %s", user, "File deleted", fileName));
        }
    }

    // проверяет логин и пароль клиента в MySQL DB
    private void checkLoginAndPassword(DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        try {
            String[] authLoginAndPassword = dataInputStream.readUTF().split(" ");
            String login = authLoginAndPassword[0];
            String password = authLoginAndPassword[1];
            String query = String.format("select login, password from users.loginandpassword;");
            con = DriverManager.getConnection(url, loginMySQL, passwordMySQL);
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                String userDB = rs.getString(1);
                String passwordDB = rs.getString(2);
                if (userDB.equals(login) && passwordDB.equals(password)) {
                    dataOutputStream.writeUTF("authOK");
                    isAuthorized = true;
                    user = login;
                    break;
                }
            }
            if (!isAuthorized) dataOutputStream.writeUTF("authFailed");
        } catch (Exception e) {
            e.printStackTrace();
//            dataOutputStream.writeUTF("authFailed");
        } finally {
            try {con.close();} catch (SQLException se) {}
            try {stmt.close();} catch (SQLException se) {}
            try {rs.close();} catch (SQLException se) {}
        }
    }

    // получает файлы Клиента и размещает в папке на Сервере
    private void downloadFiles(StringBuilder message, DataInputStream dataInputStream, InputStream inputStream) throws IOException {
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

    private void registerNewUser(DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        try {
            String[] regLoginAndPassword = dataInputStream.readUTF().split(" ");
            String login = regLoginAndPassword[0];
            String password = regLoginAndPassword[1];
            System.out.println(String.format("newUser = %s , password = %s", login, password));
            String query = String.format("select login, password from users.loginandpassword;");
            con = DriverManager.getConnection(url, loginMySQL, passwordMySQL);
            System.out.println("connected to Mysql");
            stmt = con.createStatement();
            System.out.println("Created Statement");
            rs = stmt.executeQuery(query);
            System.out.println("Got Query select from DB");
            while (rs.next()) {
                String userDB = rs.getString(1);
                System.out.println("UserDB = " + userDB);
                if (userDB.equals(login)) {
                    dataOutputStream.writeUTF("regFailed");
                    System.out.println("regFailed");
                    hasUser = true;
                    break;
                }
            }
            if (!hasUser) {
                String maxIdQuery = "select max(id) from users.loginandpassword;";
                rs = stmt.executeQuery(maxIdQuery);
                int maxIdNum = 0;
                while (rs.next()) {
                    maxIdNum = rs.getInt(1);
                }
                maxIdNum++;
                String newUser = String.format("insert into users.loginandpassword (id, login, password) VALUES (%d, '%s', '%s');", maxIdNum, login, password);
                System.out.println("Sending update to db");
            stmt.executeUpdate(newUser);
                System.out.println("updated DB");
            dataOutputStream.writeUTF("registerOK");
                System.out.println("sending registerOK");
            user = login;
                System.out.println("user = user");
            isAuthorized = true;
                System.out.println("isAuthorized = true");
            }
        } catch (Exception e) {
            e.printStackTrace();
//            dataOutputStream.writeUTF("authFailed");
        } finally {
            try {con.close();} catch (SQLException se) {}
            try {stmt.close();} catch (SQLException se) {}
            try {rs.close();} catch (SQLException se) {}
        }
    }

}



package io;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class Handler implements Runnable, Closeable {

        private static final Logger log = LoggerFactory.getLogger(Handler.class);
    private final Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream inputStream = socket.getInputStream();
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream()))
        {
            //получаем файлы от клиента
            while (true) {
                String fileName = dataInputStream.readUTF();
                OutputStream fileOutputStream = new FileOutputStream(fileName);
                long fileSize = dataInputStream.readLong();
                byte[] buffer = new byte[8192];
                int b =0;
                while (fileSize > 0 && (b = inputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    fileOutputStream.write(buffer, 0, b);
                    fileSize -= b;
                }
                fileOutputStream.close();
                dataOutputStream.writeUTF(String.format("%s added to server cloud\n", fileName));
            }


        } catch (Exception e) {
//            e.printStackTrace();
            log.error("exception = ", e);
        }
    }

    public void close() throws IOException {
        socket.close();
    }
}

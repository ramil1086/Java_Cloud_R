package io;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

//    private static final Logger log = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws IOException {

        ServerSocket server = new ServerSocket(4444);
        System.out.println("Server started");
//        log.debug("Server started");
        while (true) {
            try {
                Socket socket = server.accept();
//                log.debug("Client accepted");
                System.out.println("Client accepted");
                Handler handler = new Handler(socket);
                new Thread(handler).start();
            } catch (Exception e) {
//                log.error("Connection was broken");
                System.err.println("Connection was broken");
            }
        }
    }
}

package nio;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer buffer;


    public Server() throws IOException {
        buffer = ByteBuffer.allocate(100);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(4444));
        serverSocketChannel.configureBlocking(false);
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (serverSocketChannel.isOpen()) {
            selector.select();
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeySet.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }

                keyIterator.remove();
            }
        }


    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();
        int r;
        while (true) {
            r = channel.read(buffer);
            if (r == -1) {
                channel.close();
                return;
            }
            if (r == 0) { // что значит r == 0;
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                sb.append((char) buffer.get());
            }
            buffer.clear();
        }


        // выводим список файлов и поддиректорий
        if (sb.toString().startsWith("ls ")) {
            lsCommand(sb, channel);
        }

        // читаем файл (для текстовых файлов)
        else if (sb.toString().startsWith("cat ")) {
            catCommand(sb, channel);
        }

        // создаем директорию
        else if (sb.toString().startsWith("mkdir ")) {
            mkdirCommand(sb, channel);
        }

        // Создаем файл
        else if (sb.toString().startsWith("touch ")) {
            touchCommand(sb, channel);
        }

        //добавляем сообщение в файл
        if (sb.toString().startsWith("read \"")) {
            readCommand(sb, channel);
        }

//        channel.write(ByteBuffer.wrap(("srv answer : " + sb.toString()).getBytes(StandardCharsets.UTF_8)));
    }


    private void readCommand(StringBuilder sb, SocketChannel channel) throws IOException {
        sb.delete(0, 6);
        String msgToFile = sb.substring(0, sb.indexOf("\""));
        sb.delete(0, sb.indexOf("\"") + 1);
        Path filePath = Paths.get(sb.toString());
        if (Files.exists(filePath)) {
            Files.write(filePath, msgToFile.getBytes(), StandardOpenOption.APPEND);

            channel.write(ByteBuffer.wrap(("SRV: appended  to file").getBytes(StandardCharsets.UTF_8)));
            System.out.println("SRV: appended to file! ");
        } else {
            channel.write(ByteBuffer.wrap(("SRV: no file! " + filePath.toString()).getBytes(StandardCharsets.UTF_8)));
            System.out.println("SRV: no file! " + filePath.toString());
        }
    }

    private void touchCommand(StringBuilder sb, SocketChannel channel) throws IOException {
        sb.delete(0, 6);
        Path filePath = Paths.get(sb.toString());
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
            channel.write(ByteBuffer.wrap(("SRV: created file " + filePath.toString()).getBytes(StandardCharsets.UTF_8)));
            System.out.println("SRV: created file " + filePath.toString());
        } else {
            channel.write(ByteBuffer.wrap(("SRV: file exists! " + filePath.toString()).getBytes(StandardCharsets.UTF_8)));
            System.out.println("SRV: file exists! " + filePath.toString());
        }
    }

    private void mkdirCommand(StringBuilder sb, SocketChannel channel) throws IOException {
        sb.delete(0, 6);
        Path dir = Paths.get(sb.toString());
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
            channel.write(ByteBuffer.wrap(("SRV: created dir : " + dir.toString()).getBytes(StandardCharsets.UTF_8)));
            System.out.println("SRV: created dir : " + dir.toString());
        } else {
            channel.write(ByteBuffer.wrap(("SRV: dir exists! : " + dir.toString()).getBytes(StandardCharsets.UTF_8)));
            System.out.println("SRV: dir exists! : " + dir.toString());
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverSocketChannel.accept();
        channel.write(ByteBuffer.wrap("Welcome to server".getBytes(StandardCharsets.UTF_8)));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }

    private void lsCommand(StringBuilder sb, SocketChannel channel) throws IOException {
        sb.delete(0, 3);
        Path dir = Paths.get(sb.toString());
        Files.walk(dir).forEach( x -> {
            try {
                channel.write(ByteBuffer.wrap(x.toString().getBytes()));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        System.out.println("SRV: walkdir completed");
    }

    private void catCommand(StringBuilder sb, SocketChannel channel) throws IOException {
        sb.delete(0, 4);
        BufferedReader bis = new BufferedReader(new FileReader(sb.toString()));
        while (bis.ready()) {
            channel.write(ByteBuffer.wrap(bis.readLine().getBytes(StandardCharsets.UTF_8)));
        }
        bis.close();

        System.out.println("SRV: Readed user file : " + sb.toString());
    }
}

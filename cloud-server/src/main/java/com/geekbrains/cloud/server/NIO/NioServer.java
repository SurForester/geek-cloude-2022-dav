package com.geekbrains.cloud.server.NIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NioServer {

    private ServerSocketChannel server;
    private Selector selector;

    public NioServer() throws IOException {
        server = ServerSocketChannel.open();
        selector = Selector.open();
        server.bind(new InetSocketAddress(8182));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() throws IOException {
        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccepted();
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    private void handleAccepted() throws IOException {
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        channel.write(ByteBuffer.wrap("Ready for use.\n\r->".getBytes(StandardCharsets.UTF_8)));
    }

    private void handleRead(SelectionKey key) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(512);
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();
        while (channel.isOpen()) {
            int read = channel.read(buf);
            if (read < 0) {
                channel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            buf.flip();
            while (buf.hasRemaining()) {
                char chr = (char) buf.get();
                if (chr != '\r' && chr != '\n') {
                    sb.append(chr);
                }
            }
            buf.clear();
            //sb.append("->");
            //byte[] messageEcho = sb.toString().getBytes(StandardCharsets.UTF_8);
            //channel.write(ByteBuffer.wrap(messageEcho));
            String[] arr = sb.toString().split(" ");
            switch (arr[0]) {
                case ("ls") -> lsCommand(channel);
                case ("cat") -> catCommand(channel, arr[1]);
                case ("cd") -> cdCommand(channel, arr[1]);
                default -> channel.write(ByteBuffer.wrap("Unknown command.\r\n->".getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    // List of files & dir
    private void lsCommand(SocketChannel channel) throws IOException {
        try {
            // get files list
            List<Path> files = Files.walk(Paths.get(Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString()), 1).toList();
            for (Path file : files) {
                String fName = file.getFileName().toString();
                channel.write(ByteBuffer.wrap((fName + " \t").getBytes(StandardCharsets.UTF_8)));
            }
            channel.write(ByteBuffer.wrap("\r\n->".getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            channel.write(ByteBuffer.wrap(("Error: " + e.getMessage()).toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void catCommand(SocketChannel channel, String fileName) throws IOException {
        // read file
        Path filePath = Paths.get(Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString(), fileName);
        if (Files.exists(filePath)) {
            try {
                byte[] bytes = Files.readAllBytes(filePath);
                String text = new String(bytes);
                text = text.replace("\n", "\n\r");
                text += "\n\r->";
                channel.write(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
            } catch (IOException e) {
                channel.write(ByteBuffer.wrap(e.getMessage().getBytes(StandardCharsets.UTF_8)));
            }
        } else {
            channel.write(ByteBuffer.wrap(("File " + fileName + " not exist.\r\n->").getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void cdCommand(SocketChannel channel, String catName) throws IOException {
        // change dir
        Path currPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path newPath;
        try {
            if (catName.equals("..")) {
                // up to tree
                newPath = currPath.getParent();
            } else if (catName.equals("\\")) {
                // up to root catalog
                newPath = currPath.getRoot();
            } else {
                // check catalog for exists
                if (inCurrentDirList(catName)) {
                    // go to catalog
                    newPath = Paths.get(System.getProperty("user.dir").toString(), catName);
                } else {
                    newPath = null;
                }
            }
            if (newPath == null) {
                channel.write(ByteBuffer.wrap(("New path is already root or incorrect.\r\n").getBytes(StandardCharsets.UTF_8)));
            } else {
                String res = System.setProperty("user.dir", newPath.toAbsolutePath().toString());
            }
            channel.write(ByteBuffer.wrap(("Current path is " + System.getProperty("user.dir") + "\r\n->").getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            channel.write(ByteBuffer.wrap(e.getMessage().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private boolean inCurrentDirList(String dir) throws IOException {
        List<Path> files = Files.walk(Paths.get(Paths.get(System.getProperty("user.dir")).toAbsolutePath().toString()), 1).toList();
        for (Path file : files) {
            if (file.getFileName().endsWith(dir)) {
                return true;
            }
        }
        return false;
    }

}

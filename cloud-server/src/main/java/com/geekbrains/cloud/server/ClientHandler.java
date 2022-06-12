package com.geekbrains.cloud.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;

import org.apache.logging.log4j.Logger;

public class ClientHandler implements Runnable {
    private CloudServer server;
    private Logger logger;
    private DataInputStream inpStream;
    private DataOutputStream outStream;
    private String login;

    public String getLogin() {
        return login;
    }

    public ClientHandler(CloudServer server, Socket socket, Logger logger) throws IOException {
        try {
            this.server = server;
            this.logger = logger;
            inpStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(this::run);
            readThread.setDaemon(true);
            readThread.start();
        } catch (IOException e) {
            logger.error("ClientHandler init error: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void run() {
        while (true) {
            readMessages();
        }
    }

    private void readMessages() {
        try {
            String message = inpStream.readUTF();
            switch (getCommand(message)) {
                case ("<Login>") -> userLogin(message);
                case ("<GetCurrentList>") -> sendCurrentList();
                case ("<SendFile>") -> writeCloudFile();
                default ->
                    // unknown command
                        logger.trace("Unknown command on client " + login);
            }
        } catch (IOException e) {
            logger.error("Error in command: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    private String getCommand(String msg) {
        String[] arr = msg.split(" ");
        if (arr.length > 0) {
            return arr[0];
        } else {
            return "";
        }
    }

    private synchronized void userLogin(String msg) throws IOException {
        String[] arr = msg.split(" ");
        if (server.loginExists(arr[1])) {
            outStream.writeUTF("<Already logged>");
        } else {
            String res = server.getAuthService().userLogin(arr[1], arr[2]);
            String[] arrRes = res.split(" ");
            if (arrRes[0].equals("email:")) {
                this.login = arr[1];
                outStream.writeUTF("<Logged> " + res);
            } else {
                outStream.writeUTF("<NotLogged> " + res);
            }
        }
        outStream.flush();
    }

    private synchronized void sendCurrentList() throws IOException {
        File dir = new File("D:\\Cloud\\CloudFiles\\");
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isFile())
                outStream.writeUTF(file.getName());
        }
        outStream.writeUTF("<EndFileList>");
        outStream.flush();
    }

    private synchronized void writeCloudFile() throws IOException {
        String fileName = inpStream.readUTF();
        String content = inpStream.readUTF();
        String res = "";

        outStream.writeUTF(res);
        outStream.flush();
    }

}


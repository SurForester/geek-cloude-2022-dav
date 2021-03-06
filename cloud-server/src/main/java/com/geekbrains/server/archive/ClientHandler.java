package com.geekbrains.server.archive;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Objects;

import org.apache.logging.log4j.Logger;

public class ClientHandler implements Runnable {
    private CloudServer server;
    private Logger logger;
    private DataInputStream inpStream;
    private DataOutputStream outStream;
    private String login = "";
    private boolean loopIsRunning = true;

    public String getLogin() {
        return login;
    }

    // initialize client handler
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
        while (loopIsRunning) {
            readMessages();
        }
    }

    // main thread for read messages
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
            // disconnect current client
            loopIsRunning = false;
        }
    }

    // extract command from message
    private String getCommand(String msg) {
        String[] arr = msg.split(" ");
        if (arr.length > 0) {
            return arr[0];
        } else {
            return "";
        }
    }

    // login with user & password
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

    // send to client from cloud storage
    private synchronized void sendCurrentList() throws IOException {
        File dir = new File(CommonConstants.filesPath);
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isFile())
                outStream.writeUTF(file.getName());
        }
        outStream.writeUTF("<EndFileList>");
        outStream.flush();
    }

    // write text file from cloud client
    private synchronized void writeCloudFile() throws IOException {
        String fileName = inpStream.readUTF();
        String content = inpStream.readUTF();
        //String res = "";
        try {
            Files.writeString(Path.of(CommonConstants.filesPath, fileName),
                    content, StandardOpenOption.CREATE);
            /*File file = new File(fileName);
            //create the file.
            if (file.createNewFile()) {
                System.out.println("File is created!");
            } else {
                System.out.println("File already exists.");
            }
            //write content
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();*/
            outStream.writeUTF("File uploaded.");
        } catch (IOException e) {
            outStream.writeUTF("Error upload: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
        }
        outStream.flush();
    }

}


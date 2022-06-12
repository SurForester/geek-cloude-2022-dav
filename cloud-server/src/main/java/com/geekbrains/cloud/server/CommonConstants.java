package com.geekbrains.cloud.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class CommonConstants {
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 8181;

    public static class Network {
        private Socket socket;
        private DataInputStream inpStream;
        private DataOutputStream outStream;
        private boolean serverConnected;
        private String currentStatus;

        public void sendMessage(String message) throws IOException {
            outStream.writeUTF(message);
            outStream.flush();
        }

        public String readMessage() throws IOException {
            return inpStream.readUTF();
        }

        public Network(String server, int port) {
            try {
                socket = new Socket(server, port);
                inpStream = new DataInputStream(socket.getInputStream());
                outStream = new DataOutputStream(socket.getOutputStream());
                currentStatus = "Server connected.";
            } catch (IOException e) {
                currentStatus = e.getMessage() + "\n" + Arrays.toString(e.getStackTrace());
            }
        }


        public boolean connectCloud(String user, String pwd) {
            try {
                currentStatus = "Cloud connected. User - " + user;
                sendMessage("<Login> " + user + " " + pwd);
                String res = readMessage();
                String[] arr = res.split(" ");
                currentStatus = res;
                if (arr[0].equals("email:")) {
                    return true;
                }
                return false;
            } catch (IOException e) {
                currentStatus = e.getMessage() + "\n" + Arrays.toString(e.getStackTrace());
                return false;
            }
        }

        public String getStatus() {
            return currentStatus;
        }

    }
}

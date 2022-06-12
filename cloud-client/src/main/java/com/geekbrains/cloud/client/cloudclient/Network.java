package com.geekbrains.cloud.client.cloudclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network {

    private String server;
    private int port;
    private Socket socket;
    private DataInputStream inpStream;
    private DataOutputStream outStream;
    private String resultString = "";
    
    public Network(String server, int port) {
        this.server = server;
        this.port = port;
        try {
            socket = new Socket(server, port);
            inpStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            resultString = e.getMessage();
        }
    }

    public boolean connectCloud(String user, String pwd) {
        try {
            resultString = "";
            sendMessage("<Login> " + user + " " + pwd);
            String res = readMessage();
            String[] arr = res.split(" ");
            if (arr[0].equals("email:")) {
                return true;
            } else {
                resultString = res;
            }
        } catch (Exception e) {
            resultString = e.getMessage();
        }
        return false;
    }

    public String getStatus() {
        return resultString;
    }

    public void sendMessage(String s) {
        try {
            outStream.writeUTF(s);
            outStream.flush();
        } catch (IOException e) {
            resultString = e.getMessage();
        }
    }

    public String readMessage() {
        try {
            return inpStream.readUTF();
        } catch (IOException e) {
            return  "Error read command: " + e.getMessage();
        }
    }
}

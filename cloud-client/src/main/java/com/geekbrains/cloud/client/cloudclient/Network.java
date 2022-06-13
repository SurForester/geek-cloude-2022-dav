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
    private boolean serverConnected;
    
    public Network(String server, int port) {
        this.server = server;
        this.port = port;
        serverConnected = false;
        try {
            // connect socket
            socket = new Socket(server, port);
            // open streams
            inpStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
            serverConnected = true;
        } catch (IOException e) {
            resultString = e.getMessage();
        }
    }

    // identify user in cloud
    public boolean connectCloud(String user, String pwd) {
        try {
            resultString = "";
            // send request
            sendMessage("<Login> " + user + " " + pwd);
            // gets respond
            String res = readMessage();
            String[] arr = res.split(" ");
            if (arr[1].equals("email:")) { // if respond content "email:"
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
            if (!socket.isConnected()) { serverConnected = false; }
        }
    }

    public String readMessage() {
        try {
            return inpStream.readUTF();
        } catch (IOException e) {
            if (!socket.isConnected()) { serverConnected = false; }
            return  "Error read command: " + e.getMessage();
        }
    }

    public boolean isServerConnected() {
        return serverConnected;
    }
}

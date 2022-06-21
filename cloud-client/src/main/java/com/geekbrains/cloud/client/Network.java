package com.geekbrains.cloud.client;

import com.geekbrains.cloud.model.CloudMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network {

    private String server;
    private int port;
    private String resultString = "";
    private boolean serverConnected;
    private ObjectDecoderInputStream is;
    private ObjectEncoderOutputStream os;

    public Network(String server, int port) throws IOException {
        this.server = server;
        this.port = port;
        Socket socket = new Socket(server, port);
        os = new ObjectEncoderOutputStream(socket.getOutputStream());
        is = new ObjectDecoderInputStream(socket.getInputStream());
    }

    // identify user in cloud
    public boolean connectCloud(String user, String pwd) {
        resultString = "<logged> ...";
        return true;
        /*try {
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
        return false;*/
    }

    public String getStatus() {
        return resultString;
    }

    public CloudMessage read() throws IOException, ClassNotFoundException {
        return (CloudMessage) is.readObject();
    }

    public void write(CloudMessage msg) throws IOException {
        os.writeObject(msg);
        os.flush();
    }

    public boolean isServerConnected() {
        return serverConnected;
    }
}

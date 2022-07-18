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
    private ObjectDecoderInputStream is;
    private ObjectEncoderOutputStream os;

    public Network(String server, int port) throws IOException {
        this.server = server;
        this.port = port;
        Socket socket = new Socket(server, port);
        os = new ObjectEncoderOutputStream(socket.getOutputStream());
        is = new ObjectDecoderInputStream(socket.getInputStream());
    }

    public CloudMessage read() throws IOException, ClassNotFoundException {
        return (CloudMessage) is.readObject();
    }

    public void write(CloudMessage msg) throws IOException {
        os.writeObject(msg);
        os.flush();
    }

}

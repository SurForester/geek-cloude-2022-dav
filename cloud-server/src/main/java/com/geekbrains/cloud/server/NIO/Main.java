package com.geekbrains.cloud.server.NIO;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        NioServer server = new NioServer();
        server.start();
    }
}

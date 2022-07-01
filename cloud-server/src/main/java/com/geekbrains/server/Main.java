package com.geekbrains.server;

public class Main {
    public static void main(String[] args) {
        /*DbConnect conn = new DbConnect();
        conn.userLogin("User1", "pwd");
        conn.closeDatabase();*/
        new NettyCloudServer();
    }
}

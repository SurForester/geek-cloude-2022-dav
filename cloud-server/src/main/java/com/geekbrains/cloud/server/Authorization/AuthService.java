package com.geekbrains.cloud.server.Authorization;

public interface AuthService {
    void start();
    boolean loginExists(String login);
    String userRegister(String login, String pwd, String email);
    String userLogin(String login, String pwd);
    void end();
}

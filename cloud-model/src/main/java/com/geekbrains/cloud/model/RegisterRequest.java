package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class RegisterRequest implements CloudMessage {
    private final String login;
    private final String pwd;
    public RegisterRequest(String login, String pwd) {
        this.login = login;
        this.pwd = pwd;
    }
}

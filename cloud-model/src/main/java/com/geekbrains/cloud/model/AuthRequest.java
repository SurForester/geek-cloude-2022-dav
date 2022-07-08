package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class AuthRequest implements CloudMessage {

    private String login;
    private String pwd;

    public AuthRequest(String login, String pwd) {
        this.login = login;
        this.pwd = pwd;
    }

}

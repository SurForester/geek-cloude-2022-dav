package com.geekbrains.cloud.model;

import lombok.Data;

import java.util.List;
@Data
public class RegisterResponse implements CloudMessage {
    private String userID;
    List<TableList> files;
    public RegisterResponse(String userID, List<TableList> filesList) {
        this.userID = userID;
        this.files = filesList;
    }
}

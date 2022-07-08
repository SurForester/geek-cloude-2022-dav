package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class FileRequest implements CloudMessage {
    private final String userID;
    private final String name;
    public FileRequest(String userID, String name) {
        this.userID = userID;
        this.name = name;
    }
}

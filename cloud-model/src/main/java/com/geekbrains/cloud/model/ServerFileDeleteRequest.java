package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class ServerFileDeleteRequest implements CloudMessage {
    private final String userID;
    private final String fileName;

    public ServerFileDeleteRequest(String userID, String fileName) {
        this.userID = userID;
        this.fileName = fileName;
    }
}

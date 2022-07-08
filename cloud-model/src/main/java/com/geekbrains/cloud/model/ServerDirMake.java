package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class ServerDirMake implements CloudMessage {
    private final String userID;
    private final String nameDir;
    public ServerDirMake(String userID, String nameDir) {
        this.userID = userID;
        this.nameDir = nameDir;
    }
}

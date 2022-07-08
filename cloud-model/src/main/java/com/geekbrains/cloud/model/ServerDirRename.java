package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class ServerDirRename implements CloudMessage {
    private final String userID;
    private final String fromName;
    private final String toName;

    public ServerDirRename(String userID, String fromName, String toName) {
        this.userID = userID;
        this.fromName = fromName;
        this.toName = toName;
    }
}

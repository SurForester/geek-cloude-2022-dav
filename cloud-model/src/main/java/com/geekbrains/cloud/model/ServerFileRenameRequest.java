package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class ServerFileRenameRequest implements CloudMessage {
    private final String userID;
    private final String oldName;
    private final String newName;

    public ServerFileRenameRequest(String userID, String oldName, String newName) {
        this.userID = userID;
        this.oldName = oldName;
        this.newName = newName;
    }
}

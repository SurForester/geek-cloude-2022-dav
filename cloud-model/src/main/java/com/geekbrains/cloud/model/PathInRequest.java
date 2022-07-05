package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class PathInRequest implements CloudMessage {
    private final String userID;
    private final String path;
    public PathInRequest(String userID, String path) {
        this.userID = userID;
        this.path = path;
    }
}

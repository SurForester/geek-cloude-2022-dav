package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class ServerItemMessage {
    private String name;
    private final long size;
    private String type;
    public ServerItemMessage(String name, String type, long size) {
        this.name = name;
        this.type = type;
        this.size = size;
    }
}

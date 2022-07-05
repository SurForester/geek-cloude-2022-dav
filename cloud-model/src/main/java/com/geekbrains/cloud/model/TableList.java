package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class TableList implements CloudMessage {
    private String name;
    private String type;
    private long size;

    public TableList(String name, String type, long size) {
        this.name = name;
        this.type = type;
        this.size = size;
    }
}

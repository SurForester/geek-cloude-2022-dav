package com.geekbrains.cloud.model;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Data
public class FileMessage implements CloudMessage {

    private String userID;
    private final long size;
    private final byte[] data;
    private String name;

    public FileMessage(String userID, Path path) throws IOException {
        this.userID = userID;
        size = Files.size(path);
        data = Files.readAllBytes(path);
        name = path.getFileName().toString();
    }

    public void setName(String name) {
        this.name = name;
    }

}

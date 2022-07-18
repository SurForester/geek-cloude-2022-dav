package com.geekbrains.cloud.model;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ServerListFiles implements CloudMessage {

    private final List<TableList> files;

    public ServerListFiles(List<TableList> fileList) throws IOException {
        files = fileList;
    }

}

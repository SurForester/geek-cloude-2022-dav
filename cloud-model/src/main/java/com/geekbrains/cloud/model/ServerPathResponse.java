package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class ServerPathResponse implements CloudMessage {
    private final String pathString;
}

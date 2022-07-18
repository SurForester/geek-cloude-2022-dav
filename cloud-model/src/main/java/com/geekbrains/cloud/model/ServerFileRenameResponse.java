package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class ServerFileRenameResponse implements CloudMessage {
    private final String result;
}

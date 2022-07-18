package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class ServerFileDeleteResponse implements CloudMessage {
    private final String result;
}

package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class ErrorMessage implements CloudMessage {
    private final String message;
    private final String stackTrace;
}

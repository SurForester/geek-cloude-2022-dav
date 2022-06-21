package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class PathInRequest implements CloudMessage {

    private final String path;
    // in to selected dir

}

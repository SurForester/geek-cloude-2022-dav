package com.geekbrains.cloud.model;

import lombok.Data;

@Data
public class PathUpRequest implements CloudMessage {

    private final String userID;

}

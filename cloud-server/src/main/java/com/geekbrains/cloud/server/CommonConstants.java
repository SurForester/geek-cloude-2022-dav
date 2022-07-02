package com.geekbrains.cloud.server;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonConstants {
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 8181;
    public static final String filesPath = Paths.get("").
            resolve("cloud-server").resolve("cloudFiles").
            toAbsolutePath().normalize().toString();
    public static final String fileSeparator = System.getProperty("file.separator");
}

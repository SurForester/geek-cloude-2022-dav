module com.geekbrains.cloud.client.cloudclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.netty.codec;
    requires com.geekbrains.cloud.june.model;

//    opens com.geekbrains.cloud.model.client.cloudclient to javafx.fxml;
    exports com.geekbrains.cloud.client;
    opens com.geekbrains.cloud.client to javafx.fxml;
}
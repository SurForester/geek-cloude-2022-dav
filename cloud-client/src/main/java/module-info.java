module com.geekbrains.cloud.client.cloudclient {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.geekbrains.cloud.client.cloudclient to javafx.fxml;
    exports com.geekbrains.cloud.client.cloudclient;
}
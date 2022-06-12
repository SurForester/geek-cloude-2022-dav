package com.geekbrains.cloud.client.cloudclient;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class CloudClientController implements Initializable {

    private String localSelected = "";
    private String cloudSelected = "";
    private MultipleSelectionModel<String> localSelectionModel;
    private MultipleSelectionModel<String> cloudSelectionModel;
    private Network network;
    @FXML
    public TextField textStatus;
    @FXML
    public ListView<String> listviewLocal;
    @FXML
    public ListView<String> listviewServer;
    @FXML
    public Button buttonUpload;
    @FXML
    public TextField textUser;
    @FXML
    public PasswordField textPassword;
    @FXML
    public Button buttonLocalStorage;
    @FXML
    public Label labelLocalPath;
    @FXML
    public Button buttonServerStorage;
    @FXML
    public Label labelServerPath;
    @FXML
    public Button buttonConnect;
    @FXML
    public Button buttonDownload;

    public void connectToServer(ActionEvent actionEvent) throws IOException {
        String user = textUser.getText();
        String pwd = textPassword.getText();
        // connect to Cloud
        if (network.connectCloud(user, pwd)) {
            textUser.setDisable(true);
            textPassword.setDisable(true);
            buttonConnect.setDisable(true);
            getCloudList();
        }
        textStatus.setText(network.getStatus());
    }

    public void getLocalList() {
        listviewLocal.getItems().clear();
        File dir = new File("D:\\Cloud\\LocalFiles\\"); //labelLocalPath.getText());
        for (File file : dir.listFiles()) {
            if (file.isFile())
                listviewLocal.getItems().add(file.getName());
        }
    }

    public void getCloudList() throws IOException {
        network.sendMessage("<GetCurrentList>");
        Platform.runLater(() -> listviewServer.getItems().clear());
        try {
            String message = network.readMessage();
            while (message != "<EndFileList>") {
                String finalMessage = message;
                listviewServer.getItems().add(finalMessage);
                message = network.readMessage();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            network = new Network("localhost", 8181);
            //File dir = new File(getClass().getClassLoader().getResource("localFiles").getFile());
            labelLocalPath.setText("D:\\Cloud\\LocalFiles\\");
            labelServerPath.setText("[root]");
            localSelectionModel = listviewLocal.getSelectionModel();
            localSelectionModel.selectedItemProperty().addListener(new ChangeListener<String>() {
                public void changed(ObservableValue<? extends String> changed, String oldValue, String newValue) {
                    localSelected = newValue;
                }
            });
            cloudSelectionModel = listviewServer.getSelectionModel();
            cloudSelectionModel.selectedItemProperty().addListener(new ChangeListener<String>() {
                public void changed(ObservableValue<? extends String> changed, String oldValue, String newValue) {
                    cloudSelected = newValue;
                }
            });
            getLocalList();
        } catch (Exception e) {
            textStatus.setText(e.getMessage());
        }
    }

    public boolean uploadFile() throws IOException {
        boolean result = false;
        if (localSelected.isEmpty()) {
            textStatus.setText("Выберите локальный файл для передачи.");
            return true;
        }
        Path fileName = Paths.get(labelLocalPath.getText() + "\\" + localSelected);
        network.sendMessage("<SendFile>");
        network.sendMessage(localSelected);
        String contentFile = new String(Files.readAllBytes(fileName));
        network.sendMessage(contentFile);
        String resultString = network.readMessage();
        return result;
    }
}
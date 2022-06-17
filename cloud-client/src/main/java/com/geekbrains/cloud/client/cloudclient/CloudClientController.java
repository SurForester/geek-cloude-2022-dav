package com.geekbrains.cloud.client.cloudclient;

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

    // connect to cloud from user & pwd
    public void connectToServer(ActionEvent actionEvent) throws IOException {
        String user = textUser.getText();
        String pwd = textPassword.getText();
        // connect to Cloud
        if (network.connectCloud(user, pwd)) {
            textUser.setDisable(true);
            textPassword.setDisable(true);
            buttonConnect.setDisable(true);
            buttonUpload.setDisable(false);
            getCloudList();
        }
        textStatus.setText(network.getStatus());
    }

    // get list files from local path
    public void getLocalList() {
        listviewLocal.getItems().clear();
        File dir = new File(labelLocalPath.getText());
        for (File file : dir.listFiles()) {
            if (file.isFile())
                listviewLocal.getItems().add(file.getName());
        }
    }

    // get list files from cloud
    public void getCloudList() throws IOException {
        network.sendMessage("<GetCurrentList>");
        listviewServer.getItems().clear();
        try {
            String message = network.readMessage();
            while (!message.equals("<EndFileList>")) {
                String finalMessage = message;
                listviewServer.getItems().add(finalMessage);
                message = network.readMessage();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // initialise the class
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            network = new Network("localhost", 8181);
            Path currentWorkingDir = Paths.get("").resolve("cloud-client").resolve("localFiles").toAbsolutePath();
            labelLocalPath.setText(currentWorkingDir.normalize().toString());
            labelServerPath.setText("[root]");
            listviewLocal.getItems().add("<Empty list>");
            listviewServer.getItems().add("<Empty list>");
            // define listview selector for local
            localSelectionModel = listviewLocal.getSelectionModel();
            localSelectionModel.selectedItemProperty().addListener(new ChangeListener<String>() {
                public void changed(ObservableValue<? extends String> changed, String oldValue, String newValue) {
                    localSelected = newValue;
                }
            });
            // define listview selector for cloud
            cloudSelectionModel = listviewServer.getSelectionModel();
            cloudSelectionModel.selectedItemProperty().addListener(new ChangeListener<String>() {
                public void changed(ObservableValue<? extends String> changed, String oldValue, String newValue) {
                    cloudSelected = newValue;
                }
            });
            buttonDownload.setDisable(true);
            buttonUpload.setDisable(true);
            getLocalList();
        } catch (Exception e) {
            textStatus.setText(e.getMessage());
        }
    }

    // upload text file
    public boolean uploadFile() throws IOException {
        boolean result = false;
        String selString = listviewLocal.getSelectionModel().getSelectedItem();
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
        if (resultString.equals("File uploaded.")) {
            getCloudList();
        }
        return result;
    }
}
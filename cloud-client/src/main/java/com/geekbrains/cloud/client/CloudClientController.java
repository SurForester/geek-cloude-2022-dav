package com.geekbrains.cloud.client;

import com.geekbrains.cloud.model.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;

public class CloudClientController implements Initializable {

    private Path rootDir;
    private Path currentDir;
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
        }
        textStatus.setText(network.getStatus());
    }

    // get list files from local path
    public void getLocalList() {
        File dir = new File(labelLocalPath.getText());
        for (File file : dir.listFiles()) {
            if (file.isFile())
                listviewLocal.getItems().add(file.getName());
        }
        String[] list = new File(dir.getName()).list();
        assert list != null;
        listviewLocal.getItems().clear();
        listviewLocal.getItems().addAll(Arrays.asList(list));
    }

    // initialise the class
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            network = new Network("localhost", 8181);
            rootDir = Paths.get("").resolve("cloud-client").resolve("localFiles").toAbsolutePath();
            currentDir = Path.of(rootDir.toString());
            labelLocalPath.setText(rootDir.normalize().toString());
            labelServerPath.setText("[root]");
            listviewLocal.getItems().add("<Empty list>");
            listviewServer.getItems().add("<Empty list>");
            buttonDownload.setDisable(true);
            buttonUpload.setDisable(true);
            getLocalList();
            Thread thread = new Thread(this::listenCloudServer);
            thread.setDaemon(true);
            thread.start();
        } catch (Exception e) {
            textStatus.setText(e.getMessage());
        }
    }

    public void listenCloudServer() {
        try {
            while (true) {
                CloudMessage message = network.read();
                if (message instanceof ListFiles listFiles) {
                    listviewServer.getItems().clear();
                    listviewServer.getItems().addAll(listFiles.getFiles());
                } else if (message instanceof FileMessage fileMessage) {
                    Path current = Path.of(labelLocalPath.getText()).resolve(fileMessage.getName());
                    Files.write(current, fileMessage.getData());
                    getLocalList();
                }
            }
        } catch (Exception e) {
            System.err.println("Connection lost");
        }
    }

    // upload text file
    public void uploadFile() throws IOException {
        String fileName = listviewLocal.getSelectionModel().getSelectedItem();
        network.write(new FileMessage(Path.of(labelLocalPath.getText()).resolve(fileName)));
    }

    public void downloadFile() throws IOException {
        String fileName = listviewServer.getSelectionModel().getSelectedItem();
        network.write(new FileRequest(fileName));
    }

    public void mouseLocalViewClick(MouseEvent mouseEvent) throws IOException {
        if (mouseEvent.getClickCount() == 2) {
            String fileName = listviewServer.getSelectionModel().getSelectedItem();
            if (fileName.equals("..")) {
                currentDir = Path.of(currentDir.toString()).getParent();
                listviewLocal.getItems().clear();
                if (rootDir.toString().equals(currentDir.toString())) {
                    listviewLocal.getItems().addAll(new ListFiles(currentDir, true).getFiles());
                } else {
                    listviewLocal.getItems().addAll(new ListFiles(currentDir, false).getFiles());
                }
            } else {
                currentDir = Path.of(currentDir.toString()).getParent();
                listviewLocal.getItems().clear();
                listviewLocal.getItems().addAll(new ListFiles(currentDir, false).getFiles());
            }
        }
    }

    public void mouseServerViewClick(MouseEvent mouseEvent) throws IOException {
        if (mouseEvent.getClickCount() == 2) {
            String fileName = listviewServer.getSelectionModel().getSelectedItem();
            if (fileName.equals("..")) {
                network.write(new PathUpRequest());
            } else {
                Path inPath = Path.of(labelServerPath.getText()).resolve(fileName);
                network.write(new PathInRequest(inPath.toString()));
                labelServerPath.setText(labelServerPath.getText() + "\\" + fileName);
            }
        }
    }
}
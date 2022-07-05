package com.geekbrains.cloud.client;

import com.geekbrains.cloud.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.ResourceBundle;

public class CloudClientController implements Initializable {

    public AnchorPane ap;
    private String userID;
    @FXML
    public TableView<TableList> serverTable;
    public TableView<TableList> localTable;
    private ObservableList<TableList> serverTableList = FXCollections.observableArrayList();
    private ObservableList<TableList> localTableList = FXCollections.observableArrayList();
    @FXML
    private TableColumn<TableList, String> localFileName, localFileType, localFileSize;
    @FXML
    private TableColumn<TableList, String> serverFileName, serverFileType, serverFileSize;
    public TextArea localPath;
    public Button buttonDownload;
    private Path rootDir;
    private Network network;
    @FXML
    public TextField textStatus;
    @FXML
    public Button buttonUpload;
    @FXML
    public TextField textUser;
    @FXML
    public PasswordField textPassword;
    @FXML
    public Button buttonLocalStorage;
    @FXML
    public Button buttonConnect;

    public void connectToServer() throws IOException {
        network = new Network("localhost", 8189);
        String user = textUser.getText();
        String pwd = textPassword.getText();
        Thread thread = new Thread(this::listenCloudServer);
        thread.setDaemon(true);
        thread.start();
        network.write(new AuthRequest(user, pwd));
    }

    // get list files from local path
    public void getLocalList(boolean isRoot) {
        ObservableList<TableList> slist = localTable.getItems();
        slist.clear();
        File dir = new File(localPath.getText());
        if (!isRoot)
            slist.add(new TableList("..", "upDir", 0L));
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                slist.add(new TableList(file.getName(), "dir", 0L));
            } else {
                slist.add(new TableList(file.getName(), "file", file.length()));
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            rootDir = Paths.get("").resolve("cloud-client").resolve("localFiles").toAbsolutePath();
            localPath.setText(rootDir.normalize().toString());
            buttonDownload.setDisable(true);
            buttonUpload.setDisable(true);
            localFileName.setCellValueFactory(new PropertyValueFactory<TableList, String>("name"));
            localFileType.setCellValueFactory(new PropertyValueFactory<TableList, String>("type"));
            localFileSize.setCellValueFactory(new PropertyValueFactory<TableList, String>("size"));
            serverFileName.setCellValueFactory(new PropertyValueFactory<TableList, String>("name"));
            serverFileType.setCellValueFactory(new PropertyValueFactory<TableList, String>("type"));
            serverFileSize.setCellValueFactory(new PropertyValueFactory<TableList, String>("size"));
            getLocalList(true);
        } catch (Exception e) {
            textStatus.setText(e.getMessage());
        }
    }

    public void listenCloudServer() {
        try {
            while (true) {
                CloudMessage message = network.read();
                if (message instanceof ServerListFiles serverListFiles) {
                    ObservableList<TableList> serverList = serverTable.getItems();
                    serverList.clear();
                    serverList.addAll(serverListFiles.getFiles());
                } else if (message instanceof FileMessage fileMessage) {
                    Path current = Path.of(localPath.getText()).resolve(fileMessage.getName());
                    Files.write(current, fileMessage.getData());
                    if (rootDir.toString().equals(localPath.getText())) {
                        getLocalList(true);
                    } else {
                        getLocalList(false);
                    }
                } else if (message instanceof AuthResponse authResponse) {
                    userID = authResponse.getUserID();
                    ObservableList<TableList> serverList = serverTable.getItems();
                    serverList.clear();
                    serverList.addAll(authResponse.getFiles());
                    textUser.setDisable(true);
                    textPassword.setDisable(true);
                    buttonConnect.setDisable(true);
                    buttonUpload.setDisable(false);
                } else if (message instanceof ErrorMessage errorMessage) {
                    showError(errorMessage.getMessage(), errorMessage.getStackTrace());
                }
            }
        } catch (Exception e) {
            showError(e.getMessage(), e.getStackTrace().toString());
        }
    }

    public void uploadFile() throws IOException {
        String fileName = ((TableList) localTable.getSelectionModel().getSelectedItem()).getName();
        network.write(new FileMessage(userID, Path.of(localPath.getText()).resolve(fileName)));
    }

    public void downloadFile() throws IOException {
        String fileName = ((TableList) serverTable.getSelectionModel().getSelectedItem()).getName();
        network.write(new FileRequest(userID, fileName));
    }

    public void clickServerList(MouseEvent mouseEvent) throws IOException {
        if (mouseEvent.getClickCount() == 2) {
            String fileName = ((TableList) serverTable.getSelectionModel().getSelectedItem()).getName();
            String fileType = ((TableList) serverTable.getSelectionModel().getSelectedItem()).getType();
            if (fileName.equals("..")) {
                network.write(new PathUpRequest(userID));
            } else if (fileType.equals("dir")) {
                network.write(new PathInRequest(userID, fileName));
            }
        }
    }

    public void clickLocalList(MouseEvent mouseEvent) {
        Path currentDir = Path.of(localPath.getText());
        String fileName = ((TableList) localTable.getSelectionModel().getSelectedItem()).getName();
        String fileType = ((TableList) localTable.getSelectionModel().getSelectedItem()).getType();
        if (mouseEvent.getClickCount() == 2) {
            if (fileName.equals("..")) {
                currentDir = Path.of(localPath.getText()).getParent();
                localPath.setText(currentDir.toString());
                if (rootDir.toString().equals(currentDir.toString())) {
                    getLocalList(true);
                } else {
                    getLocalList(false);
                }
            } else if (fileType.equals("dir")) {
                currentDir = Path.of(currentDir.toString()).resolve(fileName);
                localPath.setText(currentDir.toString());
                getLocalList(false);
            }
        }
    }

    public void selectLocalStorage(ActionEvent actionEvent) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        Stage stage = (Stage) ap.getScene().getWindow();
        File dir = directoryChooser.showDialog(stage);
        if (dir != null) {
            Path currentDir = Path.of(dir.getAbsolutePath().toString()).getParent();
            localPath.setText(dir.getAbsolutePath());
            if (currentDir == null) {
                rootDir = Path.of(dir.toString()).toAbsolutePath();
                getLocalList(true);
            } else {
                getLocalList(false);
            }
        }
    }

    public void showError(String errorMessage, String stackTrace) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(errorMessage);
        alert.setContentText(stackTrace);
        alert.showAndWait();
    }
}
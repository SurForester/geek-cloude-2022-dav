package com.geekbrains.cloud.client;

import com.geekbrains.cloud.model.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class CloudClientController implements Initializable {
    @FXML
    public Button buttonMakeDir;
    @FXML
    public Button buttonRenameDir;
    @FXML
    public Button buttonRenameFile;
    @FXML
    public Button buttonDeleteDir;
    @FXML
    public Button buttonDeleteFile;
    @FXML
    public Button buttonRegister;
    @FXML
    public Label labelServerPath;
    private String user, pwd;
    public AnchorPane ap;
    private String userID;
    @FXML
    public TableView<TableList> serverTable;
    public TableView<TableList> localTable;
    //private ObservableList<TableList> serverTableList = FXCollections.observableArrayList();
    //private ObservableList<TableList> localTableList = FXCollections.observableArrayList();
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
    public Button buttonLocalStorage;
    @FXML
    public Button buttonConnect;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            rootDir = Paths.get("").resolve("cloud-client").resolve("localFiles").toAbsolutePath();
            localPath.setText(rootDir.normalize().toString());
            localFileName.setCellValueFactory(new PropertyValueFactory<>("name"));
            localFileType.setCellValueFactory(new PropertyValueFactory<>("type"));
            localFileSize.setCellValueFactory(new PropertyValueFactory<>("size"));
            serverFileName.setCellValueFactory(new PropertyValueFactory<>("name"));
            serverFileType.setCellValueFactory(new PropertyValueFactory<>("type"));
            serverFileSize.setCellValueFactory(new PropertyValueFactory<>("size"));
            getLocalList(true);
            network = new Network("localhost", 8189);
            Thread thread = new Thread(this::listenCloudServer);
            thread.setDaemon(true);
            thread.start();

        } catch (Exception e) {
            textStatus.setText(e.getMessage());
        }
    }

    public void connectToServer() {
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Connect to Cloud");
        dialog.setHeaderText(null);
        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        TextField username = new TextField();
        username.setPromptText("Username");
        username.setText("User1");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setText("pwd");
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> loginButton.setDisable(newValue.trim().isEmpty()));
        dialog.getDialogPane().setContent(grid);
        // Request focus on the username field by default.
        Platform.runLater(username::requestFocus);
        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });
        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(usernamePassword -> {
            user = usernamePassword.getKey();
            pwd = usernamePassword.getValue();
            try {
                network.write(new AuthRequest(user, pwd));
            } catch (Exception e) {
                showError(e.getMessage(), Arrays.toString(e.getStackTrace()));
            }
        });
    }

    // get list files from local path
    public void getLocalList(boolean isRoot) {
        ObservableList<TableList> slist = localTable.getItems();
        slist.clear();
        File dir = new File(localPath.getText());
        if (!isRoot) slist.add(new TableList("..", "upDir", 0L));
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                slist.add(new TableList(file.getName(), "dir", 0L));
            } else {
                slist.add(new TableList(file.getName(), "file", file.length()));
            }
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
                } else if (message instanceof ServerPathResponse serverPathResponse) {
                    Platform.runLater(() -> labelServerPath.setText(serverPathResponse.getPathString()));
                } else if (message instanceof FileMessage fileMessage) {
                    Path current = Path.of(localPath.getText()).resolve(fileMessage.getName()).toAbsolutePath();
                    Files.write(current, fileMessage.getData());
                    getLocalList(rootDir.toString().equals(localPath.getText()));
                } else if (message instanceof AuthResponse authResponse) {
                    userID = authResponse.getUserID();
                    if (userID.startsWith("OK ")) {
                        String[] arr = userID.split(" ");
                        userID = arr[1];
                        ObservableList<TableList> serverList = serverTable.getItems();
                        serverList.clear();
                        serverList.addAll(authResponse.getFiles());
                        buttonConnect.setDisable(true);
                        buttonRegister.setDisable(true);
                        buttonUpload.setDisable(false);
                        buttonDownload.setDisable(false);
                        buttonMakeDir.setDisable(false);
                        buttonRenameDir.setDisable(false);
                        buttonRenameFile.setDisable(false);
                        buttonDeleteFile.setDisable(false);
                    } else if (userID.equals("WRONG_PWD")) {
                        Platform.runLater(() -> infoDialog("Wrong user password"));
                    } else {
                        Platform.runLater(() -> {
                            if (showYesNoDialog("This user not registered, press register ...")) {
                                try {
                                    network.write(new RegisterRequest(user, pwd));
                                } catch (IOException e) {
                                    showException(e.getMessage(), Arrays.toString(e.getStackTrace()));
                                }
                            }
                        });
                    }
                } else if (message instanceof ErrorMessage errorMessage) {
                    Platform.runLater(() -> showError(errorMessage.getMessage(), errorMessage.getStackTrace()));
                } else if (message instanceof RegisterResponse registerResponse) {
                    userID = registerResponse.getUserID();
                    if (userID.startsWith("OK ")) {
                        String[] arr = userID.split(" ");
                        userID = arr[1];
                        ObservableList<TableList> serverList = serverTable.getItems();
                        serverList.clear();
                        serverList.addAll(registerResponse.getFiles());
                        buttonConnect.setDisable(true);
                        buttonRegister.setDisable(true);
                        buttonUpload.setDisable(false);
                        buttonDownload.setDisable(false);
                        buttonMakeDir.setDisable(false);
                        buttonRenameDir.setDisable(false);
                        buttonRenameFile.setDisable(false);
                        buttonDeleteFile.setDisable(false);
                    }
                } else if (message instanceof ServerFileRenameResponse serverFileRenameResponse) {
                    String res = serverFileRenameResponse.getResult();
                    if (!res.equals("OK")) {
                        Platform.runLater(() -> showError("Server file renamed successfully.", res));
                    }
                } else if (message instanceof ServerFileDeleteResponse serverFileDeleteResponse) {
                    String res = serverFileDeleteResponse.getResult();
                    if (!res.equals("OK")) {
                        Platform.runLater(() -> showError("Server file deleted successfully.", res));
                    }
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> showException(e.getMessage(), Arrays.toString(e.getStackTrace())));
        }
    }

    public void uploadFile() {
        try {
            TableList tl = localTable.getSelectionModel().getSelectedItem();
            if (tl == null) {
                infoDialog("Select file in Local list.");
            } else {
                String type = tl.getType();
                String fileName = tl.getName();
                if (type.equals("file")) {
                    network.write(new FileMessage(userID, Path.of(localPath.getText()).resolve(fileName)));
                } else {
                    infoDialog("Select the file.");
                }
            }
        } catch (IOException e) {
            showException(e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    public void downloadFile() {
        try {
            TableList tl = serverTable.getSelectionModel().getSelectedItem();
            if (tl == null) {
                infoDialog("Select file in Server list.");
            } else {
                String type = tl.getType();
                String fileName = tl.getName();
                if (type.equals("file")) {
                    network.write(new FileRequest(userID, fileName));
                } else {
                    infoDialog("Select the file.");
                }
            }
        } catch (IOException e) {
            showException(e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    public void clickServerList(MouseEvent mouseEvent) throws IOException {
        if (mouseEvent.getClickCount() == 2) {
            TableList tl = serverTable.getSelectionModel().getSelectedItem();
            if (tl == null) {
                return;
            }
            String fileName = serverTable.getSelectionModel().getSelectedItem().getName();
            String fileType = serverTable.getSelectionModel().getSelectedItem().getType();
            if (fileName.equals("..")) {
                network.write(new PathUpRequest(userID));
            } else if (fileType.equals("dir")) {
                network.write(new PathInRequest(userID, fileName));
            }
        }
    }

    public void clickLocalList(MouseEvent mouseEvent) {
        Path currentDir = Path.of(localPath.getText());
        TableList tl = localTable.getSelectionModel().getSelectedItem();
        if (tl == null) {
            return;
        }
        String fileName = localTable.getSelectionModel().getSelectedItem().getName();
        String fileType = localTable.getSelectionModel().getSelectedItem().getType();
        if (mouseEvent.getClickCount() == 2) {
            if (fileName.equals("..")) {
                currentDir = Path.of(localPath.getText()).getParent();
                localPath.setText(currentDir.toString());
                getLocalList(rootDir.toString().equals(currentDir.toString()));
            } else if (fileType.equals("dir")) {
                currentDir = Path.of(currentDir.toString()).resolve(fileName);
                localPath.setText(currentDir.toString());
                getLocalList(false);
            }
        }
    }

    public void selectLocalStorage() {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        Stage stage = (Stage) ap.getScene().getWindow();
        File dir = directoryChooser.showDialog(stage);
        if (dir != null) {
            Path currentDir = Path.of(dir.getAbsolutePath()).getParent();
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

    public boolean showYesNoDialog(String question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(null);
        alert.setContentText(question);
        ButtonType buttonTypeYes = new ButtonType("YES");
        ButtonType buttonTypeNo = new ButtonType("NO");
        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);
        Optional<ButtonType> result = alert.showAndWait();
        return result.get() == buttonTypeYes;
    }

    public void showException(String errorMessage, String stackTrace) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception info");
        alert.setHeaderText(errorMessage);
        alert.setContentText(null);
        Label label = new Label("The exception stacktrace was:");
        TextArea textArea = new TextArea(stackTrace);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);
        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

    public void makeServerDir() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Server DirName Input Dialog");
        dialog.setHeaderText("Input server dir name");
        dialog.setContentText("Enter name:");
        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        try {
            if (result.isPresent()) {
                network.write(new ServerDirMake(userID, result.get()));
            }
        } catch (IOException e) {
            showError(e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

    public void renameServerDir() {
        TableList tl = serverTable.getSelectionModel().getSelectedItem();
        if (tl == null) {
            infoDialog("Select directory in Server list.");
        } else {
            String type = tl.getType();
            if (type.equals("dir")) {
                TextInputDialog dialog = new TextInputDialog(tl.getName());
                dialog.setTitle(null);
                dialog.setHeaderText("Input server dir name for rename");
                dialog.setContentText("Enter name:");
                // Traditional way to get the response value.
                Optional<String> result = dialog.showAndWait();
                try {
                    if (result.isPresent()) {
                        network.write(new ServerDirRename(userID, tl.getName(), result.get()));
                    }
                } catch (IOException e) {
                    showError(e.getMessage(), Arrays.toString(e.getStackTrace()));
                }
            } else {
                infoDialog("Select the directory, please.");
            }
        }
    }

    public void infoDialog(String infoString) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(infoString);
        alert.showAndWait();
        alert.close();
    }

    public void renameServerFile() {
        TableList tl = serverTable.getSelectionModel().getSelectedItem();
        if (tl == null) {
            infoDialog("Select file in Server list.");
        } else {
            String type = tl.getType();
            if (type.equals("file")) {
                TextInputDialog dialog = new TextInputDialog(tl.getName());
                dialog.setTitle(null);
                dialog.setHeaderText("Edit server file name for rename");
                dialog.setContentText("Edit name:");
                // Traditional way to get the response value.
                Optional<String> result = dialog.showAndWait();
                try {
                    if (result.isPresent()) {
                        network.write(new ServerFileRenameRequest(userID, tl.getName(), result.get()));
                    }
                } catch (IOException e) {
                    showError(e.getMessage(), Arrays.toString(e.getStackTrace()));
                }
            } else {
                infoDialog("Select the file, please.");
            }
        }
    }

    public void deleteServerDir() {

    }

    public void deleteServerFile() {
        TableList tl = serverTable.getSelectionModel().getSelectedItem();
        if (tl == null) {
            infoDialog("Select file in Server list.");
        } else {
            String type = tl.getType();
            if (type.equals("file")) {
                String q = "Delete file '" + tl.getName() + "' from cloud?";
                if (!showYesNoDialog(q)) {
                    // if press NO
                    return;
                }
                try {
                    network.write(new ServerFileDeleteRequest(userID, tl.getName()));
                } catch (IOException e) {
                    showError(e.getMessage(), Arrays.toString(e.getStackTrace()));
                }
            } else {
                infoDialog("Select the file, please.");
            }
        }
    }

    public void registerUser() {
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialogReg = new Dialog<>();
        dialogReg.setTitle("Connect to Cloud");
        dialogReg.setHeaderText(null);
        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialogReg.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        TextField username = new TextField();
        username.setPromptText("Username");
        username.setText("User1");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setText("pwd");
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialogReg.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> loginButton.setDisable(newValue.trim().isEmpty()));
        dialogReg.getDialogPane().setContent(grid);
        // Request focus on the username field by default.
        Platform.runLater(username::requestFocus);
        // Convert the result to a username-password-pair when the login button is clicked.
        dialogReg.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });
        Optional<Pair<String, String>> resultReg = dialogReg.showAndWait();
        resultReg.ifPresent(usernamePassword -> {
            user = usernamePassword.getKey();
            pwd = usernamePassword.getValue();
            try {
                network.write(new RegisterRequest(user, pwd));
            } catch (IOException e) {
                showException(e.getMessage(), Arrays.toString(e.getStackTrace()));
            }
        });
    }

}
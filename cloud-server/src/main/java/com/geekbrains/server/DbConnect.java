package com.geekbrains.server;

import com.geekbrains.cloud.model.FileMessage;
import com.geekbrains.cloud.model.TableList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DbConnect {

    // временное решение, до реализации регистрации
    //private final String currUser = "User1";
    private final String connectionString = "jdbc:sqlite:cloud-server\\cloud-db.db";
    private Connection connection = null;
    private Statement statement = null;

    public DbConnect() {
        try {
            String driverName = "org.sqlite.JDBC";
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            return;
        }
        try {
            connection = DriverManager.getConnection(connectionString);
            statement = connection.createStatement();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public void reconnectDB() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
            connection = DriverManager.getConnection(connectionString);
            statement = connection.createStatement();
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public List<TableList> getServerList(String userId) throws SQLException {
        List<TableList> files = new ArrayList<>();
        String sql = "select id from directories where user_id = " + userId + " and parent_id is null";
        ResultSet resultSet = statement.executeQuery(sql);
        long rootDir = 0;
        while (resultSet.next()) {
            rootDir = resultSet.getLong("id");
        }
        resultSet.close();
        sql = "select curr_dir from users where id = " + userId;
        resultSet = statement.executeQuery(sql);
        long currDir = 0;
        while (resultSet.next()) {
            currDir = resultSet.getLong("curr_dir");
        }
        resultSet.close();
        if (currDir != rootDir) {
            files.add(new TableList("..", "upDir", 0));
        }
        // load child dirs
        resultSet = statement.executeQuery("select name from directories where parent_id = " + currDir);
        while (resultSet.next()) {
            files.add(new TableList(resultSet.getString(1), "dir", 0));
        }
        // load files on current dir
        resultSet = statement.executeQuery("select name from files where dir_id = " + currDir);
        while (resultSet.next()) {
            files.add(new TableList(resultSet.getString(1), "file", 0));
        }
        return files;
    }

    public String pathIn(String userID, String path) {
        try {
            String sql = "select id from directories where parent_id = " +
                    "(select curr_dir from users where id = " + userID + ") and " +
                    "name = '" + path + "'";
            ResultSet resultSet = statement.executeQuery(sql);
            long currDir = 0;
            while (resultSet.next()) {
                currDir = resultSet.getLong(1);
            }
            sql = "update users set curr_dir = " + currDir + " where id = " + userID;
            statement.executeUpdate(sql);
            return "OK";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public String pathUp(String userID) {
        try {
            String sql = "select parent_id from directories where id = " +
                    "(select curr_dir from users where id = " + userID + ")";
            ResultSet resultSet = statement.executeQuery(sql);
            long currDir = 0;
            while (resultSet.next()) {
                currDir = resultSet.getLong(1);
            }
            sql = "update users set curr_dir = " + currDir + " where id = " + userID;
            statement.executeUpdate(sql);
            return "OK";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public FileMessage getFile(String name, String userID) throws IOException, SQLException {
        ResultSet resultSet = statement.executeQuery("select id from files where dir_id = " +
                "(select curr_dir from users where id = '" + userID + "') and " +
                "name = '" + name + "'");
        FileMessage file = new FileMessage(userID, Path.of("server_files")
                .resolve(resultSet.getLong(1) + ".file"));
        file.setName(name);
        return file;
    }

    public String writeFile(FileMessage fileMessage) {
        try {
            String sql = "select curr_dir from users where id = " + fileMessage.getUserID();
            ResultSet resultSet = statement.executeQuery(sql);
            long currDir = 0;
            while (resultSet.next()) {
                currDir = resultSet.getLong("curr_dir");
            }
            if (currDir > 0) {
                sql = "insert into files(dir_id, name, shared) values (" +
                        currDir + ", '" + fileMessage.getName() + "', 0)";
                statement.executeUpdate(sql);
                sql = "select id from files where dir_id = " + currDir + " and name = '" +
                        fileMessage.getName() + "'";
                resultSet = statement.executeQuery(sql);
                long fileNum = 0;
                while (resultSet.next()) {
                    fileNum = resultSet.getLong(1);
                }
                Files.write(Path.of("server_files").resolve(fileNum + ".file"), fileMessage.getData());
            } else {
                return "NO_CURR_DIR";
            }
            return "OK";
        } catch (SQLException | IOException e) {
            return e.getMessage();
        }
    }

    public String userLogin(String user, String pwd) {
        try {
            String sql = "select * from users where LOGIN = '" + user + "'";
            ResultSet resultSet = statement.executeQuery(sql);
            String id = "";
            String login = "";
            String password = "";
            while (resultSet.next()) {
                id = resultSet.getString("ID");
                login = resultSet.getString("LOGIN");
                password = resultSet.getString("PWD");
            }
            if (login.isEmpty()) {
                return "NO_USER";
            }
            if (!password.equals(pwd)) {
                return "WRONG_PWD";
            }
            return "OK " + id;
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public String userRegister(String user, String pwd) {
        return "OK";
    }

    public void closeDatabase() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }
}

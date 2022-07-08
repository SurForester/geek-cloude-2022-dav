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

    private final String connectionString = "jdbc:sqlite:cloud-server" + System.getProperty("file.separator") + "cloud-db.db";
    private final String filesPath = "cloud-server" + System.getProperty("file.separator") + "cloudFiles";
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

    /*public void reconnectDB() {
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
    }*/

    public List<TableList> getServerList(String userId) throws SQLException {
        List<TableList> files = new ArrayList<>();
        if (userId.equals("0")) {
            return files;
        }
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
        /*String sql = "select id from files where dir_id = " +
                "(select curr_dir from users where id = " + userID + ") and " +
                "name = '" + name + "'";
        ResultSet resultSet = statement.executeQuery(sql);
        long fn = 0;
        while (resultSet.next()) {
            fn = resultSet.getLong("1");
        }*/
        FileMessage file = new FileMessage(userID, Path.of(filesPath).resolve("1.file"));
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
                sql = "insert into files(dir_id, name, is_shared) values (" +
                        currDir + ", '" + fileMessage.getName() + "', 0)";
                statement.executeUpdate(sql);
                sql = "select id from files where dir_id = " + currDir + " and name = '" +
                        fileMessage.getName() + "'";
                resultSet = statement.executeQuery(sql);
                long fileNum = 0;
                while (resultSet.next()) {
                    fileNum = resultSet.getLong(1);
                }
                Files.write(Path.of(filesPath).resolve(fileNum + ".file").toAbsolutePath(), fileMessage.getData());
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
        try {
            long userID = 0;
            // register user
            int res = statement.executeUpdate(
                    "insert into users values(null, '" + user + "', '" + pwd + "', null)");
            if (res > 0) {
                // inserting is OK
                ResultSet rs = statement.executeQuery(
                        "select id from users where login = '" + user + "'");
                while (rs.next()) {
                    userID = rs.getLong("ID");
                }
                if (userID > 0) {
                    // user is registered, register base dir for new user
                    long startDir = 0;
                    res = statement.executeUpdate(
                            "insert into directories values(null, " + userID + ", 'root', null)");
                    if (res > 0) {
                        // inserting base dir is OK
                        String sql = "select id from directories where user_id = " + userID +
                                " and parent_id is null";
                        rs = statement.executeQuery(sql);
                        while (rs.next()) {
                            startDir = rs.getLong(1);
                        }
                        res = statement.executeUpdate(
                                "update users set curr_dir = " + startDir + " where id = " + userID);
                        if (res > 0) {
                            return "OK " + userID;
                        } else {
                            return "Base dir did not setted to new user.";
                        }
                    } else {
                        return "Base dir not created.";
                    }
                } else {
                    return "User ID can't get.";
                }
            } else {
                return "New user not inserted into DB.";
            }
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public String makeDirectory(String userID, String name) {
        try {
            ResultSet rs = statement.executeQuery("Select curr_dir from users where id = " + userID);
            long curr_dir = 0;
            while (rs.next()) {
                curr_dir = rs.getLong(1);
            }
            int ins = statement.executeUpdate(
                    "insert into directories values(null, " + userID + ", '" + name + "', " + curr_dir + ")");
            if (ins == 1) {
                return "OK";
            } else {
                return "not OK";
            }
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public String renameDirectory(String userID, String fromName, String toName) {
        try {
            ResultSet rs = statement.executeQuery("Select curr_dir from users where id = " + userID);
            long curr_dir = 0;
            while (rs.next()) {
                curr_dir = rs.getLong(1);
            }
            String sql = "update directories set name = '" + toName +
                    "' where user_id = " + userID + " and name = '" + fromName +
                    "' and parent_id = " + curr_dir;
            if (statement.executeUpdate(sql) == 1) {
                return "OK";
            } else {
                return "not OK";
            }
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    /*public String deleteFile(String userID, String fileName) {
        try {
            return "OK";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }*/

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

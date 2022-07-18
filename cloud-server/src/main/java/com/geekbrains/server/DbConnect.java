package com.geekbrains.server;

import com.geekbrains.cloud.model.FileMessage;
import com.geekbrains.cloud.model.TableList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class DbConnect {

    private final String connectionString = "jdbc:sqlite:cloud-server" + System.getProperty("file.separator") + "cloud-db.db";
    private Connection connection = null;
    private Statement statement = null;

    public DbConnect() {
        try {
            String driverName = "org.sqlite.JDBC";
            Class.forName(driverName);
            connection = DriverManager.getConnection(connectionString);
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            log.error(e.getMessage() + "/n/r" + Arrays.toString(e.getStackTrace()));
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
            log.error(e.getMessage() + "/n/r" + Arrays.toString(e.getStackTrace()));
        }
    }*/

    public String getServerPath(String userID) {
        String pathString = "";
        try {
            String sql = "select id from directories where user_id = " + userID + " and parent_id is null";
            long base_dir = 0L;
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                base_dir = resultSet.getLong(1);
            }
            resultSet.close();
            sql = "select curr_dir from users where id = " + userID;
            long curr_dir = 0L;
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                curr_dir = resultSet.getLong(1);
            }
            resultSet.close();
            sql = "with recursive dir (id, name, parent_id) as (\n" +
                    "    select id, name, parent_id \n" +
                    "        from directories\n" +
                    "        where user_id = " + userID + " and id = " + base_dir +
                    "    union all\n" +
                    "    select d.ID, d.NAME, d.PARENT_ID\n" +
                    "        from directories d\n" +
                    "        inner join dir on d.parent_id = dir.id\n" +
                    "    )\n" +
                    "select * from dir\n" +
                    "where dir.id <= " + curr_dir + "\n" +
                    "order by dir.id;";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                pathString = pathString + resultSet.getString("name") + "\\";
            }
            resultSet.close();
            return pathString;
        } catch (SQLException sqlException) {
            return "Error: " + sqlException.getMessage();
        }
    }

    public List<TableList> getServerList(String userId) {
        try {
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
            resultSet.close();
            resultSet = statement.executeQuery("select name from files where dir_id = " + currDir);
            while (resultSet.next()) {
                files.add(new TableList(resultSet.getString(1), "file", 0));
            }
            resultSet.close();
            return files;
        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
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
            resultSet.close();
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
            resultSet.close();
            sql = "update users set curr_dir = " + currDir + " where id = " + userID;
            statement.executeUpdate(sql);
            return "OK";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public String getServerFileName(String name, String userID) {
        try {
            String sql = "select id from files where dir_id = " +
                    "(select curr_dir from users where id = " + userID + ") and " +
                    "name = '" + name + "'";
            ResultSet resultSet = statement.executeQuery(sql);
            long fileNum = 0L;
            while (resultSet.next()) {
                fileNum = resultSet.getLong("id");
            }
            resultSet.close();
            return fileNum + ".file";
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return "NO_FILE";
    }

    public String writeFile(FileMessage fileMessage, String filesPath) {
        try {
            String sql = "select curr_dir from users where id = " + fileMessage.getUserID();
            ResultSet resultSet = statement.executeQuery(sql);
            long currDir = 0;
            while (resultSet.next()) {
                currDir = resultSet.getLong("curr_dir");
            }
            resultSet.close();
            if (currDir > 0) {
                connection.setSavepoint();
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
                resultSet.close();
                Files.write(Path.of(filesPath).resolve(fileNum + ".file").toAbsolutePath(), fileMessage.getData());
                connection.commit();
            } else {
                return "NO_CURR_DIR";
            }
            return "OK";
        } catch (SQLException | IOException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                return e.getMessage();
            }
            return e.getMessage();
        }
    }

    public String userLogin(String user, String pwd) {
        try {
            String id = "";
            String login = "";
            String password = "";
            String sql = "select * from users where LOGIN = '" + user + "'";
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                id = resultSet.getString("ID");
                login = resultSet.getString("LOGIN");
                password = resultSet.getString("PWD");
            }
            resultSet.close();
            if (login.isEmpty()) {
                return "NO_USER";
            }
            if (!password.equals(pwd)) {
                return "WRONG_PWD";
            }
            // set current dir to base
            long dir_id = 0L;
            sql = "select id from directories where user_id = " + id + " and parent_id is null";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dir_id = resultSet.getLong("id");
            }
            resultSet.close();
            sql = "update users set curr_dir = " + dir_id + " where id = " + id;
            int res = statement.executeUpdate(sql);
            return "OK " + id;
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public String userRegister(String user, String pwd) {
        try {
            long userID = 0;
            // register user
            connection.setSavepoint();
            int res = statement.executeUpdate(
                    "insert into users values(null, '" + user + "', '" + pwd + "', null)");
            if (res > 0) {
                // inserting is OK
                ResultSet rs = statement.executeQuery(
                        "select id from users where login = '" + user + "'");
                while (rs.next()) {
                    userID = rs.getLong("ID");
                }
                rs.close();
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
                            connection.commit();
                            return "OK " + userID;
                        } else {
                            connection.rollback();
                            return "Base dir did not setted to new user.";
                        }
                    } else {
                        connection.rollback();
                        return "Base dir not created.";
                    }
                } else {
                    connection.rollback();
                    return "User ID can't get.";
                }
            } else {
                connection.rollback();
                return "New user not inserted into DB.";
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                return e.getMessage();
            }
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
            rs.close();
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
            rs.close();
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

    public String renameFile(String userID, String fromName, String toName) {
        try {
            ResultSet rs = statement.executeQuery("Select curr_dir from users where id = " + userID);
            long curr_dir = 0;
            while (rs.next()) {
                curr_dir = rs.getLong(1);
            }
            rs.close();
            String sql = "update files set name = '" + toName +
                    "' where name = '" + fromName + "' and dir_id = " + curr_dir;
            if (statement.executeUpdate(sql) == 1) {
                return "OK";
            } else {
                return "not OK";
            }
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    public String deleteFile(String userID, String fileName, String filesPath) {
        try {
            connection.setSavepoint();
            ResultSet rs = statement.executeQuery("Select curr_dir from users where id = " + userID);
            long curr_dir = 0;
            while (rs.next()) {
                curr_dir = rs.getLong(1);
            }
            rs.close();
            long file_num = 0L;
            String sql = "select id from files  where name = '" + fileName +
                    "' and dir_id = " + curr_dir;
            rs = statement.executeQuery(sql);
            while (rs.next()) {
                file_num = rs.getLong(1);
            }
            if (curr_dir > 0 && file_num > 0) {
                sql = "delete from files where id = " + file_num;
                int res = statement.executeUpdate(sql);
                String file = file_num + ".file";
                Path filePath = Path.of(filesPath).resolve(file);
                Files.delete(filePath);
                connection.commit();
                return "OK";
            } else {
                connection.rollback();
                return "No file ID in DB.";
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                return e.getMessage();
            }
            return e.getMessage();
        } catch (IOException io) {
            return io.getMessage();
        }
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

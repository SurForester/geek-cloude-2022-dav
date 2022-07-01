package com.geekbrains.server.archive;

import com.geekbrains.server.Authorization.AuthService;
import com.geekbrains.server.Authorization.AuthServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class CloudServer {
    private final AuthService authService;
    private static final Logger logger = LogManager.getLogger(CloudServer.class.getName());
    private List<ClientHandler> connectedUsers;

    public CloudServer() {
        logger.trace("Server start.");
        authService = new AuthServiceImpl(logger);
        try (ServerSocket server = new ServerSocket(CommonConstants.SERVER_PORT)) {
            authService.start();
            connectedUsers = new ArrayList<>();
            while (true) {
                Socket socket = server.accept();
                connectedUsers.add(new ClientHandler(this, socket, logger));
            }
        } catch (IOException e) {
            logger.error("Server error: " + e.getMessage() + "\n" + e.getStackTrace());
        } finally {
            authService.end();
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean loginExists(String login) {
        for (ClientHandler handler : connectedUsers) {
            if (handler.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void disconnectUser(ClientHandler handler) {
        connectedUsers.remove(handler);
    }
}

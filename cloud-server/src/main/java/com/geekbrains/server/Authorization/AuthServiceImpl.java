package com.geekbrains.server.Authorization;

import com.geekbrains.server.DbConnect;
import org.apache.logging.log4j.Logger;

public class AuthServiceImpl implements AuthService {

    private final Logger logger;
    private DbConnect dbConnect;

    public AuthServiceImpl(Logger logger) {
        this.logger = logger;
        //users = new HashMap<>();
        logger.trace("Auth service initiated");
    }

    @Override
    public void start() {
        logger.trace("Db connect.");
        // db connect
        dbConnect = new DbConnect();
        logger.trace("Auths started");
    }

    @Override
    public boolean loginExists(String login) {
        return false;
    }

    @Override
    public String userRegister(String login, String pwd, String email) {
        return null;
    }

    @Override
    public String userLogin(String login, String pwd) {
        return "email:";
    }

    @Override
    public void end() {
        // db disconnect
        logger.trace("DB closed.");
        dbConnect.closeDatabase();
        logger.trace("Auths stopped");
    }
}

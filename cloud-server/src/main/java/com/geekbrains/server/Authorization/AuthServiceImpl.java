package com.geekbrains.server.Authorization;

import org.apache.logging.log4j.Logger;

public class AuthServiceImpl implements AuthService {

    private Logger logger;

    public AuthServiceImpl(Logger logger) {
        this.logger = logger;
        //users = new HashMap<>();
        logger.trace("Auth service initiated");
    }

    @Override
    public void start() {
        // db connect
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
        logger.trace("Auths stopped");
    }
}
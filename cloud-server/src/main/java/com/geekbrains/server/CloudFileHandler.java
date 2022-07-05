package com.geekbrains.server;

import com.geekbrains.cloud.model.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CloudFileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private DbConnect dbConnect;

    public CloudFileHandler() {
        try {
            // base connect for User1
            dbConnect = new DbConnect();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

     @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        if (cloudMessage instanceof FileRequest fileRequest) {
            ctx.writeAndFlush(dbConnect.getFile(fileRequest.getName(), fileRequest.getUserID()));
        } else if (cloudMessage instanceof FileMessage fileMessage) {
            String res = dbConnect.writeFile(fileMessage);
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(fileMessage.getUserID())));
            } else {
                // writing error message
                ctx.writeAndFlush(new ErrorMessage("Ошибка записи файла на сервере", res));
            }
        } else if (cloudMessage instanceof PathInRequest pathInRequest) {
            // path in
            String res =  dbConnect.pathIn(pathInRequest.getUserID(), pathInRequest.getPath());
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(pathInRequest.getUserID())));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Ошибка команды PathIn", res));
            }
        } else if (cloudMessage instanceof PathUpRequest pathUpRequest) {
            // path Up
            String res = dbConnect.pathUp(pathUpRequest.getUserID());
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(pathUpRequest.getUserID())));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Ошибка команды pathUp", res));
            }
        } else if (cloudMessage instanceof AuthRequest authRequest) {
            String userid = dbConnect.userLogin(authRequest.getLogin(), authRequest.getPwd());
            if (userid.startsWith("OK ")) {
                String[] arr = userid.split(" ");
                ctx.writeAndFlush(new AuthResponse(arr[1], dbConnect.getServerList(arr[1])));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Error login.", userid));
            }
        }
    }

}

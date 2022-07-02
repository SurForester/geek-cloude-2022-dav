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
    public void channelActive(ChannelHandlerContext ctx) {
        try {
            ctx.writeAndFlush(dbConnect.getServerList());
        } catch (Exception e) {
            ctx.writeAndFlush(new ErrorMessage(e.getMessage()));
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        if (cloudMessage instanceof FileRequest fileRequest) {
            ctx.writeAndFlush(dbConnect.getFile(fileRequest.getName()));
        } else if (cloudMessage instanceof FileMessage fileMessage) {
            String res = dbConnect.writeFile(fileMessage);
            if (res.equals("OK")) {
                ctx.writeAndFlush(dbConnect.getServerList());
            } else {
                // writing error message
                ctx.writeAndFlush(new ErrorMessage(res));
            }
        } else if (cloudMessage instanceof PathInRequest pathInRequest) {
            // path in
            String res =  dbConnect.pathIn(pathInRequest.toString());
            if (res.equals("OK")) {
                ctx.writeAndFlush(dbConnect.getServerList());
            } else {
                ctx.writeAndFlush(new ErrorMessage(res));
            }
        } else if (cloudMessage instanceof PathUpRequest pathUpRequest) {
            // path Up
            String res = dbConnect.pathUp();
            if (res.equals("OK")) {
                ctx.writeAndFlush(dbConnect.getServerList());
            } else {
                ctx.writeAndFlush(new ErrorMessage(res));
            }
        }
    }

}

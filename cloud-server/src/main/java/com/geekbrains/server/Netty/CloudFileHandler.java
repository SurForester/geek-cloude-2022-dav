package com.geekbrains.server.Netty;

import com.geekbrains.cloud.model.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.file.Files;
import java.nio.file.Path;

public class CloudFileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private final Path rootDir;
    private Path currentDir;

    public CloudFileHandler() {
        rootDir = Path.of("server_files");
        currentDir = Path.of(rootDir.toString());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        currentDir = Path.of(rootDir.toString());
        ctx.writeAndFlush(new ListFiles(currentDir, true));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        if (cloudMessage instanceof FileRequest fileRequest) {
            ctx.writeAndFlush(new FileMessage(currentDir.resolve(fileRequest.getName())));
        } else if (cloudMessage instanceof FileMessage fileMessage) {
            Files.write(currentDir.resolve(fileMessage.getName()), fileMessage.getData());
            if (rootDir.toString().equals(currentDir.toString())) {
                ctx.writeAndFlush(new ListFiles(currentDir, true));
            } else {
                ctx.writeAndFlush(new ListFiles(currentDir, false));
            }
        } else if (cloudMessage instanceof PathInRequest pathInRequest) {
            // path in
            currentDir = Path.of(currentDir.toString()).resolve(pathInRequest.toString());
            ctx.writeAndFlush(new ListFiles(currentDir, false));
        } else if (cloudMessage instanceof PathUpRequest pathUpRequest) {
            // path Up
            Path path = Path.of(pathUpRequest.toString()).getParent();
            if (rootDir.toString().equals(path.toString())) {
                ctx.writeAndFlush(new ListFiles(path, true));
            } else {
                ctx.writeAndFlush(new ListFiles(path, false));
                currentDir = Path.of(path.toString());
            }
        }
    }
}

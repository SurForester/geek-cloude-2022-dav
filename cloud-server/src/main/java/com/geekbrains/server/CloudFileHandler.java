package com.geekbrains.server;

import com.geekbrains.cloud.model.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class CloudFileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private DbConnect dbConnect;
    private final String filesPath = "cloud-server" + System.getProperty("file.separator") + "cloudFiles";

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
            String serverFileName = dbConnect.getServerFileName(fileRequest.getName(), fileRequest.getUserID());
            if (!serverFileName.equals("NO_FILE")) {
                Path file = Path.of(filesPath).resolve(serverFileName);
                FileMessage fileMessage = new FileMessage(fileRequest.getUserID(),
                        fileRequest.getName(), file.toFile().length(), Files.readAllBytes(file));
                ctx.writeAndFlush(fileMessage);
            } else {
                ctx.writeAndFlush(new ErrorMessage("No file exists: " + serverFileName,
                        "Get server file."));
            }
        } else if (cloudMessage instanceof FileMessage fileMessage) {
            String res = dbConnect.writeFile(fileMessage, filesPath);
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(fileMessage.getUserID())));
            } else {
                // writing error message
                ctx.writeAndFlush(new ErrorMessage("Ошибка записи файла на сервере", res));
            }
        } else if (cloudMessage instanceof PathInRequest pathInRequest) {
            // path in
            String res = dbConnect.pathIn(pathInRequest.getUserID(), pathInRequest.getPath());
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(pathInRequest.getUserID())));
                ctx.writeAndFlush(new ServerPathResponse(dbConnect.getServerPath(pathInRequest.getUserID())));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Ошибка команды PathIn", res));
            }
        } else if (cloudMessage instanceof PathUpRequest pathUpRequest) {
            // path Up
            String res = dbConnect.pathUp(pathUpRequest.getUserID());
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(pathUpRequest.getUserID())));
                ctx.writeAndFlush(new ServerPathResponse(dbConnect.getServerPath(pathUpRequest.getUserID())));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Ошибка команды pathUp", res));
            }
        } else if (cloudMessage instanceof AuthRequest authRequest) {
            String userid = dbConnect.userLogin(authRequest.getLogin(), authRequest.getPwd());
            if (userid.startsWith("OK ")) {
                String[] arr = userid.split(" ");
                ctx.writeAndFlush(new AuthResponse(userid, dbConnect.getServerList(arr[1])));
                ctx.writeAndFlush(new ServerPathResponse(dbConnect.getServerPath(arr[1])));
            } else if (userid.equals("NO_USER") || userid.equals("WRONG_PWD")) {
                ctx.writeAndFlush(new AuthResponse(userid, dbConnect.getServerList("0")));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Error login.", userid));
            }
        } else if (cloudMessage instanceof RegisterRequest registerRequest) {
            String userid = dbConnect.userRegister(registerRequest.getLogin(), registerRequest.getPwd());
            if (userid.startsWith("OK ")) {
                String[] arr = userid.split(" ");
                ctx.writeAndFlush(new RegisterResponse(userid, dbConnect.getServerList(arr[1])));
                ctx.writeAndFlush(new ServerPathResponse(dbConnect.getServerPath(arr[1])));
            }
        } else if (cloudMessage instanceof ServerDirMake serverDirMake) {
            String res = dbConnect.makeDirectory(serverDirMake.getUserID(), serverDirMake.getNameDir());
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(serverDirMake.getUserID())));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Error make dir - " + serverDirMake.getNameDir(), res));
            }
        } else if (cloudMessage instanceof ServerDirRename serverDirRename) {
            String res = dbConnect.renameDirectory(serverDirRename.getUserID(),
                    serverDirRename.getFromName(), serverDirRename.getToName());
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(serverDirRename.getUserID())));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Error of dirRename", res));
            }
        } else if (cloudMessage instanceof ServerFileRenameRequest serverFileRenameRequest) {
            String res = dbConnect.renameFile(serverFileRenameRequest.getUserID(),
                    serverFileRenameRequest.getOldName(), serverFileRenameRequest.getNewName());
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(serverFileRenameRequest.getUserID())));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Error of fileRename", res));
            }
        } else if (cloudMessage instanceof ServerFileDeleteRequest sfdr) {
            String res = dbConnect.deleteFile(sfdr.getUserID(), sfdr.getFileName(), filesPath);
            if (res.equals("OK")) {
                ctx.writeAndFlush(new ServerFileDeleteResponse(res));
                ctx.writeAndFlush(new ServerListFiles(dbConnect.getServerList(sfdr.getUserID())));
            } else {
                ctx.writeAndFlush(new ErrorMessage("Error of fileRename", res));
            }
        }
    }

}

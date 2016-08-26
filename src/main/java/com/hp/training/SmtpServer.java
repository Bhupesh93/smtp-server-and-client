package com.hp.training;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmtpServer {
    int port;
    int clientId = 0;
    String emailStoragePath;
    private final static Logger logger = LoggerFactory.getLogger(SmtpServer.class);

    public SmtpServer(int port, String emailStoragePath) {
        this.port = port;
        this.emailStoragePath = emailStoragePath;
    }

    protected void run() throws IOException {
          @SuppressWarnings("resource")
		ServerSocket server = new ServerSocket(port);

        while (true) {
            SmtpRequestHandler smtpRequestHandler = null;
            try {
                Socket dataSocket = server.accept();
                smtpRequestHandler = new SmtpRequestHandler(dataSocket, clientId++, emailStoragePath);
                logger.debug("clientId = {}", clientId);
            } catch (IOException e) {
                logger.error("There is some problem in connection setup", e);
            }

            smtpRequestHandler.start();
        }
    }

    public static void main(String[] args) throws IOException {
        Configuration config = new Configuration("configServer.properties");
        String port = config.getValue("port", "9100");
        String emailStoragePath = config.getValue("emailStoragePath");
        if (emailStoragePath == null) {
            logger.error("Can not proceed, please specify value for emailStoreDir");
            System.exit(1);
        }
        
        SmtpServer smtpserver = new SmtpServer(Integer.parseInt(port), emailStoragePath);
        smtpserver.run();
    }
}

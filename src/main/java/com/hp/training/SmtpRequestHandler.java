package com.hp.training;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmtpRequestHandler extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(SmtpRequestHandler.class);

    private static final String[] COMMANDS = { "HELO", "MAIL FROM", "RCPT TO", "DATA", "QUIT" };

    private final int clientId;
    private Socket dataSocket;
    private String emailStoragePath;

    public SmtpRequestHandler(Socket dataSocket, int clientId, String emailStoragePath) {
        this.dataSocket = dataSocket;
        this.clientId = clientId;
        this.emailStoragePath = emailStoragePath;
    }

    public void run() {
        logger.trace("{begin} SmtpRequestHandler::run() is called.");
        BufferedReader request = null;
        PrintWriter response = null;
        try {
            request = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
            response = new PrintWriter(dataSocket.getOutputStream(), true);
            processRequests(request, response);
            logger.trace("{end} SmtpRequestHandler::run() completed - success.");
        } catch (IOException e) {
            logger.trace("{end} SmtpRequestHandler::run() completed - failure.");
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            close(request, response);
        }
    }

    private void processRequests(BufferedReader request, PrintWriter response) throws IOException {
        logger.trace("{begin} SmtpRequestHandler::processRequests() is called");
        int step = 0;
        boolean atLestOneRecipient = false;
        while (true) {
            String line = request.readLine();
            logger.debug("line = " + line );
            if (line == null || (line = line.trim()).isEmpty()) {
                continue;
            }
            int cmdLength = line.length();
            if (cmdLength > COMMANDS[step].length()) {
                cmdLength = COMMANDS[step].length();
            }
            String command = line.substring(0, cmdLength).toUpperCase();
            logger.debug("command = " + command);
            if (command.equalsIgnoreCase("QUIT")) {
                response.println("250 Bye");
                break;
            }
            if (step == 2) {
                if (command.equalsIgnoreCase(COMMANDS[step])) {
                    atLestOneRecipient = true;
                    response.println("250 Ok");
                    continue;
                } else if (atLestOneRecipient && command.equalsIgnoreCase(COMMANDS[step + 1])) {
                    response.println("354 continue");
                    try {
                        getData(request);
                        response.println("250 Ok");
                        step=1;
                        continue;
                    } catch (IOException e) {
                        logger.trace("{end} SmtpRequestHandler::processRequests() completed - failure.");
                        response.println("500 Internal error");
                        throw e;
                    }

                    
                }
            }
            if (!command.equalsIgnoreCase(COMMANDS[step])) {
                response.println("500 Invalid command or command is not as per the protocol");
                continue;
            }
            response.println("250 Ok");
            step++;
        }

        logger.trace("{end} SmtpRequestHandler::processRequests() completed - success.");
    }

    private void close(BufferedReader request, PrintWriter response) {
        if (request != null)
            try {
                request.close();
            } catch (IOException e) {
                logger.warn("Error in closing the Buffrred Reader.");
            }
        if (response != null)
            response.close();
        if (dataSocket != null)
            try {
                dataSocket.close();
            } catch (IOException e) {
                logger.warn("Error in closing data socket.");
            }
    }

    private void getData(BufferedReader br) throws IOException {
        logger.trace("{begin} SmtpRequestHandler::getData() is called");
        String emlPath = emailStoragePath + clientId + ".eml";

        FileWriter fw = null;
        String line = null;
        try {
            fw = new FileWriter(emlPath);
            while (true) {
                line = br.readLine();
                if (line == null) {
                    throw new IOException("Client closed the connection unexpctedly.");
                }
                if (line.equals(".")) {
                    break;
                }
                fw.write(line + "\n");
            }
        } catch (IOException e) {
            logger.error("I/O error in reading or storing the data.", e);
            logger.trace("{end} SmtpRequestHandler::getData() completed-failure.");
            throw e;
        } finally {
            if (fw != null) {
                fw.flush();
                fw.close();
            }
        }
    }
}

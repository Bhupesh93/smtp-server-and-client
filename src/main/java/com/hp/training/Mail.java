package com.hp.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mail {

	private final static Logger logger = LoggerFactory.getLogger(Mail.class);
	private static final int SOCKET_READ_TIMEOUT = 100 * 1000;
	private static final String[] COMMANDS = { "HELO", "MAIL FROM", "RCPT TO", "DATA", "QUIT" };

	private String host;
	private int port;
	private List<String> mailRecipients;
	private String sender;
	private String subject;
	private String message;
	private List<String> filesToBeAttached;
	private BufferedReader response = null;
	private PrintWriter request = null;
	private Socket clientSocket;
	private String temporaryEmailStoragePath;

	public Mail(String host, int port, List<String> mailRecipients, String sender, String subject,
			String message, List<String> filesToBeAttached, String temporaryEmailStoragePath) {
		this.host = host;
		this.port = port;
		this.mailRecipients = mailRecipients;
		this.message = message;
		this.sender = sender;
		this.subject = subject;
		this.filesToBeAttached = filesToBeAttached;
		this.temporaryEmailStoragePath = temporaryEmailStoragePath;
	}

	private String createMimeMessage(String host, int port, List<String> mailRecipients, String sender, String subject,
			String message, List<String> filesToBeAttached) throws IOException, MessagingException {
		logger.trace("{begin} SmtpClientProcess::createMIMEMessage() is called;");
		Session session = null;
		Message msg = new MimeMessage(session);
		try {
			msg.setFrom(new InternetAddress("localhost"));
			InternetAddress[] toAddresses = new InternetAddress[mailRecipients.size()];
			int i = 0;
			for (String mailRecipient : mailRecipients) {
				toAddresses[i] = new InternetAddress(mailRecipient);
				i++;
			}
			msg.setRecipients(Message.RecipientType.TO, toAddresses);
			msg.setSubject(subject);
			msg.setSentDate(new Date());
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(message, "text/html");
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			if (filesToBeAttached != null && filesToBeAttached.size() > 0) {
				for (String file : filesToBeAttached) {
					MimeBodyPart attachPart = new MimeBodyPart();
					try {
						attachPart.attachFile(file);
					} catch (IOException ex) {
						logger.warn("problem in attaching file");
						throw ex;
					}
					multipart.addBodyPart(attachPart);
				}
			}
			msg.setContent(multipart);
		} catch (MessagingException e) {
			logger.trace("{end} SmtpClientProcess::createMIMEMessage()  completed - failure. ");
			logger.warn("problem in creating the mime message");
			throw new MessagingException("there is some problem in creating mime message" + e);
		}
		String fileName = temporaryEmailStoragePath + Long.toString(System.currentTimeMillis());
		logger.debug("temporary email file name " + fileName);
		File eml = new File(fileName);
		OutputStream out = null;
		try {
			out = new FileOutputStream(eml);
		} catch (FileNotFoundException e1) {
			logger.trace("{end} SmtpClientProcess::createMIMEMessage()  completed - failure ");
			logger.warn("path mentioned to create temporary eml file from mime message is wrong ");
			throw e1;
		}
		try {
			msg.writeTo(out);
			logger.trace("{end} SmtpClientProcess::createMIMEMessage()  completed - success. ");
		} catch (IOException | MessagingException e) {
			logger.trace("{end} SmtpClientProcess::createMIMEMessage()  completed - failure. ");
			logger.warn("there is some problem in writing message to eml file");
			throw e;
		}
		return fileName;
	}

	private void close() throws IOException {
		logger.trace("{end}  SmtpClientProcess::close() is called. ");
		if (clientSocket != null)
			try {
				clientSocket.close();
			} catch (IOException e) {
				logger.warn("error in closing the client socket");
				logger.trace("{end}  SmtpClientProcess::connect()  completed - failure. ");
				throw e;
			}
		if (request != null)
			request.close();
		if (response != null)
			response.close();
		logger.trace("{end}  SmtpClientProcess::close()  completed - success. ");
	}

	private void connect() throws IOException {
		logger.trace("{begin} SmtpClientProcess::connect() is called;");
		try {
			clientSocket = new Socket(host, port);
		} catch (IOException e) {
			logger.error("error in creating socket or connecting to host and port");
			logger.trace("{end}  SmtpClientProcess::connect()  completed - failure. ");
			throw e;
		}
		clientSocket.setSoTimeout(SOCKET_READ_TIMEOUT);
		response = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		request = new PrintWriter(clientSocket.getOutputStream(), true);
		logger.trace("{end}  SmtpClientProcess::connect()  completed - success. ");
	}

	private void sendData(String fileName) throws IOException {
		logger.trace("{{begin} SmtpClientProcess::sendData({}) is called; ", fileName);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			logger.warn("temporay email file  file is missing");
			logger.trace("{{end} SmtpClientProcess::sendData()  completed - failure ");
		}
		String str = null;
		try {
			while ((str = br.readLine()) != null) {

				request.println(str);
			}
			request.println(".");
			logger.trace("{{end} SmtpClientProcess::sendData()  completed - success ");
		} catch (IOException e) {
			logger.warn("problem in writing  the file to server");
			logger.trace("{{end} SmtpClientProcess::sendData()  completed - failure ");
		} finally {
			br.close();
		}

	}

	public void send() throws IOException, MessagingException {
		logger.trace("{begin} SmtpClientProcess::sendMessage() is called.");
		String fileName = null;
		try {
			fileName = createMimeMessage(host, port, mailRecipients, sender, subject, message, filesToBeAttached);
			connect();
			String line = null;
			for (int step = 0; step < COMMANDS.length; step++) {

				logger.debug("Current Request  " + COMMANDS[step]);
				if (step == 0)
					request.println(COMMANDS[step] + "  " + clientSocket.getLocalAddress().toString());
				else if (step == 1)
					request.println(COMMANDS[step] + "  " + sender);
				else if (step == 2)
					request.println(COMMANDS[step] + "  " + mailRecipients.toString());
				else if (step == 3) {
					request.println(COMMANDS[step]);
					line = response.readLine();
					logger.debug("response got " + line);
					if (line.startsWith("3")) {
						sendData(fileName);
					} else {
						logger.error("bad response from the server");
						throw new IOException("bad response from the server");
					}
				} else if (step == 4)
					request.println(COMMANDS[step]);

				line = response.readLine();
				logger.debug("response got " + line);
				if (!line.startsWith("2")) {
					throw new IOException("bad response from the server");
				}
			}
			logger.trace("{end} SmtpClientProcess::sendMessage() is completed- success");
		} catch (IOException | MessagingException e) {
			logger.trace("{end} SmtpClientProcess::sendMessage() is completed- failure");
			throw e;
		} finally {
			close();
		}

	}
}
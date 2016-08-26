package com.hp.training;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmtpClient {

	private static Random randomGenerator = new Random();
	private final static Logger logger = LoggerFactory.getLogger(SmtpClient.class);

	public static void main(String[] args) throws IOException {

		int noOfFilesToBeAttached = 3, noOfMailRecipients = 5, port;
		List<String> filesToBeAttached = null;
		List<String> mailRecipients = new ArrayList<>();
		String attachmentStoragePath = null;
		String temporaryEmailStoragePath = null;

		Configuration config = new Configuration("configClient.properties");
		port = Integer.parseInt(config.getValue("port", "9100"));
		attachmentStoragePath = config.getValue("attachmentStoragePath");
		temporaryEmailStoragePath = config.getValue("temporaryEmailStoragePath");
		if (temporaryEmailStoragePath == null) {
			logger.error("Can not proceed, please specify value for temporaryEmailStoreDir");
			System.exit(1);
		}
		AddAttachments addAttachments=new AddAttachments();
		filesToBeAttached = addAttachments.getAttachments(attachmentStoragePath, noOfFilesToBeAttached);
		if (filesToBeAttached == null) {
			logger.warn("files to be attached is null");
		}
		for (int i = 0; i < noOfMailRecipients; i++) {
			mailRecipients.add("vinod" + randomGenerator.nextInt() + "@hpe.com");
		}

		Mail mail = new Mail("localhost", port, mailRecipients, "boobesh@hpe.com",
				"This is  subject  number :" + randomGenerator.nextInt(),
				"this is a  Message number " + randomGenerator.nextInt() + "\n sent from client  ", filesToBeAttached,
				temporaryEmailStoragePath);

		try {
			logger.info("Sending Email .....");
			mail.send();
			logger.info("Email sent successfully");
		} catch (IOException | MessagingException e) {
			logger.error("there is problem in sending e-mail ", e);
		}

	}

}
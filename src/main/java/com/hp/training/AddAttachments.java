package com.hp.training;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddAttachments {

	private final static Logger logger = LoggerFactory.getLogger(AddAttachments.class);

	public List<String> getAttachments(String directory, int noOfAttachments) {
		logger.trace("{begin} AddAttahments::getAttachments({},{}) is called", directory, noOfAttachments);
		List<String> filesToBeAttached = new ArrayList<>();
		List<String> attachmentFiles = listFiles(directory);
		if (attachmentFiles == null) {
			logger.trace("{end} AddAttahments::getAttachments() is completed -failure");
			return null;
		}
		for (int i = 0; i < noOfAttachments; i++) {
			filesToBeAttached.add(pickRandomFile(attachmentFiles));
		}
		logger.trace("{end} AddAttahments::getAttachments() is completed -success");
		return filesToBeAttached;
	}

	private  List<String> listFiles(String directory) {
		logger.trace("{begin} AddAttahments::listFiles({},{}) is called", directory);
		List<String> files = new ArrayList<String>();
		File dir = new File(directory);
		if (dir.exists()) {
			File elements[] = dir.listFiles();
			if (elements == null) {
				logger.error("the path name does not denote directory");
				logger.trace("{end} AddAttahments::listFiles() is completed -failure");
				return null;
			}
			for (File file : elements) {
				if (file.isFile()) {
					files.add(file.getAbsolutePath());
				}
			}
			logger.trace("{end} AddAttahments::listFiles() is completed -success");
			return files;
		} else {
			logger.error("directory doesnt exists");
			logger.trace("{end} AddAttahments::listFiles() is completed -failure");
			return null;
		}
	}

	private  String pickRandomFile(List<String> fileList) {
		logger.trace("{begin} AddAttahments::pickRandomFile() is called");
		if (fileList.isEmpty()) {
			logger.trace("{end} AddAttahments::pickRandomFiles() is completed -failure");
			return null;
		} else {
			Random random = new Random();
			logger.trace("{end} AddAttahments::pickRandomFiles() is completed -success");
			return fileList.get(random.nextInt(fileList.size()));
		}

	}
}

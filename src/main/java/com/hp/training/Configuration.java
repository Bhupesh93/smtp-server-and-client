package com.hp.training;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

	private final static Logger logger = LoggerFactory.getLogger(Configuration.class.getName());

	private Properties properties = new Properties();

	public Configuration(String fileName) throws IOException {
		loadProperties(fileName);
	}

	private void loadProperties(String fileName) throws IOException {
		logger.trace("{begin}Configuration::loadProperties() is called");
		InputStream is = null;
		try {
			is = (InputStream) new FileInputStream(fileName);
			properties.load(is);
			logger.trace("{end}Configuration::loadProperties() completed – success.");
		} catch (IOException e) {
			logger.trace("{end}Configuration::loadProperties() completed – failure.");
			logger.error("Configuration::loadProperties() could not load properties file");
			throw e;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.warn("{end}Configuration::loadProperties(...) error in closing");

				}
			}
		}
	}

	public String getValue(String key, String defaultValue) {
		String value = properties.getProperty(key);
		if (value == null || (value = value.trim()).length() == 0) {
			logger.info("No value specified for port in configuration file so default value is used");
			return defaultValue.trim();
		}
		return value;
	}

	protected String getValue(String Key) {
		String value = null;
		value = properties.getProperty(Key);
		return value;

	}

}

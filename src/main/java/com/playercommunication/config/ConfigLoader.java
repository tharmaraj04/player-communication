package com.playercommunication.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author Tharmaraj Marimuthu
 * Configuration loader for player communication application.
 * Loads settings from application.properties file in classpath.
 * Provides methods to access configuration values like queue capacity, network port, host, and thread priority.
 */
public class ConfigLoader {

	private static final Properties properties = new Properties();
	private static final String CONFIG_FILE = "application.properties";

	 static {
		try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
			if (inputStream != null) {
				properties.load(inputStream);
			} else {
				System.err.println("Configuration file '" + CONFIG_FILE + "' not found in classpath.");
			}
		} catch (Exception e) {
			System.err.println("Error loading configuration: " + e.getMessage());
		}
	}

	public static int getQueueCapacity() {
		return Integer.parseInt(properties.getProperty("queue.capacity"));
	}

    public static int getNetworkPort() {
        return Integer.parseInt(properties.getProperty("network.port"));
    }

    public static String getNetworkHost() {
        return properties.getProperty("network.host");
    }

	public static int getMaxMessageCount() {
		return Integer.parseInt(properties.getProperty("message.count.max"));
	}

}

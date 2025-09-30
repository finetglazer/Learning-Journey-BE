// src/main/java/utils/ConfigManager.java
package com.graduation.integrationtests.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static Properties properties = new Properties();
    private static final String CONFIG_FILE = "application-test.properties";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    public static String getOrderCreateUrl() {
        return properties.getProperty("orders.create.url");
    }

    public static String getOrderUpdateUrl() {
        return properties.getProperty("orders.update.url");
    }

    public static String getOrderDetailUrl(String orderId) {
        return properties.getProperty("orders.detail.url").replace("{id}", orderId);
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {

    private static final Properties props = new Properties();

    static {
        try (InputStream input = ConfigManager.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) throw new RuntimeException("config.properties not found");
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String get(String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException(
                    "Missing config value for key: '" + key + "'. " +
                            "Please update src/test/resources/config.properties before running."
            );
        }
        if (value.startsWith("YOUR_")) {
            throw new RuntimeException(
                    "Placeholder value detected for key: '" + key + "'. " +
                            "Please replace '" + value + "' with the actual value " +
                            "in src/test/resources/config.properties before running."
            );
        }
        return value;
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}

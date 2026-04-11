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
            if (input == null)
                throw new RuntimeException(
                        "config.properties not found in src/test/resources/");
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load config.properties: " + e.getMessage(), e);
        }
    }

    public static String get(String key) {
        // Check system property first — allows CI/CD to override config.properties
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()
                && !systemValue.startsWith("YOUR_")) {
            return systemValue;
        }

        String value = props.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new RuntimeException(
                    "Missing config value for key: '" + key + "'. " +
                            "Please update src/test/resources/config.properties " +
                            "before running the tests.");
        }

        // Only validate placeholder for credential keys — not test data keys
        if (value.startsWith("YOUR_") && !key.startsWith("invalid.")) {
            throw new RuntimeException(
                    "Placeholder value detected for key: '" + key + "'. " +
                            "Please replace '" + value + "' with the actual value " +
                            "provided in the submission email, in " +
                            "src/test/resources/config.properties before running.");
        }

        return value;
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}

package tn.hollywood.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Lecture de la configuration du projet.
 * Ordre de priorité : propriété système (-Dcle=valeur) > config.properties.
 * Exemple : mvn test -Dbrowser=chrome -Dheadless=true -Dbase.url=https://hollywood.tn
 */
public final class ConfigReader {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPS.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de lire config.properties", e);
        }
    }

    private ConfigReader() { }

    public static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }
        String val = PROPS.getProperty(key);
        return val == null ? null : val.trim();
    }

    public static String get(String key, String defaultValue) {
        String v = get(key);
        return (v == null || v.isEmpty()) ? defaultValue : v;
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    /** URL de base sans slash final (ex. https://hollywood.tn). */
    public static String baseUrl() {
        String url = get("base.url", "https://hollywood.tn");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

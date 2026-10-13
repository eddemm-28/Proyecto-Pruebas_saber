package co.edu.unicauca.infra;

import co.edu.unicauca.domain.INotificador;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Decide qué notificador usar: correo real si existe mail.properties, si no, consola.
 */
public class NotificadorFactory {

    public static final String CONFIG_FILE = "mail.properties";

    private NotificadorFactory() {
    }

    public static INotificador crear() {
        return crear(Paths.get(CONFIG_FILE));
    }

    public static INotificador crear(Path configFile) {
        if (!Files.exists(configFile)) {
            System.out.println("No se encontró " + configFile + ": los correos se simularán por consola.");
            return new ConsoleNotificador();
        }
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(configFile.toFile())) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("No se pudo leer " + configFile + ": " + e.getMessage());
            return new ConsoleNotificador();
        }
        if (isBlank(props.getProperty("mail.username"))
                || isBlank(props.getProperty("mail.password"))
                || isBlank(props.getProperty("mail.from"))) {
            System.out.println(configFile + " está incompleto: los correos se simularán por consola.");
            return new ConsoleNotificador();
        }
        return new EmailNotificador(props);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

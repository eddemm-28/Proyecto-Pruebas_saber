package co.edu.unicauca.infra;

import co.edu.unicauca.domain.INotificador;
import co.edu.unicauca.domain.NotificacionException;
import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.User;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Notificador que envía un correo real por SMTP.
 * La configuración viene de mail.properties (ver mail.properties.example).
 */
public class EmailNotificador implements INotificador {

    private final Session session;
    private final String from;

    public EmailNotificador(Properties config) {
        this.from = config.getProperty("mail.from");
        final String user = config.getProperty("mail.username");
        final String password = config.getProperty("mail.password");

        Properties smtp = new Properties();
        smtp.put("mail.smtp.host", config.getProperty("mail.smtp.host", "smtp.gmail.com"));
        smtp.put("mail.smtp.port", config.getProperty("mail.smtp.port", "587"));
        smtp.put("mail.smtp.auth", "true");
        smtp.put("mail.smtp.starttls.enable", "true");
        smtp.put("mail.smtp.connectiontimeout", "10000");
        smtp.put("mail.smtp.timeout", "10000");

        this.session = Session.getInstance(smtp, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    @Override
    public void notificarAsignacion(User revisor, Question pregunta) {
        if (revisor.getEmail() == null || revisor.getEmail().trim().isEmpty()) {
            throw new NotificacionException("el revisor no tiene correo registrado");
        }
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(revisor.getEmail().trim()));
            message.setSubject(asunto(pregunta), "UTF-8");
            message.setText(cuerpo(revisor, pregunta), "UTF-8");
            Transport.send(message);
        } catch (MessagingException e) {
            throw new NotificacionException("no se pudo enviar el correo (" + e.getMessage() + ")", e);
        }
    }

    static String asunto(Question pregunta) {
        return "Nueva pregunta asignada para revisión: " + pregunta.getId();
    }

    static String cuerpo(User revisor, Question pregunta) {
        return "Hola " + revisor.getFullName() + ",\n\n"
                + "Se te asignó una pregunta del Banco Saber PRO para revisar.\n\n"
                + "Id: " + pregunta.getId() + "\n"
                + "Título: " + pregunta.getNombre() + "\n"
                + "Tema: " + valor(pregunta.getTema()) + "\n"
                + "Competencia: " + valor(pregunta.getCompetencia()) + "\n"
                + "Autor: " + valor(pregunta.getAutor()) + "\n\n"
                + "Ingresa al sistema para revisarla.";
    }

    private static String valor(String s) {
        return s == null ? "-" : s;
    }
}

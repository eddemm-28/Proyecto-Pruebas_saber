package co.edu.unicauca.saberpro.client;

import co.edu.unicauca.saberpro.client.view.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada del cliente Swing (Corte 2). A diferencia del Corte 1, no inicializa
 * plugins ni bases de datos locales: todo vive en los microservicios detrás del gateway
 * (la corrección A21 del {@code QuestionPluginManager} está en el question-service).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // se usa el look and feel por defecto
        }
        AppContext ctx = new AppContext(AppContext.resolveBaseUrl());
        SwingUtilities.invokeLater(() -> new LoginFrame(ctx).setVisible(true));
    }
}

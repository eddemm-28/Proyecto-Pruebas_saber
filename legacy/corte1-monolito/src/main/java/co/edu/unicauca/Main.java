package co.edu.unicauca;

import co.edu.unicauca.access.QuestionImplRepository;
import co.edu.unicauca.domain.QuestionService;
import co.edu.unicauca.plugin.manager.QuestionPluginManager;
import co.edu.unicauca.presentation.DashboardFrame;
import co.edu.unicauca.presentation.LoginFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {

        // A21: si los plugins no cargan se avisa al usuario y se desactiva solo el generador,
        // en lugar de tragarse el error y fallar después con un NullPointerException.
        boolean pluginsOk;
        try {
            QuestionPluginManager.init();
            pluginsOk = true;
        } catch (Exception e) {
            pluginsOk = false;
            System.err.println("Error inicializando plugins: " + e.getMessage());
            javax.swing.JOptionPane.showMessageDialog(null,
                    "No se pudieron cargar los plugins de preguntas.\n" + e.getMessage(),
                    "Plugins", javax.swing.JOptionPane.WARNING_MESSAGE);
        }

        // Modo demo del banco de preguntas (datos de ejemplo en memoria, sin login)
        if (args.length > 0 && "--questions".equalsIgnoreCase(args[0])) {
            QuestionService demoService = new QuestionService(new QuestionImplRepository());
            SwingUtilities.invokeLater(() -> DashboardFrame.launchQuestionBankSuite(demoService));
            return;
        }
        if (args.length > 0 && "--plugins".equalsIgnoreCase(args[0])) {
            if (!pluginsOk) {
                return;
            }
            AppContext pluginContext = new AppContext();
            SwingUtilities.invokeLater(() ->
                    DashboardFrame.launchPluginGenerator(pluginContext.getQuestionService()));
            return;
        }

        AppContext context = new AppContext();

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(context);
            loginFrame.setVisible(true);
        });
    }
}

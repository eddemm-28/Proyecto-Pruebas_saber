package co.edu.unicauca;

import co.edu.unicauca.access.QuestionImplRepository;
import co.edu.unicauca.domain.QuestionService;
import co.edu.unicauca.plugin.manager.QuestionPluginManager;
import co.edu.unicauca.presentation.DashboardFrame;
import co.edu.unicauca.presentation.LoginFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {

        try {
            QuestionPluginManager.init();
        } catch (Exception e) {
            System.err.println("Error inicializando plugins: " + e.getMessage());
        }

        // Modo demo del banco de preguntas (datos de ejemplo en memoria, sin login)
        if (args.length > 0 && "--questions".equalsIgnoreCase(args[0])) {
            QuestionService demoService = new QuestionService(new QuestionImplRepository());
            SwingUtilities.invokeLater(() -> DashboardFrame.launchQuestionBankSuite(demoService));
            return;
        }
        if (args.length > 0 && "--plugins".equalsIgnoreCase(args[0])) {
            SwingUtilities.invokeLater(DashboardFrame::launchPluginGenerator);
            return;
        }

        AppContext context = new AppContext();

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(context);
            loginFrame.setVisible(true);
        });
    }
}

package co.edu.unicauca.presentation;

import co.edu.unicauca.AppContext;
import co.edu.unicauca.controller.QuestionController;
import co.edu.unicauca.controller.ReviewController;
import co.edu.unicauca.domain.QuestionService;
import co.edu.unicauca.domain.Role;
import co.edu.unicauca.domain.User;
import co.edu.unicauca.kernel.QuestionMicrokernel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardFrame extends JFrame {

    private static QuestionMicrokernel sharedKernel;

    private final AppContext context;

    public DashboardFrame(User user, AppContext context) {
        this.context = context;
        setTitle("Tablero - " + user.getRole().getDisplayName());
        setSize(480, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel welcomeLabel = new JLabel("Bienvenido, " + user.getFullName(), SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(welcomeLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JLabel roleLabel = new JLabel("Rol: " + user.getRole().getDisplayName(), SwingConstants.CENTER);
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(roleLabel);
        centerPanel.add(Box.createVerticalStrut(15));

        Role role = user.getRole();

        if (role == Role.AUTOR_DE_PREGUNTAS) {
            QuestionController questionController = new QuestionController(context.getQuestionService(), user);

            JButton btnNueva = styledButton("Crear pregunta", new Color(16, 129, 79));
            btnNueva.addActionListener(e -> new GUIQuestionForm(questionController).setVisible(true));
            centerPanel.add(btnNueva);
            centerPanel.add(Box.createVerticalStrut(10));

            JButton btnMias = styledButton("Mis preguntas", new Color(37, 99, 235));
            btnMias.addActionListener(e -> new GUIMyQuestions(questionController).setVisible(true));
            centerPanel.add(btnMias);
            centerPanel.add(Box.createVerticalStrut(10));
        }

        if (role == Role.ADMINISTRADOR) {
            ReviewController reviewController = new ReviewController(context.getReviewService(), user);

            JButton btnAsignar = styledButton("Asignar revisores", new Color(124, 58, 237));
            btnAsignar.addActionListener(e -> new GUIAssignReviewers(reviewController).setVisible(true));
            centerPanel.add(btnAsignar);
            centerPanel.add(Box.createVerticalStrut(10));
        }

        if (role == Role.ADMINISTRADOR || role == Role.REVISOR) {
            JButton btnBank = styledButton("Abrir Banco de Preguntas", new Color(37, 99, 235));
            btnBank.addActionListener(e -> launchQuestionBankSuite(context.getQuestionService()));
            centerPanel.add(btnBank);
            centerPanel.add(Box.createVerticalStrut(10));
        }

        if (role == Role.ADMINISTRADOR || role == Role.AUTOR_DE_PREGUNTAS) {
            JButton btnPlugins = styledButton("Generar preguntas (Plugins)", new Color(16, 129, 79));
            btnPlugins.addActionListener(e -> launchPluginGenerator());
            centerPanel.add(btnPlugins);
            centerPanel.add(Box.createVerticalStrut(10));
        }

        if (role == Role.DOCENTE) {
            JLabel info = new JLabel("Vista Docente: reportes y seguimiento (pendiente).", SwingConstants.CENTER);
            info.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(info);
        }

        if (role == Role.ESTUDIANTE) {
            JLabel info = new JLabel("Vista Estudiante: simulacros (pendiente).", SwingConstants.CENTER);
            info.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(info);
        }

        panel.add(centerPanel, BorderLayout.CENTER);
        add(panel);
    }

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(300, 40));
        return btn;
    }

    public static void launchQuestionBankSuite(QuestionService questionService) {
        GUIQuestions guiQuestions = new GUIQuestions(questionService);
        GUIObserver1 guiObserver1 = new GUIObserver1(questionService);
        GUIObserver2 guiObserver2 = new GUIObserver2(questionService);

        Point loc = guiQuestions.getLocation();
        guiObserver1.setLocation(loc.x + guiQuestions.getWidth() + 10, loc.y);
        guiObserver2.setLocation(loc.x + guiQuestions.getWidth() + 10, loc.y + guiObserver1.getHeight() + 10);

        guiQuestions.setVisible(true);
        guiObserver1.setVisible(true);
        guiObserver2.setVisible(true);
    }

    public static void launchPluginGenerator() {
        if (sharedKernel == null) {
            sharedKernel = new QuestionMicrokernel();
        }
        GUIPluginGenerator gen = new GUIPluginGenerator(sharedKernel);
        gen.setVisible(true);
    }
}

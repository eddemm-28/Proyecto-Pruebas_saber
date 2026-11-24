package co.edu.unicauca.saberpro.client.view;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.model.Catalogs;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.model.Session;
import co.edu.unicauca.saberpro.client.observer.Observer;
import co.edu.unicauca.saberpro.client.view.questions.QuestionBankPanel;
import co.edu.unicauca.saberpro.client.view.simulation.SimulationPanel;
import co.edu.unicauca.saberpro.client.view.tracking.ReportsPanel;
import co.edu.unicauca.saberpro.client.view.tracking.StudentProgressPanel;
import co.edu.unicauca.saberpro.client.view.users.UserAdminPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Ventana principal: las pestañas dependen del rol del usuario autenticado. */
public class MainFrame extends JFrame {

    private final AppContext ctx;
    private final Observer sessionObserver;

    public MainFrame(AppContext ctx) {
        super("Banco de Preguntas Saber Pro");
        this.ctx = ctx;
        Dtos.User user = ctx.session().user();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.add(UiKit.title("Banco de Preguntas Saber Pro"), BorderLayout.WEST);
        JPanel right = new JPanel();
        right.add(new JLabel(user.fullName() + "  ·  " + Catalogs.label(Catalogs.ROLES, user.role())));
        JButton logout = new JButton("Cerrar sesión");
        logout.addActionListener(e -> ctx.auth().logout());
        right.add(logout);
        header.add(right, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        switch (user.role()) {
            case "ADMINISTRADOR" -> {
                tabs.addTab("Banco de preguntas", new QuestionBankPanel(ctx, false));
                tabs.addTab("Usuarios y auditoría", new UserAdminPanel(ctx));
                tabs.addTab("Reportes", new ReportsPanel(ctx));
            }
            case "AUTOR" -> tabs.addTab("Mis preguntas", new QuestionBankPanel(ctx, true));
            case "REVISOR" -> tabs.addTab("Preguntas por revisar", new QuestionBankPanel(ctx, false));
            case "DOCENTE" -> {
                tabs.addTab("Reportes de rendimiento", new ReportsPanel(ctx));
                tabs.addTab("Banco de preguntas", new QuestionBankPanel(ctx, false));
            }
            case "ESTUDIANTE" -> {
                tabs.addTab("Simulacro", new SimulationPanel(ctx));
                tabs.addTab("Mi progreso", new StudentProgressPanel(ctx));
            }
            default -> tabs.addTab("Inicio", new JLabel("Rol sin vistas asignadas: " + user.role()));
        }

        JPanel content = new JPanel(new BorderLayout());
        content.add(header, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        setContentPane(content);

        // Observer: al cerrar sesión (o si el token expira) se vuelve al login
        sessionObserver = (source, event, data) -> {
            if (Session.LOGOUT.equals(event)) {
                dispose();
                new LoginFrame(ctx).setVisible(true);
            }
        };
        ctx.session().addObserver(sessionObserver);
        ctx.api().setOnUnauthorized(() -> SwingUtilities.invokeLater(() -> {
            if (ctx.session().token() != null) {
                UiKit.showInfo(this, "Su sesión expiró. Inicie sesión de nuevo.");
                ctx.session().end();
            }
        }));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                ctx.session().removeObserver(sessionObserver);
                if (ctx.session().token() != null) {
                    System.exit(0); // cierre con la X: termina la aplicación
                }
            }
        });

        pack();
        setLocationRelativeTo(null);
    }
}

package co.edu.unicauca.saberpro.client.view;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.infra.Async;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** RF-02: inicio de sesión contra el api-gateway. */
public class LoginFrame extends JFrame {

    private final AppContext ctx;
    private final JTextField txtLogin = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JButton btnLogin = new JButton("Ingresar");
    private final JLabel lblStatus = new JLabel(" ", SwingConstants.CENTER);

    public LoginFrame(AppContext ctx) {
        super("Banco de Preguntas Saber Pro - Ingreso");
        this.ctx = ctx;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel header = new JPanel(new BorderLayout());
        header.add(UiKit.title("Banco de Preguntas Saber Pro"), BorderLayout.NORTH);
        JLabel server = new JLabel("Servidor: " + ctx.api().baseUrl());
        server.setForeground(UiKit.MUTED);
        header.add(server, BorderLayout.SOUTH);

        JButton btnRegister = new JButton("Crear cuenta de estudiante");
        btnRegister.addActionListener(e -> new RegisterDialog(this, ctx).setVisible(true));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnRegister);
        buttons.add(btnLogin);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
        content.add(header, BorderLayout.NORTH);
        content.add(UiKit.form("Usuario:", txtLogin, "Contraseña:", txtPassword), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(lblStatus, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);
        setContentPane(content);

        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(btnLogin);
        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String login = txtLogin.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (login.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Ingrese usuario y contraseña.");
            return;
        }
        btnLogin.setEnabled(false);
        lblStatus.setForeground(UiKit.MUTED);
        lblStatus.setText("Validando...");
        Async.run(() -> ctx.auth().login(login, password),
                user -> {
                    txtPassword.setText("");
                    dispose();
                    new MainFrame(ctx).setVisible(true);
                },
                err -> {
                    btnLogin.setEnabled(true);
                    lblStatus.setForeground(UiKit.BAD);
                    lblStatus.setText(err.getMessage());
                });
    }
}

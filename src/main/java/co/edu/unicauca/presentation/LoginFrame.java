package co.edu.unicauca.presentation;

import co.edu.unicauca.AppContext;
import co.edu.unicauca.domain.User;
import co.edu.unicauca.domain.UserService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {
    
    private final UserService userService;
    private final AppContext context;
    private JTextField txtLogin;
    private JPasswordField txtPassword;

    public LoginFrame(AppContext context) {
        this.context = context;
        this.userService = context.getUserService();
        setTitle("Inicio de Sesión");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initUI();
    }
    
    private void initUI() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Login:"));
        txtLogin = new JTextField();
        panel.add(txtLogin);

        panel.add(new JLabel("Contraseña:"));
        txtPassword = new JPasswordField();
        panel.add(txtPassword);

        JButton btnLogin = new JButton("Ingresar");
        btnLogin.addActionListener(this::onLogin);
        panel.add(btnLogin);

        JButton btnRegister = new JButton("Registrarse");
        btnRegister.addActionListener(e -> {
            RegisterFrame registerFrame = new RegisterFrame(userService);
            registerFrame.setVisible(true);
        });
        panel.add(btnRegister);

        add(panel);
    }
    
    private void onLogin(ActionEvent e) {
        String login = txtLogin.getText();
        String password = new String(txtPassword.getPassword());

        try {
            User user = userService.login(login, password);
            if (user != null) {
                JOptionPane.showMessageDialog(this, "Bienvenido " + user.getFullName(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
                DashboardFrame dashboard = new DashboardFrame(user, context);
                dashboard.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Credenciales incorrectas.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

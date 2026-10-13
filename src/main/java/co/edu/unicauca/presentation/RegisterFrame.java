package co.edu.unicauca.presentation;

import co.edu.unicauca.domain.Role;
import co.edu.unicauca.domain.User;
import co.edu.unicauca.domain.UserService;
import co.edu.unicauca.domain.UserStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class RegisterFrame extends JFrame {

    private final UserService userService;
    private JTextField txtLogin;
    private JTextField txtFullName;
    private JTextField txtEmail;
    private JComboBox<Role> cmbRole;
    private JComboBox<UserStatus> cmbStatus;
    private JPasswordField txtPassword;

    public RegisterFrame(UserService userService) {
        this.userService = userService;
        setTitle("Registro de Usuario");
        setSize(400, 340);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridLayout(7, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Login:"));
        txtLogin = new JTextField();
        panel.add(txtLogin);

        panel.add(new JLabel("Nombre Completo:"));
        txtFullName = new JTextField();
        panel.add(txtFullName);

        panel.add(new JLabel("Correo:"));
        txtEmail = new JTextField();
        panel.add(txtEmail);

        panel.add(new JLabel("Rol:"));
        cmbRole = new JComboBox<>(Role.values());
        panel.add(cmbRole);

        panel.add(new JLabel("Estado:"));
        cmbStatus = new JComboBox<>(UserStatus.values());
        panel.add(cmbStatus);

        panel.add(new JLabel("Contraseña:"));
        txtPassword = new JPasswordField();
        panel.add(txtPassword);

        JButton btnRegister = new JButton("Registrar");
        btnRegister.addActionListener(this::onRegister);
        panel.add(new JLabel()); // Spacer
        panel.add(btnRegister);

        add(panel);
    }

    private void onRegister(ActionEvent e) {
        String login = txtLogin.getText();
        String fullName = txtFullName.getText();
        String email = txtEmail.getText().trim();
        Role role = (Role) cmbRole.getSelectedItem();
        UserStatus status = (UserStatus) cmbStatus.getSelectedItem();
        String password = new String(txtPassword.getPassword());

        if (login.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = new User(login, fullName, role, status, null, email);

        try {
            boolean success = userService.register(user, password);
            if (success) {
                JOptionPane.showMessageDialog(this, "Usuario registrado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Error al registrar el usuario.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de Validación", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

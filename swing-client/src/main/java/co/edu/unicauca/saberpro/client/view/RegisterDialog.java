package co.edu.unicauca.saberpro.client.view;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.infra.Async;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Arrays;

/**
 * RF-01: registro. Corrección del Corte 1: ya no se elige el rol; las cuentas nacen como
 * Estudiante y el Administrador asigna otros roles.
 */
public class RegisterDialog extends JDialog {

    private final JTextField txtLogin = new JTextField(20);
    private final JTextField txtName = new JTextField(20);
    private final JTextField txtEmail = new JTextField(20);
    private final JPasswordField txtPass = new JPasswordField(20);
    private final JPasswordField txtPass2 = new JPasswordField(20);

    public RegisterDialog(JFrame owner, AppContext ctx) {
        super(owner, "Crear cuenta", true);
        JLabel hint = new JLabel("<html>La cuenta se crea con rol <b>Estudiante</b>. Contraseña: mínimo 6 caracteres, "
                + "una mayúscula, un número y un carácter especial.</html>");
        hint.setForeground(UiKit.MUTED);

        JButton btnSave = new JButton("Registrarme");
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> {
            char[] p1 = txtPass.getPassword();
            char[] p2 = txtPass2.getPassword();
            if (!Arrays.equals(p1, p2)) {
                UiKit.showInfo(this, "Las contraseñas no coinciden.");
                return;
            }
            btnSave.setEnabled(false);
            Async.run(() -> ctx.auth().register(txtLogin.getText().trim(), txtName.getText().trim(),
                            txtEmail.getText().trim(), new String(p1)),
                    user -> {
                        UiKit.showInfo(this, "Cuenta creada. Ya puede iniciar sesión como " + user.login() + ".");
                        dispose();
                    },
                    err -> {
                        btnSave.setEnabled(true);
                        UiKit.showError(this, err);
                    });
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnCancel);
        buttons.add(btnSave);

        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        content.add(hint, BorderLayout.NORTH);
        content.add(UiKit.form("Usuario (login):", txtLogin, "Nombre completo:", txtName, "Correo:", txtEmail,
                "Contraseña:", txtPass, "Repetir contraseña:", txtPass2), BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(btnSave);
        pack();
        setLocationRelativeTo(owner);
    }
}

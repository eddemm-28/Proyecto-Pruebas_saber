package co.edu.unicauca.saberpro.client.view.users;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.controller.UserAdminController;
import co.edu.unicauca.saberpro.client.infra.Async;
import co.edu.unicauca.saberpro.client.model.Catalogs;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.observer.Observer;
import co.edu.unicauca.saberpro.client.observer.Subject;
import co.edu.unicauca.saberpro.client.view.UiKit;
import co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Map;

import static co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel.col;

/** A09 (RF-03) y A11 (RNF-09): gestión de usuarios y consulta de inicios de sesión. */
public class UserAdminPanel extends JPanel implements Observer {

    private final AppContext ctx;
    private final ListTableModel<Dtos.User> users = new ListTableModel<>(
            col("Usuario", Dtos.User::login), col("Nombre", Dtos.User::fullName), col("Correo", Dtos.User::email),
            col("Rol", Dtos.User::roleName), col("Estado", Dtos.User::status),
            col("Creado", u -> UiKit.format(u.createdAt())));
    private final JTable usersTable = new JTable(users);
    private final JComboBox<String> cbRole = new JComboBox<>();
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"", "ACTIVO", "INACTIVO"});
    private final JTextField txtQ = new JTextField(14);

    private final ListTableModel<Dtos.LoginAudit> audit = new ListTableModel<>(
            col("Fecha", a -> UiKit.format(a.occurredAt())), col("Usuario", Dtos.LoginAudit::login),
            col("Resultado", a -> a.success() ? "Exitoso" : "Fallido"), col("Motivo", Dtos.LoginAudit::failureReason),
            col("IP", Dtos.LoginAudit::ipAddress), col("Cliente", Dtos.LoginAudit::clientAgent));
    private final JTextField txtAuditLogin = new JTextField(12);

    public UserAdminPanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        cbRole.addItem("");
        Catalogs.ROLES.values().forEach(cbRole::addItem);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Usuarios", usersTab());
        tabs.addTab("Inicios de sesión", auditTab());
        add(tabs, BorderLayout.CENTER);
        ctx.users().addObserver(this);
        reloadUsers();
        reloadAudit();
    }

    private JPanel usersTab() {
        JButton search = new JButton("Buscar");
        search.addActionListener(e -> reloadUsers());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Rol:"));
        filters.add(cbRole);
        filters.add(new JLabel("Estado:"));
        filters.add(cbStatus);
        filters.add(new JLabel("Buscar:"));
        filters.add(txtQ);
        filters.add(search);

        JButton changeRole = new JButton("Cambiar rol…");
        JButton toggle = new JButton("Activar / inactivar");
        changeRole.addActionListener(e -> changeRole());
        toggle.addActionListener(e -> toggleStatus());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(changeRole);
        actions.add(toggle);

        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(filters, BorderLayout.NORTH);
        p.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private JPanel auditTab() {
        JButton search = new JButton("Consultar");
        search.addActionListener(e -> reloadAudit());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Usuario:"));
        filters.add(txtAuditLogin);
        filters.add(search);
        JLabel note = new JLabel("Los paneles avanzados de auditoría global (HU08) llegan en el Corte 3.");
        note.setForeground(UiKit.MUTED);
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(filters, BorderLayout.NORTH);
        p.add(new JScrollPane(new JTable(audit)), BorderLayout.CENTER);
        p.add(note, BorderLayout.SOUTH);
        return p;
    }

    @Override
    public void update(Subject source, String event, Object data) {
        if (UserAdminController.USERS_CHANGED.equals(event)) {
            reloadUsers();
        }
    }

    private void reloadUsers() {
        String role = key(Catalogs.ROLES, (String) cbRole.getSelectedItem());
        String status = (String) cbStatus.getSelectedItem();
        Async.run(() -> ctx.users().list(role, status == null || status.isBlank() ? null : status, txtQ.getText(), 0),
                page -> users.setRows(page.content()), err -> UiKit.showError(this, err));
    }

    private void reloadAudit() {
        Async.run(() -> ctx.users().loginAudit(txtAuditLogin.getText(), null, 0),
                page -> audit.setRows(page.content()), err -> UiKit.showError(this, err));
    }

    private Dtos.User selected() {
        int r = usersTable.getSelectedRow();
        return r < 0 ? null : users.rowAt(usersTable.convertRowIndexToModel(r));
    }

    private void changeRole() {
        Dtos.User u = selected();
        if (u == null) return;
        Object choice = JOptionPane.showInputDialog(this, "Nuevo rol para " + u.login() + ":", "Cambiar rol",
                JOptionPane.QUESTION_MESSAGE, null, Catalogs.ROLES.values().toArray(), u.roleName());
        if (choice == null) return;
        String role = key(Catalogs.ROLES, choice.toString());
        Async.run(() -> ctx.users().changeRole(u.id(), role), x -> { }, err -> UiKit.showError(this, err));
    }

    private void toggleStatus() {
        Dtos.User u = selected();
        if (u == null) return;
        String next = "ACTIVO".equals(u.status()) ? "INACTIVO" : "ACTIVO";
        int ok = JOptionPane.showConfirmDialog(this, "¿Cambiar " + u.login() + " a " + next + "?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        Async.run(() -> ctx.users().changeStatus(u.id(), next), x -> { }, err -> UiKit.showError(this, err));
    }

    private static String key(Map<String, String> catalog, String label) {
        if (label == null || label.isBlank()) return null;
        return catalog.entrySet().stream().filter(e -> e.getValue().equals(label)).map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }
}

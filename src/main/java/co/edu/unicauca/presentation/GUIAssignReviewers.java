package co.edu.unicauca.presentation;

import co.edu.unicauca.controller.ReviewController;
import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.User;
import co.edu.unicauca.infra.Observer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * HU4: el administrador ve las preguntas Pendientes de revisión y les asigna uno o más revisores.
 * Al asignar, el sistema notifica por correo a cada revisor.
 */
public class GUIAssignReviewers extends JFrame implements Observer {

    private final ReviewController controller;

    private DefaultTableModel tableModel;
    private JTable table;
    private final List<Question> pendientes = new ArrayList<>();

    private DefaultListModel<User> reviewersModel;
    private JList<User> reviewersList;
    private JLabel lblAsignados;
    private JButton btnAsignar;

    public GUIAssignReviewers(ReviewController controller) {
        this.controller = controller;
        initComponents();
        controller.addObserver(this);
        cargar();
    }

    private void initComponents() {
        setTitle("Asignar revisores");
        setSize(860, 460);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // ---- Izquierda: preguntas pendientes
        tableModel = new DefaultTableModel(new String[]{"Id", "Título", "Autor", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                           boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                setBackground(EstadoColores.colorDe((String) value));
                setForeground(Color.WHITE);
                setHorizontalAlignment(CENTER);
                return this;
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarAsignados();
            }
        });

        JPanel izquierda = new JPanel(new BorderLayout(5, 5));
        izquierda.setBorder(BorderFactory.createTitledBorder("Preguntas pendientes de revisión"));
        izquierda.add(new JScrollPane(table), BorderLayout.CENTER);

        // ---- Derecha: revisores disponibles
        reviewersModel = new DefaultListModel<>();
        reviewersList = new JList<>(reviewersModel);
        reviewersList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                User u = (User) value;
                String correo = (u.getEmail() == null || u.getEmail().isEmpty()) ? "sin correo" : u.getEmail();
                return super.getListCellRendererComponent(list,
                        u.getFullName() + " (" + u.getLogin() + ") - " + correo, index, selected, focus);
            }
        });

        lblAsignados = new JLabel("Revisores ya asignados: -");
        btnAsignar = new JButton("Asignar revisores seleccionados");
        btnAsignar.setBackground(new Color(37, 99, 235));
        btnAsignar.setForeground(Color.WHITE);
        btnAsignar.addActionListener(e -> asignar());

        JPanel derecha = new JPanel(new BorderLayout(5, 5));
        derecha.setBorder(BorderFactory.createTitledBorder("Revisores disponibles (Ctrl para varios)"));
        derecha.add(new JScrollPane(reviewersList), BorderLayout.CENTER);
        JPanel sur = new JPanel(new GridLayout(2, 1, 4, 4));
        sur.add(lblAsignados);
        sur.add(btnAsignar);
        derecha.add(sur, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        split.setResizeWeight(0.5);
        split.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(split);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                controller.removeObserver(GUIAssignReviewers.this);
            }
        });
    }

    private void cargar() {
        tableModel.setRowCount(0);
        pendientes.clear();
        for (Question q : controller.listarPendientes()) {
            pendientes.add(q);
            tableModel.addRow(new Object[]{q.getId(), q.getNombre(), q.getAutor(), q.getEstado()});
        }

        reviewersModel.clear();
        for (User u : controller.listarRevisores()) {
            reviewersModel.addElement(u);
        }
        mostrarAsignados();
    }

    private void mostrarAsignados() {
        int fila = table.getSelectedRow();
        if (fila < 0 || fila >= pendientes.size()) {
            lblAsignados.setText("Revisores ya asignados: -");
            return;
        }
        List<String> asignados = controller.revisoresAsignados(pendientes.get(fila).getId());
        lblAsignados.setText("Revisores ya asignados: " + (asignados.isEmpty() ? "ninguno" : String.join(", ", asignados)));
    }

    private void asignar() {
        int fila = table.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> logins = new ArrayList<>();
        for (User u : reviewersList.getSelectedValuesList()) {
            logins.add(u.getLogin());
        }
        if (logins.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione al menos un revisor.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final String questionId = pendientes.get(fila).getId();
        btnAsignar.setEnabled(false);

        // El envío de correos puede tardar, así que no se hace en el hilo de la interfaz.
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return controller.asignar(questionId, logins);
            }

            @Override
            protected void done() {
                btnAsignar.setEnabled(true);
                try {
                    List<String> fallos = get();
                    if (fallos.isEmpty()) {
                        JOptionPane.showMessageDialog(GUIAssignReviewers.this,
                                "Revisores asignados y notificados por correo.",
                                "Asignación exitosa", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(GUIAssignReviewers.this,
                                "Los revisores quedaron asignados, pero falló el correo para:\n" + String.join("\n", fallos),
                                "Asignación con avisos", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(GUIAssignReviewers.this, ex.getCause().getMessage(),
                            "No se pudo asignar", JOptionPane.WARNING_MESSAGE);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Observer: se vuelve a cargar cuando cambian las preguntas o las asignaciones. */
    @Override
    public void update(Object obj) {
        SwingUtilities.invokeLater(this::cargar);
    }
}

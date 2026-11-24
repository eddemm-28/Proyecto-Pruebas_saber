package co.edu.unicauca.saberpro.client.view.questions;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.controller.QuestionController;
import co.edu.unicauca.saberpro.client.infra.Async;
import co.edu.unicauca.saberpro.client.model.Catalogs;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.observer.Observer;
import co.edu.unicauca.saberpro.client.view.UiKit;
import co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Map;

import static co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel.col;

/**
 * Banco de preguntas (A16, A19, A20): filtros dinámicos, paginación y acciones según el estado.
 * Los botones de acción salen de {@code allowedActions} que calcula el patrón State en el servidor.
 * Es Observer del {@link QuestionController}.
 */
public class QuestionBankPanel extends JPanel implements Observer {

    private static final int PAGE_SIZE = 15;

    private final AppContext ctx;
    private final boolean onlyMine;
    private final ListTableModel<Dtos.QuestionSummary> model = new ListTableModel<>(
            col("Código", Dtos.QuestionSummary::code),
            col("Título", Dtos.QuestionSummary::title),
            col("Competencia", Dtos.QuestionSummary::competence),
            col("Tema", Dtos.QuestionSummary::topic),
            col("Dificultad", q -> Catalogs.label(Catalogs.DIFFICULTIES, q.difficulty())),
            col("Estado", Dtos.QuestionSummary::stateName),
            col("Autor", Dtos.QuestionSummary::authorLogin),
            col("Actualizada", q -> UiKit.format(q.updatedAt())));
    private final JTable table = new JTable(model);

    private final JComboBox<String> cbState = combo(Catalogs.STATES);
    private final JComboBox<String> cbCompetence = new JComboBox<>();
    private final JComboBox<String> cbDifficulty = combo(Catalogs.DIFFICULTIES);
    private final JTextField txtText = new JTextField(14);
    private final JLabel lblPage = new JLabel();
    private final JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private int page = 0;
    private int totalPages = 0;

    public QuestionBankPanel(AppContext ctx, boolean onlyMine) {
        super(new BorderLayout(6, 6));
        this.ctx = ctx;
        this.onlyMine = onlyMine;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        cbCompetence.addItem("");
        Catalogs.COMPETENCES.forEach(cbCompetence::addItem);
        cbCompetence.setEditable(true);

        JButton btnSearch = new JButton("Buscar");
        btnSearch.addActionListener(e -> { page = 0; reload(); });
        txtText.addActionListener(e -> { page = 0; reload(); });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Estado:"));
        filters.add(cbState);
        filters.add(new JLabel("Competencia:"));
        filters.add(cbCompetence);
        filters.add(new JLabel("Dificultad:"));
        filters.add(cbDifficulty);
        filters.add(new JLabel("Texto:"));
        filters.add(txtText);
        filters.add(btnSearch);

        JPanel top = new JPanel(new BorderLayout());
        top.add(filters, BorderLayout.CENTER);
        JPanel createButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (ctx.session().hasRole("AUTOR", "ADMINISTRADOR")) {
            JButton btnNew = new JButton("Nueva pregunta");
            btnNew.addActionListener(e -> new QuestionFormDialog(this, ctx, null).setVisible(true));
            createButtons.add(btnNew);
        }
        top.add(createButtons, BorderLayout.EAST);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(5).setCellRenderer(UiKit.stateRenderer(v -> stateKeyOf(String.valueOf(v))));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshActions();
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });

        JButton prev = new JButton("◀");
        JButton next = new JButton("▶");
        prev.addActionListener(e -> { if (page > 0) { page--; reload(); } });
        next.addActionListener(e -> { if (page + 1 < totalPages) { page++; reload(); } });
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        paging.add(prev);
        paging.add(lblPage);
        paging.add(next);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(actionsPanel, BorderLayout.CENTER);
        bottom.add(paging, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        ctx.questions().addObserver(this);
        if (ctx.session().hasRole("REVISOR")) {
            cbState.setSelectedItem(Catalogs.STATES.get("PENDIENTE_REVISION"));
        }
        reload();
    }

    /** Observer: otra vista cambió una pregunta → se recarga la tabla. */
    @Override
    public void update(co.edu.unicauca.saberpro.client.observer.Subject source, String event, Object data) {
        if (QuestionController.QUESTIONS_CHANGED.equals(event)) {
            reload();
        }
    }

    private void reload() {
        QuestionController.Filter filter = new QuestionController.Filter(
                keyOf(Catalogs.STATES, cbState), blankToNull((String) cbCompetence.getSelectedItem()), null,
                keyOf(Catalogs.DIFFICULTIES, cbDifficulty), null, blankToNull(txtText.getText()), onlyMine);
        Async.run(() -> ctx.questions().search(filter, page, PAGE_SIZE),
                result -> {
                    model.setRows(result.content());
                    totalPages = result.totalPages();
                    lblPage.setText("Página " + (totalPages == 0 ? 0 : page + 1) + " de " + totalPages
                            + "  (" + result.totalElements() + ")");
                    refreshActions();
                },
                err -> UiKit.showError(this, err));
    }

    private Dtos.QuestionSummary selected() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? null : model.rowAt(table.convertRowIndexToModel(viewRow));
    }

    private void openSelected() {
        Dtos.QuestionSummary s = selected();
        if (s == null) return;
        Async.run(() -> ctx.questions().get(s.id()),
                q -> {
                    if (q.editable() && ctx.session().user().id().equals(q.authorId())) {
                        new QuestionFormDialog(this, ctx, q).setVisible(true);
                    } else {
                        new QuestionDetailDialog(this, ctx, q).setVisible(true);
                    }
                },
                err -> UiKit.showError(this, err));
    }

    /** Botones dinámicos según allowedActions del State (A13) y el rol. */
    private void refreshActions() {
        actionsPanel.removeAll();
        Dtos.QuestionSummary s = selected();
        if (s != null) {
            JButton view = new JButton("Ver / editar");
            view.addActionListener(e -> openSelected());
            actionsPanel.add(view);
            Async.run(() -> ctx.questions().get(s.id()), q -> {
                for (String action : Catalogs.ACTIONS.keySet()) {
                    if (q.allowedActions() != null && q.allowedActions().contains(action)) {
                        JButton b = new JButton(Catalogs.ACTIONS.get(action));
                        b.addActionListener(e -> runAction(q, action));
                        actionsPanel.add(b);
                    }
                }
                actionsPanel.revalidate();
                actionsPanel.repaint();
            }, err -> { });
        }
        actionsPanel.revalidate();
        actionsPanel.repaint();
    }

    private void runAction(Dtos.Question q, String action) {
        String comment = null;
        if ("REJECT".equals(action) || "APPROVE".equals(action)) {
            comment = JOptionPane.showInputDialog(this, "REJECT".equals(action)
                    ? "Motivo del rechazo (obligatorio):" : "Comentario (opcional):");
            if (comment == null) return;
        } else if ("ARCHIVE".equals(action) || "PUBLISH".equals(action)) {
            int ok = JOptionPane.showConfirmDialog(this, "¿" + Catalogs.ACTIONS.get(action) + " la pregunta "
                    + q.code() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) return;
        }
        String c = comment;
        Async.run(() -> ctx.questions().transition(q.id(), action, c),
                updated -> UiKit.showInfo(this, "Pregunta " + updated.code() + " → " + updated.stateName()),
                err -> UiKit.showError(this, err));
    }

    private static JComboBox<String> combo(Map<String, String> catalog) {
        JComboBox<String> cb = new JComboBox<>();
        cb.addItem("");
        catalog.values().forEach(cb::addItem);
        return cb;
    }

    private static String keyOf(Map<String, String> catalog, JComboBox<String> cb) {
        Object label = cb.getSelectedItem();
        return catalog.entrySet().stream().filter(e -> e.getValue().equals(label)).map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    private static String stateKeyOf(String label) {
        return Catalogs.STATES.entrySet().stream().filter(e -> e.getValue().equals(label)).map(Map.Entry::getKey)
                .findFirst().orElse(label);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

package co.edu.unicauca.saberpro.client.view.simulation;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.controller.SimulationController;
import co.edu.unicauca.saberpro.client.controller.SimulationTimer;
import co.edu.unicauca.saberpro.client.infra.ApiException;
import co.edu.unicauca.saberpro.client.infra.Async;
import co.edu.unicauca.saberpro.client.model.Catalogs;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.observer.Observer;
import co.edu.unicauca.saberpro.client.observer.Subject;
import co.edu.unicauca.saberpro.client.view.BarChart;
import co.edu.unicauca.saberpro.client.view.UiKit;
import co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel.col;

/**
 * B14: configurar e iniciar el simulacro, responder con cronómetro sincronizado y ver el
 * resultado inmediato. Tres "pantallas" en un CardLayout. Observer del {@link SimulationController}.
 */
public class SimulationPanel extends JPanel implements Observer {

    private static final String CONFIG = "config";
    private static final String EXAM = "exam";
    private static final String RESULT = "result";
    private static final int RESYNC_EVERY_TICKS = 30;

    private final AppContext ctx;
    private final SimulationController sim;
    private final CardLayout cards = new CardLayout();

    // configuración
    private final JComboBox<String> cbStrategy = new JComboBox<>(Catalogs.STRATEGIES.values().toArray(String[]::new));
    private final JComboBox<String> cbCompetence = new JComboBox<>();
    private final JComboBox<String> cbTopic = new JComboBox<>();
    private final JComboBox<String> cbDifficulty = new JComboBox<>();
    private final JSpinner spQuestions = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
    private final JSpinner spMinutes = new JSpinner(new SpinnerNumberModel(20, 1, 300, 5));
    private final JLabel lblAvailable = new JLabel(" ");
    private List<Dtos.CatalogEntry> catalog = List.of();

    // examen
    private final JLabel lblTimer = new JLabel("--:--");
    private final JLabel lblProgress = new JLabel();
    private final JEditorPane questionView = new JEditorPane("text/html", "");
    private final JRadioButton[] rbOptions = new JRadioButton[4];
    private final ButtonGroup optionsGroup = new ButtonGroup();
    private final JPanel navigator = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
    private int index = 0;
    private int ticks = 0;
    private final Timer swingTimer;
    private boolean finishing = false;

    // resultado
    private final JLabel lblScore = UiKit.title("");
    private final BarChart resultChart = new BarChart();
    private final ListTableModel<Dtos.ResultItem> resultModel = new ListTableModel<>(
            col("#", Dtos.ResultItem::position), col("Competencia", Dtos.ResultItem::competence),
            col("Tu respuesta", r -> r.selectedOption() == null ? "—" : r.selectedOption()),
            col("Correcta", Dtos.ResultItem::correctOption), col("", r -> r.correct() ? "✔" : "✘"),
            col("Justificación", Dtos.ResultItem::justification));

    public SimulationPanel(AppContext ctx) {
        this.ctx = ctx;
        this.sim = ctx.simulations();
        setLayout(cards);
        add(configCard(), CONFIG);
        add(examCard(), EXAM);
        add(resultCard(), RESULT);
        sim.addObserver(this);
        swingTimer = new Timer(1000, e -> tick());
        loadCatalog();
        Async.run(sim::resumeActive, opt -> { }, err -> { });
    }

    // ------------------------------------------------------------------ tarjetas

    private JPanel configCard() {
        cbStrategy.addActionListener(e -> updateFiltersEnabled());
        cbCompetence.addActionListener(e -> updateAvailable());
        cbTopic.addActionListener(e -> updateAvailable());
        cbDifficulty.addActionListener(e -> updateAvailable());
        JButton start = new JButton("Iniciar simulacro");
        start.setFont(start.getFont().deriveFont(Font.BOLD, 14f));
        start.addActionListener(e -> startSimulation(start));
        JButton reload = new JButton("Actualizar catálogo");
        reload.addActionListener(e -> loadCatalog());

        JPanel form = UiKit.form("Tipo de simulacro:", cbStrategy, "Competencia:", cbCompetence, "Tema:", cbTopic,
                "Dificultad:", cbDifficulty, "Número de preguntas:", spQuestions, "Tiempo máximo (min):", spMinutes,
                "", lblAvailable);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(start);
        buttons.add(reload);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        p.add(UiKit.title("Configura tu simulacro"), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.add(form, BorderLayout.NORTH);
        center.add(buttons, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        JLabel note = new JLabel("<html>El tiempo lo controla el servidor: si se acaba, el simulacro se cierra y se califica "
                + "con las respuestas registradas.</html>");
        note.setForeground(UiKit.MUTED);
        p.add(note, BorderLayout.SOUTH);
        updateFiltersEnabled();
        return p;
    }

    private JPanel examCard() {
        lblTimer.setFont(lblTimer.getFont().deriveFont(Font.BOLD, 22f));
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        top.add(lblProgress, BorderLayout.WEST);
        top.add(lblTimer, BorderLayout.EAST);

        questionView.setEditable(false);
        JPanel options = new JPanel(new GridLayout(4, 1, 4, 4));
        for (int i = 0; i < 4; i++) {
            String label = Character.toString('A' + i);
            JRadioButton rb = new JRadioButton();
            rb.setActionCommand(label);
            rb.addActionListener(e -> answerCurrent(label));
            optionsGroup.add(rb);
            rbOptions[i] = rb;
            options.add(rb);
        }

        JButton prev = new JButton("◀ Anterior");
        JButton next = new JButton("Siguiente ▶");
        JButton finish = new JButton("Finalizar y calificar");
        prev.addActionListener(e -> show(index - 1));
        next.addActionListener(e -> show(index + 1));
        finish.addActionListener(e -> {
            int unanswered = sim.current() == null ? 0 : sim.current().questionCount() - sim.answeredCount();
            String msg = unanswered > 0 ? "Tiene " + unanswered + " pregunta(s) sin responder. ¿Finalizar de todos modos?"
                    : "¿Finalizar el simulacro?";
            if (JOptionPane.showConfirmDialog(this, msg, "Finalizar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                finishSimulation();
            }
        });
        JPanel bottom = new JPanel(new BorderLayout());
        JPanel move = new JPanel(new FlowLayout(FlowLayout.LEFT));
        move.add(prev);
        move.add(next);
        bottom.add(move, BorderLayout.WEST);
        bottom.add(finish, BorderLayout.EAST);
        bottom.add(new JScrollPane(navigator), BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        center.add(new JScrollPane(questionView), BorderLayout.CENTER);
        center.add(options, BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout());
        p.add(top, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel resultCard() {
        JButton again = new JButton("Nuevo simulacro");
        again.addActionListener(e -> {
            loadCatalog();
            cards.show(this, CONFIG);
        });
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
        top.add(lblScore, BorderLayout.WEST);
        top.add(again, BorderLayout.EAST);

        JTable table = new JTable(resultModel);
        table.getColumnModel().getColumn(5).setPreferredWidth(420);
        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        JScrollPane chartScroll = new JScrollPane(resultChart);
        chartScroll.setPreferredSize(new java.awt.Dimension(400, 170));
        center.add(chartScroll, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel p = new JPanel(new BorderLayout());
        p.add(top, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------ Observer

    @Override
    public void update(Subject source, String event, Object data) {
        switch (event) {
            case SimulationController.STARTED -> {
                index = 0;
                finishing = false;
                buildNavigator();
                show(0);
                cards.show(this, EXAM);
                swingTimer.start();
            }
            case SimulationController.ANSWERED -> {
                updateProgress();
                buildNavigator();
            }
            case SimulationController.EXPIRED -> {
                swingTimer.stop();
                UiKit.showInfo(this, "Se acabó el tiempo. El simulacro se cerró y se calificó con las respuestas registradas.");
                finishSimulation();
            }
            case SimulationController.FINISHED -> {
                swingTimer.stop();
                showResult((Dtos.SimulationResult) data);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ lógica de vista

    private void loadCatalog() {
        Async.run(sim::catalog, list -> {
            catalog = list;
            fillCombo(cbCompetence, list.stream().map(Dtos.CatalogEntry::competence).distinct().sorted().toList());
            fillCombo(cbTopic, list.stream().map(Dtos.CatalogEntry::topic).distinct().sorted().toList());
            fillCombo(cbDifficulty, Catalogs.DIFFICULTIES.values().stream().toList());
            updateAvailable();
        }, err -> lblAvailable.setText("No se pudo cargar el catálogo: " + err.getMessage()));
    }

    private void updateFiltersEnabled() {
        String strategy = key(Catalogs.STRATEGIES, (String) cbStrategy.getSelectedItem());
        cbCompetence.setEnabled("COMPETENCE".equals(strategy) || "TOPIC".equals(strategy));
        cbTopic.setEnabled("TOPIC".equals(strategy));
        cbDifficulty.setEnabled(true);
        updateAvailable();
    }

    private void updateAvailable() {
        String strategy = key(Catalogs.STRATEGIES, (String) cbStrategy.getSelectedItem());
        String competence = cbCompetence.isEnabled() ? (String) cbCompetence.getSelectedItem() : null;
        String topic = cbTopic.isEnabled() ? (String) cbTopic.getSelectedItem() : null;
        String difficulty = key(Catalogs.DIFFICULTIES, (String) cbDifficulty.getSelectedItem());
        long available = catalog.stream()
                .filter(c -> !("COMPETENCE".equals(strategy) || "TOPIC".equals(strategy)) || blank(competence)
                        || competence.equalsIgnoreCase(c.competence()))
                .filter(c -> !"TOPIC".equals(strategy) || blank(topic) || topic.equalsIgnoreCase(c.topic()))
                .filter(c -> blank(difficulty) || difficulty.equalsIgnoreCase(c.difficulty()))
                .mapToLong(Dtos.CatalogEntry::available).sum();
        lblAvailable.setText("Preguntas publicadas disponibles para esta configuración: " + available);
        lblAvailable.setForeground(available == 0 ? UiKit.BAD : UiKit.MUTED);
    }

    private void startSimulation(JButton button) {
        String strategy = key(Catalogs.STRATEGIES, (String) cbStrategy.getSelectedItem());
        Dtos.StartSimulation req = new Dtos.StartSimulation(strategy,
                cbCompetence.isEnabled() ? (String) cbCompetence.getSelectedItem() : null,
                cbTopic.isEnabled() ? (String) cbTopic.getSelectedItem() : null,
                key(Catalogs.DIFFICULTIES, (String) cbDifficulty.getSelectedItem()),
                (Integer) spQuestions.getValue(), (Integer) spMinutes.getValue());
        button.setEnabled(false);
        Async.run(() -> sim.start(req), s -> button.setEnabled(true), err -> {
            button.setEnabled(true);
            if (err.isConflict()) {
                int ok = JOptionPane.showConfirmDialog(this, err.getMessage() + "\n¿Desea retomarlo?", "Simulacro en curso",
                        JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    Async.run(sim::resumeActive, o -> { }, e2 -> UiKit.showError(this, e2));
                }
            } else {
                UiKit.showError(this, err);
            }
        });
    }

    /** Cada segundo: actualiza el cronómetro; cada 30 s re-sincroniza con el servidor. */
    private void tick() {
        SimulationTimer timer = sim.timer();
        long remaining = timer.remainingSeconds();
        lblTimer.setText("⏱ " + SimulationTimer.format(remaining));
        lblTimer.setForeground(remaining <= 60 ? UiKit.BAD : remaining <= 300 ? UiKit.WARN : UiKit.PRIMARY);
        if (remaining == 0 && !finishing) {
            swingTimer.stop();
            finishSimulation();
            return;
        }
        if (++ticks % RESYNC_EVERY_TICKS == 0) {
            Async.run(sim::refresh, s -> { }, err -> { });
        }
    }

    private void show(int i) {
        Dtos.Simulation s = sim.current();
        if (s == null || s.items().isEmpty()) return;
        index = Math.max(0, Math.min(i, s.items().size() - 1));
        Dtos.SimulationItem item = s.items().get(index);
        questionView.setText("<html><body style='font-family:sans-serif;padding:6px'>"
                + "<p style='color:#666'>Pregunta " + item.position() + " · " + esc(item.competence()) + "</p>"
                + "<p>" + esc(item.context()) + "</p><p><b>" + esc(item.statement()) + "</b></p></body></html>");
        questionView.setCaretPosition(0);
        optionsGroup.clearSelection();
        String chosen = sim.answerOf(item.questionId());
        for (int k = 0; k < 4; k++) {
            Dtos.Option o = k < item.options().size() ? item.options().get(k) : null;
            rbOptions[k].setText(o == null ? "" : o.label() + ")  " + o.text());
            rbOptions[k].setSelected(o != null && o.label().equals(chosen));
        }
        updateProgress();
    }

    private void answerCurrent(String label) {
        Dtos.Simulation s = sim.current();
        if (s == null) return;
        Dtos.SimulationItem item = s.items().get(index);
        Async.run(() -> sim.answer(item.questionId(), label), ok -> { }, err -> UiKit.showError(this, err));
    }

    private void finishSimulation() {
        if (finishing) return;
        finishing = true;
        swingTimer.stop();
        Async.run(sim::finish, r -> { }, err -> {
            finishing = false;
            UiKit.showError(this, err);
        });
    }

    private void showResult(Dtos.SimulationResult r) {
        String status = "EXPIRED".equals(r.status()) ? " (tiempo agotado)" : "";
        lblScore.setText(String.format("Puntaje: %.1f / 100 · %d de %d correctas%s", r.globalScore(),
                r.correctAnswers(), r.totalQuestions(), status));
        List<BarChart.Bar> bars = new ArrayList<>();
        r.competenceScores().forEach(c -> bars.add(new BarChart.Bar(c.competence(), c.score(), null)));
        resultChart.setBars(bars);
        resultModel.setRows(r.items());
        cards.show(this, RESULT);
    }

    private void updateProgress() {
        Dtos.Simulation s = sim.current();
        if (s == null) return;
        lblProgress.setText("Pregunta " + (index + 1) + " de " + s.questionCount() + "   ·   Respondidas: "
                + sim.answeredCount());
    }

    private void buildNavigator() {
        navigator.removeAll();
        Dtos.Simulation s = sim.current();
        if (s == null) return;
        for (int i = 0; i < s.items().size(); i++) {
            int target = i;
            JButton b = new JButton(String.valueOf(i + 1));
            b.setMargin(new java.awt.Insets(1, 4, 1, 4));
            if (sim.answerOf(s.items().get(i).questionId()) != null) {
                b.setForeground(UiKit.OK);
            }
            b.addActionListener(e -> show(target));
            navigator.add(b);
        }
        navigator.revalidate();
        navigator.repaint();
    }

    private static void fillCombo(JComboBox<String> cb, List<String> values) {
        Object selected = cb.getSelectedItem();
        cb.removeAllItems();
        cb.addItem("");
        values.forEach(cb::addItem);
        if (selected != null) cb.setSelectedItem(selected);
    }

    private static String key(Map<String, String> catalog, String label) {
        if (label == null || label.isBlank()) return null;
        return catalog.entrySet().stream().filter(e -> e.getValue().equals(label)).map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    /** Solo para pruebas: indica si hay error de conexión. */
    static boolean isNetwork(ApiException e) {
        return e.isNetworkError();
    }
}

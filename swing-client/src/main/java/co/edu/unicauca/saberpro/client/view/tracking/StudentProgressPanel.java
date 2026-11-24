package co.edu.unicauca.saberpro.client.view.tracking;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.controller.SimulationController;
import co.edu.unicauca.saberpro.client.infra.Async;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.observer.Observer;
import co.edu.unicauca.saberpro.client.observer.Subject;
import co.edu.unicauca.saberpro.client.view.BarChart;
import co.edu.unicauca.saberpro.client.view.UiKit;
import co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Map;

import static co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel.col;

/**
 * B15: historial de simulacros (RF-25) y panel de estadísticas con fortalezas y debilidades
 * (RF-26, RF-27). Se refresca solo cuando el estudiante termina un simulacro (Observer); como
 * el seguimiento se actualiza por evento asíncrono, se reintenta un par de segundos después.
 */
public class StudentProgressPanel extends JPanel implements Observer {

    private static final Map<String, String> TRENDS = Map.of("MEJORANDO", "↗ Mejorando", "ESTABLE", "→ Estable",
            "BAJANDO", "↘ Bajando", "SIN_DATOS", "Sin datos suficientes");

    private final AppContext ctx;
    private final JLabel lblSummary = new JLabel(" ");
    private final JLabel lblStrengths = new JLabel(" ");
    private final JLabel lblWeaknesses = new JLabel(" ");
    private final BarChart chart = new BarChart();
    private final ListTableModel<Dtos.CompetenceStat> statsModel = new ListTableModel<>(
            col("Competencia", Dtos.CompetenceStat::competence),
            col("Simulacros", Dtos.CompetenceStat::simulationsCount),
            col("Preguntas", Dtos.CompetenceStat::totalQuestions),
            col("Aciertos", Dtos.CompetenceStat::correctAnswers),
            col("Promedio", c -> UiKit.pct(c.averageScore())),
            col("Último", c -> UiKit.pct(c.lastScore())),
            col("Clasificación", c -> label(c.classification())));
    private final ListTableModel<Dtos.HistoryItem> historyModel = new ListTableModel<>(
            col("Fecha", h -> UiKit.format(h.finishedAt())),
            col("Tipo", Dtos.HistoryItem::strategy),
            col("Estado", h -> "EXPIRED".equals(h.finalStatus()) ? "Tiempo agotado" : "Finalizado"),
            col("Correctas", h -> h.correctAnswers() + " / " + h.totalQuestions()),
            col("Puntaje", h -> UiKit.pct(h.globalScore())),
            col("Duración", h -> (h.durationSeconds() / 60) + " min"));

    public StudentProgressPanel(AppContext ctx) {
        super(new BorderLayout(6, 6));
        this.ctx = ctx;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton refresh = new JButton("Actualizar");
        refresh.addActionListener(e -> reload());
        JPanel header = new JPanel(new BorderLayout());
        header.add(lblSummary, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(refresh);
        header.add(right, BorderLayout.EAST);

        lblStrengths.setForeground(UiKit.OK);
        lblWeaknesses.setForeground(UiKit.BAD);
        JPanel sw = new JPanel(new GridLayout(2, 1));
        sw.add(lblStrengths);
        sw.add(lblWeaknesses);

        // Gráfica y tabla lado a lado: así ninguna se queda sin altura
        JPanel statsBody = new JPanel(new GridLayout(1, 2, 8, 0));
        statsBody.add(new JScrollPane(chart));
        statsBody.add(new JScrollPane(new JTable(statsModel)));
        JPanel statsPanel = new JPanel(new BorderLayout(4, 4));
        statsPanel.add(sw, BorderLayout.NORTH);
        statsPanel.add(statsBody, BorderLayout.CENTER);

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(new JLabel("Historial de simulacros"), BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(new JTable(historyModel)), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statsPanel, historyPanel);
        split.setResizeWeight(0.6);
        add(header, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        ctx.simulations().addObserver(this);
        reload();
    }

    @Override
    public void update(Subject source, String event, Object data) {
        if (SimulationController.FINISHED.equals(event)) {
            // El tracking-service se actualiza por evento: se consulta tras un breve retraso
            Timer t = new Timer(2500, e -> reload());
            t.setRepeats(false);
            t.start();
        }
    }

    private void reload() {
        Async.run(ctx.tracking()::myStats, this::showStats, err -> UiKit.showError(this, err));
        Async.run(() -> ctx.tracking().myHistory(0), page -> historyModel.setRows(page.content()),
                err -> UiKit.showError(this, err));
    }

    private void showStats(Dtos.StudentStats s) {
        lblSummary.setText(String.format("<html><b>%d simulacro(s)</b> · Promedio %.1f · Mejor %.1f · Último %.1f · "
                        + "Tendencia: %s</html>", s.simulationsCount(), s.averageGlobalScore(), s.bestGlobalScore(),
                s.lastGlobalScore(), TRENDS.getOrDefault(s.trend(), s.trend())));
        lblStrengths.setText("Fortalezas: " + (s.strengths().isEmpty() ? "aún no identificadas" : String.join(", ", s.strengths())));
        lblWeaknesses.setText("Debilidades: " + (s.weaknesses().isEmpty() ? "ninguna por ahora" : String.join(", ", s.weaknesses())));
        chart.setBars(s.competences().stream()
                .map(c -> new BarChart.Bar(c.competence(), c.averageScore(), null)).toList());
        statsModel.setRows(s.competences());
    }

    static String label(String classification) {
        return switch (classification == null ? "" : classification) {
            case "FORTALEZA" -> "Fortaleza";
            case "DEBILIDAD" -> "Debilidad";
            default -> "En desarrollo";
        };
    }
}

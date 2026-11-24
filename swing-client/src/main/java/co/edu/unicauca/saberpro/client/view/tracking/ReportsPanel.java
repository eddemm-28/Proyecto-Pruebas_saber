package co.edu.unicauca.saberpro.client.view.tracking;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.infra.Async;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.view.BarChart;
import co.edu.unicauca.saberpro.client.view.UiKit;
import co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel.col;

/** B16 / RF-28: reportes de rendimiento para Docentes y Coordinadores. */
public class ReportsPanel extends JPanel {

    private final AppContext ctx;
    private final JTextField txtFrom = new JTextField(10);
    private final JTextField txtTo = new JTextField(10);
    private final BarChart competenceChart = new BarChart();

    private final ListTableModel<Dtos.CompetenceReportRow> competenceModel = new ListTableModel<>(
            col("Competencia", Dtos.CompetenceReportRow::competence),
            col("Estudiantes", Dtos.CompetenceReportRow::students),
            col("Simulacros", Dtos.CompetenceReportRow::simulations),
            col("Preguntas", Dtos.CompetenceReportRow::totalQuestions),
            col("Aciertos", Dtos.CompetenceReportRow::correctAnswers),
            col("Promedio", r -> UiKit.pct(r.averageScore())));
    private final ListTableModel<Dtos.StudentReportRow> studentModel = new ListTableModel<>(
            col("Estudiante", Dtos.StudentReportRow::studentLogin),
            col("Simulacros", Dtos.StudentReportRow::simulations),
            col("Promedio", r -> UiKit.pct(r.averageScore())),
            col("Mejor", r -> UiKit.pct(r.bestScore())),
            col("Último simulacro", r -> UiKit.format(r.lastFinishedAt())));
    private final ListTableModel<Dtos.TopicReportRow> topicModel = new ListTableModel<>(
            col("Competencia", Dtos.TopicReportRow::competence),
            col("Tema", Dtos.TopicReportRow::topic),
            col("Respuestas", Dtos.TopicReportRow::totalAnswers),
            col("Aciertos", Dtos.TopicReportRow::correctAnswers),
            col("Tasa de acierto", r -> UiKit.pct(r.accuracy())));
    private final JTable studentTable = new JTable(studentModel);

    public ReportsPanel(AppContext ctx) {
        super(new BorderLayout(6, 6));
        this.ctx = ctx;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton apply = new JButton("Generar reportes");
        apply.addActionListener(e -> reload());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Desde (AAAA-MM-DD):"));
        filters.add(txtFrom);
        filters.add(new JLabel("Hasta:"));
        filters.add(txtTo);
        filters.add(apply);

        JPanel byCompetence = new JPanel(new java.awt.GridLayout(2, 1, 0, 8));
        byCompetence.add(new JScrollPane(competenceChart));
        byCompetence.add(new JScrollPane(new JTable(competenceModel)));

        JButton detail = new JButton("Ver fortalezas y debilidades del estudiante");
        detail.addActionListener(e -> showStudentDetail());
        JPanel byStudent = new JPanel(new BorderLayout(4, 4));
        byStudent.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        byStudent.add(detail, BorderLayout.SOUTH);

        JPanel byTopic = new JPanel(new BorderLayout());
        JLabel hint = new JLabel("Temas con menor tasa de acierto primero: candidatos a refuerzo.");
        hint.setForeground(UiKit.MUTED);
        byTopic.add(hint, BorderLayout.NORTH);
        byTopic.add(new JScrollPane(new JTable(topicModel)), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Por competencia", byCompetence);
        tabs.addTab("Por estudiante", byStudent);
        tabs.addTab("Temas débiles", byTopic);

        add(filters, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        reload();
    }

    private void reload() {
        LocalDate from;
        LocalDate to;
        try {
            from = txtFrom.getText().isBlank() ? null : LocalDate.parse(txtFrom.getText().trim());
            to = txtTo.getText().isBlank() ? null : LocalDate.parse(txtTo.getText().trim());
        } catch (DateTimeParseException e) {
            UiKit.showInfo(this, "Use el formato de fecha AAAA-MM-DD.");
            return;
        }
        LocalDate f = from;
        LocalDate t = to;
        Async.run(() -> ctx.tracking().competenceReport(f, t), rows -> {
            competenceModel.setRows(rows);
            competenceChart.setBars(rows.stream().map(r -> new BarChart.Bar(r.competence(), r.averageScore(), null)).toList());
        }, err -> UiKit.showError(this, err));
        Async.run(() -> ctx.tracking().studentReport(f, t), studentModel::setRows, err -> UiKit.showError(this, err));
        Async.run(() -> ctx.tracking().topicReport(f, t), topicModel::setRows, err -> UiKit.showError(this, err));
    }

    private void showStudentDetail() {
        int r = studentTable.getSelectedRow();
        Dtos.StudentReportRow row = r < 0 ? null : studentModel.rowAt(studentTable.convertRowIndexToModel(r));
        if (row == null) return;
        Async.run(() -> ctx.tracking().studentStats(row.studentId()), s -> JOptionPane.showMessageDialog(this,
                String.format("%s%nSimulacros: %d · Promedio: %.1f%nFortalezas: %s%nDebilidades: %s",
                        row.studentLogin(), s.simulationsCount(), s.averageGlobalScore(),
                        s.strengths().isEmpty() ? "—" : String.join(", ", s.strengths()),
                        s.weaknesses().isEmpty() ? "—" : String.join(", ", s.weaknesses())),
                "Detalle del estudiante", JOptionPane.INFORMATION_MESSAGE), err -> UiKit.showError(this, err));
    }
}

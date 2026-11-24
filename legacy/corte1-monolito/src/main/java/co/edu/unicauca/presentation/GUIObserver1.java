package co.edu.unicauca.presentation;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionService;
import co.edu.unicauca.infra.Observer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

/**
 * Vista observadora que muestra la información estadística de cuántas preguntas hay por cada estado.
 */
public class GUIObserver1 extends JFrame implements Observer {
    private final QuestionService questionService;

    private JLabel lblBorradorCount;
    private JLabel lblPendienteCount;
    private JLabel lblEliminadaCount;
    private JLabel lblTotalCount;

    public GUIObserver1(QuestionService questionService) {
        this.questionService = questionService;
        this.questionService.addObserver(this);
        initComponents();
        updateDisplay();
    }

    private void initComponents() {
        setTitle("VISTA DE ESTADÍSTICAS");
        setSize(320, 260);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout(10, 10));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel statsPanel = new JPanel(new GridLayout(4, 2, 8, 12));
        statsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Preguntas por estado",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 13)));

        JLabel lblBorrador = new JLabel("Borrador:");
        lblBorrador.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblBorradorCount = new JLabel("0");
        lblBorradorCount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblBorradorCount.setForeground(new Color(0, 102, 204));

        JLabel lblPendiente = new JLabel("Pendiente de revisión:");
        lblPendiente.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblPendienteCount = new JLabel("0");
        lblPendienteCount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPendienteCount.setForeground(new Color(230, 138, 0));

        JLabel lblEliminada = new JLabel("Eliminada:");
        lblEliminada.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblEliminadaCount = new JLabel("0");
        lblEliminadaCount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEliminadaCount.setForeground(new Color(204, 0, 0));

        JLabel lblTotal = new JLabel("Total preguntas:");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotalCount = new JLabel("0");
        lblTotalCount.setFont(new Font("Segoe UI", Font.BOLD, 14));

        statsPanel.add(lblBorrador);
        statsPanel.add(lblBorradorCount);

        statsPanel.add(lblPendiente);
        statsPanel.add(lblPendienteCount);

        statsPanel.add(lblEliminada);
        statsPanel.add(lblEliminadaCount);

        statsPanel.add(lblTotal);
        statsPanel.add(lblTotalCount);

        contentPanel.add(statsPanel, BorderLayout.CENTER);
        add(contentPanel);
    }

    private void updateDisplay() {
        Map<String, Integer> counts = questionService.getQuestionCountByState();
        int borrador = counts.getOrDefault(Question.STATE_BORRADOR, 0);
        int pendiente = counts.getOrDefault(Question.STATE_PENDIENTE_REVISION, 0);
        int eliminada = counts.getOrDefault(Question.STATE_ELIMINADA, 0);
        int total = questionService.getTotalQuestionsCount();

        lblBorradorCount.setText(String.valueOf(borrador));
        lblPendienteCount.setText(String.valueOf(pendiente));
        lblEliminadaCount.setText(String.valueOf(eliminada));
        lblTotalCount.setText(String.valueOf(total));
    }

    @Override
    public void update(Object obj) {
        SwingUtilities.invokeLater(this::updateDisplay);
    }
}

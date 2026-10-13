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
 * Vista observadora que muestra una gráfica en pastel del porcentaje de preguntas
 * en estado Borrador, Pendiente de revisión y Eliminada.
 */
public class GUIObserver2 extends JFrame implements Observer {
    private final QuestionService questionService;
    private PieChartPanel pieChartPanel;
    private JLabel lblLegendBorrador;
    private JLabel lblLegendPendiente;
    private JLabel lblLegendEliminada;

    public GUIObserver2(QuestionService questionService) {
        this.questionService = questionService;
        this.questionService.addObserver(this);
        initComponents();
        updateDisplay();
    }

    private void initComponents() {
        setTitle("VISTA GRÁFICA");
        setSize(380, 380);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Distribución de preguntas",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 13)));

        pieChartPanel = new PieChartPanel();
        container.add(pieChartPanel, BorderLayout.CENTER);

        // Panel de Leyenda de porcentajes
        JPanel legendPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        legendPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        lblLegendBorrador = new JLabel("Borrador: 0%");
        lblLegendBorrador.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLegendBorrador.setForeground(new Color(37, 99, 235));

        lblLegendPendiente = new JLabel("Pendiente revisión: 0%");
        lblLegendPendiente.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLegendPendiente.setForeground(new Color(217, 119, 6));

        lblLegendEliminada = new JLabel("Eliminada: 0%");
        lblLegendEliminada.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLegendEliminada.setForeground(new Color(220, 38, 38));

        legendPanel.add(lblLegendBorrador);
        legendPanel.add(lblLegendPendiente);
        legendPanel.add(lblLegendEliminada);

        container.add(legendPanel, BorderLayout.SOUTH);
        contentPanel.add(container, BorderLayout.CENTER);
        add(contentPanel);
    }

    private void updateDisplay() {
        Map<String, Double> percentages = questionService.getQuestionPercentageByState();
        double pctBorrador = percentages.getOrDefault(Question.STATE_BORRADOR, 0.0);
        double pctPendiente = percentages.getOrDefault(Question.STATE_PENDIENTE_REVISION, 0.0);
        double pctEliminada = percentages.getOrDefault(Question.STATE_ELIMINADA, 0.0);

        pieChartPanel.setPercentages(pctBorrador, pctPendiente, pctEliminada);
        lblLegendBorrador.setText(String.format("Borrador: %.1f%%", pctBorrador));
        lblLegendPendiente.setText(String.format("Pendiente revisión: %.1f%%", pctPendiente));
        lblLegendEliminada.setText(String.format("Eliminada: %.1f%%", pctEliminada));
    }

    @Override
    public void update(Object obj) {
        SwingUtilities.invokeLater(this::updateDisplay);
    }

    /**
     * Componente Swing personalizado para renderizar la gráfica circular (Pie Chart).
     */
    private static class PieChartPanel extends JPanel {
        private double pctBorrador = 0.0;
        private double pctPendiente = 0.0;
        private double pctEliminada = 0.0;

        public void setPercentages(double borrador, double pendiente, double eliminada) {
            this.pctBorrador = borrador;
            this.pctPendiente = pendiente;
            this.pctEliminada = eliminada;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int diameter = Math.min(width, height) - 40;
            int x = (width - diameter) / 2;
            int y = (height - diameter) / 2;

            double totalPct = pctBorrador + pctPendiente + pctEliminada;
            if (totalPct <= 0) {
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillOval(x, y, diameter, diameter);
                g2.dispose();
                return;
            }

            int angleBorrador = (int) Math.round((pctBorrador / 100.0) * 360.0);
            int anglePendiente = (int) Math.round((pctPendiente / 100.0) * 360.0);
            int angleEliminada = 360 - angleBorrador - anglePendiente;

            int currentAngle = 90;

            // Borrador (Azul)
            g2.setColor(new Color(37, 99, 235));
            g2.fillArc(x, y, diameter, diameter, currentAngle, angleBorrador);
            currentAngle += angleBorrador;

            // Pendiente de revisión (Naranja/Ámbar)
            g2.setColor(new Color(245, 158, 11));
            g2.fillArc(x, y, diameter, diameter, currentAngle, anglePendiente);
            currentAngle += anglePendiente;

            // Eliminada (Rojo)
            g2.setColor(new Color(239, 68, 68));
            g2.fillArc(x, y, diameter, diameter, currentAngle, angleEliminada);

            // Borde exterior
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval(x, y, diameter, diameter);

            g2.dispose();
        }
    }
}

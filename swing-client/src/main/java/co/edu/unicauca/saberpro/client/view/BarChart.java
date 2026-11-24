package co.edu.unicauca.saberpro.client.view;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Gráfica de barras horizontales 0–100 % hecha con Java2D (sin librerías externas).
 * Marca con líneas los umbrales de debilidad (50 %) y fortaleza (70 %).
 */
public class BarChart extends JComponent {

    public record Bar(String label, double value, Color color) {
    }

    private List<Bar> bars = new ArrayList<>();
    private double low = 50;
    private double high = 70;

    public BarChart() {
        setPreferredSize(new Dimension(420, 220));
    }

    public void setBars(List<Bar> bars) {
        this.bars = bars == null ? List.of() : List.copyOf(bars);
        int h = Math.max(120, 40 + this.bars.size() * 30);
        setPreferredSize(new Dimension(420, h));
        revalidate();
        repaint();
    }

    public void setThresholds(double low, double high) {
        this.low = low;
        this.high = high;
        repaint();
    }

    public static Color colorFor(double v) {
        return v >= 70 ? UiKit.OK : v < 50 ? UiKit.BAD : UiKit.WARN;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground() == null ? Color.WHITE : getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        if (bars.isEmpty()) {
            g.setColor(UiKit.MUTED);
            g.drawString("Sin datos todavía", 12, 24);
            g.dispose();
            return;
        }
        FontMetrics fm = g.getFontMetrics();
        int labelW = Math.min(240, bars.stream().mapToInt(b -> fm.stringWidth(b.label())).max().orElse(80) + 12);
        int left = labelW + 8;
        int right = getWidth() - 56;
        int width = Math.max(20, right - left);
        int y = 14;

        for (Bar b : bars) {
            g.setColor(Color.DARK_GRAY);
            String label = b.label();
            while (fm.stringWidth(label) > labelW - 4 && label.length() > 4) {
                label = label.substring(0, label.length() - 2);
            }
            g.drawString(label, 6, y + 14);
            g.setColor(new Color(0xEEEEEE));
            g.fillRoundRect(left, y, width, 18, 6, 6);
            int w = (int) Math.round(width * Math.max(0, Math.min(100, b.value())) / 100.0);
            g.setColor(b.color() == null ? colorFor(b.value()) : b.color());
            g.fillRoundRect(left, y, w, 18, 6, 6);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.format("%.1f%%", b.value()), right + 6, y + 14);
            y += 30;
        }
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        for (double t : new double[]{low, high}) {
            int x = left + (int) Math.round(width * t / 100.0);
            g.setColor(t == low ? UiKit.BAD : UiKit.OK);
            g.drawLine(x, 6, x, y - 6);
        }
        g.dispose();
    }
}

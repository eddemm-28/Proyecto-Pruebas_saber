package co.edu.unicauca.saberpro.client.view;

import co.edu.unicauca.saberpro.client.infra.ApiException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Utilidades visuales comunes. */
public final class UiKit {

    public static final Color PRIMARY = new Color(0x1F4E79);
    public static final Color OK = new Color(0x2E7D32);
    public static final Color WARN = new Color(0xB26A00);
    public static final Color BAD = new Color(0xC62828);
    public static final Color MUTED = new Color(0x666666);

    /** Colores por estado (extiende EstadoColores del Corte 1 a los 7 estados). */
    public static final Map<String, Color> STATE_COLORS = Map.of(
            "BORRADOR", new Color(0x1565C0),
            "PENDIENTE_REVISION", new Color(0xB26A00),
            "EN_REVISION", new Color(0x6A1B9A),
            "APROBADA", new Color(0x2E7D32),
            "RECHAZADA", new Color(0xC62828),
            "PUBLICADA", new Color(0x00695C),
            "ARCHIVADA", new Color(0x757575));

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private UiKit() {
    }

    public static String format(Instant i) {
        return i == null ? "" : DATE_TIME.format(i);
    }

    public static String pct(double v) {
        return String.format("%.1f %%", v);
    }

    public static void showError(Component parent, ApiException e) {
        JOptionPane.showMessageDialog(parent, e.userMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        l.setForeground(PRIMARY);
        return l;
    }

    public static JComponent padded(JComponent c) {
        c.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return c;
    }

    /** Formulario de dos columnas: etiqueta | campo. */
    public static JPanel form(Object... labelsAndFields) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        for (int i = 0; i < labelsAndFields.length; i += 2) {
            c.gridy = i / 2;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            Object label = labelsAndFields[i];
            p.add(label instanceof Component comp ? comp : new JLabel(String.valueOf(label)), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            p.add((Component) labelsAndFields[i + 1], c);
        }
        return p;
    }

    /** Renderer que colorea la celda del estado. */
    public static DefaultTableCellRenderer stateRenderer(Function<Object, String> keyOf) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                           int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                Color color = STATE_COLORS.get(keyOf.apply(value));
                if (!selected && color != null) {
                    c.setForeground(color);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        };
    }

    /** Modelo de tabla de solo lectura a partir de una lista de objetos y columnas. */
    public static final class ListTableModel<T> extends AbstractTableModel {

        public record Column<T>(String name, Function<T, Object> value) {
        }

        private final List<Column<T>> columns;
        private final List<T> rows = new ArrayList<>();

        @SafeVarargs
        public ListTableModel(Column<T>... columns) {
            this.columns = List.of(columns);
        }

        public static <T> Column<T> col(String name, Function<T, Object> value) {
            return new Column<>(name, value);
        }

        public void setRows(List<T> data) {
            rows.clear();
            if (data != null) rows.addAll(data);
            fireTableDataChanged();
        }

        public T rowAt(int index) {
            return index < 0 || index >= rows.size() ? null : rows.get(index);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.size(); }
        @Override public String getColumnName(int column) { return columns.get(column).name(); }
        @Override public Object getValueAt(int row, int column) { return columns.get(column).value().apply(rows.get(row)); }
    }
}

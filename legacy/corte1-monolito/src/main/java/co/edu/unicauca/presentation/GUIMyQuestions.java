package co.edu.unicauca.presentation;

import co.edu.unicauca.controller.QuestionController;
import co.edu.unicauca.domain.PageResult;
import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionFilter;
import co.edu.unicauca.infra.Observer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * HU2 y HU3: listado de las preguntas del autor con filtros, paginación y estados con color.
 * Desde aquí también puede enviar un borrador a revisión. Se refresca solo (Observer).
 */
public class GUIMyQuestions extends JFrame implements Observer {

    private static final String TODOS = "Todos";
    private static final int COL_ESTADO = 5;

    private final QuestionController controller;

    private JComboBox<String> cbEstado;
    private JComboBox<String> cbDificultad;
    private JTextField txtTema;
    private JTextField txtCompetencia;
    private JTextField txtBuscar;

    private DefaultTableModel tableModel;
    private JTable table;
    private final List<Question> paginaActual = new ArrayList<>();

    private JButton btnAnterior;
    private JButton btnSiguiente;
    private JLabel lblPagina;
    private JComboBox<Integer> cbTamano;

    private int pagina = 1;

    public GUIMyQuestions(QuestionController controller) {
        this.controller = controller;
        initComponents();
        controller.addObserver(this);
        cargar();
    }

    private void initComponents() {
        setTitle("Mis preguntas - " + controller.getAuthor().getFullName());
        setSize(900, 560);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // ---- Filtros (arriba)
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));

        cbEstado = new JComboBox<>(new String[]{TODOS, Question.STATE_BORRADOR,
                Question.STATE_PENDIENTE_REVISION, Question.STATE_ELIMINADA});
        cbDificultad = new JComboBox<>(new String[]{TODOS, "Fácil", "Media", "Difícil"});
        txtTema = new JTextField(10);
        txtCompetencia = new JTextField(10);
        txtBuscar = new JTextField(12);

        filtros.add(new JLabel("Estado:"));
        filtros.add(cbEstado);
        filtros.add(new JLabel("Dificultad:"));
        filtros.add(cbDificultad);
        filtros.add(new JLabel("Tema:"));
        filtros.add(txtTema);
        filtros.add(new JLabel("Competencia:"));
        filtros.add(txtCompetencia);
        filtros.add(new JLabel("Buscar:"));
        filtros.add(txtBuscar);

        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> {
            pagina = 1;
            cargar();
        });
        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> limpiarFiltros());
        filtros.add(btnBuscar);
        filtros.add(btnLimpiar);

        // ---- Tabla (centro)
        String[] columnas = {"Id", "Título", "Tema", "Competencia", "Dificultad", "Estado"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(new EstadoRenderer());
        table.getColumnModel().getColumn(0).setMaxWidth(80);

        // ---- Botones y paginación (abajo)
        JButton btnNueva = new JButton("Nueva pregunta");
        btnNueva.addActionListener(e -> new GUIQuestionForm(controller).setVisible(true));

        JButton btnVerEditar = new JButton("Ver / Editar");
        btnVerEditar.addActionListener(e -> abrirSeleccionada());

        JButton btnEnviar = new JButton("Enviar a revisión");
        btnEnviar.setBackground(new Color(217, 119, 6));
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.addActionListener(e -> enviarARevision());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(btnNueva);
        acciones.add(btnVerEditar);
        acciones.add(btnEnviar);

        btnAnterior = new JButton("< Anterior");
        btnAnterior.addActionListener(e -> {
            pagina--;
            cargar();
        });
        btnSiguiente = new JButton("Siguiente >");
        btnSiguiente.addActionListener(e -> {
            pagina++;
            cargar();
        });
        lblPagina = new JLabel("Página 1 de 1");
        cbTamano = new JComboBox<>(new Integer[]{5, 10, 20});
        cbTamano.setSelectedItem(10);
        cbTamano.addActionListener(e -> {
            pagina = 1;
            cargar();
        });

        JPanel paginacion = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        paginacion.add(new JLabel("Filas:"));
        paginacion.add(cbTamano);
        paginacion.add(btnAnterior);
        paginacion.add(lblPagina);
        paginacion.add(btnSiguiente);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(acciones, BorderLayout.WEST);
        sur.add(paginacion, BorderLayout.EAST);

        JPanel contenedor = new JPanel(new BorderLayout(8, 8));
        contenedor.setBorder(new EmptyBorder(10, 10, 10, 10));
        contenedor.add(filtros, BorderLayout.NORTH);
        contenedor.add(new JScrollPane(table), BorderLayout.CENTER);
        contenedor.add(sur, BorderLayout.SOUTH);
        add(contenedor);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                controller.removeObserver(GUIMyQuestions.this);
            }
        });
    }

    private QuestionFilter leerFiltro() {
        QuestionFilter f = new QuestionFilter();
        if (!TODOS.equals(cbEstado.getSelectedItem())) {
            f.setEstado((String) cbEstado.getSelectedItem());
        }
        if (!TODOS.equals(cbDificultad.getSelectedItem())) {
            f.setDificultad((String) cbDificultad.getSelectedItem());
        }
        f.setTema(txtTema.getText());
        f.setCompetencia(txtCompetencia.getText());
        f.setTexto(txtBuscar.getText());
        return f;
    }

    private void limpiarFiltros() {
        cbEstado.setSelectedIndex(0);
        cbDificultad.setSelectedIndex(0);
        txtTema.setText("");
        txtCompetencia.setText("");
        txtBuscar.setText("");
        pagina = 1;
        cargar();
    }

    private void cargar() {
        int tamano = (Integer) cbTamano.getSelectedItem();
        PageResult<Question> resultado = controller.listarMisPreguntas(leerFiltro(), pagina, tamano);
        pagina = resultado.getPage();

        tableModel.setRowCount(0);
        paginaActual.clear();
        for (Question q : resultado.getItems()) {
            paginaActual.add(q);
            tableModel.addRow(new Object[]{q.getId(), q.getNombre(), q.getTema(),
                    q.getCompetencia(), q.getDificultad(), q.getEstado()});
        }

        lblPagina.setText("Página " + resultado.getPage() + " de " + resultado.getTotalPages()
                + " (" + resultado.getTotalItems() + " preguntas)");
        btnAnterior.setEnabled(resultado.hasPrevious());
        btnSiguiente.setEnabled(resultado.hasNext());
    }

    private Question preguntaSeleccionada() {
        int fila = table.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta de la tabla.",
                    "Atención", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return paginaActual.get(fila);
    }

    private void abrirSeleccionada() {
        Question q = preguntaSeleccionada();
        if (q != null) {
            boolean editable = Question.STATE_BORRADOR.equals(q.getEstado());
            new GUIQuestionForm(controller, q, editable).setVisible(true);
        }
    }

    private void enviarARevision() {
        Question q = preguntaSeleccionada();
        if (q == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "¿Enviar la pregunta " + q.getId() + " a revisión? Ya no podrá editarla.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.enviarARevision(q.getId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo enviar", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Observer: el servicio avisa cuando cambia algo y se vuelve a cargar la tabla. */
    @Override
    public void update(Object obj) {
        cargar();
    }

    /** Pinta la celda del estado con el color correspondiente. */
    private static class EstadoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(CENTER);
            setFont(getFont().deriveFont(Font.BOLD));
            setBackground(EstadoColores.colorDe((String) value));
            setForeground(Color.WHITE);
            return this;
        }
    }
}

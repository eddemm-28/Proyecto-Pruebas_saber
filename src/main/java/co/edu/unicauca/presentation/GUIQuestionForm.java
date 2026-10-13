package co.edu.unicauca.presentation;

import co.edu.unicauca.controller.QuestionController;
import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionDistractors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * HU1: formulario para crear (o editar) una pregunta de selección múltiple con única respuesta.
 * Si la pregunta no es editable (no está en borrador) se muestra en modo solo lectura.
 */
public class GUIQuestionForm extends JFrame {

    private final QuestionController controller;
    private final Question existing;   // null si es una pregunta nueva
    private final boolean editable;

    private JTextField txtTitulo;
    private JTextArea txtContexto;
    private JTextArea txtPregunta;
    private JTextField txtOpcionA, txtOpcionB, txtOpcionC, txtOpcionD;
    private JComboBox<String> cbRespuesta;
    private JTextArea txtJustificacion;
    private JTextArea txtBibliografia;
    private JTextField txtCompetencia;
    private JTextField txtTema;
    private JTextField txtSubtema;
    private JComboBox<String> cbDificultad;

    public GUIQuestionForm(QuestionController controller) {
        this(controller, null, true);
    }

    public GUIQuestionForm(QuestionController controller, Question existing, boolean editable) {
        this.controller = controller;
        this.existing = existing;
        this.editable = editable;
        initComponents();
        if (existing != null) {
            cargarPregunta(existing);
        }
    }

    private void initComponents() {
        String titulo = existing == null ? "Nueva pregunta"
                : (editable ? "Editar pregunta " + existing.getId() : "Ver pregunta " + existing.getId());
        setTitle(titulo);
        setSize(620, 760);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int y = 0;
        txtTitulo = new JTextField();
        y = addRow(panel, gbc, y, "Título:", txtTitulo);

        txtContexto = new JTextArea(4, 30);
        y = addRow(panel, gbc, y, "Contexto:", areaConScroll(txtContexto));

        txtPregunta = new JTextArea(3, 30);
        y = addRow(panel, gbc, y, "Pregunta directa:", areaConScroll(txtPregunta));

        txtOpcionA = new JTextField();
        y = addRow(panel, gbc, y, "Opción A:", txtOpcionA);
        txtOpcionB = new JTextField();
        y = addRow(panel, gbc, y, "Opción B:", txtOpcionB);
        txtOpcionC = new JTextField();
        y = addRow(panel, gbc, y, "Opción C:", txtOpcionC);
        txtOpcionD = new JTextField();
        y = addRow(panel, gbc, y, "Opción D:", txtOpcionD);

        cbRespuesta = new JComboBox<>(new String[]{"A", "B", "C", "D"});
        y = addRow(panel, gbc, y, "Respuesta correcta:", cbRespuesta);

        txtJustificacion = new JTextArea(3, 30);
        y = addRow(panel, gbc, y, "Justificación:", areaConScroll(txtJustificacion));

        txtBibliografia = new JTextArea(2, 30);
        y = addRow(panel, gbc, y, "Bibliografía:", areaConScroll(txtBibliografia));

        txtCompetencia = new JTextField();
        y = addRow(panel, gbc, y, "Competencia:", txtCompetencia);
        txtTema = new JTextField();
        y = addRow(panel, gbc, y, "Tema:", txtTema);
        txtSubtema = new JTextField();
        y = addRow(panel, gbc, y, "Subtema:", txtSubtema);

        cbDificultad = new JComboBox<>(Question.DIFICULTADES);
        y = addRow(panel, gbc, y, "Nivel de dificultad:", cbDificultad);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCerrar = new JButton(editable ? "Cancelar" : "Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        botones.add(btnCerrar);

        if (editable) {
            JButton btnGuardar = new JButton("Guardar");
            btnGuardar.setBackground(new Color(40, 167, 69));
            btnGuardar.setForeground(Color.WHITE);
            btnGuardar.addActionListener(e -> guardar());
            botones.add(btnGuardar);
        } else {
            setCamposEditables(false);
        }

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 2;
        panel.add(botones, gbc);

        add(new JScrollPane(panel));
    }

    private int addRow(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent campo) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0.25;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.75;
        panel.add(campo, gbc);
        return y + 1;
    }

    private JScrollPane areaConScroll(JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return new JScrollPane(area);
    }

    private void setCamposEditables(boolean valor) {
        for (JTextComponent c : new JTextComponent[]{txtTitulo, txtContexto, txtPregunta, txtOpcionA, txtOpcionB,
                txtOpcionC, txtOpcionD, txtJustificacion, txtBibliografia, txtCompetencia, txtTema, txtSubtema}) {
            c.setEditable(valor);
        }
        cbRespuesta.setEnabled(valor);
        cbDificultad.setEnabled(valor);
    }

    private void cargarPregunta(Question q) {
        txtTitulo.setText(q.getNombre());
        txtContexto.setText(q.getContexto());
        txtPregunta.setText(q.getPregunta());
        if (q.getDistractors() != null) {
            txtOpcionA.setText(q.getDistractors().getOptionA());
            txtOpcionB.setText(q.getDistractors().getOptionB());
            txtOpcionC.setText(q.getDistractors().getOptionC());
            txtOpcionD.setText(q.getDistractors().getOptionD());
        }
        cbRespuesta.setSelectedItem(q.getRespuestaCorrecta());
        txtJustificacion.setText(q.getJustificacion());
        txtBibliografia.setText(q.getBibliografia());
        txtCompetencia.setText(q.getCompetencia());
        txtTema.setText(q.getTema());
        txtSubtema.setText(q.getSubtema());
        cbDificultad.setSelectedItem(q.getDificultad());
    }

    private Question leerFormulario() {
        Question q = (existing != null) ? existing : new Question();
        q.setNombre(txtTitulo.getText());
        q.setContexto(txtContexto.getText());
        q.setPregunta(txtPregunta.getText());
        q.setDistractors(new QuestionDistractors(txtOpcionA.getText(), txtOpcionB.getText(),
                txtOpcionC.getText(), txtOpcionD.getText()));
        q.setRespuestaCorrecta((String) cbRespuesta.getSelectedItem());
        q.setJustificacion(txtJustificacion.getText());
        q.setBibliografia(txtBibliografia.getText());
        q.setCompetencia(txtCompetencia.getText());
        q.setTema(txtTema.getText());
        q.setSubtema(txtSubtema.getText());
        q.setDificultad((String) cbDificultad.getSelectedItem());
        return q;
    }

    private void guardar() {
        try {
            Question q = leerFormulario();
            if (existing == null) {
                Question creada = controller.crear(q);
                JOptionPane.showMessageDialog(this, "Pregunta " + creada.getId() + " guardada como Borrador.",
                        "Pregunta creada", JOptionPane.INFORMATION_MESSAGE);
            } else {
                controller.actualizar(q);
                JOptionPane.showMessageDialog(this, "Pregunta actualizada.",
                        "Pregunta actualizada", JOptionPane.INFORMATION_MESSAGE);
            }
            dispose();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo guardar", JOptionPane.WARNING_MESSAGE);
        }
    }
}

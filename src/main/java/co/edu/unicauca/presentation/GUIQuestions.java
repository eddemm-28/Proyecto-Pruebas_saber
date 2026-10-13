package co.edu.unicauca.presentation;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Ventana principal de la interfaz gráfica para la gestión de preguntas del Banco Saber PRO.
 */
public class GUIQuestions extends JFrame {
    private final QuestionService questionService;

    private JComboBox<Question> cbQuestions;
    private JButton btnCargar;

    private JLabel lblIdValue;
    private JLabel lblNombreValue;
    private JTextArea txtPreguntaValue;
    private JTextArea txtOpcionesValue;
    private JLabel lblRespuestaCorrectaValue;
    private JLabel lblEstadoActualValue;

    private JComboBox<String> cbNuevoEstado;
    private JButton btnActualizarEstado;

    private Question currentQuestion;

    public GUIQuestions(QuestionService questionService) {
        this.questionService = questionService;
        initComponents();
        loadQuestionsToCombo();
        if (cbQuestions.getItemCount() > 0) {
            cbQuestions.setSelectedIndex(0);
            cargarPreguntaSeleccionada();
        }
    }

    private void initComponents() {
        setTitle("Banco de Preguntas Saber PRO - VENTANA PRINCIPAL");
        setSize(550, 680);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header Label
        JLabel titleLabel = new JLabel("VENTANA PRINCIPAL - GESTIÓN DE PREGUNTAS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(15));

        // --- Panel 1: SELECCIONAR PREGUNTA ---
        JPanel pnlSeleccionar = new JPanel(new GridBagLayout());
        pnlSeleccionar.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "SELECCIONAR PREGUNTA",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));
        pnlSeleccionar.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        pnlSeleccionar.add(new JLabel("Pregunta:"), gbc);

        cbQuestions = new JComboBox<>();
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        pnlSeleccionar.add(cbQuestions, gbc);

        btnCargar = new JButton("Cargar pregunta");
        btnCargar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        pnlSeleccionar.add(btnCargar, gbc);

        mainPanel.add(pnlSeleccionar);
        mainPanel.add(Box.createVerticalStrut(15));

        // --- Panel 2: FORMULARIO DE PREGUNTA ---
        JPanel pnlFormulario = new JPanel(new GridBagLayout());
        pnlFormulario.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "FORMULARIO DE PREGUNTA",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));
        pnlFormulario.setAlignmentX(Component.LEFT_ALIGNMENT);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Id
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3; gbc.gridwidth = 1;
        pnlFormulario.add(new JLabel("Id:"), gbc);
        lblIdValue = new JLabel("-");
        lblIdValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridx = 1; gbc.weightx = 0.7;
        pnlFormulario.add(lblIdValue, gbc);

        // Nombre
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        pnlFormulario.add(new JLabel("Nombre:"), gbc);
        lblNombreValue = new JLabel("-");
        lblNombreValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 1; gbc.weightx = 0.7;
        pnlFormulario.add(lblNombreValue, gbc);

        // Enunciado / Pregunta
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        pnlFormulario.add(new JLabel("Pregunta:"), gbc);
        txtPreguntaValue = new JTextArea(3, 25);
        txtPreguntaValue.setLineWrap(true);
        txtPreguntaValue.setWrapStyleWord(true);
        txtPreguntaValue.setEditable(false);
        txtPreguntaValue.setBackground(new Color(245, 245, 245));
        gbc.gridx = 1; gbc.weightx = 0.7;
        pnlFormulario.add(new JScrollPane(txtPreguntaValue), gbc);

        // Opciones
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        pnlFormulario.add(new JLabel("Opciones:"), gbc);
        txtOpcionesValue = new JTextArea(5, 25);
        txtOpcionesValue.setLineWrap(true);
        txtOpcionesValue.setWrapStyleWord(true);
        txtOpcionesValue.setEditable(false);
        txtOpcionesValue.setBackground(new Color(245, 245, 245));
        gbc.gridx = 1; gbc.weightx = 0.7;
        pnlFormulario.add(new JScrollPane(txtOpcionesValue), gbc);

        // Respuesta correcta
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        pnlFormulario.add(new JLabel("Respuesta correcta:"), gbc);
        lblRespuestaCorrectaValue = new JLabel("-");
        lblRespuestaCorrectaValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridx = 1; gbc.weightx = 0.7;
        pnlFormulario.add(lblRespuestaCorrectaValue, gbc);

        // Estado actual
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
        pnlFormulario.add(new JLabel("Estado actual:"), gbc);
        lblEstadoActualValue = new JLabel("-");
        lblEstadoActualValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEstadoActualValue.setForeground(new Color(0, 102, 204));
        gbc.gridx = 1; gbc.weightx = 0.7;
        pnlFormulario.add(lblEstadoActualValue, gbc);

        // Nuevo estado
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.3;
        pnlFormulario.add(new JLabel("Nuevo estado:"), gbc);
        cbNuevoEstado = new JComboBox<>(new String[]{
                Question.STATE_BORRADOR,
                Question.STATE_PENDIENTE_REVISION,
                Question.STATE_ELIMINADA
        });
        gbc.gridx = 1; gbc.weightx = 0.7;
        pnlFormulario.add(cbNuevoEstado, gbc);

        // Botón Actualizar Estado
        btnActualizarEstado = new JButton("Actualizar estado");
        btnActualizarEstado.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnActualizarEstado.setBackground(new Color(40, 167, 69));
        btnActualizarEstado.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 8, 8, 8);
        pnlFormulario.add(btnActualizarEstado, gbc);

        mainPanel.add(pnlFormulario);

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);

        // Action Listeners
        btnCargar.addActionListener((ActionEvent e) -> cargarPreguntaSeleccionada());
        cbQuestions.addActionListener((ActionEvent e) -> cargarPreguntaSeleccionada());
        btnActualizarEstado.addActionListener((ActionEvent e) -> actualizarEstadoPregunta());
    }

    public void loadQuestionsToCombo() {
        cbQuestions.removeAllItems();
        for (Question q : questionService.getAllQuestions()) {
            cbQuestions.addItem(q);
        }
    }

    private void cargarPreguntaSeleccionada() {
        Question selected = (Question) cbQuestions.getSelectedItem();
        if (selected != null) {
            currentQuestion = questionService.getQuestionById(selected.getId());
            if (currentQuestion != null) {
                lblIdValue.setText(currentQuestion.getId());
                lblNombreValue.setText(currentQuestion.getNombre());
                txtPreguntaValue.setText(currentQuestion.getPregunta());
                txtOpcionesValue.setText(currentQuestion.getDistractors().getFormattedText());
                lblRespuestaCorrectaValue.setText(currentQuestion.getRespuestaCorrecta());
                lblEstadoActualValue.setText(currentQuestion.getEstado());
                lblEstadoActualValue.setForeground(EstadoColores.colorDe(currentQuestion.getEstado()));
                cbNuevoEstado.setSelectedItem(currentQuestion.getEstado());
            }
        }
    }

    private void actualizarEstadoPregunta() {
        if (currentQuestion == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta válida.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nuevoEstado = (String) cbNuevoEstado.getSelectedItem();
        if (nuevoEstado != null) {
            boolean exito = questionService.updateQuestionState(currentQuestion.getId(), nuevoEstado);
            if (exito) {
                lblEstadoActualValue.setText(nuevoEstado);
                lblEstadoActualValue.setForeground(EstadoColores.colorDe(nuevoEstado));
                JOptionPane.showMessageDialog(this,
                        "Estado de la pregunta [" + currentQuestion.getId() + "] actualizado a: " + nuevoEstado,
                        "Actualización Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error al actualizar el estado de la pregunta.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

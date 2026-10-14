package co.edu.unicauca.presentation;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionService;
import co.edu.unicauca.kernel.QuestionMicrokernel;
import co.edu.unicauca.plugin.api.QuestionRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class GUIPluginGenerator extends JFrame {

    private final QuestionMicrokernel kernel;
    private final QuestionService questionService;

    private JComboBox<String> cbType;
    private JTextField txtTitle;
    private JTextArea txtContent;
    private JTextField txtOptA, txtOptB, txtOptC, txtOptD;
    private JTextField txtCorrect;
    private JTextField txtCompetence;
    private JComboBox<String> cbDifficulty;
    private JTextArea txtResult;

    public GUIPluginGenerator(QuestionMicrokernel kernel, QuestionService questionService) {
        this.kernel = kernel;
        this.questionService = questionService;
        initComponents();
    }

    private void initComponents() {
        setTitle("Generador de Preguntas por Plugin");
        setSize(560, 620);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int y = 0;

        gbc.gridx = 0; gbc.gridy = y;
        panel.add(new JLabel("Tipo:"), gbc);
        cbType = new JComboBox<>(new String[]{"MULTIPLE_CHOICE", "CASE", "MULTIMEDIA"});
        gbc.gridx = 1; panel.add(cbType, gbc);

        y++;
        gbc.gridx = 0; gbc.gridy = y; panel.add(new JLabel("Título:"), gbc);
        txtTitle = new JTextField();
        gbc.gridx = 1; panel.add(txtTitle, gbc);

        y++;
        gbc.gridx = 0; gbc.gridy = y; panel.add(new JLabel("Enunciado:"), gbc);
        txtContent = new JTextArea(3, 25);
        txtContent.setLineWrap(true);
        gbc.gridx = 1; panel.add(new JScrollPane(txtContent), gbc);

        y++;
        gbc.gridx = 0; gbc.gridy = y; panel.add(new JLabel("Opción A:"), gbc);
        txtOptA = new JTextField();
        gbc.gridx = 1; panel.add(txtOptA, gbc);

        y++;
        gbc.gridx = 0; gbc.gridy = y; panel.add(new JLabel("Opción B:"), gbc);
        txtOptB = new JTextField();
        gbc.gridx = 1; panel.add(txtOptB, gbc);

        y++;
        gbc.gridx = 0; gbc.gridy = y; panel.add(new JLabel("Opción C:"), gbc);
        txtOptC = new JTextField();
        gbc.gridx = 1; panel.add(txtOptC, gbc);

        y++;
        gbc.gridx = 0; gbc.gridy = y; panel.add(new JLabel("Opción D:"), gbc);
        txtOptD = new JTextField();
        gbc.gridx = 1; panel.add(txtOptD, gbc);

        y++;
        gbc.gridx = 0; gbc.gridy = y; panel.add(new JLabel("Respuesta correcta:"), gbc);
        txtCorrect = new JTextField();
        gbc.gridx = 1; panel.add(txtCorrect, gbc);

        y++;
        gbc.gridx = 0; gbc.gridy = y; panel.add(new JLabel("Competencia:"), gbc);
        txtCompetence = new JTextField();
        gbc.gridx = 1; panel.add(txtCompetence, gbc);

        y++;
        gbc.gridx = 0; gbc.gridy = y; panel.add(new JLabel("Dificultad:"), gbc);
        cbDifficulty = new JComboBox<>(new String[]{"Fácil", "Media", "Difícil"});
        gbc.gridx = 1; panel.add(cbDifficulty, gbc);

        y++;
        JButton btnGenerate = new JButton("Generar pregunta");
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
        panel.add(btnGenerate, gbc);

        y++;
        txtResult = new JTextArea(6, 40);
        txtResult.setEditable(false);
        gbc.gridy = y;
        panel.add(new JScrollPane(txtResult), gbc);

        btnGenerate.addActionListener(e -> onGenerate());
        add(panel);
    }

    private void onGenerate() {
        try {
            QuestionRequest req = new QuestionRequest(
                    txtTitle.getText(),
                    txtContent.getText(),
                    (String) cbType.getSelectedItem(),
                    List.of(txtOptA.getText(), txtOptB.getText(), txtOptC.getText(), txtOptD.getText()),
                    txtCorrect.getText(),
                    txtCompetence.getText(),
                    (String) cbDifficulty.getSelectedItem()
            );
            Question q = kernel.generate(req);
            // Guardar también en el banco real (SQLite) para que aparezca en "Banco de Preguntas"
            if (!questionService.saveQuestion(q)) {
                throw new IllegalStateException("La pregunta se generó pero no se pudo guardar en la base de datos.");
            }
            txtResult.setText("Pregunta generada:\n"
                    + "Id: " + q.getId() + "\n"
                    + "Título: " + q.getNombre() + "\n"
                    + "Enunciado: " + q.getPregunta() + "\n"
                    + q.getDistractors().getFormattedText() + "\n"
                    + "Respuesta: " + q.getRespuestaCorrecta() + "\n"
                    + "Total en banco: " + kernel.size());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

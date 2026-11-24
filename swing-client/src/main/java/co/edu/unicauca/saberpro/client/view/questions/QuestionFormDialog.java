package co.edu.unicauca.saberpro.client.view.questions;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.infra.Async;
import co.edu.unicauca.saberpro.client.model.Catalogs;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.view.UiKit;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Alta y edición de preguntas (RF-05, RF-06, A14). Permite crear manualmente o mediante un
 * plugin del microkernel; en ambos casos el servidor aplica las mismas validaciones (A15) y
 * devuelve todos los errores juntos.
 */
public class QuestionFormDialog extends JDialog {

    private final AppContext ctx;
    private final Dtos.Question existing;

    private final JComboBox<String> cbMode = new JComboBox<>();
    private final List<Dtos.PluginInfo> plugins = new ArrayList<>();
    private final JTextField txtTitle = new JTextField(40);
    private final JComboBox<String> cbType = new JComboBox<>(Catalogs.TYPES.values().toArray(String[]::new));
    private final JTextArea txtContext = area(4);
    private final JTextArea txtStatement = area(2);
    private final JTextField[] txtOptions = {new JTextField(40), new JTextField(40), new JTextField(40), new JTextField(40)};
    private final JRadioButton[] rbCorrect = {new JRadioButton("A"), new JRadioButton("B"), new JRadioButton("C"), new JRadioButton("D")};
    private final JTextArea txtJustification = area(2);
    private final JTextField txtBibliography = new JTextField(40);
    private final JComboBox<String> cbCompetence = new JComboBox<>(Catalogs.COMPETENCES.toArray(String[]::new));
    private final JTextField txtTopic = new JTextField(20);
    private final JTextField txtSubtopic = new JTextField(20);
    private final JComboBox<String> cbDifficulty = new JComboBox<>(Catalogs.DIFFICULTIES.values().toArray(String[]::new));
    private final JTextField txtMediaUrl = new JTextField(40);

    public QuestionFormDialog(Component parent, AppContext ctx, Dtos.Question existing) {
        super(SwingUtilities.getWindowAncestor(parent), existing == null ? "Nueva pregunta" : "Editar " + existing.code(),
                ModalityType.APPLICATION_MODAL);
        this.ctx = ctx;
        this.existing = existing;
        cbCompetence.setEditable(true);
        ButtonGroup group = new ButtonGroup();
        JPanel correct = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (JRadioButton rb : rbCorrect) {
            group.add(rb);
            correct.add(rb);
        }
        JPanel options = new JPanel(new GridLayout(4, 1, 2, 2));
        for (int i = 0; i < 4; i++) {
            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.add(new JLabel(Character.toString('A' + i) + ")"), BorderLayout.WEST);
            row.add(txtOptions[i], BorderLayout.CENTER);
            options.add(row);
        }

        cbMode.addItem("Manual");
        if (existing == null) {
            Async.run(() -> ctx.questions().plugins(), list -> {
                plugins.addAll(list);
                list.forEach(p -> cbMode.addItem("Plugin: " + p.name()));
            }, err -> { });
        } else {
            cbMode.setEnabled(false);
        }
        cbMode.addActionListener(e -> syncTypeWithPlugin());

        JLabel help = new JLabel("<html>Reglas: contexto obligatorio, una sola pregunta, 4 opciones distintas, una correcta "
                + "y sin \"todas/ninguna de las anteriores\".</html>");
        help.setForeground(UiKit.MUTED);

        JPanel form = UiKit.form(
                "Modo de creación:", cbMode,
                "Título:", txtTitle,
                "Tipo:", cbType,
                "Contexto:", new JScrollPane(txtContext),
                "Pregunta directa:", new JScrollPane(txtStatement),
                "Opciones:", options,
                "Respuesta correcta:", correct,
                "Justificación:", new JScrollPane(txtJustification),
                "Bibliografía:", txtBibliography,
                "Competencia:", cbCompetence,
                "Tema:", txtTopic,
                "Subtema:", txtSubtopic,
                "Dificultad:", cbDifficulty,
                "URL multimedia:", txtMediaUrl);

        JButton save = new JButton(existing == null ? "Crear en Borrador" : "Guardar cambios");
        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> save(save));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);

        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        content.add(help, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(form);
        scroll.setPreferredSize(new Dimension(720, 620));
        content.add(scroll, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);

        if (existing != null) {
            fill(existing);
        } else {
            rbCorrect[0].setSelected(true);
            cbDifficulty.setSelectedItem("Media");
        }
        pack();
        setLocationRelativeTo(parent);
    }

    private void fill(Dtos.Question q) {
        txtTitle.setText(q.title());
        cbType.setSelectedItem(Catalogs.label(Catalogs.TYPES, q.type()));
        txtContext.setText(q.context());
        txtStatement.setText(q.statement());
        for (int i = 0; i < 4 && i < q.options().size(); i++) {
            txtOptions[i].setText(q.options().get(i).text());
        }
        for (JRadioButton rb : rbCorrect) {
            rb.setSelected(rb.getText().equals(q.correctOption()));
        }
        txtJustification.setText(q.justification());
        txtBibliography.setText(q.bibliography());
        cbCompetence.setSelectedItem(q.competence());
        txtTopic.setText(q.topic());
        txtSubtopic.setText(q.subtopic());
        cbDifficulty.setSelectedItem(Catalogs.label(Catalogs.DIFFICULTIES, q.difficulty()));
        txtMediaUrl.setText(q.mediaUrl());
    }

    private void syncTypeWithPlugin() {
        Dtos.PluginInfo plugin = selectedPlugin();
        cbType.setEnabled(plugin == null);
        if (plugin != null) {
            cbType.setSelectedItem(Catalogs.label(Catalogs.TYPES, plugin.type()));
        }
    }

    private Dtos.PluginInfo selectedPlugin() {
        int i = cbMode.getSelectedIndex();
        return i <= 0 || i - 1 >= plugins.size() ? null : plugins.get(i - 1);
    }

    private void save(JButton button) {
        List<String> options = new ArrayList<>();
        for (JTextField t : txtOptions) options.add(t.getText());
        String correct = null;
        for (JRadioButton rb : rbCorrect) if (rb.isSelected()) correct = rb.getText();
        String type = key(Catalogs.TYPES, (String) cbType.getSelectedItem());
        String difficulty = key(Catalogs.DIFFICULTIES, (String) cbDifficulty.getSelectedItem());
        String competence = cbCompetence.getSelectedItem() == null ? null : cbCompetence.getSelectedItem().toString();
        String media = txtMediaUrl.getText().isBlank() ? null : txtMediaUrl.getText().trim();
        Dtos.PluginInfo plugin = selectedPlugin();
        String finalCorrect = correct;

        button.setEnabled(false);
        Async.run(() -> {
                    if (plugin != null) {
                        return ctx.questions().generateWithPlugin(plugin.id(), new Dtos.PluginRequest(txtTitle.getText(),
                                txtContext.getText(), txtStatement.getText(), options, finalCorrect,
                                txtJustification.getText(), txtBibliography.getText(), competence, txtTopic.getText(),
                                txtSubtopic.getText(), difficulty, media));
                    }
                    Dtos.QuestionRequest req = new Dtos.QuestionRequest(txtTitle.getText(), type, txtContext.getText(),
                            txtStatement.getText(), options, finalCorrect, txtJustification.getText(),
                            txtBibliography.getText(), competence, txtTopic.getText(), txtSubtopic.getText(), difficulty,
                            media, existing == null ? null : existing.version());
                    return existing == null ? ctx.questions().create(req) : ctx.questions().update(existing.id(), req);
                },
                q -> {
                    UiKit.showInfo(this, "Pregunta " + q.code() + " guardada en estado " + q.stateName() + ".");
                    dispose();
                },
                err -> {
                    button.setEnabled(true);
                    UiKit.showError(this, err);
                });
    }

    private static String key(Map<String, String> catalog, String label) {
        return catalog.entrySet().stream().filter(e -> e.getValue().equals(label)).map(Map.Entry::getKey)
                .findFirst().orElse(label);
    }

    private static JTextArea area(int rows) {
        JTextArea a = new JTextArea(rows, 40);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }
}

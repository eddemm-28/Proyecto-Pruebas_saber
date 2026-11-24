package co.edu.unicauca.saberpro.client.view.questions;

import co.edu.unicauca.saberpro.client.AppContext;
import co.edu.unicauca.saberpro.client.infra.Async;
import co.edu.unicauca.saberpro.client.model.Catalogs;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.view.UiKit;
import co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;

import static co.edu.unicauca.saberpro.client.view.UiKit.ListTableModel.col;

/** Vista de solo lectura con historial de cambios (RF-29) y de estados (RF-30). */
public class QuestionDetailDialog extends JDialog {

    public QuestionDetailDialog(Component parent, AppContext ctx, Dtos.Question q) {
        super(SwingUtilities.getWindowAncestor(parent), q.code() + " · " + q.stateName(), ModalityType.MODELESS);

        JEditorPane html = new JEditorPane("text/html", render(q));
        html.setEditable(false);
        html.setCaretPosition(0);

        ListTableModel<Dtos.ChangeHistory> historyModel = new ListTableModel<>(
                col("Fecha", h -> UiKit.format(h.changedAt())), col("Usuario", Dtos.ChangeHistory::changedBy),
                col("Campo", Dtos.ChangeHistory::field), col("Antes", Dtos.ChangeHistory::oldValue),
                col("Después", Dtos.ChangeHistory::newValue));
        ListTableModel<Dtos.StateAudit> auditModel = new ListTableModel<>(
                col("Fecha", a -> UiKit.format(a.changedAt())), col("Usuario", Dtos.StateAudit::changedBy),
                col("De", a -> Catalogs.label(Catalogs.STATES, a.fromState())),
                col("A", a -> Catalogs.label(Catalogs.STATES, a.toState())), col("Comentario", Dtos.StateAudit::comment));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Contenido", new JScrollPane(html));
        boolean canSeeHistory = ctx.session().hasRole("ADMINISTRADOR") || ctx.session().user().id().equals(q.authorId());
        if (canSeeHistory) {
            tabs.addTab("Historial de cambios", new JScrollPane(new JTable(historyModel)));
            tabs.addTab("Estados", new JScrollPane(new JTable(auditModel)));
            Async.run(() -> ctx.questions().history(q.id()), historyModel::setRows, err -> { });
            Async.run(() -> ctx.questions().stateAudit(q.id()), auditModel::setRows, err -> { });
        }
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabs.setPreferredSize(new Dimension(760, 560));
        setContentPane(tabs);
        pack();
        setLocationRelativeTo(parent);
    }

    static String render(Dtos.Question q) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif;padding:6px'>");
        sb.append("<h2>").append(esc(q.title())).append("</h2>");
        sb.append("<p><b>").append(esc(Catalogs.label(Catalogs.TYPES, q.type()))).append("</b> · ")
                .append(esc(q.competence())).append(" › ").append(esc(q.topic())).append(" › ").append(esc(q.subtopic()))
                .append(" · Dificultad: ").append(esc(q.difficultyName())).append(" · Autor: ").append(esc(q.authorLogin()))
                .append("</p>");
        sb.append("<p><b>Contexto</b><br>").append(esc(q.context())).append("</p>");
        if (q.mediaUrl() != null) sb.append("<p><b>Recurso:</b> ").append(esc(q.mediaUrl())).append("</p>");
        sb.append("<p><b>").append(esc(q.statement())).append("</b></p><ol type='A'>");
        for (Dtos.Option o : q.options()) {
            boolean ok = o.label().equals(q.correctOption());
            sb.append("<li>").append(ok ? "<b style='color:#2E7D32'>" : "").append(esc(o.text()))
                    .append(ok ? " ✔</b>" : "").append("</li>");
        }
        sb.append("</ol><p><b>Justificación</b><br>").append(esc(q.justification())).append("</p>");
        sb.append("<p><b>Bibliografía</b><br>").append(esc(q.bibliography())).append("</p></body></html>");
        return sb.toString();
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}

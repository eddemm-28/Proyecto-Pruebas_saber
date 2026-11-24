package co.edu.unicauca.saberpro.client.controller;

import co.edu.unicauca.saberpro.client.infra.ApiClient;
import co.edu.unicauca.saberpro.client.model.Catalogs;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.observer.Subject;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Banco de preguntas (A12–A19) desde el cliente. Es el Subject del patrón Observer: cuando
 * una pregunta cambia, todas las vistas suscritas se refrescan (como en el Corte 1).
 */
public class QuestionController extends Subject {

    public static final String QUESTIONS_CHANGED = "QUESTIONS_CHANGED";

    private final ApiClient api;

    public QuestionController(ApiClient api) {
        this.api = api;
    }

    public record Filter(String state, String competence, String topic, String difficulty, String type, String text,
                         boolean onlyMine) {
        public static Filter none(boolean onlyMine) {
            return new Filter(null, null, null, null, null, null, onlyMine);
        }
    }

    public Dtos.Page<Dtos.QuestionSummary> search(Filter f, int page, int size) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("state", f.state());
        q.put("competence", f.competence());
        q.put("topic", f.topic());
        q.put("difficulty", f.difficulty());
        q.put("type", f.type());
        q.put("q", f.text());
        q.put("scope", f.onlyMine() ? "mine" : "all");
        q.put("page", page);
        q.put("size", size);
        return api.get("/api/questions", q, new TypeReference<Dtos.Page<Dtos.QuestionSummary>>() { });
    }

    public Dtos.Question get(UUID id) {
        return api.get("/api/questions/" + id, null, Dtos.Question.class);
    }

    public Dtos.Question create(Dtos.QuestionRequest req) {
        Dtos.Question q = api.post("/api/questions", req, Dtos.Question.class);
        notifyObservers(QUESTIONS_CHANGED, q);
        return q;
    }

    public Dtos.Question update(UUID id, Dtos.QuestionRequest req) {
        Dtos.Question q = api.put("/api/questions/" + id, req, Dtos.Question.class);
        notifyObservers(QUESTIONS_CHANGED, q);
        return q;
    }

    public Dtos.Question generateWithPlugin(String pluginId, Dtos.PluginRequest req) {
        Dtos.Question q = api.post("/api/plugins/" + pluginId + "/questions", req, Dtos.Question.class);
        notifyObservers(QUESTIONS_CHANGED, q);
        return q;
    }

    /** Ejecuta una acción del State (SUBMIT, APPROVE, PUBLISH...). */
    public Dtos.Question transition(UUID id, String action, String comment) {
        String path = Catalogs.ACTION_PATHS.get(action);
        if (path == null) {
            throw new IllegalArgumentException("Acción desconocida: " + action);
        }
        Dtos.Question q = api.post("/api/questions/" + id + "/" + path, new Dtos.Transition(comment), Dtos.Question.class);
        notifyObservers(QUESTIONS_CHANGED, q);
        return q;
    }

    public List<Dtos.PluginInfo> plugins() {
        return api.get("/api/plugins", null, new TypeReference<List<Dtos.PluginInfo>>() { });
    }

    public List<Dtos.ChangeHistory> history(UUID id) {
        return api.get("/api/questions/" + id + "/history", null, new TypeReference<List<Dtos.ChangeHistory>>() { });
    }

    public List<Dtos.StateAudit> stateAudit(UUID id) {
        return api.get("/api/questions/" + id + "/state-audit", null, new TypeReference<List<Dtos.StateAudit>>() { });
    }

    public Map<String, Long> countByState() {
        return api.get("/api/questions/stats/by-state", null, new TypeReference<Map<String, Long>>() { });
    }
}

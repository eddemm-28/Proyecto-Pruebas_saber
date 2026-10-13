package co.edu.unicauca.kernel;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.plugin.api.QuestionPlugin;
import co.edu.unicauca.plugin.api.QuestionRequest;
import co.edu.unicauca.plugin.manager.QuestionPluginManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestionMicrokernel {

    private final Map<String, Question> questions = new LinkedHashMap<>();

    public Question generate(QuestionRequest request) {
        QuestionPlugin plugin = QuestionPluginManager.getInstance().getPluginFor(request.getType());
        if (plugin == null) {
            throw new IllegalArgumentException("No hay plugin para el tipo: " + request.getType());
        }
        Question q = plugin.generate(request);
        if (q != null) {
            questions.put(q.getId(), q);
        }
        return q;
    }

    public Question findById(String id) {
        return questions.get(id);
    }

    public List<Question> findAll() {
        return new ArrayList<>(questions.values());
    }

    public int size() {
        return questions.size();
    }
}

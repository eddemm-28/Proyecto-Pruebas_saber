package co.edu.unicauca.saberpro.questions.domain.validation;

import co.edu.unicauca.saberpro.questions.domain.model.Difficulty;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static co.edu.unicauca.saberpro.questions.domain.validation.QuestionRule.blank;

/**
 * Reglas estructurales RF-08 a RF-13 (A15). Se aplican igual a preguntas manuales y a las
 * generadas por cualquier plugin (Selección múltiple, Caso y Multimedia).
 */
public final class Rules {

    public static final int TITLE_MAX = 150;
    public static final int CONTEXT_MIN = 20;
    public static final int CONTEXT_MAX = 4000;
    public static final int CASE_CONTEXT_MIN = 100;
    public static final int STATEMENT_MIN = 10;
    public static final int STATEMENT_MAX = 1000;
    public static final int OPTION_MAX = 500;
    public static final int JUSTIFICATION_MIN = 10;

    private Rules() {
    }

    /** Campos obligatorios y catálogos (RF-05). */
    public static final QuestionRule REQUIRED_FIELDS = (q, e) -> {
        if (blank(q.title())) e.add("El título es obligatorio.");
        else if (q.title().length() > TITLE_MAX) e.add("El título no puede superar " + TITLE_MAX + " caracteres.");
        if (blank(q.justification()) || q.justification().length() < JUSTIFICATION_MIN)
            e.add("La justificación es obligatoria (mínimo " + JUSTIFICATION_MIN + " caracteres).");
        if (blank(q.bibliography())) e.add("La bibliografía es obligatoria.");
        if (blank(q.competence())) e.add("La competencia es obligatoria.");
        if (blank(q.topic())) e.add("El tema es obligatorio.");
        if (blank(q.subtopic())) e.add("El subtema es obligatorio.");
        if (Difficulty.tryParse(q.difficulty()).isEmpty()) e.add("La dificultad debe ser Fácil, Media o Difícil.");
    };

    /** RF-08: contexto obligatorio. */
    public static final QuestionRule CONTEXT_REQUIRED = (q, e) -> {
        if (blank(q.context())) {
            e.add("RF-08: el contexto es obligatorio.");
        } else if (q.context().length() < CONTEXT_MIN) {
            e.add("RF-08: el contexto debe tener al menos " + CONTEXT_MIN + " caracteres.");
        } else if (q.context().length() > CONTEXT_MAX) {
            e.add("El contexto no puede superar " + CONTEXT_MAX + " caracteres.");
        }
    };

    /** RF-09: la pregunta directa formula una única pregunta. */
    public static final QuestionRule SINGLE_QUESTION = (q, e) -> {
        String s = q.statement();
        if (blank(s)) {
            e.add("RF-09: la pregunta directa es obligatoria.");
            return;
        }
        if (s.length() < STATEMENT_MIN || s.length() > STATEMENT_MAX) {
            e.add("La pregunta directa debe tener entre " + STATEMENT_MIN + " y " + STATEMENT_MAX + " caracteres.");
        }
        long closing = TextNormalizer.count(s, '?');
        long opening = TextNormalizer.count(s, '¿');
        if (closing > 1 || opening > 1) {
            e.add("RF-09: la pregunta directa debe formular una sola pregunta.");
        }
    };

    /** RF-10: exactamente cuatro opciones, no vacías y sin repetir. */
    public static final QuestionRule FOUR_OPTIONS = (q, e) -> {
        List<String> options = q.options();
        if (options.size() != 4) {
            e.add("RF-10: debe haber exactamente 4 opciones (1 correcta y 3 distractores).");
            return;
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            String label = QuestionContent.LABELS.get(i);
            String opt = options.get(i);
            if (blank(opt)) {
                e.add("RF-10: la opción " + label + " es obligatoria.");
            } else if (opt.length() > OPTION_MAX) {
                e.add("La opción " + label + " no puede superar " + OPTION_MAX + " caracteres.");
            } else if (!seen.add(TextNormalizer.normalize(opt))) {
                e.add("RF-10: la opción " + label + " está repetida.");
            }
        }
    };

    /** RF-11: una única respuesta correcta, expresada como letra A–D. */
    public static final QuestionRule SINGLE_CORRECT_ANSWER = (q, e) -> {
        String c = q.correctOption();
        if (blank(c) || !QuestionContent.LABELS.contains(c)) {
            e.add("RF-11: debe indicar una única respuesta correcta (A, B, C o D).");
        }
    };

    /** Fórmulas prohibidas, sobre el texto normalizado (minúsculas, sin tildes ni signos). */
    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("\\b(todas?|ninguna|ninguno)( de)? (las|los) anteriores\\b"),
            Pattern.compile("^(todas?|ninguna|ninguno)( de)?( las| los)? (anteriores|opciones|respuestas)( anteriores)?$"),
            Pattern.compile("^(las )?(opciones )?[a-d] y [a-d]( son correctas)?$"),
            Pattern.compile("^ningun[oa]? es correct[oa]$"));

    /** RF-12: bloqueo de "todas/ninguna de las anteriores" y similares. */
    public static final QuestionRule NO_ALL_OR_NONE = (q, e) -> {
        for (int i = 0; i < q.options().size(); i++) {
            String norm = TextNormalizer.normalize(q.options().get(i));
            for (Pattern p : FORBIDDEN) {
                if (p.matcher(norm).find()) {
                    e.add("RF-12: la opción " + QuestionContent.LABELS.get(i)
                            + " usa una fórmula no permitida (\"todas/ninguna de las anteriores\" o similar).");
                    break;
                }
            }
        }
    };

    private static final Pattern REPEATED_WORD = Pattern.compile("(?iU)\\b(\\p{L}{2,})\\s+\\1\\b");

    /** RF-13: validación básica de longitud y gramática. */
    public static final QuestionRule BASIC_GRAMMAR = (q, e) -> {
        checkText("El contexto", q.context(), e);
        checkText("La pregunta directa", q.statement(), e);
        String s = q.statement();
        if (!blank(s) && s.contains("?") && !s.contains("¿")) {
            e.add("RF-13: la pregunta directa abre con \"¿\" si cierra con \"?\".");
        }
        if (!blank(s) && s.indexOf('¿') >= 0 && s.indexOf('?') < s.indexOf('¿')) {
            e.add("RF-13: los signos de interrogación de la pregunta directa están desbalanceados.");
        }
    };

    private static void checkText(String field, String text, List<String> e) {
        if (blank(text)) return;
        // Primera letra, saltando signos iniciales como "¿", "¡", comillas o paréntesis
        int first = text.codePoints().filter(Character::isLetterOrDigit).findFirst().orElse(-1);
        if (first != -1 && Character.isLetter(first) && Character.isLowerCase(first)) {
            e.add("RF-13: " + field.toLowerCase() + " debe iniciar con mayúscula.");
        }
        if (text.contains("  ")) {
            e.add("RF-13: " + field.toLowerCase() + " contiene espacios dobles.");
        }
        Matcher m = REPEATED_WORD.matcher(text);
        if (m.find()) {
            e.add("RF-13: " + field.toLowerCase() + " repite la palabra \"" + m.group(1) + "\".");
        }
        if (TextNormalizer.count(text, '(') != TextNormalizer.count(text, ')')) {
            e.add("RF-13: " + field.toLowerCase() + " tiene paréntesis sin cerrar.");
        }
    }

    /** Plugin Caso: el contexto debe ser un caso de estudio sustancial. */
    public static final QuestionRule CASE_PLUGIN = (q, e) -> {
        if (q.type() == QuestionType.CASE && !blank(q.context()) && q.context().length() < CASE_CONTEXT_MIN) {
            e.add("Las preguntas basadas en caso requieren un contexto de al menos " + CASE_CONTEXT_MIN + " caracteres.");
        }
    };

    private static final Pattern URL = Pattern.compile("^https?://[\\w.-]+(:\\d+)?(/\\S*)?$");

    /** Plugin Multimedia: recurso con URL http(s) válida. */
    public static final QuestionRule MULTIMEDIA_PLUGIN = (q, e) -> {
        if (q.type() == QuestionType.MULTIMEDIA) {
            if (blank(q.mediaUrl())) {
                e.add("Las preguntas multimedia requieren la URL del recurso.");
            } else if (!URL.matcher(q.mediaUrl()).matches()) {
                e.add("La URL del recurso multimedia no es válida.");
            }
        }
    };
}

package co.edu.unicauca.saberpro.client.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Valores de referencia y nombres legibles para la interfaz. */
public final class Catalogs {

    private Catalogs() {
    }

    /** Competencias sugeridas (genéricas y específicas de Ingeniería de Sistemas). El combo es editable. */
    public static final List<String> COMPETENCES = List.of(
            "Lectura crítica", "Razonamiento cuantitativo", "Competencias ciudadanas", "Comunicación escrita",
            "Inglés", "Formulación de proyectos de ingeniería", "Pensamiento científico - Matemáticas y estadística",
            "Diseño de software");

    public static final Map<String, String> DIFFICULTIES = ordered("FACIL", "Fácil", "MEDIA", "Media", "DIFICIL", "Difícil");

    public static final Map<String, String> TYPES = ordered("MULTIPLE_CHOICE", "Selección múltiple",
            "CASE", "Basada en caso", "MULTIMEDIA", "Con recurso multimedia");

    public static final Map<String, String> STATES = ordered(
            "BORRADOR", "Borrador", "PENDIENTE_REVISION", "Pendiente de revisión", "EN_REVISION", "En revisión",
            "APROBADA", "Aprobada", "RECHAZADA", "Rechazada", "PUBLICADA", "Publicada", "ARCHIVADA", "Archivada");

    public static final Map<String, String> ROLES = ordered("ADMINISTRADOR", "Administrador", "AUTOR", "Autor de preguntas",
            "REVISOR", "Revisor", "DOCENTE", "Docente / Coordinador", "ESTUDIANTE", "Estudiante");

    public static final Map<String, String> STRATEGIES = ordered("MIXED", "Mixto (todas las competencias)",
            "COMPETENCE", "Por competencia", "TOPIC", "Por tema", "DIFFICULTY", "Por dificultad");

    /** Acción del State → texto del botón. */
    public static final Map<String, String> ACTIONS = ordered(
            "SUBMIT", "Enviar a revisión", "START_REVIEW", "Iniciar revisión", "APPROVE", "Aprobar",
            "REJECT", "Rechazar", "RETURN_TO_DRAFT", "Devolver a borrador", "PUBLISH", "Publicar", "ARCHIVE", "Archivar");

    public static final Map<String, String> ACTION_PATHS = ordered(
            "SUBMIT", "submit", "START_REVIEW", "start-review", "APPROVE", "approve", "REJECT", "reject",
            "RETURN_TO_DRAFT", "return-to-draft", "PUBLISH", "publish", "ARCHIVE", "archive");

    public static String label(Map<String, String> catalog, String key) {
        return key == null ? "" : catalog.getOrDefault(key, key);
    }

    private static Map<String, String> ordered(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(m);
    }
}

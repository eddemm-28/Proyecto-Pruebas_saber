package co.edu.unicauca.saberpro.questions.domain.model;

/** Tipos de pregunta soportados por los plugins del microkernel. */
public enum QuestionType {
    MULTIPLE_CHOICE("Selección múltiple"),
    CASE("Basada en caso"),
    MULTIMEDIA("Con recurso multimedia");

    private final String displayName;

    QuestionType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static QuestionType parse(String value) {
        if (value == null || value.isBlank()) {
            return MULTIPLE_CHOICE;
        }
        return QuestionType.valueOf(value.trim().toUpperCase());
    }
}

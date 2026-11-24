package co.edu.unicauca.saberpro.questions.domain.state;

/** Los 7 estados del ciclo de vida de una pregunta (RF-14, RF-15, RNF-16). */
public enum QuestionStatus {
    BORRADOR("Borrador"),
    PENDIENTE_REVISION("Pendiente de revisión"),
    EN_REVISION("En revisión"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada"),
    PUBLICADA("Publicada"),
    ARCHIVADA("Archivada");

    private final String displayName;

    QuestionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

package co.edu.unicauca.saberpro.tracking.domain.model;

/** RF-27: clasificación automática de una competencia para un estudiante. */
public enum Classification {
    FORTALEZA("Fortaleza"),
    EN_DESARROLLO("En desarrollo"),
    DEBILIDAD("Debilidad");

    private final String displayName;

    Classification(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

package co.edu.unicauca.saberpro.users.domain;

/** Roles del sistema (RF-03). "Coordinador" se modela como DOCENTE en el Corte 2. */
public enum Role {
    ADMINISTRADOR("Administrador"),
    AUTOR("Autor de preguntas"),
    REVISOR("Revisor"),
    DOCENTE("Docente / Coordinador"),
    ESTUDIANTE("Estudiante");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

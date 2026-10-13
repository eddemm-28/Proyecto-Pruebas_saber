package co.edu.unicauca.domain;

/**
 * Enum que representa los roles posibles en el sistema según HU01 / RF-03
 * y la guía de laboratorio 2.
 */
public enum Role {
    ADMINISTRADOR("Administrador"),
    AUTOR_DE_PREGUNTAS("Autor de preguntas"),
    REVISOR("Revisor"),
    DOCENTE("Docente"),
    ESTUDIANTE("Estudiante");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Role fromDisplayName(String displayName) {
        if (displayName == null) return null;
        for (Role role : values()) {
            if (role.displayName.equalsIgnoreCase(displayName.trim()) || role.name().equalsIgnoreCase(displayName.trim())) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + displayName);
    }
}

package co.edu.unicauca.domain;

/**
 * Enum que representa los estados del usuario (Activo / Inactivo).
 */
public enum UserStatus {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static UserStatus fromDisplayName(String displayName) {
        if (displayName == null) return null;
        for (UserStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName.trim()) || status.name().equalsIgnoreCase(displayName.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado no válido: " + displayName);
    }
}


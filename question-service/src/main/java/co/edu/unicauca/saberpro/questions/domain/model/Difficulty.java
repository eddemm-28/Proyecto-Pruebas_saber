package co.edu.unicauca.saberpro.questions.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

public enum Difficulty {
    FACIL("Fácil"),
    MEDIA("Media"),
    DIFICIL("Difícil");

    private final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Acepta "FACIL", "Fácil", "facil", "Difícil"... (compatibilidad con el Corte 1). */
    public static Optional<Difficulty> tryParse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String norm = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        for (Difficulty d : values()) {
            if (d.name().equals(norm)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }
}

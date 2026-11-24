package co.edu.unicauca.saberpro.questions.domain.validation;

import java.text.Normalizer;
import java.util.Locale;

final class TextNormalizer {

    private TextNormalizer() {
    }

    /** Minúsculas, sin tildes, sin signos y con espacios simples. */
    static String normalize(String s) {
        if (s == null) return "";
        String noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    static long count(String s, char c) {
        return s == null ? 0 : s.chars().filter(ch -> ch == c).count();
    }
}

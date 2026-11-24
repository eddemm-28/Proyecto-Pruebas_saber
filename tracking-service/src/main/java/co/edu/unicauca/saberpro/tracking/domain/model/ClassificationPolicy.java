package co.edu.unicauca.saberpro.tracking.domain.model;

/**
 * RF-27: reglas de fortalezas y debilidades (parametrizables).
 * Con menos de {@code minQuestions} respondidas en una competencia no se clasifica como
 * fortaleza ni debilidad: queda "En desarrollo" para no sacar conclusiones con pocos datos.
 */
public record ClassificationPolicy(double strengthThreshold, double weaknessThreshold, int minQuestions) {

    public static final ClassificationPolicy DEFAULT = new ClassificationPolicy(70.0, 50.0, 5);

    public ClassificationPolicy {
        if (weaknessThreshold > strengthThreshold) {
            throw new IllegalArgumentException("El umbral de debilidad no puede superar al de fortaleza");
        }
    }

    public Classification classify(double averageScore, int totalQuestions) {
        if (totalQuestions < minQuestions) {
            return Classification.EN_DESARROLLO;
        }
        if (averageScore >= strengthThreshold) {
            return Classification.FORTALEZA;
        }
        if (averageScore < weaknessThreshold) {
            return Classification.DEBILIDAD;
        }
        return Classification.EN_DESARROLLO;
    }
}

package co.edu.unicauca.presentation;

import co.edu.unicauca.domain.Question;

import java.awt.Color;

/**
 * Colores de cada estado de pregunta. Son los mismos de la gráfica del observador.
 */
public final class EstadoColores {

    private static final Color AZUL = new Color(37, 99, 235);
    private static final Color AMBAR = new Color(217, 119, 6);
    private static final Color ROJO = new Color(220, 38, 38);
    private static final Color GRIS = new Color(107, 114, 128);

    private EstadoColores() {
    }

    public static Color colorDe(String estado) {
        if (Question.STATE_BORRADOR.equals(estado)) {
            return AZUL;
        }
        if (Question.STATE_PENDIENTE_REVISION.equals(estado)) {
            return AMBAR;
        }
        if (Question.STATE_ELIMINADA.equals(estado)) {
            return ROJO;
        }
        return GRIS;
    }
}

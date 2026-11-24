package co.edu.unicauca.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validación estructural de una pregunta (HU03). Se aplica al grabar.
 * Devuelve la lista de errores para poder mostrarlos todos juntos al usuario.
 */
public class QuestionValidator {

    private static final int MIN_TEXTO = 10;
    private static final int MAX_TITULO = 100;

    public List<String> validate(Question q) {
        List<String> errores = new ArrayList<>();

        if (q == null) {
            errores.add("La pregunta no puede ser nula.");
            return errores;
        }

        if (esVacio(q.getNombre())) {
            errores.add("El título es obligatorio.");
        } else if (q.getNombre().trim().length() > MAX_TITULO) {
            errores.add("El título no puede superar " + MAX_TITULO + " caracteres.");
        }

        validarTextoLargo(errores, q.getContexto(), "El contexto");
        validarTextoLargo(errores, q.getPregunta(), "La pregunta directa");
        validarOpciones(errores, q.getDistractors());
        validarRespuesta(errores, q.getRespuestaCorrecta());
        validarTextoLargo(errores, q.getJustificacion(), "La justificación de la respuesta");

        if (esVacio(q.getBibliografia())) {
            errores.add("La bibliografía es obligatoria.");
        }
        if (esVacio(q.getCompetencia())) {
            errores.add("La competencia es obligatoria.");
        }
        if (esVacio(q.getTema())) {
            errores.add("El tema es obligatorio.");
        }
        if (esVacio(q.getSubtema())) {
            errores.add("El subtema es obligatorio.");
        }
        if (esVacio(q.getDificultad())
                || !Arrays.asList(Question.DIFICULTADES).contains(q.getDificultad())) {
            errores.add("El nivel de dificultad debe ser Fácil, Media o Difícil.");
        }
        if (esVacio(q.getAutor())) {
            errores.add("La pregunta debe tener un autor.");
        }
        return errores;
    }

    /** Lanza IllegalArgumentException con todos los errores si la pregunta no es válida. */
    public void validateOrThrow(Question q) {
        List<String> errores = validate(q);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errores));
        }
    }

    private void validarTextoLargo(List<String> errores, String texto, String nombreCampo) {
        if (esVacio(texto)) {
            errores.add(nombreCampo + " es obligatorio.");
        } else if (texto.trim().length() < MIN_TEXTO) {
            errores.add(nombreCampo + " debe tener al menos " + MIN_TEXTO + " caracteres.");
        }
    }

    private void validarOpciones(List<String> errores, QuestionDistractors d) {
        if (d == null) {
            errores.add("Debe ingresar las cuatro opciones de respuesta.");
            return;
        }
        String[] opciones = {d.getOptionA(), d.getOptionB(), d.getOptionC(), d.getOptionD()};
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < opciones.length; i++) {
            char letra = (char) ('A' + i);
            if (esVacio(opciones[i])) {
                errores.add("La opción " + letra + " es obligatoria.");
            } else if (!vistas.add(opciones[i].trim().toLowerCase())) {
                errores.add("La opción " + letra + " está repetida.");
            }
        }
    }

    private void validarRespuesta(List<String> errores, String respuesta) {
        if (esVacio(respuesta) || !respuesta.trim().toUpperCase().matches("[A-D]")) {
            errores.add("La respuesta correcta debe ser A, B, C o D.");
        }
    }

    private boolean esVacio(String s) {
        return s == null || s.trim().isEmpty();
    }
}

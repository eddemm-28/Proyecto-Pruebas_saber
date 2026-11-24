package co.edu.unicauca.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionFilterTest {

    @Test
    @DisplayName("Un filtro vacío acepta cualquier pregunta")
    void filtroVacio() {
        assertTrue(new QuestionFilter().matches(TestData.preguntaValida("a")));
    }

    @Test
    @DisplayName("Filtra por estado y dificultad exactos")
    void estadoYDificultad() {
        Question q = TestData.preguntaValida("a");
        QuestionFilter f = new QuestionFilter();
        f.setEstado(Question.STATE_BORRADOR);
        f.setDificultad("Media");
        assertTrue(f.matches(q));

        f.setEstado(Question.STATE_PENDIENTE_REVISION);
        assertFalse(f.matches(q));
    }

    @Test
    @DisplayName("Filtra por tema y competencia sin importar mayúsculas")
    void temaYCompetencia() {
        Question q = TestData.preguntaValida("a");
        QuestionFilter f = new QuestionFilter();
        f.setTema("solid");
        f.setCompetencia("DISEÑO");
        assertTrue(f.matches(q));

        f.setTema("bases de datos");
        assertFalse(f.matches(q));
    }

    @Test
    @DisplayName("La búsqueda de texto mira título, contexto y pregunta")
    void busquedaTexto() {
        Question q = TestData.preguntaValida("a");
        QuestionFilter f = new QuestionFilter();
        f.setTexto("fácil de modificar");
        assertTrue(f.matches(q));
        f.setTexto("xyz inexistente");
        assertFalse(f.matches(q));
    }
}

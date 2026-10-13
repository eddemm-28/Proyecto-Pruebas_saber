package co.edu.unicauca.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    @DisplayName("Calcula el total de páginas redondeando hacia arriba")
    void totalPaginas() {
        PageResult<String> p = new PageResult<>(List.of("a"), 1, 10, 25);
        assertEquals(3, p.getTotalPages());
    }

    @Test
    @DisplayName("Sin elementos hay una página vacía")
    void sinElementos() {
        PageResult<String> p = new PageResult<>(List.of(), 1, 10, 0);
        assertEquals(1, p.getTotalPages());
        assertFalse(p.hasNext());
        assertFalse(p.hasPrevious());
    }

    @Test
    @DisplayName("Indica si hay página anterior y siguiente")
    void anteriorYSiguiente() {
        PageResult<String> p = new PageResult<>(List.of("a"), 2, 10, 25);
        assertTrue(p.hasPrevious());
        assertTrue(p.hasNext());
    }
}

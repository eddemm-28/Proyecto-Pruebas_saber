package co.edu.unicauca.domain;

import co.edu.unicauca.access.QuestionImplRepository;
import co.edu.unicauca.infra.Observer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las historias de usuario del primer corte en QuestionService (HU1, HU2 y HU3).
 */
class QuestionServiceHistoriasTest {

    private QuestionService service;

    @BeforeEach
    void setUp() {
        service = new QuestionService(new QuestionImplRepository());
    }

    // ---------- HU1: crear pregunta ----------

    @Test
    @DisplayName("HU1: crea la pregunta en estado Borrador con id nuevo")
    void crearPregunta() {
        Question creada = service.createQuestion(TestData.preguntaValida("autor1"));

        assertEquals("P-006", creada.getId());   // el repositorio de ejemplo ya trae P-001..P-005
        assertEquals(Question.STATE_BORRADOR, creada.getEstado());
        assertNotNull(service.getQuestionById("P-006"));
    }

    @Test
    @DisplayName("HU1: aunque llegue con otro estado, se guarda como Borrador")
    void crearSiempreBorrador() {
        Question q = TestData.preguntaValida("autor1");
        q.setEstado(Question.STATE_PENDIENTE_REVISION);
        assertEquals(Question.STATE_BORRADOR, service.createQuestion(q).getEstado());
    }

    @Test
    @DisplayName("HU1: no guarda una pregunta que falla la validación estructural")
    void crearInvalida() {
        Question q = TestData.preguntaValida("autor1");
        q.setRespuestaCorrecta("Z");
        int antes = service.getTotalQuestionsCount();

        assertThrows(IllegalArgumentException.class, () -> service.createQuestion(q));
        assertEquals(antes, service.getTotalQuestionsCount());
    }

    @Test
    @DisplayName("HU1: no se puede crear una pregunta nula")
    void crearNula() {
        assertThrows(IllegalArgumentException.class, () -> service.createQuestion(null));
    }

    @Test
    @DisplayName("HU1: crear una pregunta notifica a los observadores")
    void crearNotifica() {
        int[] llamadas = {0};
        Observer o = obj -> llamadas[0]++;
        service.addObserver(o);
        service.createQuestion(TestData.preguntaValida("autor1"));
        assertEquals(1, llamadas[0]);
    }

    @Test
    @DisplayName("Editar: el autor puede editar su borrador")
    void editarBorrador() {
        Question q = service.createQuestion(TestData.preguntaValida("autor1"));
        q.setNombre("Título nuevo");
        service.updateQuestion(q, "autor1");
        assertEquals("Título nuevo", service.getQuestionById(q.getId()).getNombre());
    }

    @Test
    @DisplayName("Editar: otro usuario no puede editar la pregunta")
    void editarAjena() {
        Question q = service.createQuestion(TestData.preguntaValida("autor1"));
        assertThrows(IllegalStateException.class, () -> service.updateQuestion(q, "autor2"));
    }

    @Test
    @DisplayName("Editar: no se edita una pregunta que ya está en revisión")
    void editarPendiente() {
        Question q = service.createQuestion(TestData.preguntaValida("autor1"));
        service.sendToReview(q.getId(), "autor1");
        assertThrows(IllegalStateException.class, () -> service.updateQuestion(q, "autor1"));
    }

    @Test
    @DisplayName("Editar: también se valida la estructura al editar")
    void editarInvalida() {
        Question q = service.createQuestion(TestData.preguntaValida("autor1"));
        q.setBibliografia("");
        assertThrows(IllegalArgumentException.class, () -> service.updateQuestion(q, "autor1"));
    }

    // ---------- HU2: Borrador -> Pendiente de revisión ----------

    @Test
    @DisplayName("HU2: el autor envía su borrador a Pendiente de revisión")
    void enviarARevision() {
        Question q = service.createQuestion(TestData.preguntaValida("autor1"));
        service.sendToReview(q.getId(), "autor1");
        assertEquals(Question.STATE_PENDIENTE_REVISION, service.getQuestionById(q.getId()).getEstado());
    }

    @Test
    @DisplayName("HU2: un autor no puede enviar a revisión la pregunta de otro")
    void enviarAjena() {
        Question q = service.createQuestion(TestData.preguntaValida("autor1"));
        assertThrows(IllegalStateException.class, () -> service.sendToReview(q.getId(), "autor2"));
        assertEquals(Question.STATE_BORRADOR, service.getQuestionById(q.getId()).getEstado());
    }

    @Test
    @DisplayName("HU2: solo se envían a revisión las preguntas en borrador")
    void enviarDosVeces() {
        Question q = service.createQuestion(TestData.preguntaValida("autor1"));
        service.sendToReview(q.getId(), "autor1");
        assertThrows(IllegalStateException.class, () -> service.sendToReview(q.getId(), "autor1"));
    }

    @Test
    @DisplayName("HU2: enviar una pregunta inexistente falla")
    void enviarInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.sendToReview("P-999", "autor1"));
    }

    // ---------- HU3: listar mis preguntas con filtros y paginación ----------

    private void crearVarias(String autor, int cantidad) {
        for (int i = 1; i <= cantidad; i++) {
            Question q = TestData.preguntaValida(autor);
            q.setNombre("Pregunta " + i);
            service.createQuestion(q);
        }
    }

    @Test
    @DisplayName("HU3: solo lista las preguntas del autor")
    void listaSoloLasMias() {
        crearVarias("autor1", 3);
        crearVarias("autor2", 2);

        PageResult<Question> r = service.searchByAuthor("autor1", null, 1, 10);
        assertEquals(3, r.getTotalItems());
        for (Question q : r.getItems()) {
            assertEquals("autor1", q.getAutor());
        }
    }

    @Test
    @DisplayName("HU3: pagina los resultados")
    void paginacion() {
        crearVarias("autor1", 12);

        PageResult<Question> p1 = service.searchByAuthor("autor1", null, 1, 5);
        PageResult<Question> p3 = service.searchByAuthor("autor1", null, 3, 5);

        assertEquals(5, p1.getItems().size());
        assertEquals(3, p1.getTotalPages());
        assertEquals(2, p3.getItems().size());
        assertEquals(12, p3.getTotalItems());
        assertEquals("Pregunta 11", p3.getItems().get(0).getNombre());
    }

    @Test
    @DisplayName("HU3: una página fuera de rango se ajusta al límite")
    void paginaFueraDeRango() {
        crearVarias("autor1", 3);
        assertEquals(1, service.searchByAuthor("autor1", null, 0, 5).getPage());
        assertEquals(1, service.searchByAuthor("autor1", null, 99, 5).getPage());
    }

    @Test
    @DisplayName("HU3: el tamaño de página debe ser positivo")
    void tamanoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.searchByAuthor("autor1", null, 1, 0));
    }

    @Test
    @DisplayName("HU3: filtra por estado")
    void filtroPorEstado() {
        crearVarias("autor1", 4);
        service.sendToReview("P-007", "autor1");

        QuestionFilter f = new QuestionFilter();
        f.setEstado(Question.STATE_PENDIENTE_REVISION);

        PageResult<Question> r = service.searchByAuthor("autor1", f, 1, 10);
        assertEquals(1, r.getTotalItems());
        assertEquals("P-007", r.getItems().get(0).getId());
    }

    @Test
    @DisplayName("HU3: filtra por tema y texto")
    void filtroPorTemaYTexto() {
        crearVarias("autor1", 3);
        Question otra = TestData.preguntaValida("autor1");
        otra.setNombre("Sobre bases de datos");
        otra.setTema("Bases de datos");
        service.createQuestion(otra);

        QuestionFilter porTema = new QuestionFilter();
        porTema.setTema("bases");
        assertEquals(1, service.searchByAuthor("autor1", porTema, 1, 10).getTotalItems());

        QuestionFilter porTexto = new QuestionFilter();
        porTexto.setTexto("Pregunta 2");
        assertEquals(1, service.searchByAuthor("autor1", porTexto, 1, 10).getTotalItems());
    }

    @Test
    @DisplayName("HU3: sin preguntas devuelve una lista vacía")
    void sinPreguntas() {
        PageResult<Question> r = service.searchByAuthor("nadie", null, 1, 10);
        assertTrue(r.getItems().isEmpty());
        assertEquals(1, r.getTotalPages());
    }
}

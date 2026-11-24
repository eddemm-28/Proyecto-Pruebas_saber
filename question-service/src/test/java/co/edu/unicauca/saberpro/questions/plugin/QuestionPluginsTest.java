package co.edu.unicauca.saberpro.questions.plugin;

import co.edu.unicauca.saberpro.questions.TestQuestions;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;
import co.edu.unicauca.saberpro.questions.domain.validation.QuestionValidator;
import co.edu.unicauca.saberpro.questions.plugin.api.CorrectAnswerNormalizer;
import co.edu.unicauca.saberpro.questions.plugin.impl.CaseQuestionPlugin;
import co.edu.unicauca.saberpro.questions.plugin.impl.MultimediaQuestionPlugin;
import co.edu.unicauca.saberpro.questions.plugin.impl.MultipleChoiceQuestionPlugin;
import co.edu.unicauca.saberpro.questions.plugin.registry.PluginLoadException;
import co.edu.unicauca.saberpro.questions.plugin.registry.QuestionPluginRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A14 y A21: microkernel, estandarización de respuesta y carga robusta de plugins. */
class QuestionPluginsTest {

    private final QuestionValidator validator = new QuestionValidator();

    @Test
    void registroCargaLosTresPluginsDesdePropertiesA21() {
        QuestionPluginRegistry registry = QuestionPluginRegistry.load(
                QuestionPluginRegistry.DEFAULT_RESOURCE, getClass().getClassLoader());
        assertEquals(3, registry.all().size());
        assertTrue(registry.find("multiple-choice").isPresent());
        assertTrue(registry.find("case-question").isPresent());
        assertTrue(registry.find("multimedia").isPresent());
        assertEquals(List.of(), registry.loadErrors());
    }

    @Test
    void recursoInexistenteFallaConMensajeClaro() {
        PluginLoadException ex = assertThrows(PluginLoadException.class,
                () -> QuestionPluginRegistry.load("no-existe.properties", getClass().getClassLoader()));
        assertTrue(ex.getMessage().contains("no-existe.properties"));
    }

    @Test
    void respuestaCorrectaSeEstandarizaALetra() {
        List<String> opts = List.of("Singleton", "State", "Adapter", "Facade");
        assertEquals("B", CorrectAnswerNormalizer.toLabel("b", opts));
        assertEquals("B", CorrectAnswerNormalizer.toLabel("State", opts));
        assertEquals("D", CorrectAnswerNormalizer.toLabel(" facade ", opts));
        assertNull(CorrectAnswerNormalizer.toLabel("Observer", opts));
    }

    @Test
    void seleccionMultipleGeneraPreguntaValida() {
        QuestionContent q = new MultipleChoiceQuestionPlugin()
                .generate(TestQuestions.pluginRequest("State", TestQuestions.valid().context(), null));
        assertEquals("B", q.correctOption(), "el texto de la opción se convierte a letra");
        assertEquals("DIFICIL", q.difficulty());
        assertEquals(QuestionType.MULTIPLE_CHOICE, q.type());
        assertEquals(List.of(), validator.validate(q));
    }

    @Test
    void casoRotulaElContextoYExigeContextoLargo() {
        String caso = "Una universidad migra su sistema monolítico de matrícula a microservicios. "
                + "Durante la migración, cada módulo debe seguir funcionando mientras se reemplaza gradualmente.";
        QuestionContent q = new CaseQuestionPlugin().generate(TestQuestions.pluginRequest("B", caso, null));
        assertTrue(q.context().startsWith("Caso de estudio: "));
        assertEquals(List.of(), validator.validate(q));

        QuestionContent corto = new CaseQuestionPlugin().generate(
                TestQuestions.pluginRequest("B", "Contexto corto pero mayor a veinte.", null));
        assertTrue(validator.validate(corto).stream().anyMatch(e -> e.contains("caso")));
    }

    @Test
    void multimediaPasaPorLasMismasReglas() {
        QuestionContent sinUrl = new MultimediaQuestionPlugin()
                .generate(TestQuestions.pluginRequest("B", TestQuestions.valid().context(), null));
        assertTrue(validator.validate(sinUrl).stream().anyMatch(e -> e.contains("URL")));

        QuestionContent conUrl = new MultimediaQuestionPlugin()
                .generate(TestQuestions.pluginRequest("B", TestQuestions.valid().context(), "https://x.co/a.png"));
        assertEquals(List.of(), validator.validate(conUrl));
    }

    @Test
    void pluginNoPuedeSaltarseRF12() {
        QuestionContent q = new MultipleChoiceQuestionPlugin().generate(new co.edu.unicauca.saberpro.questions.plugin.api.PluginRequest(
                "Título", TestQuestions.valid().context(), TestQuestions.valid().statement(),
                List.of("Uno", "Dos", "Tres", "Todas las anteriores"), "A", "Justificación suficiente.",
                "Biblio", "Comp", "Tema", "Sub", null, null));
        assertTrue(validator.validate(q).stream().anyMatch(e -> e.contains("RF-12")));
    }
}

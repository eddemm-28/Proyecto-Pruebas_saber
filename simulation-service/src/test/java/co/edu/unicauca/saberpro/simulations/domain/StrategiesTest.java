package co.edu.unicauca.saberpro.simulations.domain;

import co.edu.unicauca.saberpro.simulations.domain.exception.InsufficientQuestionsException;
import co.edu.unicauca.saberpro.simulations.domain.exception.InvalidSimulationConfigException;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;
import co.edu.unicauca.saberpro.simulations.domain.strategy.QuestionSelectionStrategy;
import co.edu.unicauca.saberpro.simulations.domain.strategy.SelectionStrategyResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static co.edu.unicauca.saberpro.simulations.support.Fakes.question;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B06 / RF-21, RF-22: patrón Strategy. */
class StrategiesTest {

    private final SelectionStrategyResolver resolver = new SelectionStrategyResolver();

    private List<QuestionSnapshot> bank() {
        List<QuestionSnapshot> bank = new ArrayList<>();
        for (int i = 0; i < 10; i++) bank.add(question("Lectura crítica", "Inferencia", i % 2 == 0 ? "FACIL" : "MEDIA", "A"));
        for (int i = 0; i < 10; i++) bank.add(question("Razonamiento cuantitativo", "Estadística", i % 2 == 0 ? "MEDIA" : "DIFICIL", "B"));
        for (int i = 0; i < 4; i++) bank.add(question("Diseño de software", "Patrones", "DIFICIL", "C"));
        return bank;
    }

    private List<QuestionSnapshot> run(SimulationConfig cfg, long seed) {
        QuestionSelectionStrategy s = resolver.resolve(cfg.strategy());
        return s.select(bank(), cfg, new Random(seed));
    }

    @Test
    void porCompetenciaSoloTraeEsaCompetenciaSinRepetir() {
        List<QuestionSnapshot> r = run(new SimulationConfig(StrategyType.COMPETENCE, "lectura crítica", null, null, 8, 30), 1);
        assertEquals(8, r.size());
        assertTrue(r.stream().allMatch(q -> q.competence().equals("Lectura crítica")));
        assertEquals(8, new HashSet<>(r).size());
    }

    @Test
    void porCompetenciaConDificultad() {
        List<QuestionSnapshot> r = run(new SimulationConfig(StrategyType.COMPETENCE, "Lectura crítica", null, "facil", 5, 30), 2);
        assertTrue(r.stream().allMatch(q -> q.difficulty().equals("FACIL")));
    }

    @Test
    void porTemaYPorDificultad() {
        assertTrue(run(new SimulationConfig(StrategyType.TOPIC, null, "Estadística", null, 6, 30), 3)
                .stream().allMatch(q -> q.topic().equals("Estadística")));
        assertTrue(run(new SimulationConfig(StrategyType.DIFFICULTY, null, null, "DIFICIL", 9, 30), 4)
                .stream().allMatch(q -> q.difficulty().equals("DIFICIL")));
    }

    @Test
    void mixtaReparteEntreCompetencias() {
        List<QuestionSnapshot> r = run(new SimulationConfig(StrategyType.MIXED, null, null, null, 12, 30), 5);
        Map<String, Long> perCompetence = r.stream().collect(Collectors.groupingBy(QuestionSnapshot::competence, Collectors.counting()));
        assertEquals(3, perCompetence.size());
        assertEquals(4, perCompetence.get("Diseño de software").intValue());
        assertEquals(4, perCompetence.get("Lectura crítica").intValue());
        assertEquals(4, perCompetence.get("Razonamiento cuantitativo").intValue());
    }

    @Test
    void mixtaCompletaConOtrasCompetenciasSiUnaSeAgota() {
        List<QuestionSnapshot> r = run(new SimulationConfig(StrategyType.MIXED, null, null, null, 20, 30), 6);
        assertEquals(20, r.size());
        assertEquals(20, new HashSet<>(r).size());
    }

    @Test
    void esAleatoria() {
        SimulationConfig cfg = new SimulationConfig(StrategyType.COMPETENCE, "Lectura crítica", null, null, 5, 30);
        assertNotEquals(run(cfg, 10).stream().map(QuestionSnapshot::questionId).toList(),
                run(cfg, 99).stream().map(QuestionSnapshot::questionId).toList());
    }

    @Test
    void faltanPreguntas() {
        InsufficientQuestionsException ex = assertThrows(InsufficientQuestionsException.class,
                () -> run(new SimulationConfig(StrategyType.COMPETENCE, "Diseño de software", null, null, 5, 30), 1));
        assertEquals(4, ex.getAvailable());
    }

    @Test
    void preguntasArchivadasNoSeUsan() {
        List<QuestionSnapshot> pool = new ArrayList<>(bank());
        pool.replaceAll(q -> q.competence().equals("Diseño de software") ? q.archived(2) : q);
        assertThrows(InsufficientQuestionsException.class, () -> resolver.resolve(StrategyType.COMPETENCE)
                .select(pool, new SimulationConfig(StrategyType.COMPETENCE, "Diseño de software", null, null, 1, 30), new Random()));
    }

    @Test
    void configuracionInvalida() {
        InvalidSimulationConfigException ex = assertThrows(InvalidSimulationConfigException.class,
                () -> new SimulationConfig(StrategyType.TOPIC, null, null, "EXTREMA", 0, 0).validate());
        assertEquals(4, ex.getErrors().size());
    }
}

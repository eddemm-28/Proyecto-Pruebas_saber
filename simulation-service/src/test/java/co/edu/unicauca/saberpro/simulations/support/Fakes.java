package co.edu.unicauca.saberpro.simulations.support;

import co.edu.unicauca.saberpro.simulations.application.dto.CatalogEntry;
import co.edu.unicauca.saberpro.simulations.application.port.out.ProcessedEventStore;
import co.edu.unicauca.saberpro.simulations.application.port.out.QuestionReplicaRepository;
import co.edu.unicauca.saberpro.simulations.application.port.out.SimulationEventPublisher;
import co.edu.unicauca.saberpro.simulations.application.port.out.SimulationRepository;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.model.Simulation;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationStatus;
import co.edu.unicauca.saberpro.simulations.domain.strategy.QuestionSelectionStrategy.CandidateCriteria;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Dobles en memoria de los puertos de salida (adaptadores de prueba del hexágono). */
public final class Fakes {

    private Fakes() {
    }

    public static QuestionSnapshot question(String competence, String topic, String difficulty, String correct) {
        return new QuestionSnapshot(UUID.randomUUID(), "Q-" + UUID.randomUUID().toString().substring(0, 6),
                "Contexto de la pregunta", "¿Enunciado?",
                List.of(new QuestionSnapshot.Option("A", "a"), new QuestionSnapshot.Option("B", "b"),
                        new QuestionSnapshot.Option("C", "c"), new QuestionSnapshot.Option("D", "d")),
                correct, "Justificación", competence, topic, "Sub", difficulty, 1, true, Instant.EPOCH);
    }

    public static final class MutableClock extends Clock {
        private Instant now;

        public MutableClock(Instant start) {
            this.now = start;
        }

        public void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    public static final class InMemoryReplica implements QuestionReplicaRepository {
        public final Map<UUID, QuestionSnapshot> store = new LinkedHashMap<>();

        public InMemoryReplica add(QuestionSnapshot... qs) {
            for (QuestionSnapshot q : qs) store.put(q.questionId(), q);
            return this;
        }

        @Override public Optional<QuestionSnapshot> findById(UUID id) { return Optional.ofNullable(store.get(id)); }

        @Override public List<QuestionSnapshot> findAllById(Collection<UUID> ids) {
            return ids.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override public List<QuestionSnapshot> findAvailable(CandidateCriteria c) {
            return store.values().stream().filter(QuestionSnapshot::available)
                    .filter(q -> c.competence() == null || c.competence().equalsIgnoreCase(q.competence()))
                    .filter(q -> c.topic() == null || c.topic().equalsIgnoreCase(q.topic()))
                    .filter(q -> c.difficulty() == null || c.difficulty().equalsIgnoreCase(q.difficulty()))
                    .toList();
        }

        @Override public void save(QuestionSnapshot s) { store.put(s.questionId(), s); }

        @Override public List<CatalogEntry> catalog() {
            return store.values().stream().filter(QuestionSnapshot::available)
                    .collect(Collectors.groupingBy(q -> List.of(q.competence(), q.topic(), q.difficulty()), Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> new CatalogEntry(e.getKey().get(0), e.getKey().get(1), e.getKey().get(2), e.getValue()))
                    .toList();
        }
    }

    public static final class InMemorySimulations implements SimulationRepository {
        public final Map<UUID, Simulation> store = new LinkedHashMap<>();

        @Override public Optional<Simulation> findById(UUID id) { return Optional.ofNullable(store.get(id)); }

        @Override public Optional<Simulation> findActiveByStudent(UUID studentId) {
            return store.values().stream()
                    .filter(s -> s.getStudentId().equals(studentId) && s.getStatus() == SimulationStatus.IN_PROGRESS)
                    .findFirst();
        }

        @Override public List<Simulation> findOverdue(Instant cutoff, int limit) {
            return store.values().stream()
                    .filter(s -> s.getStatus() == SimulationStatus.IN_PROGRESS && s.getExpiresAt().isBefore(cutoff))
                    .limit(limit).toList();
        }

        @Override public Simulation save(Simulation s) { store.put(s.getId(), s); return s; }
    }

    public static final class RecordingPublisher implements SimulationEventPublisher {
        public final List<Simulation> published = new ArrayList<>();

        @Override public void publishFinished(Simulation simulation, String correlationId) {
            published.add(simulation);
        }
    }

    public static final class InMemoryProcessed implements ProcessedEventStore {
        public final Set<UUID> ids = new HashSet<>();

        @Override public boolean markProcessed(UUID eventId, String eventType) { return ids.add(eventId); }
    }
}

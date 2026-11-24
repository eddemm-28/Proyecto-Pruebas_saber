package co.edu.unicauca.saberpro.simulations.application.service;

import co.edu.unicauca.saberpro.simulations.application.port.in.QuestionReplicaSync;
import co.edu.unicauca.saberpro.simulations.application.port.out.ProcessedEventStore;
import co.edu.unicauca.saberpro.simulations.application.port.out.QuestionReplicaRepository;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;

import java.util.Optional;
import java.util.UUID;

/**
 * B05: idempotente por {@code eventId} y protegido contra eventos fuera de orden con
 * {@code sourceVersion} (un evento con versión menor o igual a la réplica se ignora).
 */
public class QuestionReplicaService implements QuestionReplicaSync {

    private final QuestionReplicaRepository replica;
    private final ProcessedEventStore processed;

    public QuestionReplicaService(QuestionReplicaRepository replica, ProcessedEventStore processed) {
        this.replica = replica;
        this.processed = processed;
    }

    @Override
    public boolean onQuestionPublished(UUID eventId, QuestionSnapshot snapshot) {
        if (!processed.markProcessed(eventId, "PreguntaPublicada")) {
            return false;
        }
        Optional<QuestionSnapshot> current = replica.findById(snapshot.questionId());
        if (current.isPresent() && current.get().sourceVersion() >= snapshot.sourceVersion()) {
            return false;
        }
        replica.save(snapshot);
        return true;
    }

    @Override
    public boolean onQuestionArchived(UUID eventId, UUID questionId, long version) {
        if (!processed.markProcessed(eventId, "PreguntaArchivada")) {
            return false;
        }
        Optional<QuestionSnapshot> current = replica.findById(questionId);
        if (current.isEmpty() || current.get().sourceVersion() > version || !current.get().available()) {
            return false;
        }
        replica.save(current.get().archived(version));
        return true;
    }
}

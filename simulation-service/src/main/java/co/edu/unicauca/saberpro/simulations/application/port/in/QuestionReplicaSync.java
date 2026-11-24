package co.edu.unicauca.saberpro.simulations.application.port.in;

import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;

import java.util.UUID;

/** B05: mantiene la réplica local a partir de PreguntaPublicada / PreguntaArchivada. */
public interface QuestionReplicaSync {

    /** @return {@code false} si el evento era duplicado o más viejo que la réplica */
    boolean onQuestionPublished(UUID eventId, QuestionSnapshot snapshot);

    boolean onQuestionArchived(UUID eventId, UUID questionId, long version);
}

package co.edu.unicauca.saberpro.simulations.application.port.out;

import co.edu.unicauca.saberpro.simulations.application.dto.CatalogEntry;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.strategy.QuestionSelectionStrategy.CandidateCriteria;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: réplica local de preguntas publicadas. */
public interface QuestionReplicaRepository {

    Optional<QuestionSnapshot> findById(UUID questionId);

    List<QuestionSnapshot> findAllById(Collection<UUID> questionIds);

    /** Preguntas disponibles que cumplen los criterios (los nulos no filtran). */
    List<QuestionSnapshot> findAvailable(CandidateCriteria criteria);

    void save(QuestionSnapshot snapshot);

    List<CatalogEntry> catalog();
}

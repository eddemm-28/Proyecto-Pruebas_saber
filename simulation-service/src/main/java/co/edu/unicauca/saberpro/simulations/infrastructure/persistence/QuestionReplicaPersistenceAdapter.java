package co.edu.unicauca.saberpro.simulations.infrastructure.persistence;

import co.edu.unicauca.saberpro.simulations.application.dto.CatalogEntry;
import co.edu.unicauca.saberpro.simulations.application.port.out.QuestionReplicaRepository;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.strategy.QuestionSelectionStrategy.CandidateCriteria;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity.QuestionReplicaEntity;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.repository.QuestionReplicaJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: réplica de preguntas en PostgreSQL. */
@Component
public class QuestionReplicaPersistenceAdapter implements QuestionReplicaRepository {

    private final QuestionReplicaJpaRepository jpa;

    public QuestionReplicaPersistenceAdapter(QuestionReplicaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<QuestionSnapshot> findById(UUID questionId) {
        return jpa.findById(questionId).map(QuestionReplicaPersistenceAdapter::toDomain);
    }

    @Override
    public List<QuestionSnapshot> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(QuestionReplicaPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<QuestionSnapshot> findAvailable(CandidateCriteria c) {
        Specification<QuestionReplicaEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("available")));
            if (c.competence() != null) p.add(cb.equal(cb.lower(root.get("competence")), c.competence().toLowerCase()));
            if (c.topic() != null) p.add(cb.equal(cb.lower(root.get("topic")), c.topic().toLowerCase()));
            if (c.difficulty() != null) p.add(cb.equal(root.get("difficulty"), c.difficulty().toUpperCase()));
            return cb.and(p.toArray(Predicate[]::new));
        };
        return jpa.findAll(spec).stream().map(QuestionReplicaPersistenceAdapter::toDomain).toList();
    }

    @Override
    public void save(QuestionSnapshot s) {
        QuestionReplicaEntity e = jpa.findById(s.questionId()).orElseGet(QuestionReplicaEntity::new);
        e.questionId = s.questionId();
        e.code = s.code();
        e.context = s.context();
        e.statement = s.statement();
        e.options.clear();
        s.options().forEach(o -> e.options.add(new QuestionReplicaEntity.OptionEmbeddable(o.label(), o.text())));
        e.correctOption = s.correctOption();
        e.justification = s.justification();
        e.competence = s.competence();
        e.topic = s.topic();
        e.subtopic = s.subtopic();
        e.difficulty = s.difficulty();
        e.available = s.available();
        e.sourceVersion = s.sourceVersion();
        e.publishedAt = s.publishedAt();
        e.syncedAt = Instant.now();
        jpa.save(e);
    }

    @Override
    public List<CatalogEntry> catalog() {
        return jpa.catalog();
    }

    static QuestionSnapshot toDomain(QuestionReplicaEntity e) {
        return new QuestionSnapshot(e.questionId, e.code, e.context, e.statement,
                e.options.stream().map(o -> new QuestionSnapshot.Option(o.label, o.text)).toList(),
                e.correctOption, e.justification, e.competence, e.topic, e.subtopic, e.difficulty,
                e.sourceVersion, e.available, e.publishedAt);
    }
}

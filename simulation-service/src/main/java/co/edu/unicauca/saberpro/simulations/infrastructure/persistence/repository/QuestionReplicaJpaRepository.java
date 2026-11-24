package co.edu.unicauca.saberpro.simulations.infrastructure.persistence.repository;

import co.edu.unicauca.saberpro.simulations.application.dto.CatalogEntry;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity.QuestionReplicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface QuestionReplicaJpaRepository extends JpaRepository<QuestionReplicaEntity, UUID>,
        JpaSpecificationExecutor<QuestionReplicaEntity> {

    @Query("""
            SELECT new co.edu.unicauca.saberpro.simulations.application.dto.CatalogEntry(
                   r.competence, r.topic, r.difficulty, COUNT(r))
            FROM QuestionReplicaEntity r
            WHERE r.available = true
            GROUP BY r.competence, r.topic, r.difficulty
            ORDER BY r.competence, r.topic, r.difficulty""")
    List<CatalogEntry> catalog();
}

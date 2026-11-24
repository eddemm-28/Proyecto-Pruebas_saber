package co.edu.unicauca.saberpro.simulations.infrastructure.persistence.repository;

import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query("SELECT e FROM OutboxEventEntity e WHERE e.publishedAt IS NULL ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findPending(Pageable pageable);

    long countByPublishedAtIsNull();
}

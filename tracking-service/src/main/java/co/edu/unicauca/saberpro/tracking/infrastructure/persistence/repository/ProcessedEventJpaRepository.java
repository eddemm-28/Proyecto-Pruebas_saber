package co.edu.unicauca.saberpro.tracking.infrastructure.persistence.repository;

import co.edu.unicauca.saberpro.tracking.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}

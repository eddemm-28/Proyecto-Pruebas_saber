package co.edu.unicauca.saberpro.simulations.infrastructure.persistence.repository;

import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity.SimulationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationJpaRepository extends JpaRepository<SimulationEntity, UUID> {

    Optional<SimulationEntity> findFirstByStudentIdAndStatus(UUID studentId, String status);

    List<SimulationEntity> findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(String status, Instant cutoff, Pageable page);
}

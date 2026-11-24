package co.edu.unicauca.saberpro.simulations.application.port.out;

import co.edu.unicauca.saberpro.simulations.domain.model.Simulation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de simulacros. */
public interface SimulationRepository {

    Optional<Simulation> findById(UUID id);

    Optional<Simulation> findActiveByStudent(UUID studentId);

    /** Simulacros en curso cuyo vencimiento es anterior a {@code cutoff}. */
    List<Simulation> findOverdue(Instant cutoff, int limit);

    Simulation save(Simulation simulation);
}

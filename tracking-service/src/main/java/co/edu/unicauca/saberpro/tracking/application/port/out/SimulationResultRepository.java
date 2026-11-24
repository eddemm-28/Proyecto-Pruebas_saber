package co.edu.unicauca.saberpro.tracking.application.port.out;

import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult;

import java.util.List;
import java.util.UUID;

public interface SimulationResultRepository {

    boolean exists(UUID simulationId);

    void save(SimulationResult result);

    /** Historial más reciente primero. */
    List<SimulationResult> findByStudent(UUID studentId, int page, int size);

    long countByStudent(UUID studentId);

    List<SimulationResult> findAllByStudent(UUID studentId);
}

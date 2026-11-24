package co.edu.unicauca.saberpro.simulations.application.port.out;

import co.edu.unicauca.saberpro.simulations.domain.model.Simulation;

/** Puerto de salida: emite {@code SimulacroFinalizado} (B09). El adaptador usa el outbox. */
public interface SimulationEventPublisher {

    void publishFinished(Simulation simulation, String correlationId);
}

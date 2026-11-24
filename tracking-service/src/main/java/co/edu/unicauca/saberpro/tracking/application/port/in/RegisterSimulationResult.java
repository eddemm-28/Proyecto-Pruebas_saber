package co.edu.unicauca.saberpro.tracking.application.port.in;

import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult;

import java.util.UUID;

/** B10: puerto de entrada que alimenta el adaptador de RabbitMQ con SimulacroFinalizado. */
public interface RegisterSimulationResult {

    /** @return {@code false} si el evento o el simulacro ya se habían registrado */
    boolean register(UUID eventId, SimulationResult result);
}

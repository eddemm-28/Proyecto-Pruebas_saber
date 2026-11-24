package co.edu.unicauca.saberpro.simulations.domain.exception;

import java.util.UUID;

/** El estudiante ya tiene un simulacro en curso → HTTP 409 (puede retomarlo). */
public class ActiveSimulationExistsException extends SimulationDomainException {

    private final UUID activeSimulationId;

    public ActiveSimulationExistsException(UUID activeSimulationId) {
        super("Ya tiene un simulacro en curso. Termínelo o retómelo antes de iniciar otro.");
        this.activeSimulationId = activeSimulationId;
    }

    public UUID getActiveSimulationId() {
        return activeSimulationId;
    }
}

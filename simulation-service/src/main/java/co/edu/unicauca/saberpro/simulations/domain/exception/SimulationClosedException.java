package co.edu.unicauca.saberpro.simulations.domain.exception;

/** El simulacro ya terminó o expiró → HTTP 409. */
public class SimulationClosedException extends SimulationDomainException {
    public SimulationClosedException(String message) {
        super(message);
    }
}

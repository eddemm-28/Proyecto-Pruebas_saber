package co.edu.unicauca.saberpro.simulations.domain.exception;

/** Base de las excepciones de dominio del simulador. */
public abstract class SimulationDomainException extends RuntimeException {
    protected SimulationDomainException(String message) {
        super(message);
    }
}

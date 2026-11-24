package co.edu.unicauca.saberpro.simulations.domain.exception;

/** El simulacro es de otro estudiante → HTTP 403. */
public class SimulationAccessDeniedException extends SimulationDomainException {
    public SimulationAccessDeniedException(String message) {
        super(message);
    }
}

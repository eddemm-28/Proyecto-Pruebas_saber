package co.edu.unicauca.saberpro.simulations.domain.exception;

/** → HTTP 404 */
public class SimulationNotFoundException extends SimulationDomainException {
    public SimulationNotFoundException(String message) {
        super(message);
    }
}

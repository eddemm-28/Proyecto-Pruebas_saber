package co.edu.unicauca.saberpro.simulations.domain.exception;

import java.util.List;

/** → HTTP 422 con la lista de errores. */
public class InvalidSimulationConfigException extends SimulationDomainException {

    private final List<String> errors;

    public InvalidSimulationConfigException(List<String> errors) {
        super("Configuración de simulacro inválida");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}

package co.edu.unicauca.saberpro.simulations.domain.exception;

/** No hay suficientes preguntas publicadas para la configuración pedida → HTTP 422. */
public class InsufficientQuestionsException extends SimulationDomainException {

    private final int available;
    private final int requested;

    public InsufficientQuestionsException(int available, int requested) {
        super("Solo hay " + available + " pregunta(s) disponible(s) para esa configuración y se pidieron " + requested + ".");
        this.available = available;
        this.requested = requested;
    }

    public int getAvailable() { return available; }
    public int getRequested() { return requested; }
}

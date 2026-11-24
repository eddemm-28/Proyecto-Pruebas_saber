package co.edu.unicauca.saberpro.events;

/**
 * Nombres de exchanges, routing keys y colas.
 * La cola pertenece al consumidor: {@code <servicio-consumidor>.<evento>.q}.
 */
public final class EventTopology {

    public static final String EXCHANGE = "saberpro.events";
    public static final String DEAD_LETTER_EXCHANGE = "saberpro.events.dlx";

    public static final String RK_QUESTION_PUBLISHED = "question.published";
    public static final String RK_QUESTION_ARCHIVED = "question.archived";
    public static final String RK_SIMULATION_FINISHED = "simulation.finished";

    public static final String Q_SIMULATION_QUESTION_PUBLISHED = "simulation.question-published.q";
    public static final String Q_SIMULATION_QUESTION_ARCHIVED = "simulation.question-archived.q";
    public static final String Q_TRACKING_SIMULATION_FINISHED = "tracking.simulation-finished.q";

    public static final String HEADER_EVENT_TYPE = "x-event-type";

    private EventTopology() {
    }

    /** {@code simulation.question-published.q} → {@code simulation.question-published.dlq}. */
    public static String deadLetterQueueOf(String queue) {
        return queue.endsWith(".q") ? queue.substring(0, queue.length() - 2) + ".dlq" : queue + ".dlq";
    }
}

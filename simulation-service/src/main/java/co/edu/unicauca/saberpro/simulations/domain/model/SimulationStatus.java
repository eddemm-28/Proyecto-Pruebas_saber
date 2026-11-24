package co.edu.unicauca.saberpro.simulations.domain.model;

public enum SimulationStatus {
    IN_PROGRESS,
    FINISHED,
    EXPIRED;

    public boolean isClosed() {
        return this != IN_PROGRESS;
    }
}

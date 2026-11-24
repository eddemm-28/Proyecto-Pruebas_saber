package co.edu.unicauca.infra;

/**
 * Interfaz para los observadores en el patrón Observer.
 */
public interface Observer {
    /**
     * Método llamado por el sujeto observable cuando su estado cambia.
     * @param obj Objeto con información del evento o el sujeto mismo.
     */
    void update(Object obj);
}

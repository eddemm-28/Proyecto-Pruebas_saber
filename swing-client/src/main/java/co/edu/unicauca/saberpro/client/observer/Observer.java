package co.edu.unicauca.saberpro.client.observer;

/** Patrón Observer (heredado del Corte 1): las vistas se suscriben a los modelos/controladores. */
@FunctionalInterface
public interface Observer {

    void update(Subject source, String event, Object data);
}

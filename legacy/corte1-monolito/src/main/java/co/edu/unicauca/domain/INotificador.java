package co.edu.unicauca.domain;

/**
 * Abstracción para avisarle a un revisor que le asignaron una pregunta (DIP).
 * Se puede implementar por correo, consola, etc.
 */
public interface INotificador {
    /**
     * @throws NotificacionException si no se pudo enviar la notificación
     */
    void notificarAsignacion(User revisor, Question pregunta);
}

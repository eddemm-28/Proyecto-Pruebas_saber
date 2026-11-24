package co.edu.unicauca.infra;

import co.edu.unicauca.domain.INotificador;
import co.edu.unicauca.domain.NotificacionException;
import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.User;

/**
 * Notificador de respaldo: imprime el "correo" por consola.
 * Se usa cuando no hay un archivo mail.properties configurado.
 */
public class ConsoleNotificador implements INotificador {

    @Override
    public void notificarAsignacion(User revisor, Question pregunta) {
        if (revisor.getEmail() == null || revisor.getEmail().trim().isEmpty()) {
            throw new NotificacionException("el revisor no tiene correo registrado");
        }
        System.out.println("[CORREO SIMULADO] Para: " + revisor.getEmail());
        System.out.println("  Asunto: " + EmailNotificador.asunto(pregunta));
        System.out.println("  " + EmailNotificador.cuerpo(revisor, pregunta).replace("\n", "\n  "));
    }
}

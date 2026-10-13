package co.edu.unicauca.domain;

import java.util.ArrayList;
import java.util.List;

/** Notificador de pruebas: guarda a quién "le envió" correo y puede simular fallos. */
class FakeNotificador implements INotificador {
    final List<String> enviadosA = new ArrayList<>();
    final List<String> preguntas = new ArrayList<>();
    String loginQueFalla = null;

    @Override
    public void notificarAsignacion(User revisor, Question pregunta) {
        if (revisor.getLogin().equals(loginQueFalla)) {
            throw new NotificacionException("servidor de correo caído");
        }
        enviadosA.add(revisor.getEmail());
        preguntas.add(pregunta.getId());
    }
}

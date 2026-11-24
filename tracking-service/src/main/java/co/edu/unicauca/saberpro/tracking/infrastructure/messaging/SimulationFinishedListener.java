package co.edu.unicauca.saberpro.tracking.infrastructure.messaging;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.EventTypes;
import co.edu.unicauca.saberpro.events.messaging.EventHandler;
import co.edu.unicauca.saberpro.events.payload.SimulacroFinalizadoEvent;
import co.edu.unicauca.saberpro.events.rabbit.RabbitEventDispatcher;
import co.edu.unicauca.saberpro.tracking.application.port.in.RegisterSimulationResult;
import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** B10: adaptador de entrada RabbitMQ para {@code SimulacroFinalizado}. */
@Component
public class SimulationFinishedListener implements EventHandler<SimulacroFinalizadoEvent> {

    private static final Logger log = LoggerFactory.getLogger(SimulationFinishedListener.class);

    private final RabbitEventDispatcher dispatcher;
    private final RegisterSimulationResult register;

    public SimulationFinishedListener(RabbitEventDispatcher dispatcher, RegisterSimulationResult register) {
        this.dispatcher = dispatcher;
        this.register = register;
    }

    @RabbitListener(queues = EventTopology.Q_TRACKING_SIMULATION_FINISHED)
    public void onMessage(Message message) {
        dispatcher.dispatch(message, this);
    }

    @Override
    public String eventType() {
        return EventTypes.SIMULACRO_FINALIZADO;
    }

    @Override
    public Class<SimulacroFinalizadoEvent> payloadType() {
        return SimulacroFinalizadoEvent.class;
    }

    @Override
    public void handle(EventEnvelope<SimulacroFinalizadoEvent> envelope) {
        SimulacroFinalizadoEvent e = envelope.payload();
        SimulationResult result = new SimulationResult(e.simulationId(), e.studentId(), e.studentLogin(),
                e.finalStatus(), e.strategy(), e.startedAt(), e.finishedAt(), e.totalQuestions(), e.correctAnswers(),
                e.globalScore(),
                e.competenceScores().stream().map(c -> new SimulationResult.CompetenceResult(c.competence(), c.total(),
                        c.correct(), c.score())).toList(),
                e.answers().stream().map(a -> new SimulationResult.AnswerResult(a.questionId(), a.competence(), a.topic(),
                        a.difficulty(), a.selectedOption() != null, a.correct())).toList());
        boolean applied = register.register(envelope.eventId(), result);
        log.info("SimulacroFinalizado {} de {}: {}", e.simulationId(), e.studentLogin(),
                applied ? "registrado" : "duplicado, ignorado");
    }
}

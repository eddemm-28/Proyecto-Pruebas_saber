package co.edu.unicauca.saberpro.simulations.infrastructure.messaging;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.EventTypes;
import co.edu.unicauca.saberpro.events.messaging.EventHandler;
import co.edu.unicauca.saberpro.events.payload.PreguntaArchivadaEvent;
import co.edu.unicauca.saberpro.events.payload.PreguntaPublicadaEvent;
import co.edu.unicauca.saberpro.events.rabbit.RabbitEventDispatcher;
import co.edu.unicauca.saberpro.simulations.application.port.in.QuestionReplicaSync;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * B05: adaptador de entrada (driving) de RabbitMQ. Traduce los eventos del question-service
 * al modelo propio del simulador y llama al puerto {@link QuestionReplicaSync}.
 */
@Component
public class QuestionEventsListener {

    private static final Logger log = LoggerFactory.getLogger(QuestionEventsListener.class);

    private final RabbitEventDispatcher dispatcher;
    private final EventHandler<PreguntaPublicadaEvent> publishedHandler;
    private final EventHandler<PreguntaArchivadaEvent> archivedHandler;

    public QuestionEventsListener(RabbitEventDispatcher dispatcher, QuestionReplicaSync sync) {
        this.dispatcher = dispatcher;
        this.publishedHandler = new PublishedHandler(sync);
        this.archivedHandler = new ArchivedHandler(sync);
    }

    @RabbitListener(queues = EventTopology.Q_SIMULATION_QUESTION_PUBLISHED)
    public void onQuestionPublished(Message message) {
        dispatcher.dispatch(message, publishedHandler);
    }

    @RabbitListener(queues = EventTopology.Q_SIMULATION_QUESTION_ARCHIVED)
    public void onQuestionArchived(Message message) {
        dispatcher.dispatch(message, archivedHandler);
    }

    record PublishedHandler(QuestionReplicaSync sync) implements EventHandler<PreguntaPublicadaEvent> {
        @Override public String eventType() { return EventTypes.PREGUNTA_PUBLICADA; }
        @Override public Class<PreguntaPublicadaEvent> payloadType() { return PreguntaPublicadaEvent.class; }

        @Override
        public void handle(EventEnvelope<PreguntaPublicadaEvent> envelope) {
            PreguntaPublicadaEvent p = envelope.payload();
            QuestionSnapshot snapshot = new QuestionSnapshot(p.questionId(), p.code(), p.context(), p.statement(),
                    p.options().stream().map(o -> new QuestionSnapshot.Option(o.label(), o.text())).toList(),
                    p.correctOption(), p.justification(), p.competence(), p.topic(), p.subtopic(), p.difficulty(),
                    p.version(), true, p.publishedAt());
            boolean applied = sync.onQuestionPublished(envelope.eventId(), snapshot);
            log.info("PreguntaPublicada {} ({}): {}", p.code(), p.questionId(), applied ? "réplica actualizada" : "ignorado");
        }
    }

    record ArchivedHandler(QuestionReplicaSync sync) implements EventHandler<PreguntaArchivadaEvent> {
        @Override public String eventType() { return EventTypes.PREGUNTA_ARCHIVADA; }
        @Override public Class<PreguntaArchivadaEvent> payloadType() { return PreguntaArchivadaEvent.class; }

        @Override
        public void handle(EventEnvelope<PreguntaArchivadaEvent> envelope) {
            PreguntaArchivadaEvent p = envelope.payload();
            boolean applied = sync.onQuestionArchived(envelope.eventId(), p.questionId(), p.version());
            log.info("PreguntaArchivada {}: {}", p.questionId(), applied ? "retirada de nuevos simulacros" : "ignorado");
        }
    }
}

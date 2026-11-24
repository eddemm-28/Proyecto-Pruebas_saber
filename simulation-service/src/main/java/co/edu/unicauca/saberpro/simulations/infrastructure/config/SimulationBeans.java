package co.edu.unicauca.saberpro.simulations.infrastructure.config;

import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.rabbit.CommonRabbitConfiguration;
import co.edu.unicauca.saberpro.simulations.application.port.in.QuestionReplicaSync;
import co.edu.unicauca.saberpro.simulations.application.port.in.SimulationUseCases;
import co.edu.unicauca.saberpro.simulations.application.port.out.ProcessedEventStore;
import co.edu.unicauca.saberpro.simulations.application.port.out.QuestionReplicaRepository;
import co.edu.unicauca.saberpro.simulations.application.port.out.SimulationEventPublisher;
import co.edu.unicauca.saberpro.simulations.application.port.out.SimulationRepository;
import co.edu.unicauca.saberpro.simulations.application.service.QuestionReplicaService;
import co.edu.unicauca.saberpro.simulations.application.service.SimulationService;
import co.edu.unicauca.saberpro.simulations.domain.scoring.ScoringPolicy;
import co.edu.unicauca.saberpro.simulations.domain.strategy.SelectionStrategyResolver;
import org.springframework.amqp.core.Declarables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Duration;
import java.util.Random;

/**
 * Composición del hexágono: aquí (y solo aquí) se conectan los puertos con sus adaptadores.
 * Dominio y aplicación no tienen anotaciones de Spring.
 */
@Configuration
public class SimulationBeans {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SimulationUseCases simulationUseCases(SimulationRepository simulations, QuestionReplicaRepository replica,
                                          SimulationEventPublisher events, Clock clock,
                                          @Value("${simulation.grace-seconds:2}") long graceSeconds,
                                          PlatformTransactionManager tx) {
        SimulationService service = new SimulationService(simulations, replica, events,
                new SelectionStrategyResolver(), new ScoringPolicy(), clock, Duration.ofSeconds(graceSeconds),
                new Random());
        return TransactionalPorts.wrap(SimulationUseCases.class, service, tx);
    }

    @Bean
    QuestionReplicaSync questionReplicaSync(QuestionReplicaRepository replica, ProcessedEventStore processed,
                                            PlatformTransactionManager tx) {
        return TransactionalPorts.wrap(QuestionReplicaSync.class, new QuestionReplicaService(replica, processed), tx);
    }

    /** Colas propias del simulador (el consumidor declara sus colas). */
    @Bean
    Declarables questionPublishedQueue() {
        return CommonRabbitConfiguration.durableQueueWithDeadLetter(
                EventTopology.Q_SIMULATION_QUESTION_PUBLISHED, EventTopology.RK_QUESTION_PUBLISHED);
    }

    @Bean
    Declarables questionArchivedQueue() {
        return CommonRabbitConfiguration.durableQueueWithDeadLetter(
                EventTopology.Q_SIMULATION_QUESTION_ARCHIVED, EventTopology.RK_QUESTION_ARCHIVED);
    }
}

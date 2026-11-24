package co.edu.unicauca.saberpro.tracking.infrastructure.config;

import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.rabbit.CommonRabbitConfiguration;
import co.edu.unicauca.saberpro.tracking.application.port.in.RegisterSimulationResult;
import co.edu.unicauca.saberpro.tracking.application.port.in.TrackingQueries;
import co.edu.unicauca.saberpro.tracking.application.port.out.CompetenceStatsRepository;
import co.edu.unicauca.saberpro.tracking.application.port.out.ProcessedEventStore;
import co.edu.unicauca.saberpro.tracking.application.port.out.ReportsRepository;
import co.edu.unicauca.saberpro.tracking.application.port.out.SimulationResultRepository;
import co.edu.unicauca.saberpro.tracking.application.service.TrackingService;
import co.edu.unicauca.saberpro.tracking.domain.model.ClassificationPolicy;
import co.edu.unicauca.saberpro.tracking.domain.service.StatisticsCalculator;
import org.springframework.amqp.core.Declarables;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.time.Clock;

/** Composición del hexágono del seguimiento. */
@Configuration
public class TrackingBeans {

    @Bean
    TrackingService trackingServiceCore(SimulationResultRepository results, CompetenceStatsRepository stats,
                                        ReportsRepository reports, ProcessedEventStore processed,
                                        @Value("${tracking.classification.strength-threshold:70}") double strength,
                                        @Value("${tracking.classification.weakness-threshold:50}") double weakness,
                                        @Value("${tracking.classification.min-questions:5}") int minQuestions) {
        return new TrackingService(results, stats, reports, processed,
                new StatisticsCalculator(new ClassificationPolicy(strength, weakness, minQuestions)), Clock.systemUTC());
    }

    /** @Primary: el núcleo también implementa los puertos; los adaptadores deben recibir el proxy transaccional. */
    @Bean
    @Primary
    RegisterSimulationResult registerSimulationResult(TrackingService core, PlatformTransactionManager tx) {
        return transactional(RegisterSimulationResult.class, core, tx);
    }

    @Bean
    @Primary
    TrackingQueries trackingQueries(TrackingService core, PlatformTransactionManager tx) {
        return transactional(TrackingQueries.class, core, tx);
    }

    @Bean
    Declarables simulationFinishedQueue() {
        return CommonRabbitConfiguration.durableQueueWithDeadLetter(
                EventTopology.Q_TRACKING_SIMULATION_FINISHED, EventTopology.RK_SIMULATION_FINISHED);
    }

    @SuppressWarnings("unchecked")
    private static <T> T transactional(Class<T> port, Object target, PlatformTransactionManager tx) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setInterfaces(port);
        factory.addAdvice(new TransactionInterceptor(tx, new MatchAlwaysTransactionAttributeSource()));
        return (T) factory.getProxy();
    }
}

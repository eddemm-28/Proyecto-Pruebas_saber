package co.edu.unicauca.saberpro.simulations.infrastructure.scheduling;

import co.edu.unicauca.saberpro.simulations.application.port.in.SimulationUseCases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** B07 / RF-23: adaptador de entrada temporal que cierra en servidor los simulacros vencidos. */
@Component
public class ExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpirationScheduler.class);

    private final SimulationUseCases useCases;

    public ExpirationScheduler(SimulationUseCases useCases) {
        this.useCases = useCases;
    }

    @Scheduled(fixedDelayString = "${simulation.expiration-check-ms:30000}", initialDelay = 10000)
    public void closeOverdue() {
        int closed = useCases.expireOverdue();
        if (closed > 0) {
            log.info("{} simulacro(s) vencido(s) cerrado(s) por el servidor", closed);
        }
    }
}

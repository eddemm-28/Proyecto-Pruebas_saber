package co.edu.unicauca.saberpro.simulations.application.port.in;

import co.edu.unicauca.saberpro.simulations.application.dto.AnswerOutcome;
import co.edu.unicauca.saberpro.simulations.application.dto.CatalogEntry;
import co.edu.unicauca.saberpro.simulations.application.dto.SimulationResultView;
import co.edu.unicauca.saberpro.simulations.application.dto.SimulationView;
import co.edu.unicauca.saberpro.simulations.application.dto.StudentRef;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puertos de entrada del simulador (los usa el adaptador web y el planificador). */
public interface SimulationUseCases {

    /** RF-21, RF-22, RF-23 (B06, B07). */
    SimulationView start(StudentRef student, SimulationConfig config);

    /** RF-23 (B07): rechaza respuestas fuera de tiempo. */
    AnswerOutcome answer(UUID simulationId, UUID questionId, String option, UUID studentId, String correlationId);

    /** RF-24 (B08, B09). */
    SimulationResultView finish(UUID simulationId, UUID studentId, String correlationId);

    SimulationView get(UUID simulationId, UUID studentId, String correlationId);

    Optional<SimulationView> active(UUID studentId, String correlationId);

    SimulationResultView result(UUID simulationId, UUID studentId);

    List<CatalogEntry> catalog();

    /** B07: cierre en servidor de los simulacros vencidos. @return cuántos se cerraron */
    int expireOverdue();
}

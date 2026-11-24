package co.edu.unicauca.saberpro.tracking.application.port.out;

import co.edu.unicauca.saberpro.tracking.domain.model.CompetenceStat;

import java.util.List;
import java.util.UUID;

public interface CompetenceStatsRepository {

    List<CompetenceStat> findByStudent(UUID studentId);

    void saveAll(List<CompetenceStat> stats);
}

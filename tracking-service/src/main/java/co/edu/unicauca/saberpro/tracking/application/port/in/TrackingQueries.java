package co.edu.unicauca.saberpro.tracking.application.port.in;

import co.edu.unicauca.saberpro.tracking.domain.model.Reports;
import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult;
import co.edu.unicauca.saberpro.tracking.domain.model.StudentStatistics;

import java.util.List;
import java.util.UUID;

/** B11–B13: consultas del seguimiento académico. */
public interface TrackingQueries {

    HistoryPage history(UUID studentId, int page, int size);

    StudentStatistics statistics(UUID studentId);

    List<Reports.CompetenceRow> competenceReport(Reports.Period period);

    List<Reports.StudentRow> studentReport(Reports.Period period);

    /** Temas ordenados de menor a mayor tasa de acierto (los más débiles primero). */
    List<Reports.TopicRow> topicReport(Reports.Period period, int limit);

    record HistoryPage(List<SimulationResult> content, int page, int size, long totalElements, int totalPages) {
    }
}

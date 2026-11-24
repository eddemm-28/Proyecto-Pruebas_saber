package co.edu.unicauca.saberpro.tracking.application.port.out;

import co.edu.unicauca.saberpro.tracking.domain.model.Reports;

import java.util.List;

/** Consultas agregadas para reportes (RF-28); el adaptador las resuelve con GROUP BY en SQL. */
public interface ReportsRepository {

    List<Reports.CompetenceRow> byCompetence(Reports.Period period);

    List<Reports.StudentRow> byStudent(Reports.Period period);

    List<Reports.TopicRow> byTopic(Reports.Period period);
}

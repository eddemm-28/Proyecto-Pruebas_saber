package co.edu.unicauca.saberpro.tracking.infrastructure.web;

import co.edu.unicauca.saberpro.tracking.application.port.in.TrackingQueries;
import co.edu.unicauca.saberpro.tracking.domain.model.Reports;
import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult;
import co.edu.unicauca.saberpro.tracking.domain.model.StudentStatistics;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** Adaptador de entrada REST del seguimiento (B11–B13). */
@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final TrackingQueries queries;

    public TrackingController(TrackingQueries queries) {
        this.queries = queries;
    }

    public record HistoryItem(UUID simulationId, String finalStatus, String strategy, Instant startedAt,
                              Instant finishedAt, long durationSeconds, int totalQuestions, int correctAnswers,
                              double globalScore, List<SimulationResult.CompetenceResult> competenceScores) {
        static HistoryItem from(SimulationResult r) {
            return new HistoryItem(r.simulationId(), r.finalStatus(), r.strategy(), r.startedAt(), r.finishedAt(),
                    r.durationSeconds(), r.totalQuestions(), r.correctAnswers(), r.globalScore(), r.competenceScores());
        }
    }

    public record HistoryResponse(List<HistoryItem> content, int page, int size, long totalElements, int totalPages) {
        static HistoryResponse from(TrackingQueries.HistoryPage p) {
            return new HistoryResponse(p.content().stream().map(HistoryItem::from).toList(), p.page(), p.size(),
                    p.totalElements(), p.totalPages());
        }
    }

    // ------------------------------------------------------------ Estudiante (B11, B12)

    @GetMapping("/students/me/history")
    public HistoryResponse myHistory(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return HistoryResponse.from(queries.history(currentUserId(auth), page, size));
    }

    @GetMapping("/students/me/stats")
    public StudentStatistics myStats(Authentication auth) {
        return queries.statistics(currentUserId(auth));
    }

    // ------------------------------------------------------------ Docente / Coordinador (B11–B13)

    @GetMapping("/students/{studentId}/history")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public HistoryResponse studentHistory(@PathVariable UUID studentId, @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return HistoryResponse.from(queries.history(studentId, page, size));
    }

    @GetMapping("/students/{studentId}/stats")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public StudentStatistics studentStats(@PathVariable UUID studentId) {
        return queries.statistics(studentId);
    }

    @GetMapping("/reports/competences")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public List<Reports.CompetenceRow> competences(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return queries.competenceReport(period(from, to));
    }

    @GetMapping("/reports/students")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public List<Reports.StudentRow> students(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return queries.studentReport(period(from, to));
    }

    @GetMapping("/reports/topics")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMINISTRADOR')")
    public List<Reports.TopicRow> topics(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "20") int limit) {
        return queries.topicReport(period(from, to), limit);
    }

    /** {@code to} es inclusivo: se toma hasta el inicio del día siguiente. */
    private static Reports.Period period(LocalDate from, LocalDate to) {
        return new Reports.Period(from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC),
                to == null ? null : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private static UUID currentUserId(Authentication auth) {
        return UUID.fromString(((JwtAuthenticationToken) auth).getToken().getSubject());
    }
}

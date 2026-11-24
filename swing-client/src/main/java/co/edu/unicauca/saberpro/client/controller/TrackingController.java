package co.edu.unicauca.saberpro.client.controller;

import co.edu.unicauca.saberpro.client.infra.ApiClient;
import co.edu.unicauca.saberpro.client.model.Dtos;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** B15 y B16: historial, estadísticas y reportes. */
public class TrackingController {

    private final ApiClient api;

    public TrackingController(ApiClient api) {
        this.api = api;
    }

    public Dtos.Page<Dtos.HistoryItem> myHistory(int page) {
        return api.get("/api/tracking/students/me/history", Map.of("page", page, "size", 20),
                new TypeReference<Dtos.Page<Dtos.HistoryItem>>() { });
    }

    public Dtos.StudentStats myStats() {
        return api.get("/api/tracking/students/me/stats", null, Dtos.StudentStats.class);
    }

    public Dtos.StudentStats studentStats(UUID studentId) {
        return api.get("/api/tracking/students/" + studentId + "/stats", null, Dtos.StudentStats.class);
    }

    public List<Dtos.CompetenceReportRow> competenceReport(LocalDate from, LocalDate to) {
        return api.get("/api/tracking/reports/competences", period(from, to),
                new TypeReference<List<Dtos.CompetenceReportRow>>() { });
    }

    public List<Dtos.StudentReportRow> studentReport(LocalDate from, LocalDate to) {
        return api.get("/api/tracking/reports/students", period(from, to),
                new TypeReference<List<Dtos.StudentReportRow>>() { });
    }

    public List<Dtos.TopicReportRow> topicReport(LocalDate from, LocalDate to) {
        Map<String, Object> q = period(from, to);
        q.put("limit", 20);
        return api.get("/api/tracking/reports/topics", q, new TypeReference<List<Dtos.TopicReportRow>>() { });
    }

    private static Map<String, Object> period(LocalDate from, LocalDate to) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("from", from);
        q.put("to", to);
        return q;
    }
}

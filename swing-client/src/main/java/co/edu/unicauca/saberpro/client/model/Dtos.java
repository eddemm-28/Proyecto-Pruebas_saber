package co.edu.unicauca.saberpro.client.model;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Contratos JSON que el cliente intercambia con el api-gateway. */
public final class Dtos {

    private Dtos() {
    }

    // ------------------------------------------------------------ usuarios
    public record User(UUID id, String login, String fullName, String email, String role, String roleName,
                       String status, Instant createdAt) {
    }

    public record LoginRequest(String login, String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, Instant expiresAt, User user) {
    }

    public record RegisterRequest(String login, String fullName, String email, String password) {
    }

    public record ChangeRole(String role) {
    }

    public record ChangeStatus(String status) {
    }

    public record LoginAudit(Long id, UUID userId, String login, boolean success, String failureReason,
                             String ipAddress, String clientAgent, Instant occurredAt) {
    }

    public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> Page<T> empty() {
            return new Page<>(List.of(), 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------ preguntas
    public record Option(String label, String text) {
    }

    public record QuestionSummary(UUID id, String code, String title, String type, String competence, String topic,
                                  String subtopic, String difficulty, String state, String stateName,
                                  String authorLogin, Instant updatedAt) {
    }

    public record Question(UUID id, String code, String title, String type, String context, String statement,
                           List<Option> options, String correctOption, String justification, String bibliography,
                           String competence, String topic, String subtopic, String difficulty, String difficultyName,
                           String state, String stateName, boolean editable, Set<String> allowedActions,
                           UUID authorId, String authorLogin, String pluginId, String mediaUrl, Instant createdAt,
                           Instant updatedAt, Instant publishedAt, Instant archivedAt, long version) {
    }

    public record QuestionRequest(String title, String type, String context, String statement, List<String> options,
                                  String correctOption, String justification, String bibliography, String competence,
                                  String topic, String subtopic, String difficulty, String mediaUrl, Long version) {
    }

    public record PluginRequest(String title, String context, String statement, List<String> options,
                                String correctAnswer, String justification, String bibliography, String competence,
                                String topic, String subtopic, String difficulty, String mediaUrl) {
    }

    public record PluginInfo(String id, String name, String type) {
    }

    public record Transition(String comment) {
    }

    public record ChangeHistory(Long id, String field, String oldValue, String newValue, String changedBy, Instant changedAt) {
    }

    public record StateAudit(Long id, String fromState, String toState, String changedBy, String comment, Instant changedAt) {
    }

    // ------------------------------------------------------------ simulacros
    public record CatalogEntry(String competence, String topic, String difficulty, long available) {
    }

    public record StartSimulation(String strategy, String competence, String topic, String difficulty,
                                  Integer questionCount, Integer timeLimitMinutes) {
    }

    public record SimulationItem(int position, UUID questionId, String code, String competence, String context,
                                 String statement, List<Option> options, String selectedOption) {
    }

    public record Simulation(UUID id, String status, String strategy, Instant startedAt, Instant expiresAt,
                             Instant serverTime, long remainingSeconds, int questionCount, int answeredCount,
                             List<SimulationItem> items) {
    }

    public record Answer(String selectedOption) {
    }

    public record AnswerOutcome(boolean accepted, String status, long remainingSeconds, String message) {
    }

    public record CompetenceScore(String competence, int totalQuestions, int correctAnswers, double score) {
    }

    public record ResultItem(int position, UUID questionId, String competence, String topic, String statement,
                             List<Option> options, String selectedOption, String correctOption, boolean correct,
                             String justification) {
    }

    public record SimulationResult(UUID id, String status, String strategy, Instant startedAt, Instant finishedAt,
                                   int totalQuestions, int answeredQuestions, int correctAnswers, double globalScore,
                                   List<CompetenceScore> competenceScores, List<ResultItem> items) {
    }

    // ------------------------------------------------------------ seguimiento
    public record HistoryCompetence(String competence, int totalQuestions, int correctAnswers, double score) {
    }

    public record HistoryItem(UUID simulationId, String finalStatus, String strategy, Instant startedAt,
                              Instant finishedAt, long durationSeconds, int totalQuestions, int correctAnswers,
                              double globalScore, List<HistoryCompetence> competenceScores) {
    }

    public record CompetenceStat(UUID studentId, String studentLogin, String competence, int simulationsCount,
                                 int totalQuestions, int correctAnswers, double averageScore, double lastScore,
                                 String classification, Instant updatedAt) {
    }

    public record TrendPoint(UUID simulationId, Instant finishedAt, double globalScore) {
    }

    public record StudentStats(UUID studentId, String studentLogin, int simulationsCount, double averageGlobalScore,
                               double bestGlobalScore, double lastGlobalScore, String trend,
                               List<TrendPoint> recentScores, List<CompetenceStat> competences,
                               List<String> strengths, List<String> weaknesses) {
    }

    public record CompetenceReportRow(String competence, long students, long simulations, long totalQuestions,
                                      long correctAnswers, double averageScore) {
    }

    public record StudentReportRow(UUID studentId, String studentLogin, long simulations, double averageScore,
                                   double bestScore, Instant lastFinishedAt) {
    }

    public record TopicReportRow(String competence, String topic, long totalAnswers, long correctAnswers, double accuracy) {
    }
}

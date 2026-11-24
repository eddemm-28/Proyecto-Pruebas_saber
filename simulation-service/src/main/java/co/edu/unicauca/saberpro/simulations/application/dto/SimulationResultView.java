package co.edu.unicauca.saberpro.simulations.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Resultado inmediato (RF-24): puntajes y, por pregunta, la respuesta correcta y su justificación. */
public record SimulationResultView(
        UUID id,
        String status,
        String strategy,
        Instant startedAt,
        Instant finishedAt,
        int totalQuestions,
        int answeredQuestions,
        int correctAnswers,
        double globalScore,
        List<CompetenceScoreView> competenceScores,
        List<Item> items) {

    public record CompetenceScoreView(String competence, int totalQuestions, int correctAnswers, double score) {
    }

    public record Item(int position, UUID questionId, String competence, String topic, String statement,
                       List<SimulationView.Option> options, String selectedOption, String correctOption,
                       boolean correct, String justification) {
    }
}

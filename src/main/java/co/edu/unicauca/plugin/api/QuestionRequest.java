package co.edu.unicauca.plugin.api;

import java.util.List;

public class QuestionRequest {
    private String title;
    private String content;
    private String type;
    private List<String> options;
    private String correctAnswer;
    private String competence;
    private String difficulty;

    public QuestionRequest() {}

    public QuestionRequest(String title, String content, String type,
                           List<String> options, String correctAnswer,
                           String competence, String difficulty) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.competence = competence;
        this.difficulty = difficulty;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getCompetence() { return competence; }
    public void setCompetence(String competence) { this.competence = competence; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}

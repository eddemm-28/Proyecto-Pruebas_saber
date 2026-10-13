package co.edu.unicauca.domain;

/**
 * Representa las opciones o distractores de una pregunta del banco Saber Pro.
 */
public class QuestionDistractors {
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    public QuestionDistractors() {
    }

    public QuestionDistractors(String optionA, String optionB, String optionC, String optionD) {
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    /**
     * Retorna una representación formateada multilínea de las opciones.
     */
    public String getFormattedText() {
        return "A. " + (optionA != null ? optionA : "") + "\n" +
               "B. " + (optionB != null ? optionB : "") + "\n" +
               "C. " + (optionC != null ? optionC : "") + "\n" +
               "D. " + (optionD != null ? optionD : "");
    }

    @Override
    public String toString() {
        return getFormattedText();
    }
}

package co.edu.unicauca.domain;

/**
 * Entidad principal del dominio que representa una pregunta en el Banco de Preguntas.
 */
public class Question {
    public static final String STATE_BORRADOR = "Borrador";
    public static final String STATE_PENDIENTE_REVISION = "Pendiente de revisión";
    public static final String STATE_ELIMINADA = "Eliminada";

    public static final String[] DIFICULTADES = {"Fácil", "Media", "Difícil"};

    private String id;
    private String nombre;
    private String pregunta;
    private QuestionDistractors distractors;
    private String respuestaCorrecta;
    private String estado;

    // Campos agregados para el primer corte (Diseño Centrado en Evidencia)
    private String contexto;
    private String justificacion;
    private String bibliografia;
    private String competencia;
    private String tema;
    private String subtema;
    private String dificultad;
    private String autor;

    public Question() {
    }

    public Question(String id, String nombre, String pregunta, QuestionDistractors distractors, String respuestaCorrecta, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.pregunta = pregunta;
        this.distractors = distractors;
        this.respuestaCorrecta = respuestaCorrecta;
        this.estado = estado;
    }

    public Question(String id, String nombre, String contexto, String pregunta,
                    QuestionDistractors distractors, String respuestaCorrecta,
                    String justificacion, String bibliografia, String competencia,
                    String tema, String subtema, String dificultad,
                    String estado, String autor) {
        this(id, nombre, pregunta, distractors, respuestaCorrecta, estado);
        this.contexto = contexto;
        this.justificacion = justificacion;
        this.bibliografia = bibliografia;
        this.competencia = competencia;
        this.tema = tema;
        this.subtema = subtema;
        this.dificultad = dificultad;
        this.autor = autor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public QuestionDistractors getDistractors() {
        return distractors;
    }

    public void setDistractors(QuestionDistractors distractors) {
        this.distractors = distractors;
    }

    public String getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    public void setRespuestaCorrecta(String respuestaCorrecta) {
        this.respuestaCorrecta = respuestaCorrecta;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getContexto() {
        return contexto;
    }

    public void setContexto(String contexto) {
        this.contexto = contexto;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(String justificacion) {
        this.justificacion = justificacion;
    }

    public String getBibliografia() {
        return bibliografia;
    }

    public void setBibliografia(String bibliografia) {
        this.bibliografia = bibliografia;
    }

    public String getCompetencia() {
        return competencia;
    }

    public void setCompetencia(String competencia) {
        this.competencia = competencia;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getSubtema() {
        return subtema;
    }

    public void setSubtema(String subtema) {
        this.subtema = subtema;
    }

    public String getDificultad() {
        return dificultad;
    }

    public void setDificultad(String dificultad) {
        this.dificultad = dificultad;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    @Override
    public String toString() {
        return id + " - " + nombre;
    }
}

package co.edu.unicauca.domain;

/**
 * Filtros para el listado de preguntas (HU de "mis preguntas").
 * Un campo vacío o nulo significa "sin filtro".
 */
public class QuestionFilter {
    private String estado;
    private String tema;
    private String competencia;
    private String dificultad;
    private String texto;

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }

    public String getCompetencia() { return competencia; }
    public void setCompetencia(String competencia) { this.competencia = competencia; }

    public String getDificultad() { return dificultad; }
    public void setDificultad(String dificultad) { this.dificultad = dificultad; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public boolean matches(Question q) {
        if (hay(estado) && !estado.equals(q.getEstado())) {
            return false;
        }
        if (hay(dificultad) && !dificultad.equals(q.getDificultad())) {
            return false;
        }
        if (hay(tema) && !contiene(q.getTema(), tema)) {
            return false;
        }
        if (hay(competencia) && !contiene(q.getCompetencia(), competencia)) {
            return false;
        }
        if (hay(texto)) {
            boolean enAlgunCampo = contiene(q.getNombre(), texto)
                    || contiene(q.getPregunta(), texto)
                    || contiene(q.getContexto(), texto);
            if (!enAlgunCampo) {
                return false;
            }
        }
        return true;
    }

    private boolean hay(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private boolean contiene(String campo, String buscado) {
        return campo != null && campo.toLowerCase().contains(buscado.trim().toLowerCase());
    }
}

package co.edu.unicauca.saberpro.client.infra;

import java.util.List;

/** Error devuelto por el gateway (cuerpo RFC 7807) o fallo de red. */
public class ApiException extends RuntimeException {

    private final int status;
    private final List<String> errors;
    private final String body;

    public ApiException(int status, String message, List<String> errors, String body) {
        super(message);
        this.status = status;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.body = body;
    }

    public int getStatus() { return status; }
    public List<String> getErrors() { return errors; }
    public String getBody() { return body; }

    public boolean isUnauthorized() { return status == 401; }
    public boolean isConflict() { return status == 409; }
    public boolean isNetworkError() { return status == 0; }

    /** Mensaje listo para un JOptionPane. */
    public String userMessage() {
        StringBuilder sb = new StringBuilder(getMessage() == null ? "Error" : getMessage());
        for (String e : errors) {
            sb.append("\n • ").append(e);
        }
        return sb.toString();
    }
}

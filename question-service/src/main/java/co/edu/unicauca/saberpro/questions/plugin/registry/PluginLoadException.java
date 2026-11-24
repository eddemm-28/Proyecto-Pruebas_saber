package co.edu.unicauca.saberpro.questions.plugin.registry;

import java.util.List;

public class PluginLoadException extends RuntimeException {

    private final List<String> details;

    public PluginLoadException(String message, List<String> details) {
        super(details.isEmpty() ? message : message + ": " + details);
        this.details = List.copyOf(details);
    }

    public List<String> getDetails() {
        return details;
    }
}

package co.edu.unicauca.saberpro.questions.plugin.registry;

import co.edu.unicauca.saberpro.questions.plugin.api.QuestionPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Registro de plugins del microkernel (A14, A21).
 *
 * <p>Corrige el fallo de {@code QuestionPluginManager.init()} del Corte 1: ya no es un
 * singleton estático que podía quedar {@code null}. Se construye una sola vez como bean de
 * Spring al arrancar; si no carga ningún plugin el servicio no arranca (fail-fast), y un plugin
 * defectuoso no impide cargar los demás (sus errores quedan en {@link #loadErrors()}).</p>
 */
public final class QuestionPluginRegistry {

    public static final String DEFAULT_RESOURCE = "plugins.properties";

    private final Map<String, QuestionPlugin> plugins;
    private final List<String> loadErrors;

    private QuestionPluginRegistry(Map<String, QuestionPlugin> plugins, List<String> loadErrors) {
        this.plugins = Collections.unmodifiableMap(plugins);
        this.loadErrors = List.copyOf(loadErrors);
    }

    public static QuestionPluginRegistry of(Collection<? extends QuestionPlugin> plugins) {
        Map<String, QuestionPlugin> map = new LinkedHashMap<>();
        plugins.forEach(p -> map.put(p.id(), p));
        if (map.isEmpty()) {
            throw new PluginLoadException("No hay plugins registrados", List.of());
        }
        return new QuestionPluginRegistry(map, List.of());
    }

    /** Carga los plugins declarados como {@code plugin.<nombre>=<clase>} en el recurso indicado. */
    public static QuestionPluginRegistry load(String resource, ClassLoader classLoader) {
        Properties props = new Properties();
        try (InputStream in = classLoader.getResourceAsStream(resource)) {
            if (in == null) {
                throw new PluginLoadException("No se encontró " + resource + " en el classpath", List.of());
            }
            props.load(in);
        } catch (IOException e) {
            throw new PluginLoadException("No se pudo leer " + resource + ": " + e.getMessage(), List.of());
        }

        Map<String, QuestionPlugin> map = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (String key : new TreeSet<>(props.stringPropertyNames())) {
            if (!key.startsWith("plugin.")) {
                continue;
            }
            String className = props.getProperty(key).trim();
            try {
                Object obj = Class.forName(className, true, classLoader).getDeclaredConstructor().newInstance();
                if (obj instanceof QuestionPlugin plugin) {
                    if (map.putIfAbsent(plugin.id(), plugin) != null) {
                        errors.add(className + ": id de plugin duplicado '" + plugin.id() + "'");
                    }
                } else {
                    errors.add(className + " no implementa QuestionPlugin");
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                errors.add(className + ": " + e);
            }
        }
        if (map.isEmpty()) {
            throw new PluginLoadException("No se pudo cargar ningún plugin", errors);
        }
        return new QuestionPluginRegistry(map, errors);
    }

    public Optional<QuestionPlugin> find(String id) {
        return Optional.ofNullable(plugins.get(id));
    }

    public Collection<QuestionPlugin> all() {
        return plugins.values();
    }

    public List<String> loadErrors() {
        return loadErrors;
    }
}

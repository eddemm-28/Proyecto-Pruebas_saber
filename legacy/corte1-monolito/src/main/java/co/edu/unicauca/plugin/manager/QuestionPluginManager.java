package co.edu.unicauca.plugin.manager;

import co.edu.unicauca.plugin.api.QuestionPlugin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Gestor de plugins del microkernel.
 *
 * <p>A21 (Corte 2): corrección del fallo de inicialización. Antes:
 * <ul>
 *   <li>{@code init()} asignaba {@code instance} antes de cargar, así que si la carga fallaba
 *       quedaba una instancia vacía o {@code null} y el microkernel fallaba después con un
 *       {@link NullPointerException} difícil de rastrear;</li>
 *   <li>un solo plugin mal configurado abortaba la carga de todos los demás;</li>
 *   <li>{@code Main} se tragaba la excepción y seguía como si nada.</li>
 * </ul>
 * Ahora la instancia solo se publica si la carga termina, cada plugin falla de forma aislada,
 * {@code init()} es idempotente y {@link #getInstance()} inicializa de forma perezosa o
 * lanza {@link IllegalStateException} con un mensaje claro.</p>
 */
public final class QuestionPluginManager {

    private static final String FILE_NAME = "plugins.properties";
    private static volatile QuestionPluginManager instance;

    private final Map<String, QuestionPlugin> plugins = new LinkedHashMap<>();
    private final List<String> loadErrors = new ArrayList<>();

    private QuestionPluginManager() {}

    public static QuestionPluginManager getInstance() {
        if (instance == null) {
            try {
                init();
            } catch (Exception e) {
                throw new IllegalStateException("El gestor de plugins no está inicializado: " + e.getMessage(), e);
            }
        }
        return instance;
    }

    public static synchronized void init() throws Exception {
        if (instance != null) {
            return;
        }
        QuestionPluginManager candidate = new QuestionPluginManager();
        candidate.loadPlugins();
        if (candidate.plugins.isEmpty()) {
            throw new Exception("No se pudo cargar ningún plugin. Errores: " + candidate.loadErrors);
        }
        instance = candidate;
    }

    /** Solo para pruebas. */
    static synchronized void reset() {
        instance = null;
    }

    private void loadPlugins() throws Exception {
        Properties props = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader() != null
                ? Thread.currentThread().getContextClassLoader()
                : QuestionPluginManager.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(FILE_NAME)) {
            if (in == null) {
                throw new Exception("No se encontró " + FILE_NAME + " en el classpath");
            }
            props.load(in);
        }

        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("plugin.")) continue;
            String className = props.getProperty(key).trim();
            try {
                Object obj = Class.forName(className, true, cl).getDeclaredConstructor().newInstance();
                if (obj instanceof QuestionPlugin plugin) {
                    plugins.put(plugin.getName(), plugin);
                } else {
                    loadErrors.add(className + " no implementa QuestionPlugin");
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                loadErrors.add(className + ": " + e);
            }
        }
        if (!loadErrors.isEmpty()) {
            System.err.println("Plugins que no se pudieron cargar: " + loadErrors);
        }
    }

    public QuestionPlugin getPluginFor(String type) {
        for (QuestionPlugin plugin : plugins.values()) {
            if (plugin.supports(type)) {
                return plugin;
            }
        }
        return null;
    }

    public List<QuestionPlugin> getAll() {
        return new ArrayList<>(plugins.values());
    }

    public List<String> getLoadErrors() {
        return Collections.unmodifiableList(loadErrors);
    }
}

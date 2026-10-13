package co.edu.unicauca.plugin.manager;

import co.edu.unicauca.plugin.api.QuestionPlugin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class QuestionPluginManager {

    private static final String FILE_NAME = "plugins.properties";
    private static QuestionPluginManager instance;

    private final Map<String, QuestionPlugin> plugins = new LinkedHashMap<>();

    private QuestionPluginManager() {}

    public static QuestionPluginManager getInstance() {
        return instance;
    }

    public static void init() throws Exception {
        instance = new QuestionPluginManager();
        instance.loadPlugins();
        if (instance.plugins.isEmpty()) {
            throw new Exception("No se pudieron cargar los plugins");
        }
    }

    private void loadPlugins() throws Exception {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in == null) {
                throw new Exception("No se encontró " + FILE_NAME);
            }
            props.load(in);
        }

        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("plugin.")) continue;
            String className = props.getProperty(key);
            Class<?> clazz = Class.forName(className);
            Object obj = clazz.getDeclaredConstructor().newInstance();
            if (obj instanceof QuestionPlugin) {
                QuestionPlugin plugin = (QuestionPlugin) obj;
                plugins.put(plugin.getName(), plugin);
            }
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
}

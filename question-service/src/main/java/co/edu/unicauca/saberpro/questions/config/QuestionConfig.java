package co.edu.unicauca.saberpro.questions.config;

import co.edu.unicauca.saberpro.questions.domain.validation.QuestionValidator;
import co.edu.unicauca.saberpro.questions.plugin.registry.QuestionPluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuestionConfig {

    private static final Logger log = LoggerFactory.getLogger(QuestionConfig.class);

    /**
     * A21: el registro de plugins se crea una sola vez al arrancar. Si no se puede cargar ningún
     * plugin, el contexto de Spring falla y el contenedor no queda "sano" a medias.
     */
    @Bean
    QuestionPluginRegistry questionPluginRegistry() {
        QuestionPluginRegistry registry = QuestionPluginRegistry.load(
                QuestionPluginRegistry.DEFAULT_RESOURCE, QuestionConfig.class.getClassLoader());
        log.info("Plugins cargados: {}", registry.all().stream().map(p -> p.id()).toList());
        if (!registry.loadErrors().isEmpty()) {
            log.warn("Plugins con error de carga: {}", registry.loadErrors());
        }
        return registry;
    }

    @Bean
    QuestionValidator questionValidator() {
        return new QuestionValidator();
    }
}

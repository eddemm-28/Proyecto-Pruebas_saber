package co.edu.unicauca.saberpro.simulations.domain.strategy;

import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Elige la estrategia concreta según la configuración (contexto del patrón Strategy). */
public class SelectionStrategyResolver {

    private final Map<StrategyType, QuestionSelectionStrategy> strategies = new EnumMap<>(StrategyType.class);

    public SelectionStrategyResolver() {
        this(List.of(new ByCompetenceStrategy(), new ByTopicStrategy(), new ByDifficultyStrategy(),
                new MixedBalancedStrategy()));
    }

    public SelectionStrategyResolver(Collection<? extends QuestionSelectionStrategy> available) {
        available.forEach(s -> strategies.put(s.type(), s));
    }

    public QuestionSelectionStrategy resolve(StrategyType type) {
        QuestionSelectionStrategy s = strategies.get(type);
        if (s == null) {
            throw new IllegalArgumentException("Estrategia no soportada: " + type);
        }
        return s;
    }
}

package co.edu.unicauca.saberpro.questions.plugin.pipeline;

import java.util.List;

public class Pipeline<T> {

    private final List<Filter<T>> filters;

    public Pipeline(List<Filter<T>> filters) {
        this.filters = List.copyOf(filters);
    }

    public T execute(T input) {
        T result = input;
        for (Filter<T> filter : filters) {
            result = filter.process(result);
        }
        return result;
    }
}

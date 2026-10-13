package co.edu.unicauca.pipeline;

import java.util.List;

public class Pipeline<T> {

    private final List<Filter<T>> filters;

    public Pipeline(List<Filter<T>> filters) {
        this.filters = filters;
    }

    public T execute(T input) {
        T result = input;
        for (Filter<T> filter : filters) {
            result = filter.process(result);
        }
        return result;
    }
}

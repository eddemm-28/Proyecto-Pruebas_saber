package co.edu.unicauca.pipeline;

public interface Filter<T> {
    T process(T input);
}

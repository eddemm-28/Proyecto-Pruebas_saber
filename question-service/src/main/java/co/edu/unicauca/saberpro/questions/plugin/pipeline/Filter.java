package co.edu.unicauca.saberpro.questions.plugin.pipeline;

/** Tuberías y Filtros (heredado del Taller 5). */
@FunctionalInterface
public interface Filter<T> {
    T process(T input);
}

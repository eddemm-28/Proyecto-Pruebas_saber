package co.edu.unicauca.infra;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase base para el Sujeto Observable en el patrón Observer.
 */
public abstract class Subject {
    private final List<Observer> observers = new ArrayList<>();

    public void addObserver(Observer observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notifyAllObservers() {
        for (Observer observer : observers) {
            observer.update(this);
        }
    }

    public void notifyAllObservers(Object arg) {
        for (Observer observer : observers) {
            observer.update(arg);
        }
    }
}

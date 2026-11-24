package co.edu.unicauca.saberpro.client.observer;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Sujeto observable. Las notificaciones se entregan siempre en el hilo de Swing. */
public class Subject {

    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    public void addObserver(Observer o) {
        if (o != null && !observers.contains(o)) {
            observers.add(o);
        }
    }

    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    protected void notifyObservers(String event, Object data) {
        Runnable r = () -> observers.forEach(o -> o.update(this, event, data));
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    public int observerCount() {
        return observers.size();
    }
}

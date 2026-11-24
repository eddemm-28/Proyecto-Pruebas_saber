package co.edu.unicauca.saberpro.client.infra;

import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Ejecuta llamadas HTTP fuera del hilo de Swing (EDT) y entrega el resultado en el EDT. */
public final class Async {

    private Async() {
    }

    public static <T> void run(Callable<T> task, Consumer<T> onSuccess, Consumer<ApiException> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    ApiException api = cause instanceof ApiException a ? a
                            : new ApiException(0, "Error inesperado: " + cause, java.util.List.of(), null);
                    onError.accept(api);
                }
            }
        }.execute();
    }
}

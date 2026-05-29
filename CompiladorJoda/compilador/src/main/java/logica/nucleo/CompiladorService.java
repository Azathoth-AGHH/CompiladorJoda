package logica.nucleo;

import java.util.function.Consumer;
import java.util.function.Function;

public final class CompiladorService {

    private final CompiladorJoda compilador = new CompiladorJoda();

    public void setInputCallback(Function<String, String> callback) {
        compilador.setInputCallback(callback);
    }

    public void compilarAsync(String rutaArchivo,
                               Consumer<ResultadoCompilacion> onSuccess,
                               Consumer<Throwable> onError) {
        Thread hilo = new Thread(() -> {
            try {
                ResultadoCompilacion resultado = compilador.compilarYEjecutar(rutaArchivo);
                onSuccess.accept(resultado);
            } catch (Exception e) {
                onError.accept(e);
            }
        }, "joda-compilador");
        hilo.setDaemon(true);
        hilo.start();
    }
}
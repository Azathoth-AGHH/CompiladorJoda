package vista_consola;

import logica.nucleo.CompiladorJoda;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Punto de entrada de la aplicacion de consola del Compilador JODA.
 *
 * Uso:
 *   java -cp out vista_consola.Main <ruta_archivo.joda>
 *
 * Si no se pasa argumento, se carga el archivo de ejemplo por defecto.
 */
public class Main {

    private static final String ARCHIVO_EJEMPLO = "recursos/ejemplo.joda";

    public static void main(String[] args) {

        String rutaArchivo = args.length > 0 ? args[0] : ARCHIVO_EJEMPLO;
        String codigoFuente;

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(rutaArchivo));
            codigoFuente = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("[ERROR] No se pudo leer el archivo: "
                    + ImpresorResultados.limpiar(rutaArchivo));
            System.out.println("  Causa: " + ImpresorResultados.limpiar(e.getMessage()));
            System.out.println("  Uso: java -cp out vista_consola.Main <ruta_archivo.joda>");
            return;
        }

        // -- Compilacion --
        CompiladorJoda compilador = new CompiladorJoda();
        CompiladorJoda.ResultadoCompilacion resultado = compilador.compilar(codigoFuente);

        // -- Impresion de resultados --
        ImpresorResultados impresor = new ImpresorResultados();
        impresor.imprimir(resultado);
    }
}
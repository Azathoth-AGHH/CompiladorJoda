package vista_consola;

import logica.nucleo.CompiladorJoda;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada del Compilador JODA (Modo Consola)
 *
 * Uso:
 * java -cp out vista_consola.Main archivo.joda
 *
 * Si no se envia archivo, usa recursos/ejemplo.joda
 */
public class Main {

    private static final String ARCHIVO_EJEMPLO = "recursos/ejemplo.joda";

    public static void main(String[] args) {

        configurarConsola();
        mostrarBienvenida();

        String rutaArchivo = obtenerRutaArchivo(args);
        String codigoFuente = leerArchivo(rutaArchivo);

        if (codigoFuente == null) {
            return;
        }

        ejecutarCompilacion(codigoFuente);
    }

    // ------------------------------------------------------------
    // CONFIGURACION INICIAL
    // ------------------------------------------------------------

    private static void configurarConsola() {
        try {
            System.setProperty("file.encoding", "UTF-8");
        } catch (Exception ignored) {
        }
    }

    private static void mostrarBienvenida() {
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("              COMPILADOR JODA v1.0");
        System.out.println("     Joint Object-Deployment Assembly");
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println();
    }

    // ------------------------------------------------------------
    // ARCHIVO
    // ------------------------------------------------------------

    private static String obtenerRutaArchivo(String[] args) {

        if (args.length > 0) {
            return args[0];
        }

        System.out.println("[INFO] No se especifico archivo.");
        System.out.println("[INFO] Se utilizara archivo de ejemplo.\n");

        return ARCHIVO_EJEMPLO;
    }

    private static String leerArchivo(String rutaArchivo) {

        try {
            Path ruta = Paths.get(rutaArchivo);
            byte[] bytes = Files.readAllBytes(ruta);

            System.out.println("[OK] Archivo cargado correctamente:");
            System.out.println("     " + rutaArchivo + "\n");

            return new String(bytes, StandardCharsets.UTF_8);

        } catch (IOException e) {

            System.out.println("[ERROR] No se pudo leer el archivo.");
            System.out.println("Ruta: " + rutaArchivo);
            System.out.println("Causa: " + e.getMessage());
            System.out.println();
            System.out.println("Uso correcto:");
            System.out.println("java -cp out vista_consola.Main archivo.joda");

            return null;
        }
    }

    // ------------------------------------------------------------
    // COMPILACION
    // ------------------------------------------------------------

    private static void ejecutarCompilacion(String codigoFuente) {

        try {
            CompiladorJoda compilador = new CompiladorJoda();

            CompiladorJoda.ResultadoCompilacion resultado =
                    compilador.compilar(codigoFuente);

            ImpresorResultados impresor = new ImpresorResultados();
            impresor.imprimir(resultado);

        } catch (Exception e) {

            System.out.println("[ERROR INTERNO DEL COMPILADOR]");
            System.out.println(e.getMessage());
        }
    }
}
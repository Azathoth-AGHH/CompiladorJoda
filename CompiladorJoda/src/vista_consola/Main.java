package vista_consola;

import logica.nucleo.CompiladorJoda;
import logica.nucleo.ResultadoCompilacion;

/*Clase principal del compilador
Coordina la ejecucion del pipeline(conjunto de procesos) hibrido y
manda a llamar a ImpresorResultados*/
public class Main {

    private static final String ARCHIVO_DEFECTO = "recursos/ejemplo.joda";

    public static void main(String[] args) {
        // Determina la ruta del archivo a compilar
        String rutaArchivo;
        if (args.length >= 1) {
            rutaArchivo = args[0];
        } else {
            rutaArchivo = ARCHIVO_DEFECTO;
        }

        // Instanciar el compilador (nucleo del sistema hibrido)
        CompiladorJoda compilador = new CompiladorJoda();


        /*Ejecuta el pipeline de esta forma:
            1.Lectura
            2.Lexico
            3.Documentacion
            4.Sintactico
            5.Semantico
            6.JVM-J (si no hay errores)
         */
        ResultadoCompilacion resultado = compilador.compilarYEjecutar(rutaArchivo);

        // Manda a llamar a ImpresorResultados
        ImpresorResultados impresor = new ImpresorResultados();
        impresor.imprimirReporte(resultado);
    }
}
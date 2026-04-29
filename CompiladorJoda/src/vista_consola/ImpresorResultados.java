package vista_consola;

import logica.documentador.Documentador;
import logica.lexico.Token;
import logica.nucleo.CompiladorJoda;
import logica.semantico.AnalizadorSemantico;
import logica.semantico.EntradaTablaSimbolos;
import logica.sintactico.AnalizadorSintactico;

import java.util.List;

/**
 * Responsable de imprimir en la terminal todos los resultados
 * producidos por el compilador JODA.
 *
 * Esta clase sera reemplazada en el futuro por una vista JavaFX.
 * Solo este paquete usa System.out.println.
 */
public class ImpresorResultados {

    // Ancho de columnas para la tabla del documentador
    private static final int COL_LINEA     =  6;
    private static final int COL_LEXEMA    = 28;
    private static final int COL_CATEGORIA = 26;
    private static final int COL_DETALLE   = 45;

    // ------------------------------------------------------------------ //
    //  Impresion del resultado completo                                    //
    // ------------------------------------------------------------------ //

    public void imprimir(CompiladorJoda.ResultadoCompilacion resultado) {
        imprimirEncabezado();
        imprimirSeparador('=', 80);

        imprimirErroresLexicos(resultado.getTokens());
        imprimirErroresSintacticos(resultado.getErroresSintacticos());
        imprimirErroresSemanticos(resultado.getResultadoSemantico());

        imprimirSeparador('=', 80);
        imprimirTablaDocumentador(resultado.getTablaDoc());

        imprimirSeparador('=', 80);
        imprimirTablaSimbolos(resultado.getResultadoSemantico()
                .getTablaSimbolos());

        imprimirSeparador('=', 80);
        if (resultado.isExitoso()) {
            imprimirSalidaEjecucion(resultado.getSalidaEjecucion());
        } else {
            System.out.println("  [COMPILADOR] Compilacion fallida. Corrija los errores antes de ejecutar.");
        }
        imprimirSeparador('=', 80);
    }

    // ------------------------------------------------------------------ //
    //  Secciones individuales                                              //
    // ------------------------------------------------------------------ //

    private void imprimirEncabezado() {
        System.out.println();
        imprimirSeparador('*', 80);
        System.out.println("  COMPILADOR JODA  -  Joint Object-Deployment Assembly");
        System.out.println("  Version: 1.0  |  Plataforma: JVM-J");
        imprimirSeparador('*', 80);
        System.out.println();
    }

    private void imprimirErroresLexicos(List<Token> tokens) {
        boolean hayErrores = false;
        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.ERROR) {
                if (!hayErrores) {
                    System.out.println("  --- ERRORES LEXICOS ---");
                    hayErrores = true;
                }
                System.out.printf("  [Linea %3d] Error lexico: caracter no reconocido '%s'%n",
                        t.getLinea(), limpiar(t.getLexema()));
            }
        }
        if (!hayErrores) {
            System.out.println("  [LEXICO] Analisis lexico completado sin errores.");
        }
        System.out.println();
    }

    private void imprimirErroresSintacticos(
            List<AnalizadorSintactico.ErrorSintactico> errores) {
        if (errores.isEmpty()) {
            System.out.println("  [SINTACTICO] Analisis sintactico completado sin errores.");
        } else {
            System.out.println("  --- ERRORES SINTACTICOS ---");
            for (AnalizadorSintactico.ErrorSintactico e : errores) {
                System.out.printf("  [Linea %3d] %s%n", e.getLinea(),
                        limpiar(e.getDescripcion()));
            }
        }
        System.out.println();
    }

    private void imprimirErroresSemanticos(
            AnalizadorSemantico.ResultadoSemantico resultado) {
        if (!resultado.tieneErrores()) {
            System.out.println("  [SEMANTICO] Analisis semantico completado sin errores.");
        } else {
            System.out.println("  --- ERRORES SEMANTICOS ---");
            for (AnalizadorSemantico.ErrorSemantico e : resultado.getErrores()) {
                System.out.printf("  [Linea %3d] %s%n", e.getLinea(),
                        limpiar(e.getDescripcion()));
            }
        }
        System.out.println();
    }

    private void imprimirTablaDocumentador(List<Documentador.FilaDoc> tabla) {
        System.out.println("  TABLA DE TOKENS (DOCUMENTADOR)");
        imprimirSeparador('-', 80);

        // Encabezado
        System.out.printf("  %-" + COL_LINEA     + "s"
                        + "%-" + COL_LEXEMA    + "s"
                        + "%-" + COL_CATEGORIA + "s"
                        + "%-" + COL_DETALLE   + "s%n",
                "Linea", "Lexema", "Categoria", "Descripcion");
        imprimirSeparador('-', 80);

        // Filas
        for (Documentador.FilaDoc fila : tabla) {
            System.out.printf("  %-" + COL_LINEA     + "d"
                            + "%-" + COL_LEXEMA    + "s"
                            + "%-" + COL_CATEGORIA + "s"
                            + "%-" + COL_DETALLE   + "s%n",
                    fila.getLinea(),
                    truncar(limpiar(fila.getLexema()),    COL_LEXEMA    - 2),
                    truncar(limpiar(fila.getCategoria()), COL_CATEGORIA - 2),
                    truncar(limpiar(fila.getDetalle()),   COL_DETALLE   - 2));
        }
        imprimirSeparador('-', 80);
        System.out.printf("  Total de tokens: %d%n%n", tabla.size());
    }

    private void imprimirTablaSimbolos(List<EntradaTablaSimbolos> tabla) {
        System.out.println("  TABLA DE SIMBOLOS (SEMANTICO)");
        imprimirSeparador('-', 80);
        System.out.printf("  %-20s %-10s %-15s %-12s %-6s%n",
                "Nombre", "Tipo", "Ambito", "Categoria", "Linea");
        imprimirSeparador('-', 80);
        for (EntradaTablaSimbolos e : tabla) {
            System.out.printf("  %-20s %-10s %-15s %-12s %-6d%n",
                    limpiar(e.getNombre()),
                    limpiar(e.getTipo()),
                    limpiar(e.getAmbito()),
                    e.getCategoria().name(),
                    e.getLineaDecl());
        }
        imprimirSeparador('-', 80);
        System.out.printf("  Total de simbolos: %d%n%n", tabla.size());
    }

    private void imprimirSalidaEjecucion(List<String> lineas) {
        System.out.println("  SALIDA DE EJECUCION (JVM-J)");
        imprimirSeparador('-', 80);
        if (lineas.isEmpty()) {
            System.out.println("  (sin salida)");
        } else {
            for (String linea : lineas) {
                System.out.println("  " + limpiar(linea));
            }
        }
        System.out.println();
    }

    // ------------------------------------------------------------------ //
    //  Utilidades de formato                                               //
    // ------------------------------------------------------------------ //

    private void imprimirSeparador(char caracter, int largo) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < largo - 2; i++) sb.append(caracter);
        System.out.println(sb.toString());
    }

    private String truncar(String texto, int maxLen) {
        if (texto == null) return "";
        return texto.length() > maxLen ? texto.substring(0, maxLen - 3) + "..." : texto;
    }

    /** Elimina caracteres especiales no ASCII para evitar simbolos ? en consola. */
    public static String limpiar(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
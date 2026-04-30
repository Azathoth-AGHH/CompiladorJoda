package vista_consola;

import logica.documentador.Documentador;
import logica.lexico.Token;
import logica.nucleo.CompiladorJoda;
import logica.semantico.AnalizadorSemantico;
import logica.semantico.EntradaTablaSimbolos;
import logica.sintactico.AnalizadorSintactico;

import java.util.List;

public class ImpresorResultados {

    private static final String OK = "[OK]";
    private static final String ERROR = "[ERROR]";
    private static final String INFO = "[INFO]";

    public void imprimir(CompiladorJoda.ResultadoCompilacion resultado) {

        encabezado();

        bloque("ANALISIS LEXICO");
        imprimirErroresLexicos(resultado.getTokens());

        bloque("ANALISIS SINTACTICO");
        imprimirErroresSintacticos(resultado.getErroresSintacticos());

        bloque("ANALISIS SEMANTICO");
        imprimirErroresSemanticos(resultado.getResultadoSemantico());

        bloque("TABLA DE TOKENS");
        imprimirTablaDocumentador(resultado.getTablaDoc());

        bloque("TABLA DE SIMBOLOS");
        imprimirTablaSimbolos(resultado.getResultadoSemantico().getTablaSimbolos()
        );

        bloque("RESULTADO FINAL");

        if (resultado.isExitoso()) {
            System.out.println("  " + OK + " COMPILACION EXITOSA");
            imprimirSalidaEjecucion(resultado.getSalidaEjecucion());
        } else {
            System.out.println("  " + ERROR + " COMPILACION FALLIDA");
            System.out.println("  Corrija los errores detectados.");
        }

        linea();
    }

    private void encabezado() {
        linea();
        System.out.println("         COMPILADOR JODA");
        System.out.println("         Version 1.0");
        linea();
    }

    private void bloque(String titulo) {
        System.out.println();
        System.out.println("┌──────────────────────────────────────────────┐");
        System.out.printf ("│ %-44s │%n", titulo);
        System.out.println("└──────────────────────────────────────────────┘");
    }

    private void linea() {
        System.out.println("════════════════════════════════════════════════════════════════════");
    }

    private void imprimirErroresLexicos(List<Token> tokens) {

        boolean hay = false;

        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.ERROR) {
                hay = true;
                System.out.printf("  %s Linea %d -> Caracter invalido: %s%n",
                        ERROR, t.getLinea(), t.getLexema());
            }
        }

        if (!hay)
            System.out.println("  " + OK + " Sin errores lexicos.");
    }

    private void imprimirErroresSintacticos(
            List<AnalizadorSintactico.ErrorSintactico> errores) {

        if (errores.isEmpty()) {
            System.out.println("  " + OK + " Sintaxis correcta.");
            return;
        }

        for (var e : errores) {
            System.out.printf("  %s Linea %d -> %s%n",
                    ERROR, e.getLinea(), e.getDescripcion());
        }
    }

    private void imprimirErroresSemanticos(
            AnalizadorSemantico.ResultadoSemantico r) {

        if (!r.tieneErrores()) {
            System.out.println("  " + OK + " Semantica valida.");
            return;
        }

        for (var e : r.getErrores()) {
            System.out.printf("  %s Linea %d -> %s%n",
                    ERROR, e.getLinea(), e.getDescripcion());
        }
    }

    private void imprimirTablaDocumentador(List<Documentador.FilaDoc> tabla) {

        System.out.printf("%-6s %-20s %-20s %-30s%n",
                "Linea", "Lexema", "Categoria", "Descripcion");

        linea();

        for (var f : tabla) {
            System.out.printf("%-6d %-20s %-20s %-30s%n",
                    f.getLinea(),
                    truncar(f.getLexema(),20),
                    truncar(f.getCategoria(),20),
                    truncar(f.getDetalle(),30));
        }

        System.out.println("\nTotal Tokens: " + tabla.size());
    }

    private void imprimirTablaSimbolos(List<EntradaTablaSimbolos> tabla) {

        System.out.printf("%-18s %-10s %-10s %-12s%n", "Nombre","Tipo","Ambito","Linea");

        linea();

        for (var e : tabla) {
            System.out.printf("%-18s %-10s %-10s %-12d%n",
                    e.getNombre(),
                    e.getTipo(),
                    e.getAmbito(),
                    e.getLineaDecl());
        }

        System.out.println("\nTotal Simbolos: " + tabla.size());
    }

    private void imprimirSalidaEjecucion(List<String> salida) {

        if (salida.isEmpty()) {
            System.out.println("  " + INFO + " Sin salida.");
            return;
        }

        System.out.println("\nSALIDA:");
        
        for(String s: salida){
            System.out.println("  > " + s);
        }

    }

    private String truncar(String t, int n) {
        if (t == null) return "";
        return t.length() > n ? t.substring(0,n-3)+"..." : t;
    }

    public static String limpiar(String texto) {
        return texto == null ? "" : texto;
    }
}
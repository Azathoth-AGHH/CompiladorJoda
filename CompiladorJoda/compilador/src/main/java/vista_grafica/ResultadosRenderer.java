package vista_grafica;

import java.util.List;

import logica.documentador.DocumentadorLinea;
import logica.lexico.Token;
import logica.nucleo.ResultadoCompilacion;

public final class ResultadosRenderer {

    private ResultadosRenderer() {}

    public static String renderizarErrores(ResultadoCompilacion r) {
        StringBuilder sb = new StringBuilder();
        appendSeccion(sb, "ERRORES LEXICOS",    r.getErroresLexicos());
        appendSeccion(sb, "ERRORES SINTACTICOS", r.getErroresSintacticos());
        appendSeccion(sb, "ERRORES SEMANTICOS",  r.getErroresSemanticos());
        appendSeccion(sb, "ADVERTENCIAS",        r.getAdvertenciasSemanticas());
        if (sb.isEmpty()) sb.append("No se detectaron errores ni advertencias.\n");
        return sb.toString();
    }

    public static String renderizarTokens(ResultadoCompilacion r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s  %-28s  %s%n", "LINEA", "TIPO DE TOKEN", "LEXEMA"));
        sb.append("-".repeat(70)).append('\n');
        for (Token t : r.getTokens()) {
            if (t.getTipo() == Token.Tipo.T_FIN_ARCHIVO) continue;
            sb.append(String.format("%-6d  %-28s  '%s'%n",
                t.getLinea(), t.getTipo().name(), t.getLexema()));
        }
        return sb.toString();
    }

    public static String renderizarDocDescriptiva(ResultadoCompilacion r) {
        List<String> lineas = new DocumentadorLinea()
            .documentarPorLinea(r.getTokens(), r.getCodigoFuente());
        StringBuilder sb = new StringBuilder(lineas.size() * 80);
        for (String l : lineas) sb.append(l).append('\n');
        return sb.toString();
    }

    public static String renderizarSalida(ResultadoCompilacion r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.isExitoCompilacion()
            ? "=== RESULTADO DE EJECUCION JVM-J ===\n\n"
            : "=== COMPILACION DETENIDA ===\n\n");
        List<String> salidas = r.getSalidasEjecucion();
        if (salidas.isEmpty()) {
            if (r.isExitoCompilacion()) sb.append("(Sin salida en consola)\n");
        } else {
            for (String s : salidas) sb.append(s).append('\n');
        }
        return sb.toString();
    }

    private static void appendSeccion(StringBuilder sb, String titulo, List<String> items) {
        if (items.isEmpty()) return;
        sb.append("=== ").append(titulo).append(" ===\n");
        for (String item : items) sb.append("  ").append(item).append('\n');
        sb.append('\n');
    }
}
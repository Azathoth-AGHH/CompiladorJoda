package vista_grafica;

import java.util.List;

import logica.documentador.DocumentadorLinea;
import logica.lexico.Token;
import logica.nucleo.ResultadoCompilacion;

public final class ResultadosRenderer {

    private ResultadosRenderer() {}

    // ---------------------------------------------------------------
    // ERRORES Y ADVERTENCIAS
    // ---------------------------------------------------------------

    public static String renderizarErrores(ResultadoCompilacion r) {
        StringBuilder sb = new StringBuilder();
        appendSeccion(sb, "ERRORES LEXICOS",     r.getErroresLexicos());
        appendSeccion(sb, "ERRORES SINTACTICOS", r.getErroresSintacticos());
        appendSeccion(sb, "ERRORES SEMANTICOS",  r.getErroresSemanticos());
        appendSeccion(sb, "ADVERTENCIAS",        r.getAdvertenciasSemanticas());
        if (sb.isEmpty()) sb.append("No se detectaron errores ni advertencias.\n");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // TOKENS (LEXICO)
    // Si hay errores: muestra cada error formateado como fila de la tabla
    //   LINEA  | TIPO_ERROR          | mensaje del error
    // Si no hay errores: tabla normal de tokens.
    // ---------------------------------------------------------------

    public static String renderizarTokens(ResultadoCompilacion r) {

        if (r.tieneErrores()) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-6s  %-28s  %s%n", "LINEA", "TIPO DE ERROR", "DESCRIPCION"));
            sb.append("-".repeat(70)).append('\n');

            appendFilasError(sb, r.getErroresLexicos(),     "ERROR_LEXICO");
            appendFilasError(sb, r.getErroresSintacticos(), "ERROR_SINTACTICO");
            appendFilasError(sb, r.getErroresSemanticos(),  "ERROR_SEMANTICO");

            if (sb.toString().endsWith("-".repeat(70) + "\n")) {
                // Solo habia advertencias, ningun error con linea
                appendFilasError(sb, r.getAdvertenciasSemanticas(), "ADVERTENCIA");
            }

            return sb.toString();
        }

        // ---- Tabla normal ----
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

    // ---------------------------------------------------------------
    // DOC. TECNICA (TOKEN A TOKEN)
    // Si hay errores: muestra cada error con el mismo estilo narrativo
    //   --- Linea N ---
    //   [TIPO_ERROR          ] Lexema: 'descripcion...'  -> mensaje
    // Si no hay errores: narrativa normal.
    // ---------------------------------------------------------------

    public static String renderizarDocTecnica(ResultadoCompilacion r) {

        if (r.tieneErrores()) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== DOCUMENTACION TECNICA - COMPILADOR JODA ===\n");
            sb.append("Errores detectados durante el analisis:\n\n");

            appendNarrativaErrores(sb, r.getErroresLexicos(),     "ERROR_LEXICO");
            appendNarrativaErrores(sb, r.getErroresSintacticos(), "ERROR_SINTACTICO");
            appendNarrativaErrores(sb, r.getErroresSemanticos(),  "ERROR_SEMANTICO");
            appendNarrativaErrores(sb, r.getAdvertenciasSemanticas(), "ADVERTENCIA");

            sb.append("\n=== FIN DE DOCUMENTACION ===\n");
            return sb.toString();
        }

        // ---- Narrativa normal (usa la documentacion ya generada) ----
        return r.getDocumentacion() != null ? r.getDocumentacion() : "";
    }

    // ---------------------------------------------------------------
    // DOC. DESCRIPTIVA (LINEA X LINEA)
    // Si hay errores: muestra cada error con el mismo formato
    //   Linea N    | [TIPO] mensaje del error
    // Si no hay errores: descriptiva normal.
    // ---------------------------------------------------------------

    public static String renderizarDocDescriptiva(ResultadoCompilacion r) {

        if (r.tieneErrores()) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== DOCUMENTACION DESCRIPTIVA - LINEA POR LINEA ===\n\n");

            appendDescriptivaErrores(sb, r.getErroresLexicos(),     "Error lexico");
            appendDescriptivaErrores(sb, r.getErroresSintacticos(), "Error sintactico");
            appendDescriptivaErrores(sb, r.getErroresSemanticos(),  "Error semantico");
            appendDescriptivaErrores(sb, r.getAdvertenciasSemanticas(), "Advertencia");

            sb.append("\n=== FIN DE DOCUMENTACION DESCRIPTIVA ===");
            return sb.toString();
        }

        // ---- Descriptiva normal ----
        if (r.getTokens().isEmpty() || r.getCodigoFuente() == null) return "";
        List<String> lineas = new DocumentadorLinea()
                .documentarPorLinea(r.getTokens(), r.getCodigoFuente());
        StringBuilder sb = new StringBuilder(lineas.size() * 80);
        for (String l : lineas) sb.append(l).append('\n');
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // SALIDA DE EJECUCION
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // HELPERS PRIVADOS
    // ---------------------------------------------------------------

    private static void appendSeccion(StringBuilder sb, String titulo, List<String> items) {
        if (items == null || items.isEmpty()) return;
        sb.append("=== ").append(titulo).append(" ===\n");
        for (String item : items) sb.append("  ").append(item).append('\n');
        sb.append('\n');
    }

    /**
     * Formatea cada mensaje de error como fila de la tabla de tokens:
     *   LINEA  TIPO_ERROR                    mensaje
     * Extrae el numero de linea del texto del mensaje si lo contiene.
     */
    private static void appendFilasError(StringBuilder sb,
                                          List<String> mensajes,
                                          String tipoColumna) {
        if (mensajes == null || mensajes.isEmpty()) return;
        for (String msg : mensajes) {
            int linea = extraerLinea(msg);
            String lineaStr = linea > 0 ? String.valueOf(linea) : "-";
            // El lexema/descripcion es el mensaje completo limpio
            String descripcion = limpiarMensaje(msg);
            sb.append(String.format("%-6s  %-28s  %s%n", lineaStr, tipoColumna, descripcion));
        }
    }

    /**
     * Formatea cada error con el estilo narrativo token-a-token:
     *   --- Linea N ---
     *   [TIPO_ERROR           ] Lexema: 'linea N'   -> mensaje
     */
    private static void appendNarrativaErrores(StringBuilder sb,
                                                List<String> mensajes,
                                                String tipo) {
        if (mensajes == null || mensajes.isEmpty()) return;
        for (String msg : mensajes) {
            int linea = extraerLinea(msg);
            sb.append("\n--- Linea ").append(linea > 0 ? linea : "?").append(" ---\n");
            sb.append("  [").append(padRight(tipo, 28)).append("] ");
            sb.append("Lexema: '").append(padRight("linea " + (linea > 0 ? linea : "?"), 20)).append("' -> ");
            sb.append(limpiarMensaje(msg)).append("\n");
        }
    }

    /**
     * Formatea cada error con el estilo descriptivo linea-por-linea:
     *   Linea N    | [Tipo] mensaje
     */
    private static void appendDescriptivaErrores(StringBuilder sb,
                                                   List<String> mensajes,
                                                   String etiqueta) {
        if (mensajes == null || mensajes.isEmpty()) return;
        for (String msg : mensajes) {
            int linea = extraerLinea(msg);
            String lineaStr = linea > 0 ? String.valueOf(linea) : "?";
            sb.append(String.format("Linea %-4s | [%s] %s%n",
                    lineaStr, etiqueta, limpiarMensaje(msg)));
        }
    }

    /**
     * Extrae el numero de linea de un mensaje de error del tipo:
     *   "Error lexico en linea 4: ..."
     *   "Error sintactico en linea 12: ..."
     * Retorna -1 si no encuentra el numero.
     */
    private static int extraerLinea(String mensaje) {
        if (mensaje == null) return -1;
        String lower = mensaje.toLowerCase();
        int idx = lower.indexOf("linea ");
        if (idx < 0) return -1;
        int start = idx + 6;
        StringBuilder num = new StringBuilder();
        while (start < mensaje.length() && Character.isDigit(mensaje.charAt(start))) {
            num.append(mensaje.charAt(start++));
        }
        try {
            return num.length() > 0 ? Integer.parseInt(num.toString()) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Elimina el prefijo redundante del mensaje para no repetirlo en la columna
     * cuando el tipo ya indica de que clase de error se trata.
     * Ejemplo:
     *   "Error lexico en linea 4: cadena no cerrada."
     *   ->  "cadena no cerrada."
     */
    private static String limpiarMensaje(String mensaje) {
        if (mensaje == null) return "";
        // Buscar el ':' que sigue al numero de linea y devolver lo que viene despues
        int idx = mensaje.indexOf(':');
        if (idx >= 0 && idx < mensaje.length() - 1) {
            return mensaje.substring(idx + 1).trim();
        }
        return mensaje.trim();
    }

    private static String padRight(String texto, int longitud) {
        if (texto == null) texto = "";
        if (texto.length() >= longitud) return texto.substring(0, longitud);
        return texto + " ".repeat(longitud - texto.length());
    }
}
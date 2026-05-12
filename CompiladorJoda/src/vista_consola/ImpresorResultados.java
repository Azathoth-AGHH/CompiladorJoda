package vista_consola;

import logica.lexico.Token;
import logica.nucleo.ResultadoCompilacion;
import logica.semantico.EntradaTablaSimbolos;

import java.util.List;

/*
Impresor de resultados del compilador JODA.
UNICA claseque usa System.out.println en todo el sistema.
Recibe un ResultadoCompilacion y formatea la salida en la consola.
 */
public class ImpresorResultados {

    // Anchos de columna para tablas
    private static final int COL_LEXEMA = 28;
    private static final int COL_TOKEN  = 28;
    private static final int COL_LINEA  = 6;

    // Imprime el reporte completo del proceso de compilacion.
    public void imprimirReporte(ResultadoCompilacion resultado) {
        imprimirEncabezado();
        imprimirInfoArchivo(resultado);
        imprimirCodigoFuente(resultado.getCodigoFuente());
        imprimirDocumentacion(resultado.getDocumentacion());
        imprimirTablaTokens(resultado.getTokens());
        imprimirErroresLexicos(resultado.getErroresLexicos());
        imprimirErroresSintacticos(resultado.getErroresSintacticos());
        imprimirErroresSemanticos(resultado.getErroresSemanticos());
        imprimirAdvertencias(resultado.getAdvertenciasSemanticas());
        imprimirTablaSimbolos(resultado.getTablaSimbolos());
        imprimirEstadoCompilacion(resultado);
        imprimirSalidaEjecucion(resultado);
    }

    // Secciones del reporte
    private void imprimirEncabezado() {
        linea('=', 70);
        centrar("COMPILADOR JODA v2.0  -  Joint Object-Deployment Assembly", 70);
        centrar("JVM-J: JODA Virtual Machine  |  Arquitectura Hibrida ISW", 70);
        linea('=', 70);
        System.out.println();
    }

    private void imprimirInfoArchivo(ResultadoCompilacion resultado) {
        titulo("INFORMACION DEL ARCHIVO");
        System.out.println("  Archivo : " + resultado.getNombreArchivo());
        System.out.println("  Eslogan : \"Precision in every line, power in every data\"");
        System.out.println();
    }

    private void imprimirCodigoFuente(String codigo) {
        titulo("CODIGO FUENTE JODA");
        if (codigo == null || codigo.isEmpty()) {
            System.out.println("  [Sin codigo fuente]");
        } else {
            String[] lineas = codigo.split("\n");
            for (int i = 0; i < lineas.length; i++) {
                System.out.printf("  %3d | %s%n", i + 1, lineas[i]);
            }
        }
        System.out.println();
    }


    private void imprimirDocumentacion(String doc) {
        titulo("DOCUMENTACION");
        if (doc == null || doc.isEmpty()) {
            System.out.println("  [Sin documentacion]");
            System.out.println();
            return;
        }
        String lineaActual = "";
        for (String linea : doc.split("\n")) {
            linea = linea.trim();
            if (linea.startsWith("--- Linea")) {
                String num = linea.replaceAll("[^0-9]", "");
                System.out.println("\n  Linea " + num + ":");
                lineaActual = num;
            } else if (linea.contains("Lexema")) {
                int idxLex = linea.indexOf("Lexema");
                int idxExp = linea.indexOf("->");
                if (idxLex != -1 && idxExp != -1) {
                    String lexema = linea.substring(idxLex, idxExp).trim();
                    String explicacion = linea.substring(idxExp).trim();
                    System.out.println("    " + lexema + "  " + explicacion);
                }
            }
        }
        System.out.println();
    }

    private void imprimirTablaTokens(List<Token> tokens) {
        titulo("TABLA DE TOKENS  (Analisis Lexico)");
        if (tokens == null || tokens.isEmpty()) {
            System.out.println("  [Sin tokens generados]");
            System.out.println();
            return;
        }

        // Encabezado de tabla
        String sep = "+" + repetir('-', COL_LINEA + 2)
                   + "+" + repetir('-', COL_LEXEMA + 2)
                   + "+" + repetir('-', COL_TOKEN + 2) + "+";
        System.out.println("  " + sep);
        System.out.printf("  | %-" + COL_LINEA + "s | %-" + COL_LEXEMA + "s | %-" + COL_TOKEN + "s |%n",
            "Linea", "Lexema", "Token");
        System.out.println("  " + sep);

        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.T_FIN_ARCHIVO) continue;
            String lexema = truncar(t.getLexema(), COL_LEXEMA);
            String tipo   = truncar(t.getTipo().name(), COL_TOKEN);
            System.out.printf("  | %-" + COL_LINEA + "d | %-" + COL_LEXEMA + "s | %-" + COL_TOKEN + "s |%n",
                t.getLinea(), lexema, tipo);
        }

        System.out.println("  " + sep);
        System.out.printf("  Total de tokens: %d%n", tokens.size() - 1);
        System.out.println();
    }

    private void imprimirErroresLexicos(List<String> errores) {
        if (errores == null || errores.isEmpty()) {
            System.out.println("  [OK] Analisis Lexico: Sin errores.");
            System.out.println();
            return;
        }
        titulo("ERRORES LEXICOS");
        for (String e : errores) {
            System.out.println("  [!] " + e);
        }
        System.out.println();
    }

    private void imprimirErroresSintacticos(List<String> errores) {
        if (errores == null || errores.isEmpty()) {
            System.out.println("  [OK] Analisis Sintactico: Sin errores.");
            System.out.println();
            return;
        }
        titulo("ERRORES SINTACTICOS");
        for (String e : errores) {
            System.out.println("  [!] " + e);
        }
        System.out.println();
    }

    private void imprimirErroresSemanticos(List<String> errores) {
        if (errores == null || errores.isEmpty()) {
            System.out.println("  [OK] Analisis Semantico: Sin errores.");
            System.out.println();
            return;
        }
        titulo("ERRORES SEMANTICOS");
        for (String e : errores) {
            System.out.println("  [!] " + e);
        }
        System.out.println();
    }

    private void imprimirAdvertencias(List<String> advertencias) {
        if (advertencias == null || advertencias.isEmpty()) return;
        titulo("ADVERTENCIAS");
        for (String a : advertencias) {
            System.out.println("  [~] " + a);
        }
        System.out.println();
    }

    private void imprimirTablaSimbolos(List<EntradaTablaSimbolos> tabla) {
        titulo("TABLA DE SIMBOLOS  (Analisis Semantico)");
        if (tabla == null || tabla.isEmpty()) {
            System.out.println("  [Sin simbolos declarados]");
            System.out.println();
            return;
        }

        int cNombre = 22, cTipo = 10, cCat = 12, cLinea = 6, cValor = 18;
        String sep = "+" + repetir('-', cNombre+2) + "+" + repetir('-', cTipo+2)
                   + "+" + repetir('-', cCat+2) + "+" + repetir('-', cLinea+2)
                   + "+" + repetir('-', cValor+2) + "+";

        System.out.println("  " + sep);
        System.out.printf("  | %-"+cNombre+"s | %-"+cTipo+"s | %-"+cCat+"s | %-"+cLinea+"s | %-"+cValor+"s |%n",
            "Nombre", "Tipo", "Categoria", "Linea", "Valor");
        System.out.println("  " + sep);

        for (EntradaTablaSimbolos e : tabla) {
            String valor = e.getValor() != null ? String.valueOf(e.getValor()) : "(sin valor)";
            System.out.printf("  | %-"+cNombre+"s | %-"+cTipo+"s | %-"+cCat+"s | %-"+cLinea+"d | %-"+cValor+"s |%n",
                truncar(e.getNombre(), cNombre),
                truncar(e.getTipoDato().name().toLowerCase(), cTipo),
                truncar(e.getCategoria().name().toLowerCase(), cCat),
                e.getLineaDeclaracion(),
                truncar(valor, cValor));
        }

        System.out.println("  " + sep);
        System.out.println();
    }

    private void imprimirEstadoCompilacion(ResultadoCompilacion resultado) {
        titulo("ESTADO DE COMPILACION");
        linea('-', 70);
        if (resultado.isExitoCompilacion()) {
            System.out.println("  [EXITO] Compilacion completada sin errores.");
            System.out.println("  [JVM-J] Activando fase de interpretacion/ejecucion...");
        } else {
            System.out.println("  [FALLO] La compilacion termino con errores.");
            System.out.println("  [JVM-J] La ejecucion fue detenida. Corrija los errores e intente de nuevo.");
        }
        linea('-', 70);
        System.out.println();
    }

    private void imprimirSalidaEjecucion(ResultadoCompilacion resultado) {
        if (!resultado.isExitoCompilacion()) return;

        titulo("SALIDA DE EJECUCION  (JVM-J: JODA Virtual Machine)");
        linea('-', 70);

        List<String> salidas = resultado.getSalidasEjecucion();
        if (salidas == null || salidas.isEmpty()) {
            System.out.println("  [JVM-J] El programa no genero salida en consola.");
        } else {
            for (String salida : salidas) {
                System.out.println(salida);
            }
        }

        linea('-', 70);
        if (resultado.isExitoEjecucion()) {
            System.out.println("  [JVM-J] Ejecucion finalizada exitosamente.");
        } else {
            System.out.println("  [JVM-J] La ejecucion termino con errores en tiempo de ejecucion.");
        }
        System.out.println();
    }

    // Utilidades de formato
    private void titulo(String texto) {
        System.out.println("  >> " + texto);
        linea('-', 70);
    }

    private void linea(char caracter, int longitud) {
        System.out.println(repetir(caracter, longitud));
    }

    private void centrar(String texto, int ancho) {
        int espacios = Math.max(0, (ancho - texto.length()) / 2);
        System.out.println(repetir(' ', espacios) + texto);
    }

    private String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private String truncar(String texto, int max) {
        if (texto == null) return "";
        if (texto.length() <= max) return texto;
        return texto.substring(0, max - 3) + "...";
    }
}
package logica.documentador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logica.lexico.Token;

public final class DocumentadorLinea implements IDocumentador {

    @Override
    public String documentar(List<Token> tokens, String codigoFuente) {
        List<String> lineas = documentarPorLinea(tokens, codigoFuente);
        StringBuilder sb = new StringBuilder(lineas.size() * 80);
        for (String l : lineas) sb.append(l).append('\n');
        return sb.toString();
    }

    public List<String> documentarPorLinea(List<Token> tokens, String codigoFuente) {
        Map<Integer, List<Token>> porLinea = agruparPorLinea(tokens);
        String[] lineasCodigo = codigoFuente.split("\n", -1);

        int maxLinea = porLinea.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

        List<String> resultado = new ArrayList<>(maxLinea + 2);
        resultado.add("=== DOCUMENTACION DESCRIPTIVA - LINEA POR LINEA ===\n");

        for (int num = 1; num <= maxLinea; num++) {
            List<Token> toks = porLinea.get(num);
            if (toks == null || toks.isEmpty()) continue;
            String textoLinea = num <= lineasCodigo.length ? lineasCodigo[num - 1].trim() : "";
            resultado.add(String.format("Linea %-4d | %s", num, generarDescripcion(toks, textoLinea)));
        }

        resultado.add("\n=== FIN DE DOCUMENTACION DESCRIPTIVA ===");
        return resultado;
    }

    private Map<Integer, List<Token>> agruparPorLinea(List<Token> tokens) {
        Map<Integer, List<Token>> mapa = new HashMap<>();
        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.T_FIN_ARCHIVO) continue;
            mapa.computeIfAbsent(t.getLinea(), k -> new ArrayList<>()).add(t);
        }
        return mapa;
    }

    private String generarDescripcion(List<Token> tokens, String textoLinea) {
        if (tokens.isEmpty()) return "(linea vacia)";
        Token primero = tokens.get(0);

        return switch (primero.getTipo()) {
            case T_ENTRY  -> "Inicio del bloque principal de ejecucion 'entry'.";
            case T_OBJECT -> "Definicion de la clase '" + tokenEn(tokens, 1) + "'.";
            case T_METHOD -> "Declaracion del metodo '" + tokenEn(tokens, 1) + "'.";
            case T_DEFINE -> describirDefine(tokens);
            case T_IDENTIFICADOR -> describirIdentificador(tokens, primero);
            case T_IF     -> "Inicio de condicional 'if'. Condicion: " + extraerEntreParen(tokens) + ".";
            case T_ELSE   -> "Bloque alternativo 'else'.";
            case T_LOOP   -> "Inicio de ciclo 'loop'. Condicion: " + extraerEntreParen(tokens) + ".";
            case T_SELECT -> "Seleccion multiple 'select' sobre: " + extraerEntreParen(tokens) + ".";
            case T_CASE   -> "Caso con valor '" + tokenEn(tokens, 1) + "'.";
            case T_OUT    -> "Salida en consola: " + extraerEntreParen(tokens) + ".";
            case T_INPUT  -> "Entrada de usuario en variable '" + extraerEntreParen(tokens) + "'.";
            case T_LLAVE_ABRE  -> "Apertura de bloque. Inicia nuevo ambito.";
            case T_LLAVE_CIERRA -> "Cierre de bloque. Ambito liberado.";
            case T_COMENTARIO  -> "Comentario: \"" + primero.getLexema() + "\".";
            case T_RETURN -> "Retorno de valor: " + tokenEn(tokens, 1) + ".";
            case T_INT, T_DEC, T_STRING, T_BOOL, T_VOID ->
                "Referencia al tipo de dato '" + primero.getLexema() + "'.";
            default -> textoLinea.isEmpty()
                ? "Token '" + primero.getTipo() + "': " + primero.getLexema() + "."
                : "Instruccion: " + (textoLinea.length() > 60
                    ? textoLinea.substring(0, 57) + "..." : textoLinea) + ".";
        };
    }

    private String describirDefine(List<Token> tokens) {
        if (tokens.size() < 3) return "Declaracion de variable con 'define'.";
        String tipo   = legibilizarTipo(tokens.get(1).getLexema());
        String nombre = tokens.get(2).getLexema();
        StringBuilder valor = new StringBuilder();
        boolean asignado = false;
        for (int i = 3; i < tokens.size(); i++) {
            if (tokens.get(i).getTipo() == Token.Tipo.T_ASIGNACION) { asignado = true; continue; }
            if (asignado && tokens.get(i).getTipo() != Token.Tipo.T_PUNTO_Y_COMA) {
                if (valor.length() > 0) valor.append(' ');
                valor.append(tokens.get(i).getLexema());
            }
        }
        return asignado && valor.length() > 0
            ? "Se declara variable " + tipo + " '" + nombre + "' con valor: " + valor + "."
            : "Se declara variable " + tipo + " '" + nombre + "' sin valor inicial.";
    }

    private String describirIdentificador(List<Token> tokens, Token primero) {
        if (tokens.size() < 2) return "Uso de identificador '" + primero.getLexema() + "'.";
        Token segundo = tokens.get(1);
        if (segundo.getTipo() == Token.Tipo.T_INCREMENTO)
            return "Incremento '++' sobre '" + primero.getLexema() + "'.";
        if (segundo.getTipo() == Token.Tipo.T_DECREMENTO)
            return "Decremento '--' sobre '" + primero.getLexema() + "'.";
        if (segundo.getTipo() == Token.Tipo.T_ASIGNACION) {
            StringBuilder val = new StringBuilder();
            for (int i = 2; i < tokens.size(); i++) {
                if (tokens.get(i).getTipo() != Token.Tipo.T_PUNTO_Y_COMA) {
                    if (val.length() > 0) val.append(' ');
                    val.append(tokens.get(i).getLexema());
                }
            }
            return "Asignacion de '" + val + "' a la variable '" + primero.getLexema() + "'.";
        }
        return "Uso del identificador '" + primero.getLexema() + "'.";
    }

    private String extraerEntreParen(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        boolean dentro = false;
        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.T_PARENTESIS_ABRE)  { dentro = true; continue; }
            if (t.getTipo() == Token.Tipo.T_PARENTESIS_CIERRA) break;
            if (dentro) { if (sb.length() > 0) sb.append(' '); sb.append(t.getLexema()); }
        }
        return sb.length() > 0 ? sb.toString() : "(sin parametros)";
    }

    private String tokenEn(List<Token> tokens, int idx) {
        return idx < tokens.size() ? tokens.get(idx).getLexema() : "?";
    }

    private String legibilizarTipo(String tipo) {
        return switch (tipo.toLowerCase()) {
            case "int"    -> "entera (int)";
            case "dec"    -> "decimal (dec)";
            case "string" -> "cadena (string)";
            case "bool"   -> "booleana (bool)";
            case "void"   -> "sin retorno (void)";
            default       -> "'" + tipo + "'";
        };
    }
}
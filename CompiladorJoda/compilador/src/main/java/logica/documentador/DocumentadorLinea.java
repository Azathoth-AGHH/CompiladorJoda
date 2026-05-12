package logica.documentador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logica.lexico.Token;

/**
 * DocumentadorLinea: genera una descripcion narrativa linea por linea del codigo JODA.
 * A diferencia del Documentador tecnico (que trabaja token a token),
 * este analiza el contexto de cada linea para producir frases comprensibles.
 *
 * Ejemplo de salida:
 *   Linea 1  | Inicio del bloque principal de ejecucion 'entry'.
 *   Linea 2  | Se declara la variable entera 'contador' con valor inicial 0.
 *   Linea 3  | Inicio de ciclo 'loop': se repite mientras 'contador < 10'.
 */
public class DocumentadorLinea {

    /**
     * Genera una lista de descripciones por linea a partir de los tokens.
     * Cada entrada del mapa: numero de linea -> descripcion narrativa.
     */
    public List<String> documentarPorLinea(List<Token> tokens, String codigoFuente) {
        // Agrupar tokens por numero de linea
        Map<Integer, List<Token>> tokensPorLinea = new HashMap<>();
        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.T_FIN_ARCHIVO) continue;
            tokensPorLinea
                .computeIfAbsent(t.getLinea(), k -> new ArrayList<>())
                .add(t);
        }

        // Dividir el codigo fuente en lineas para contexto textual
        String[] lineasCodigo = codigoFuente.split("\n", -1);

        List<String> resultado = new ArrayList<>();
        resultado.add("=== DOCUMENTACION DESCRIPTIVA - LINEA POR LINEA ===\n");

        int maxLinea = 0;
        for (int k : tokensPorLinea.keySet()) {
            if (k > maxLinea) maxLinea = k;
        }

        for (int num = 1; num <= maxLinea; num++) {
            List<Token> toksLinea = tokensPorLinea.get(num);
            if (toksLinea == null || toksLinea.isEmpty()) continue;

            String textoLinea = (num <= lineasCodigo.length)
                ? lineasCodigo[num - 1].trim() : "";

            String descripcion = generarDescripcion(toksLinea, textoLinea, num);
            resultado.add(String.format("Linea %-4d | %s", num, descripcion));
        }

        resultado.add("\n=== FIN DE DOCUMENTACION DESCRIPTIVA ===");
        return resultado;
    }

    /**
     * Analiza los tokens de una linea y genera la frase descriptiva.
     */
    private String generarDescripcion(List<Token> tokens, String textoLinea, int numLinea) {
        if (tokens.isEmpty()) return "(linea vacia)";

        Token primero = tokens.get(0);

        switch (primero.getTipo()) {

            // --- ESTRUCTURA ---
            case T_ENTRY:
                return "Inicio del bloque principal de ejecucion 'entry' del programa.";

            case T_OBJECT: {
                String nombre = tokens.size() > 1 ? tokens.get(1).getLexema() : "?";
                return "Definicion de la clase (objeto) '" + nombre + "'.";
            }

            case T_METHOD: {
                String nombre = tokens.size() > 1 ? tokens.get(1).getLexema() : "?";
                return "Declaracion del metodo '" + nombre + "'.";
            }

            // --- DECLARACION DE VARIABLES ---
            case T_DEFINE: {
                // Espera: define <tipo> <nombre> [= <valor>] ;
                if (tokens.size() >= 3) {
                    String tipo = tokens.get(1).getLexema();
                    String nombre = tokens.get(2).getLexema();
                    String tipoLeg = legibilizarTipo(tipo);

                    // Buscar si tiene asignacion
                    boolean tieneAsignacion = false;
                    StringBuilder valorSb = new StringBuilder();
                    for (int i = 3; i < tokens.size(); i++) {
                        if (tokens.get(i).getTipo() == Token.Tipo.T_ASIGNACION) {
                            tieneAsignacion = true;
                        } else if (tieneAsignacion
                                && tokens.get(i).getTipo() != Token.Tipo.T_PUNTO_Y_COMA) {
                            if (valorSb.length() > 0) valorSb.append(" ");
                            valorSb.append(tokens.get(i).getLexema());
                        }
                    }

                    if (tieneAsignacion && valorSb.length() > 0) {
                        return "Se declara la variable " + tipoLeg + " '" + nombre
                            + "' con valor inicial: " + valorSb + ".";
                    } else {
                        return "Se declara la variable " + tipoLeg + " '" + nombre
                            + "' sin valor inicial.";
                    }
                }
                return "Declaracion de variable con 'define'.";
            }

            // --- ASIGNACION ---
            case T_IDENTIFICADOR: {
                if (tokens.size() >= 3) {
                    Token segundo = tokens.get(1);
                    if (segundo.getTipo() == Token.Tipo.T_ASIGNACION) {
                        StringBuilder valSb = new StringBuilder();
                        for (int i = 2; i < tokens.size(); i++) {
                            if (tokens.get(i).getTipo() != Token.Tipo.T_PUNTO_Y_COMA) {
                                if (valSb.length() > 0) valSb.append(" ");
                                valSb.append(tokens.get(i).getLexema());
                            }
                        }
                        return "Se asigna el valor '" + valSb + "' a la variable '" + primero.getLexema() + "'.";
                    }
                    if (segundo.getTipo() == Token.Tipo.T_INCREMENTO) {
                        return "Se incrementa en 1 la variable '" + primero.getLexema() + "' (operacion '++').";
                    }
                    if (segundo.getTipo() == Token.Tipo.T_DECREMENTO) {
                        return "Se decrementa en 1 la variable '" + primero.getLexema() + "' (operacion '--').";
                    }
                }
                return "Referencia o uso de la variable '" + primero.getLexema() + "'.";
            }

            // --- CONTROL DE FLUJO ---
            case T_IF: {
                String cond = extraerCondicion(tokens);
                return "Inicio de estructura condicional 'if'. Condicion evaluada: " + cond + ".";
            }

            case T_ELSE:
                return "Bloque alternativo 'else': se ejecuta si la condicion del 'if' fue falsa.";

            case T_LOOP: {
                String cond = extraerCondicion(tokens);
                return "Inicio de ciclo 'loop'. Se repite mientras: " + cond + ".";
            }

            case T_SELECT: {
                String var = extraerEntreParen(tokens);
                return "Inicio de estructura de seleccion 'select' sobre la variable: " + var + ".";
            }

            case T_CASE: {
                String val = tokens.size() > 1 ? tokens.get(1).getLexema() : "?";
                return "Caso de seleccion con valor '" + val + "'.";
            }

            // --- ENTRADA / SALIDA ---
            case T_OUT: {
                String expr = extraerEntreParen(tokens);
                return "Instruccion de salida: se imprime en consola la expresion: " + expr + ".";
            }

            case T_INPUT: {
                String var = extraerEntreParen(tokens);
                return "Instruccion de entrada: se lee un valor del usuario y se guarda en '" + var + "'.";
            }

            // --- LLAVES ---
            case T_LLAVE_ABRE:
                return "Apertura de bloque de codigo. Inicia un nuevo ambito de memoria.";

            case T_LLAVE_CIERRA:
                return "Cierre de bloque de codigo. El ambito actual queda liberado.";

            // --- COMENTARIOS ---
            case T_COMENTARIO:
                return "Comentario del programador: \"" + primero.getLexema() + "\".";

            // --- RETURN ---
            case T_RETURN: {
                if (tokens.size() > 2) {
                    String val = tokens.get(1).getLexema();
                    return "Instruccion 'return': se retorna el valor '" + val + "' al punto de llamada.";
                }
                return "Instruccion 'return': fin de ejecucion del metodo actual.";
            }

            // --- TIPOS COMO PRIMERA PALABRA (por si acaso) ---
            case T_INT:
            case T_DEC:
            case T_STRING:
            case T_BOOL:
            case T_VOID:
                return "Referencia al tipo de dato '" + primero.getLexema() + "'.";

            default:
                // Intento generico basado en el contenido de la linea
                if (!textoLinea.isEmpty()) {
                    return "Instruccion: " + resumirLinea(textoLinea) + ".";
                }
                return "Token de tipo '" + primero.getTipo() + "' con valor '" + primero.getLexema() + "'.";
        }
    }

    // --- Utilidades ---

    /** Convierte el nombre tecnico del tipo al nombre legible en espanol. */
    private String legibilizarTipo(String tipo) {
        switch (tipo.toLowerCase()) {
            case "int":    return "entera (int)";
            case "dec":    return "decimal (dec)";
            case "string": return "cadena de texto (string)";
            case "bool":   return "booleana (bool)";
            case "void":   return "sin retorno (void)";
            case "object": return "de tipo objeto";
            default:       return "de tipo '" + tipo + "'";
        }
    }

    /**
     * Extrae la condicion entre parentesis de tokens como if/loop/select.
     * Ej: [T_IF, T_PAREN_ABRE, T_ID, T_MENOR, T_LIT, T_PAREN_CIERRA] -> "x < 10"
     */
    private String extraerCondicion(List<Token> tokens) {
        return extraerEntreParen(tokens);
    }

    private String extraerEntreParen(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        boolean dentro = false;
        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.T_PARENTESIS_ABRE) {
                dentro = true;
                continue;
            }
            if (t.getTipo() == Token.Tipo.T_PARENTESIS_CIERRA) {
                break;
            }
            if (dentro) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(t.getLexema());
            }
        }
        return sb.length() > 0 ? sb.toString() : "(sin parametros)";
    }

    /** Recorta la linea de codigo para que no sea muy larga. */
    private String resumirLinea(String linea) {
        if (linea.length() > 60) return linea.substring(0, 57) + "...";
        return linea;
    }
}
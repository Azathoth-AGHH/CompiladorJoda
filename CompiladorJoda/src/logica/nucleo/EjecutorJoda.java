package logica.nucleo;

import logica.lexico.Token;
import java.util.*;

/**
 * Ejecutor/Interprete simplificado para el lenguaje JODA.
 *
 * Recorre el bloque 'entry' y procesa las siguientes instrucciones:
 *   - out( expr )  -> genera una linea de salida
 *   - define tipo id = valor  -> almacena variable en memoria
 *   - id = expr  -> reasignacion de variable
 *   - if / loop -> flujo de control basico
 *
 * Este modulo simula la "JVM-J" descrita en el documento de JODA.
 */
public class EjecutorJoda {

    // Memoria de variables: nombre -> valor (como String para simplicidad)
    private final Map<String, String> memoria = new LinkedHashMap<>();
    private final List<String>        salida  = new ArrayList<>();

    /**
     * Ejecuta el bloque 'entry' encontrado en la lista de tokens.
     *
     * @param tokens lista completa de tokens del programa
     * @return lista de lineas generadas por instrucciones 'out'
     */
    public List<String> ejecutar(List<Token> tokens) {
        memoria.clear();
        salida.clear();

        int inicio = buscarEntry(tokens);
        if (inicio < 0) {
            salida.add("[JVM-J] No se encontro el bloque 'entry'.");
            return salida;
        }

        ejecutarBloque(tokens, inicio);
        return salida;
    }

    // ------------------------------------------------------------------ //
    //  Busqueda del bloque entry                                           //
    // ------------------------------------------------------------------ //

    private int buscarEntry(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getTipo() == Token.Tipo.PR_ENTRY) {
                // Buscar la llave de apertura
                for (int j = i + 1; j < tokens.size(); j++) {
                    if (tokens.get(j).getTipo() == Token.Tipo.DEL_LLAVE_A) {
                        return j + 1; // inicio del contenido del bloque
                    }
                }
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ //
    //  Ejecucion de bloque                                                 //
    // ------------------------------------------------------------------ //

    /** Ejecuta sentencias hasta encontrar '}' o EOF. Retorna el indice post-bloque. */
    private int ejecutarBloque(List<Token> tokens, int pos) {
        while (pos < tokens.size()) {
            Token t = tokens.get(pos);
            if (t.getTipo() == Token.Tipo.DEL_LLAVE_C
                    || t.getTipo() == Token.Tipo.EOF) {
                pos++; // consume '}'
                break;
            }

            switch (t.getTipo()) {
                case PR_DEFINE:
                    pos = ejecutarDefine(tokens, pos);
                    break;
                case PR_OUT:
                    pos = ejecutarOut(tokens, pos);
                    break;
                case PR_IF:
                    pos = ejecutarIf(tokens, pos);
                    break;
                case PR_LOOP:
                    pos = ejecutarLoop(tokens, pos);
                    break;
                case IDENTIFICADOR:
                    pos = ejecutarAsignacion(tokens, pos);
                    break;
                case PR_RETURN:
                    // Consumir hasta ;
                    while (pos < tokens.size()
                            && tokens.get(pos).getTipo() != Token.Tipo.DEL_PUNTO_COMA) {
                        pos++;
                    }
                    pos++;
                    break;
                default:
                    pos++;
                    break;
            }
        }
        return pos;
    }

    // ------------------------------------------------------------------ //
    //  Instrucciones                                                        //
    // ------------------------------------------------------------------ //

    /** define tipo id = valor ; */
    private int ejecutarDefine(List<Token> tokens, int pos) {
        pos++; // consume 'define'
        if (pos >= tokens.size()) return pos;

        // Saltar tipo (y posible [])
        pos++;
        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.DEL_CORCHETE_A) {
            pos += 2; // consume []
        }

        // Nombre de variable
        if (pos >= tokens.size()
                || tokens.get(pos).getTipo() != Token.Tipo.IDENTIFICADOR) {
            return avanzarHastaPuntoComa(tokens, pos);
        }
        String nombre = tokens.get(pos).getLexema();
        pos++;

        // Asignacion opcional
        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.OP_ASIGNACION) {
            pos++; // consume =
            String[] resultado = evaluarExpresion(tokens, pos);
            memoria.put(nombre, resultado[0]);
            pos = Integer.parseInt(resultado[1]);
        }

        // Consumir hasta ;
        return avanzarHastaPuntoComa(tokens, pos);
    }

    /** out( expr ) ; */
    private int ejecutarOut(List<Token> tokens, int pos) {
        pos++; // consume 'out'
        pos++; // consume '('

        String[] resultado = evaluarExpresion(tokens, pos);
        String valor = resultado[0];
        pos = Integer.parseInt(resultado[1]);

        salida.add(limpiar(valor));

        // Consumir ')' y ';'
        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.DEL_PAREN_C) {
            pos++;
        }
        return avanzarHastaPuntoComa(tokens, pos);
    }

    /** if ( cond ) { bloque } [else { bloque }] */
    private int ejecutarIf(List<Token> tokens, int pos) {
        pos++; // consume 'if'
        pos++; // consume '('

        String[] evalCond = evaluarExpresion(tokens, pos);
        boolean condicion = esVerdadero(evalCond[0]);
        pos = Integer.parseInt(evalCond[1]);

        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.DEL_PAREN_C) {
            pos++;
        }
        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.DEL_LLAVE_A) {
            pos++;
        }

        if (condicion) {
            pos = ejecutarBloque(tokens, pos);
        } else {
            pos = saltarBloque(tokens, pos);
        }

        // else opcional
        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.PR_ELSE) {
            pos++; // consume 'else'
            if (pos < tokens.size()
                    && tokens.get(pos).getTipo() == Token.Tipo.DEL_LLAVE_A) {
                pos++;
            }
            if (!condicion) {
                pos = ejecutarBloque(tokens, pos);
            } else {
                pos = saltarBloque(tokens, pos);
            }
        }
        return pos;
    }

    /** loop ( cond ) { bloque } */
    private int ejecutarLoop(List<Token> tokens, int pos) {
        pos++; // consume 'loop'
        pos++; // consume '('

        int inicioCondicion = pos;

        // Limite de iteraciones para evitar bucles infinitos en la VM
        int maxIteraciones = 10_000;
        int iteraciones    = 0;

        while (iteraciones < maxIteraciones) {
            String[] evalCond = evaluarExpresion(tokens, inicioCondicion);
            boolean condicion = esVerdadero(evalCond[0]);
            pos = Integer.parseInt(evalCond[1]);

            if (pos < tokens.size()
                    && tokens.get(pos).getTipo() == Token.Tipo.DEL_PAREN_C) {
                pos++;
            }
            if (pos < tokens.size()
                    && tokens.get(pos).getTipo() == Token.Tipo.DEL_LLAVE_A) {
                pos++;
            }

            if (!condicion) {
                pos = saltarBloque(tokens, pos);
                break;
            }

            int posPost = ejecutarBloque(tokens, pos);
            iteraciones++;

            // Recalcular condicion desde el inicio del loop en la proxima iteracion
            // (se re-parsea la condicion cada vez)
            pos = posPost;
            // Para re-evaluar la condicion necesitamos retroceder al inicio:
            pos = inicioCondicion; // re-iniciar evaluacion de condicion
        }

        if (iteraciones >= maxIteraciones) {
            salida.add("[JVM-J] Advertencia: limite de iteraciones alcanzado en 'loop'.");
        }

        // Saltar el bloque si quedamos al inicio por el reset
        // Buscamos el '}' de cierre del loop para avanzar el cursor principal
        pos = Integer.parseInt(evaluarExpresion(tokens, inicioCondicion)[1]);
        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.DEL_PAREN_C) pos++;
        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.DEL_LLAVE_A) pos++;
        return saltarBloque(tokens, pos);
    }

    /** id = expr ; */
    private int ejecutarAsignacion(List<Token> tokens, int pos) {
        String nombre = tokens.get(pos).getLexema();
        pos++;
        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.OP_ASIGNACION) {
            pos++; // consume =
            String[] resultado = evaluarExpresion(tokens, pos);
            memoria.put(nombre, resultado[0]);
            pos = Integer.parseInt(resultado[1]);
        } else if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.OP_INCREMENTO) {
            String val = memoria.getOrDefault(nombre, "0");
            try {
                double d = Double.parseDouble(val);
                memoria.put(nombre, formatearNumero(d + 1));
            } catch (NumberFormatException e) { /* ignorar */ }
            pos++;
        } else if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.OP_DECREMENTO) {
            String val = memoria.getOrDefault(nombre, "0");
            try {
                double d = Double.parseDouble(val);
                memoria.put(nombre, formatearNumero(d - 1));
            } catch (NumberFormatException e) { /* ignorar */ }
            pos++;
        }
        return avanzarHastaPuntoComa(tokens, pos);
    }

    // ------------------------------------------------------------------ //
    //  Evaluacion de expresiones                                           //
    // ------------------------------------------------------------------ //

    /**
     * Evalua una expresion a partir de 'pos'.
     * Retorna un arreglo de dos elementos: [valor, nuevaPosicion].
     * Simplificacion: soporta operaciones binarias izquierda a derecha.
     */
    private String[] evaluarExpresion(List<Token> tokens, int pos) {
        if (pos >= tokens.size()) return new String[]{"", String.valueOf(pos)};

        String[] termIzq = evaluarTermino(tokens, pos);
        String valIzq = termIzq[0];
        pos = Integer.parseInt(termIzq[1]);

        while (pos < tokens.size() && esOperadorBinario(tokens.get(pos).getTipo())) {
            Token op = tokens.get(pos);
            pos++;
            String[] termDer = evaluarTermino(tokens, pos);
            String valDer = termDer[0];
            pos = Integer.parseInt(termDer[1]);
            valIzq = aplicarOperador(valIzq, op.getTipo(), valDer);
        }

        return new String[]{valIzq, String.valueOf(pos)};
    }

    private String[] evaluarTermino(List<Token> tokens, int pos) {
        if (pos >= tokens.size()) return new String[]{"", String.valueOf(pos)};

        Token t = tokens.get(pos);
        switch (t.getTipo()) {
            case LIT_ENTERO:
            case LIT_DECIMAL:
                return new String[]{t.getLexema(), String.valueOf(pos + 1)};

            case LIT_CADENA:
                // Quitar comillas
                String s = t.getLexema();
                if (s.length() >= 2) s = s.substring(1, s.length() - 1);
                return new String[]{s, String.valueOf(pos + 1)};

            case PR_TRUE:
                return new String[]{"true", String.valueOf(pos + 1)};
            case PR_FALSE:
                return new String[]{"false", String.valueOf(pos + 1)};

            case IDENTIFICADOR:
                String nombre = t.getLexema();
                pos++;
                // Acceso a metodo: Scientific.sqrt etc. (simplificado)
                if (pos < tokens.size()
                        && tokens.get(pos).getTipo() == Token.Tipo.DEL_PUNTO) {
                    pos++;
                    if (pos < tokens.size()) pos++; // nombre metodo
                    if (pos < tokens.size()
                            && tokens.get(pos).getTipo() == Token.Tipo.DEL_PAREN_A) {
                        pos++;
                        String[] arg = evaluarExpresion(tokens, pos);
                        pos = Integer.parseInt(arg[1]);
                        String resultado = aplicarMetodoLibreria(nombre, arg[0]);
                        if (pos < tokens.size()
                                && tokens.get(pos).getTipo() == Token.Tipo.DEL_PAREN_C) {
                            pos++;
                        }
                        return new String[]{resultado, String.valueOf(pos)};
                    }
                }
                String valor = memoria.getOrDefault(nombre, "0");
                return new String[]{valor, String.valueOf(pos)};

            case OP_NOT:
                String[] sub = evaluarTermino(tokens, pos + 1);
                String negado = esVerdadero(sub[0]) ? "false" : "true";
                return new String[]{negado, sub[1]};

            case OP_RESTA:
                String[] subN = evaluarTermino(tokens, pos + 1);
                try {
                    double d = Double.parseDouble(subN[0]);
                    return new String[]{formatearNumero(-d), subN[1]};
                } catch (NumberFormatException e) {
                    return new String[]{"0", subN[1]};
                }

            case DEL_PAREN_A:
                String[] inner = evaluarExpresion(tokens, pos + 1);
                pos = Integer.parseInt(inner[1]);
                if (pos < tokens.size()
                        && tokens.get(pos).getTipo() == Token.Tipo.DEL_PAREN_C) {
                    pos++;
                }
                return new String[]{inner[0], String.valueOf(pos)};

            default:
                return new String[]{"", String.valueOf(pos)};
        }
    }

    private String aplicarOperador(String izq, Token.Tipo op, String der) {
        // Concatenacion de cadenas con +
        if (op == Token.Tipo.OP_SUMA) {
            // Si alguno no es numerico, concatenar
            try {
                double a = Double.parseDouble(izq);
                double b = Double.parseDouble(der);
                return formatearNumero(a + b);
            } catch (NumberFormatException e) {
                return limpiar(izq + der);
            }
        }
        try {
            double a = Double.parseDouble(izq);
            double b = Double.parseDouble(der);
            switch (op) {
                case OP_RESTA:          return formatearNumero(a - b);
                case OP_MULTIPLICACION: return formatearNumero(a * b);
                case OP_DIVISION:       return b != 0 ? formatearNumero(a / b) : "Error: division por cero";
                case OP_MODULO:         return formatearNumero(a % b);
                case OP_IGUAL:          return String.valueOf(a == b);
                case OP_DIFERENTE:      return String.valueOf(a != b);
                case OP_MAYOR:          return String.valueOf(a > b);
                case OP_MENOR:          return String.valueOf(a < b);
                case OP_MAYOR_IGUAL:    return String.valueOf(a >= b);
                case OP_MENOR_IGUAL:    return String.valueOf(a <= b);
                default:                return "0";
            }
        } catch (NumberFormatException e) {
            // Comparaciones de cadenas
            switch (op) {
                case OP_IGUAL:     return String.valueOf(izq.equals(der));
                case OP_DIFERENTE: return String.valueOf(!izq.equals(der));
                default:           return "false";
            }
        }
    }

    private String aplicarMetodoLibreria(String libreria, String argumento) {
        // Simplificacion de Scientific.*
        try {
            double d = Double.parseDouble(argumento);
            if (libreria.equalsIgnoreCase("Scientific")) {
                return formatearNumero(Math.sqrt(d));
            }
        } catch (NumberFormatException e) { /* ignorar */ }
        return argumento;
    }

    // ------------------------------------------------------------------ //
    //  Utilidades                                                          //
    // ------------------------------------------------------------------ //

    /** Salta un bloque completo { ... } respetando bloques anidados. */
    private int saltarBloque(List<Token> tokens, int pos) {
        int nivel = 1;
        while (pos < tokens.size() && nivel > 0) {
            Token.Tipo tipo = tokens.get(pos).getTipo();
            if (tipo == Token.Tipo.DEL_LLAVE_A) nivel++;
            else if (tipo == Token.Tipo.DEL_LLAVE_C) nivel--;
            pos++;
        }
        return pos;
    }

    private int avanzarHastaPuntoComa(List<Token> tokens, int pos) {
        while (pos < tokens.size()
                && tokens.get(pos).getTipo() != Token.Tipo.DEL_PUNTO_COMA
                && tokens.get(pos).getTipo() != Token.Tipo.EOF) {
            pos++;
        }
        if (pos < tokens.size()
                && tokens.get(pos).getTipo() == Token.Tipo.DEL_PUNTO_COMA) {
            pos++;
        }
        return pos;
    }

    private boolean esVerdadero(String valor) {
        if ("true".equalsIgnoreCase(valor)) return true;
        if ("false".equalsIgnoreCase(valor)) return false;
        try { return Double.parseDouble(valor) != 0; }
        catch (NumberFormatException e) { return !valor.isEmpty(); }
    }

    private boolean esOperadorBinario(Token.Tipo tipo) {
        switch (tipo) {
            case OP_SUMA: case OP_RESTA: case OP_MULTIPLICACION:
            case OP_DIVISION: case OP_MODULO:
            case OP_IGUAL: case OP_DIFERENTE:
            case OP_MAYOR: case OP_MENOR:
            case OP_MAYOR_IGUAL: case OP_MENOR_IGUAL:
            case OP_AND: case OP_OR:
                return true;
            default: return false;
        }
    }

    private String formatearNumero(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    private String limpiar(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
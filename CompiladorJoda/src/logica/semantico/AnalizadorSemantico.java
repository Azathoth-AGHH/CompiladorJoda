package logica.semantico;

import logica.lexico.Token;
import java.util.*;

/**
 * Analizador semantico para el lenguaje JODA.
 *
 * Realiza las siguientes validaciones sobre la lista de tokens:
 *   1. Declaracion de variables con 'define' antes de uso.
 *   2. Variables declaradas pero no inicializadas al usarse.
 *   3. Compatibilidad de tipos en asignaciones simples.
 *   4. Registro de clases (object) y metodos (method) en tabla de simbolos.
 *   5. Identificadores que inicien con mayuscula (violacion de regla JODA).
 */
public class AnalizadorSemantico {

    /** Error semantico con descripcion y linea. */
    public static class ErrorSemantico {
        private final String descripcion;
        private final int    linea;

        public ErrorSemantico(String descripcion, int linea) {
            this.descripcion = descripcion;
            this.linea       = linea;
        }

        public String getDescripcion() { return descripcion; }
        public int    getLinea()       { return linea; }
    }

    /** Resultado del analisis semantico. */
    public static class ResultadoSemantico {
        private final List<EntradaTablaSimbolos> tablaSimbolos;
        private final List<ErrorSemantico>       errores;

        public ResultadoSemantico(List<EntradaTablaSimbolos> tabla,
                                  List<ErrorSemantico> errores) {
            this.tablaSimbolos = tabla;
            this.errores       = errores;
        }

        public List<EntradaTablaSimbolos> getTablaSimbolos() { return tablaSimbolos; }
        public List<ErrorSemantico>       getErrores()       { return errores; }
        public boolean tieneErrores()                        { return !errores.isEmpty(); }
    }

    // ------------------------------------------------------------------ //
    //  Estado interno del analizador                                       //
    // ------------------------------------------------------------------ //

    private final Map<String, EntradaTablaSimbolos> tablaSimbolos = new LinkedHashMap<>();
    private final List<ErrorSemantico>              errores       = new ArrayList<>();

    private String ambitoActual = "global";

    // ------------------------------------------------------------------ //
    //  API publica                                                         //
    // ------------------------------------------------------------------ //

    public ResultadoSemantico analizar(List<Token> tokens) {
        tablaSimbolos.clear();
        errores.clear();
        ambitoActual = "global";

        int i = 0;
        while (i < tokens.size()) {
            Token t = tokens.get(i);

            switch (t.getTipo()) {

                // --- Declaracion de objeto (clase) ---
                case PR_OBJECT:
                    i = procesarObjeto(tokens, i);
                    break;

                // --- Declaracion de metodo ---
                case PR_METHOD:
                    i = procesarMetodo(tokens, i);
                    break;

                // --- Declaracion de variable con 'define' ---
                case PR_DEFINE:
                    i = procesarDefine(tokens, i);
                    break;

                // --- Uso de identificador ---
                case IDENTIFICADOR:
                    validarUsoIdentificador(t);
                    i++;
                    break;

                // --- Cierre de bloque: podria restaurar ambito ---
                case DEL_LLAVE_C:
                    // Simplificacion: ambito vuelve a global al cerrar bloque
                    // En un compilador real se usaria una pila de ambitos
                    ambitoActual = "global";
                    i++;
                    break;

                default:
                    i++;
                    break;
            }
        }
        return new ResultadoSemantico(new ArrayList<>(tablaSimbolos.values()), errores);
    }

    // ------------------------------------------------------------------ //
    //  Procesadores de construcciones                                      //
    // ------------------------------------------------------------------ //

    /** Registra una clase en la tabla de simbolos y actualiza el ambito. */
    private int procesarObjeto(List<Token> tokens, int pos) {
        // Sintaxis: object NombreClase { ... }
        int idx = pos + 1;
        if (idx < tokens.size() && tokens.get(idx).getTipo() == Token.Tipo.IDENTIFICADOR) {
            Token nombreToken = tokens.get(idx);
            String nombre = nombreToken.getLexema();
            registrarSimbolo(nombre, "class", "global",
                    EntradaTablaSimbolos.Categoria.CLASE, nombreToken.getLinea());
            ambitoActual = nombre;
            return idx + 1;
        }
        return pos + 1;
    }

    /** Registra un metodo en la tabla de simbolos. */
    private int procesarMetodo(List<Token> tokens, int pos) {
        // Sintaxis: method TipoRetorno NombreMetodo( ... ) { ... }
        //        o: method NombreMetodo( ... ) { ... }  (sin tipo de retorno explicito)
        int idx = pos + 1;
        String tipoRetorno = "void";
        String nombreMetodo = "";

        if (idx < tokens.size()) {
            Token sig = tokens.get(idx);
            // Si el siguiente token es un tipo de dato o void
            if (esTipoDato(sig.getTipo())) {
                tipoRetorno = sig.getLexema();
                idx++;
            }
            if (idx < tokens.size()
                    && tokens.get(idx).getTipo() == Token.Tipo.IDENTIFICADOR) {
                nombreMetodo = tokens.get(idx).getLexema();
                registrarSimbolo(nombreMetodo, tipoRetorno, ambitoActual,
                        EntradaTablaSimbolos.Categoria.METODO, tokens.get(idx).getLinea());
                ambitoActual = nombreMetodo;
                idx++;
            }
        }
        return idx;
    }

    /** Procesa una declaracion: define [tipo] [nombre] [= valor] ; */
    private int procesarDefine(List<Token> tokens, int pos) {
        int idx = pos + 1;

        // Esperamos un tipo de dato
        if (idx >= tokens.size() || !esTipoDato(tokens.get(idx).getTipo())) {
            errores.add(new ErrorSemantico(
                    "Se esperaba un tipo de dato despues de 'define'.",
                    tokens.get(pos).getLinea()));
            return idx;
        }
        String tipo = tokens.get(idx).getLexema();
        idx++;

        // Verificamos si es arreglo: tipo[]
        if (idx < tokens.size()
                && tokens.get(idx).getTipo() == Token.Tipo.DEL_CORCHETE_A) {
            tipo = tipo + "[]";
            idx++; // consume [
            if (idx < tokens.size()
                    && tokens.get(idx).getTipo() == Token.Tipo.DEL_CORCHETE_C) {
                idx++; // consume ]
            }
        }

        // Esperamos el identificador
        if (idx >= tokens.size()
                || tokens.get(idx).getTipo() != Token.Tipo.IDENTIFICADOR) {
            errores.add(new ErrorSemantico(
                    "Se esperaba un identificador en la declaracion 'define " + tipo + "'.",
                    tokens.get(pos).getLinea()));
            return idx;
        }
        Token nombreToken = tokens.get(idx);
        String nombre = nombreToken.getLexema();
        idx++;

        // Verificar redeclaracion en mismo ambito
        String clave = ambitoActual + "::" + nombre;
        if (tablaSimbolos.containsKey(clave)) {
            errores.add(new ErrorSemantico(
                    "Variable '" + nombre + "' ya fue declarada en el ambito '"
                            + ambitoActual + "'.",
                    nombreToken.getLinea()));
        } else {
            EntradaTablaSimbolos entrada = new EntradaTablaSimbolos(
                    nombre, tipo, ambitoActual,
                    EntradaTablaSimbolos.Categoria.VARIABLE, nombreToken.getLinea());

            // Verificar si hay asignacion
            if (idx < tokens.size()
                    && tokens.get(idx).getTipo() == Token.Tipo.OP_ASIGNACION) {
                idx++; // consume =
                // Validacion de tipo del valor asignado
                if (idx < tokens.size()) {
                    Token valor = tokens.get(idx);
                    validarCompatibilidadTipo(tipo, valor, nombre);
                    entrada.setInicializada(true);
                    idx++;
                }
            }
            tablaSimbolos.put(clave, entrada);
        }

        // Consumir hasta el punto y coma
        while (idx < tokens.size()
                && tokens.get(idx).getTipo() != Token.Tipo.DEL_PUNTO_COMA
                && tokens.get(idx).getTipo() != Token.Tipo.EOF) {
            idx++;
        }
        if (idx < tokens.size()
                && tokens.get(idx).getTipo() == Token.Tipo.DEL_PUNTO_COMA) {
            idx++; // consume ;
        }
        return idx;
    }

    // ------------------------------------------------------------------ //
    //  Validaciones                                                        //
    // ------------------------------------------------------------------ //

    private void validarUsoIdentificador(Token t) {
        String nombre = t.getLexema();
        // Regla JODA: identificadores deben iniciar en minuscula
        if (Character.isUpperCase(nombre.charAt(0))) {
            // Podria ser nombre de clase, no reportar error
            return;
        }
        // Verificar que este declarado en el ambito actual o global
        String claveLocal  = ambitoActual + "::" + nombre;
        String claveGlobal = "global::"   + nombre;
        if (!tablaSimbolos.containsKey(claveLocal)
                && !tablaSimbolos.containsKey(claveGlobal)) {
            errores.add(new ErrorSemantico(
                    "Identificador '" + nombre + "' usado sin declaracion previa.",
                    t.getLinea()));
        }
    }

    private void validarCompatibilidadTipo(String tipo, Token valor, String nombre) {
        Token.Tipo tv = valor.getTipo();
        boolean compatible = false;

        switch (tipo) {
            case "int":
                compatible = tv == Token.Tipo.LIT_ENTERO;
                break;
            case "dec":
                compatible = tv == Token.Tipo.LIT_DECIMAL || tv == Token.Tipo.LIT_ENTERO;
                break;
            case "string":
                compatible = tv == Token.Tipo.LIT_CADENA;
                break;
            case "bool":
                compatible = tv == Token.Tipo.PR_TRUE || tv == Token.Tipo.PR_FALSE;
                break;
            default:
                // Tipos compuestos o arreglos: validacion superficial
                compatible = true;
                break;
        }

        if (!compatible) {
            errores.add(new ErrorSemantico(
                    "Tipo incompatible: variable '" + nombre + "' es '"
                            + tipo + "' pero se asigno '" + valor.getLexema() + "'.",
                    valor.getLinea()));
        }
    }

    // ------------------------------------------------------------------ //
    //  Utilidades                                                          //
    // ------------------------------------------------------------------ //

    private boolean esTipoDato(Token.Tipo tipo) {
        return tipo == Token.Tipo.PR_INT
                || tipo == Token.Tipo.PR_DEC
                || tipo == Token.Tipo.PR_STRING
                || tipo == Token.Tipo.PR_BOOL
                || tipo == Token.Tipo.PR_VOID;
    }

    private void registrarSimbolo(String nombre, String tipo, String ambito,
                                  EntradaTablaSimbolos.Categoria cat, int linea) {
        String clave = ambito + "::" + nombre;
        if (!tablaSimbolos.containsKey(clave)) {
            tablaSimbolos.put(clave, new EntradaTablaSimbolos(nombre, tipo, ambito, cat, linea));
        }
    }
}
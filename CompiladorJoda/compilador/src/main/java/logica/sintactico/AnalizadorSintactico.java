package logica.sintactico;

import java.util.ArrayList;
import java.util.List;

import logica.lexico.Token;

/*
Analizador Sintactico (Parser) de JODA.
Implementa un parser de descenso recursivo que construye el AST
a partir de la lista de tokens producida por AnalizadorLexico.

CORRECCION v2.1:
  - parsearSuma ahora solo consume T_SUMA y T_RESTA, pero NO T_INCREMENTO
    ni T_DECREMENTO. Antes, cuando '+' se tokenizaba siempre como T_SUMA,
    esto era inofensivo. Con el lexer corregido que emite T_INCREMENTO (++),
    es imprescindible que parsearSuma no lo consuma como parte de una suma.
  - parsearAsignacionOIncrementoPostfijo verifica T_INCREMENTO y T_DECREMENTO
    ANTES de intentar parsear una asignacion (esto ya estaba correcto).
  - Mejora de recuperacion de errores: al encontrar un token inesperado en
    parsearPrimario, el parser intenta continuar sin romper el resto del bloque.
*/
public class AnalizadorSintactico {

    private final List<Token> tokens;
    private int posicion;
    private final List<String> errores;

    public AnalizadorSintactico(List<Token> tokens) {
        this.tokens   = tokens;
        this.posicion = 0;
        this.errores  = new ArrayList<>();
    }

    // ---------------------------------------------------------------
    // PUNTO DE ENTRADA
    // ---------------------------------------------------------------

    public NodoAST.NodoEntry parsear() {
        saltarComentarios();

        if (!esTokenActual(Token.Tipo.T_ENTRY)) {
            errores.add("Error sintactico en linea " + tokenActual().getLinea()
                + ": se esperaba 'entry' al inicio del programa, "
                + "pero se encontro '" + tokenActual().getLexema() + "'.");
            return null;
        }

        return parsearEntry();
    }

    // ---------------------------------------------------------------
    // REGLAS GRAMATICALES
    // ---------------------------------------------------------------

    // entry { [instrucciones] }
    private NodoAST.NodoEntry parsearEntry() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_ENTRY);
        consumir(Token.Tipo.T_LLAVE_ABRE);
        List<NodoAST> instrucciones = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoEntry(instrucciones, linea);
    }

    // Lee instrucciones hasta encontrar el token de cierre indicado.
    private List<NodoAST> parsearBloqueInstrucciones(Token.Tipo tokenCierre) {
        List<NodoAST> instrucciones = new ArrayList<>();

        while (!esTokenActual(tokenCierre) && !esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) {
            saltarComentarios();
            if (esTokenActual(tokenCierre) || esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) break;

            NodoAST instruccion = parsearInstruccion();
            if (instruccion != null) instrucciones.add(instruccion);
        }

        return instrucciones;
    }

    // Decide que tipo de instruccion parsear segun el token actual.
    private NodoAST parsearInstruccion() {
        saltarComentarios();
        Token t = tokenActual();

        switch (t.getTipo()) {
            case T_DEFINE:       return parsearDefine();
            case T_IF:           return parsearIf();
            case T_LOOP:         return parsearLoop();
            case T_SELECT:       return parsearSelect();
            case T_OUT:          return parsearOut();
            case T_INPUT:        return parsearInput();
            case T_IDENTIFICADOR:return parsearAsignacionOIncrementoPostfijo();
            case T_OBJECT:       return parsearObject();
            case T_METHOD:       return parsearMethod();
            default:
                errores.add("Error sintactico en linea " + t.getLinea()
                    + ": instruccion inesperada '" + t.getLexema()
                    + "' (" + t.getTipo() + ").");
                avanzar(); // recuperacion: saltar el token problematico
                return null;
        }
    }

    // define tipo identificador [= expresion] ;
    private NodoAST.NodoDefine parsearDefine() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_DEFINE);

        String tipo = tokenActual().getLexema();
        if (!esTipoValido()) {
            errores.add("Error sintactico en linea " + linea
                + ": tipo de dato invalido '" + tipo + "'.");
        }
        avanzar();

        String identificador = tokenActual().getLexema();
        consumir(Token.Tipo.T_IDENTIFICADOR);

        NodoAST expresion = null;
        if (esTokenActual(Token.Tipo.T_ASIGNACION)) {
            consumir(Token.Tipo.T_ASIGNACION);
            expresion = parsearExpresion();
        }

        consumir(Token.Tipo.T_PUNTO_Y_COMA);
        return new NodoAST.NodoDefine(tipo, identificador, expresion, linea);
    }

    // if (condicion) { ... } [else { ... }]
    private NodoAST.NodoIf parsearIf() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_IF);
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        NodoAST condicion = parsearExpresion();
        consumir(Token.Tipo.T_PARENTESIS_CIERRA);
        consumir(Token.Tipo.T_LLAVE_ABRE);
        List<NodoAST> bloqueThen = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
        consumir(Token.Tipo.T_LLAVE_CIERRA);

        List<NodoAST> bloqueElse = null;
        if (esTokenActual(Token.Tipo.T_ELSE)) {
            consumir(Token.Tipo.T_ELSE);
            consumir(Token.Tipo.T_LLAVE_ABRE);
            bloqueElse = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
            consumir(Token.Tipo.T_LLAVE_CIERRA);
        }

        return new NodoAST.NodoIf(condicion, bloqueThen, bloqueElse, linea);
    }

    // loop (condicion) { ... }
    private NodoAST.NodoLoop parsearLoop() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_LOOP);
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        NodoAST condicion = parsearExpresion();
        consumir(Token.Tipo.T_PARENTESIS_CIERRA);
        consumir(Token.Tipo.T_LLAVE_ABRE);
        List<NodoAST> cuerpo = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoLoop(condicion, cuerpo, linea);
    }

    // select (variable) { case valor: instrucciones ... }
    private NodoAST.NodoSelect parsearSelect() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_SELECT);
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        NodoAST variable = parsearExpresion();
        consumir(Token.Tipo.T_PARENTESIS_CIERRA);
        consumir(Token.Tipo.T_LLAVE_ABRE);

        List<NodoAST.NodoCaso> casos = new ArrayList<>();
        while (esTokenActual(Token.Tipo.T_CASE)) {
            int lineaCaso = tokenActual().getLinea();
            consumir(Token.Tipo.T_CASE);
            NodoAST valor = parsearExpresion();
            // Consumir ':' o ';' como separador del case
            if (esTokenActual(Token.Tipo.T_PUNTO_Y_COMA)) avanzar();

            List<NodoAST> instrsCaso = new ArrayList<>();
            while (!esTokenActual(Token.Tipo.T_CASE)
                    && !esTokenActual(Token.Tipo.T_LLAVE_CIERRA)
                    && !esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) {
                NodoAST instr = parsearInstruccion();
                if (instr != null) instrsCaso.add(instr);
            }
            casos.add(new NodoAST.NodoCaso(valor, instrsCaso, lineaCaso));
        }

        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoSelect(variable, casos, linea);
    }

    // out(expresion);
    private NodoAST.NodoOut parsearOut() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_OUT);
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        NodoAST expresion = parsearExpresion();
        consumir(Token.Tipo.T_PARENTESIS_CIERRA);
        consumir(Token.Tipo.T_PUNTO_Y_COMA);
        return new NodoAST.NodoOut(expresion, linea);
    }

    // input(identificador);
    private NodoAST.NodoInput parsearInput() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_INPUT);
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        String id = tokenActual().getLexema();
        consumir(Token.Tipo.T_IDENTIFICADOR);
        consumir(Token.Tipo.T_PARENTESIS_CIERRA);
        consumir(Token.Tipo.T_PUNTO_Y_COMA);
        return new NodoAST.NodoInput(id, linea);
    }

    /*
     * identificador = expresion;
     * identificador++;
     * identificador--;
     *
     * El orden de verificacion importa:
     * Primero se comprueba si viene '++' o '--' (T_INCREMENTO / T_DECREMENTO).
     * Si no, se asume que es una asignacion y se espera '='.
     * Esto funciona correctamente ahora que el lexer emite T_INCREMENTO
     * como un token unico en lugar de dos T_SUMA consecutivos.
     */
    private NodoAST parsearAsignacionOIncrementoPostfijo() {
        int linea    = tokenActual().getLinea();
        String id    = tokenActual().getLexema();
        consumir(Token.Tipo.T_IDENTIFICADOR);

        if (esTokenActual(Token.Tipo.T_INCREMENTO)) {
            avanzar(); // consumir '++'
            consumir(Token.Tipo.T_PUNTO_Y_COMA);
            return new NodoAST.NodoIncrementoPostfijo(id, "++", linea);
        }

        if (esTokenActual(Token.Tipo.T_DECREMENTO)) {
            avanzar(); // consumir '--'
            consumir(Token.Tipo.T_PUNTO_Y_COMA);
            return new NodoAST.NodoIncrementoPostfijo(id, "--", linea);
        }

        // Es una asignacion: identificador = expresion;
        consumir(Token.Tipo.T_ASIGNACION);
        NodoAST expresion = parsearExpresion();
        consumir(Token.Tipo.T_PUNTO_Y_COMA);
        return new NodoAST.NodoAsignacion(id, expresion, linea);
    }

    // object Nombre { ... }
    private NodoAST.NodoObject parsearObject() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_OBJECT);
        String nombre = tokenActual().getLexema();
        avanzar(); // nombre puede empezar con mayuscula
        consumir(Token.Tipo.T_LLAVE_ABRE);
        List<NodoAST> miembros = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoObject(nombre, miembros, linea);
    }

    // method Nombre(params) { ... }
    private NodoAST.NodoMethod parsearMethod() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_METHOD);
        String nombre = tokenActual().getLexema();
        avanzar();
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        // Parametros: soporte futuro; por ahora se ignoran
        while (!esTokenActual(Token.Tipo.T_PARENTESIS_CIERRA)
                && !esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) {
            avanzar();
        }
        consumir(Token.Tipo.T_PARENTESIS_CIERRA);
        consumir(Token.Tipo.T_LLAVE_ABRE);
        List<NodoAST> cuerpo = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoMethod(nombre, cuerpo, linea);
    }

    // ---------------------------------------------------------------
    // PARSEO DE EXPRESIONES  (precedencia ascendente)
    // ---------------------------------------------------------------

    // Nivel 1 (menor precedencia): expresion logica OR
    private NodoAST parsearExpresion() { return parsearOr(); }

    // ||
    private NodoAST parsearOr() {
        NodoAST izq = parsearAnd();
        while (esTokenActual(Token.Tipo.T_OR)) {
            int linea = tokenActual().getLinea();
            String op = tokenActual().getLexema();
            avanzar();
            NodoAST der = parsearAnd();
            izq = new NodoAST.NodoBinario(izq, op, der, linea);
        }
        return izq;
    }

    // &&
    private NodoAST parsearAnd() {
        NodoAST izq = parsearIgualdad();
        while (esTokenActual(Token.Tipo.T_AND)) {
            int linea = tokenActual().getLinea();
            String op = tokenActual().getLexema();
            avanzar();
            NodoAST der = parsearIgualdad();
            izq = new NodoAST.NodoBinario(izq, op, der, linea);
        }
        return izq;
    }

    // ==  !=
    private NodoAST parsearIgualdad() {
        NodoAST izq = parsearRelacional();
        while (esTokenActual(Token.Tipo.T_IGUAL_IGUAL)
                || esTokenActual(Token.Tipo.T_DIFERENTE)) {
            int linea = tokenActual().getLinea();
            String op = tokenActual().getLexema();
            avanzar();
            NodoAST der = parsearRelacional();
            izq = new NodoAST.NodoBinario(izq, op, der, linea);
        }
        return izq;
    }

    // >  <  >=  <=
    private NodoAST parsearRelacional() {
        NodoAST izq = parsearSuma();
        while (esTokenActual(Token.Tipo.T_MAYOR) || esTokenActual(Token.Tipo.T_MENOR)
                || esTokenActual(Token.Tipo.T_MAYOR_IGUAL)
                || esTokenActual(Token.Tipo.T_MENOR_IGUAL)) {
            int linea = tokenActual().getLinea();
            String op = tokenActual().getLexema();
            avanzar();
            NodoAST der = parsearSuma();
            izq = new NodoAST.NodoBinario(izq, op, der, linea);
        }
        return izq;
    }

    /*
     * +  -
     *
     * IMPORTANTE: este nivel SOLO consume T_SUMA y T_RESTA.
     * NO debe consumir T_INCREMENTO (++) ni T_DECREMENTO (--).
     * Con el lexer corregido, '++' llega como T_INCREMENTO (un token unico),
     * por lo que no hay riesgo de confusion con la suma. Aun asi, la condicion
     * del while lo excluye explicitamente por claridad y seguridad.
     */
    private NodoAST parsearSuma() {
        NodoAST izq = parsearMultiplicacion();
        while (esTokenActual(Token.Tipo.T_SUMA) || esTokenActual(Token.Tipo.T_RESTA)) {
            int linea = tokenActual().getLinea();
            String op = tokenActual().getLexema();
            avanzar();
            NodoAST der = parsearMultiplicacion();
            izq = new NodoAST.NodoBinario(izq, op, der, linea);
        }
        return izq;
    }

    // *  /  %
    private NodoAST parsearMultiplicacion() {
        NodoAST izq = parsearUnario();
        while (esTokenActual(Token.Tipo.T_MULTIPLICACION)
                || esTokenActual(Token.Tipo.T_DIVISION)
                || esTokenActual(Token.Tipo.T_MODULO)) {
            int linea = tokenActual().getLinea();
            String op = tokenActual().getLexema();
            avanzar();
            NodoAST der = parsearUnario();
            izq = new NodoAST.NodoBinario(izq, op, der, linea);
        }
        return izq;
    }

    // !expr   -expr
    private NodoAST parsearUnario() {
        if (esTokenActual(Token.Tipo.T_NOT)) {
            int linea = tokenActual().getLinea();
            avanzar();
            return new NodoAST.NodoUnario("!", parsearPrimario(), linea);
        }
        if (esTokenActual(Token.Tipo.T_RESTA)) {
            int linea = tokenActual().getLinea();
            avanzar();
            return new NodoAST.NodoUnario("-", parsearPrimario(), linea);
        }
        return parsearPrimario();
    }

    // Nivel mas alto: literales, identificadores, expresiones parentizadas
    private NodoAST parsearPrimario() {
        Token t = tokenActual();

        switch (t.getTipo()) {
            case T_LITERAL_ENTERO:
                avanzar();
                return new NodoAST.NodoLiteralEntero(
                    Integer.parseInt(t.getLexema()), t.getLinea());

            case T_LITERAL_DECIMAL:
                avanzar();
                return new NodoAST.NodoLiteralDecimal(
                    Double.parseDouble(t.getLexema()), t.getLinea());

            case T_LITERAL_CADENA:
                avanzar();
                return new NodoAST.NodoLiteralCadena(t.getLexema(), t.getLinea());

            case T_LITERAL_BOOL:
            case T_TRUE:
            case T_FALSE:
                avanzar();
                return new NodoAST.NodoLiteralBool("true".equals(t.getLexema()), t.getLinea());

            case T_IDENTIFICADOR:
                avanzar();
                return new NodoAST.NodoIdentificador(t.getLexema(), t.getLinea());

            case T_PARENTESIS_ABRE:
                avanzar();
                NodoAST expr = parsearExpresion();
                consumir(Token.Tipo.T_PARENTESIS_CIERRA);
                return expr;

            default:
                errores.add("Error sintactico en linea " + t.getLinea()
                    + ": expresion invalida, token inesperado '"
                    + t.getLexema() + "' (" + t.getTipo() + ").");
                avanzar(); // recuperacion minima
                // Retornar un nodo neutro para no propagar null y romper el AST
                return new NodoAST.NodoLiteralEntero(0, t.getLinea());
        }
    }

    // ---------------------------------------------------------------
    // METODOS AUXILIARES
    // ---------------------------------------------------------------

    private Token tokenActual() {
        if (posicion < tokens.size()) return tokens.get(posicion);
        return tokens.get(tokens.size() - 1); // EOF
    }

    private void avanzar() {
        if (posicion < tokens.size() - 1) posicion++;
    }

    private boolean esTokenActual(Token.Tipo tipo) {
        return tokenActual().getTipo() == tipo;
    }

    private void consumir(Token.Tipo tipo) {
        if (esTokenActual(tipo)) {
            avanzar();
        } else {
            errores.add("Error sintactico en linea " + tokenActual().getLinea()
                + ": se esperaba '" + tipo
                + "' pero se encontro '" + tokenActual().getLexema()
                + "' (" + tokenActual().getTipo() + ").");
        }
    }

    private void saltarComentarios() {
        while (esTokenActual(Token.Tipo.T_COMENTARIO)) avanzar();
    }

    private boolean esTipoValido() {
        Token.Tipo t = tokenActual().getTipo();
        return t == Token.Tipo.T_INT    || t == Token.Tipo.T_DEC
            || t == Token.Tipo.T_STRING || t == Token.Tipo.T_BOOL
            || t == Token.Tipo.T_VOID   || t == Token.Tipo.T_OBJECT;
    }

    public List<String> getErrores()  { return errores; }
    public boolean tieneErrores()     { return !errores.isEmpty(); }
}

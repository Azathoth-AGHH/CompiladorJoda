package logica.sintactico;

import logica.lexico.Token;
import java.util.ArrayList;
import java.util.List;

/*
Analizador Sintactico (Parser) de JODA.
Implementa un parser de descenso recursivo que construye el AST
a partir de la lista de tokens producida por el AnalizadorLexico.
*/
public class AnalizadorSintactico {

    private final List<Token> tokens;
    private int posicion;
    private final List<String> errores;

    public AnalizadorSintactico(List<Token> tokens) {
        this.tokens = tokens;
        this.posicion = 0;
        this.errores = new ArrayList<>();
    }

    /*
    Punto de entrada del analisis. Intenta reconocer el programa completo.
    Un programa JODA valido comienza con el bloque 'entry'.
    */
    public NodoAST.NodoEntry parsear() {
        saltarComentarios();

        if (!esTokenActual(Token.Tipo.T_ENTRY)) {
            errores.add("Error sintactico en linea " + tokenActual().getLinea()
                + ": se esperaba 'entry' al inicio del programa.");
            return null;
        }

        return parsearEntry();
    }

    // Reglas gramaticales (metodos de parseo)
    //entry { [instrucciones] }
    private NodoAST.NodoEntry parsearEntry() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_ENTRY);
        consumir(Token.Tipo.T_LLAVE_ABRE);

        List<NodoAST> instrucciones = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);

        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoEntry(instrucciones, linea);
    }

    //Lee instrucciones hasta encontrar el token de cierre indicado.
    private List<NodoAST> parsearBloqueInstrucciones(Token.Tipo tokenCierre) {
        List<NodoAST> instrucciones = new ArrayList<>();

        while (!esTokenActual(tokenCierre) && !esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) {
            saltarComentarios();
            if (esTokenActual(tokenCierre) || esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) break;

            NodoAST instruccion = parsearInstruccion();
            if (instruccion != null) {
                instrucciones.add(instruccion);
            }
        }

        return instrucciones;
    }

    //Decide que tipo de instruccion parsear segun el token actual.
    private NodoAST parsearInstruccion() {
        saltarComentarios();
        Token t = tokenActual();

        switch (t.getTipo()) {
            case T_DEFINE:
                return parsearDefine();
            case T_IF:
                return parsearIf();
            case T_LOOP:
                return parsearLoop();
            case T_SELECT:
                return parsearSelect();
            case T_OUT:
                return parsearOut();
            case T_INPUT:
                return parsearInput();
            case T_IDENTIFICADOR:
                return parsearAsignacionOIncrementoPostfijo();
            case T_OBJECT:
                return parsearObject();
            case T_METHOD:
                return parsearMethod();
            default:
                errores.add("Error sintactico en linea " + t.getLinea()
                    + ": instruccion inesperada '" + t.getLexema() + "'.");
                avanzar(); // recuperacion: saltamos el token problematico
                return null;
        }
    }

    //define tipo identificador = expresion;
    private NodoAST.NodoDefine parsearDefine() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_DEFINE);

        String tipo = tokenActual().getLexema();
        if (!esTipoValido()) {
            errores.add("Error sintactico en linea " + linea + ": tipo de dato invalido '" + tipo + "'.");
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

    //if (condicion) { ... } else { ... }
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

    //loop (condicion) { ... }
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

    //select (variable) { case valor: instrucciones }
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
            // consumir el ':'
            if (esTokenActual(Token.Tipo.T_PUNTO_Y_COMA)) {
                // Se acepta ; o : (JODA usa : en el spec; toleramos ambos)
                avanzar();
            }
            // Recolectar instrucciones del case hasta el siguiente case o '}'
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

    //out(expresion);
    private NodoAST.NodoOut parsearOut() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_OUT);
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        NodoAST expresion = parsearExpresion();
        consumir(Token.Tipo.T_PARENTESIS_CIERRA);
        consumir(Token.Tipo.T_PUNTO_Y_COMA);
        return new NodoAST.NodoOut(expresion, linea);
    }

    //input(identificador);
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

    //identificador = expresion;   o   identificador++;  o   identificador--;
    private NodoAST parsearAsignacionOIncrementoPostfijo() {
        int linea = tokenActual().getLinea();
        String id = tokenActual().getLexema();
        consumir(Token.Tipo.T_IDENTIFICADOR);

        if (esTokenActual(Token.Tipo.T_INCREMENTO)) {
            avanzar();
            consumir(Token.Tipo.T_PUNTO_Y_COMA);
            return new NodoAST.NodoIncrementoPostfijo(id, "++", linea);
        }
        if (esTokenActual(Token.Tipo.T_DECREMENTO)) {
            avanzar();
            consumir(Token.Tipo.T_PUNTO_Y_COMA);
            return new NodoAST.NodoIncrementoPostfijo(id, "--", linea);
        }

        consumir(Token.Tipo.T_ASIGNACION);
        NodoAST expresion = parsearExpresion();
        consumir(Token.Tipo.T_PUNTO_Y_COMA);
        return new NodoAST.NodoAsignacion(id, expresion, linea);
    }

    //object Nombre { ... }
    private NodoAST.NodoObject parsearObject() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_OBJECT);
        String nombre = tokenActual().getLexema();
        avanzar(); // El nombre del objeto puede empezar con mayuscula
        consumir(Token.Tipo.T_LLAVE_ABRE);
        List<NodoAST> miembros = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoObject(nombre, miembros, linea);
    }

    //method Nombre(params) { ... }
    private NodoAST.NodoMethod parsearMethod() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_METHOD);
        String nombre = tokenActual().getLexema();
        avanzar();
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        // Por ahora ignoramos parametros (soporte futuro)
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

    // Parseo de expresiones (precedencia ascendente)
    // Nivel maximo: expresion logica (&&, ||)
    private NodoAST parsearExpresion() {
        return parsearOr();
    }

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

    private NodoAST parsearIgualdad() {
        NodoAST izq = parsearRelacional();
        while (esTokenActual(Token.Tipo.T_IGUAL_IGUAL) || esTokenActual(Token.Tipo.T_DIFERENTE)) {
            int linea = tokenActual().getLinea();
            String op = tokenActual().getLexema();
            avanzar();
            NodoAST der = parsearRelacional();
            izq = new NodoAST.NodoBinario(izq, op, der, linea);
        }
        return izq;
    }

    private NodoAST parsearRelacional() {
        NodoAST izq = parsearSuma();
        while (esTokenActual(Token.Tipo.T_MAYOR) || esTokenActual(Token.Tipo.T_MENOR)
                || esTokenActual(Token.Tipo.T_MAYOR_IGUAL) || esTokenActual(Token.Tipo.T_MENOR_IGUAL)) {
            int linea = tokenActual().getLinea();
            String op = tokenActual().getLexema();
            avanzar();
            NodoAST der = parsearSuma();
            izq = new NodoAST.NodoBinario(izq, op, der, linea);
        }
        return izq;
    }

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

    private NodoAST parsearMultiplicacion() {
        NodoAST izq = parsearUnario();
        while (esTokenActual(Token.Tipo.T_MULTIPLICACION) || esTokenActual(Token.Tipo.T_DIVISION)
                || esTokenActual(Token.Tipo.T_MODULO)) {
            int linea = tokenActual().getLinea();
            String op = tokenActual().getLexema();
            avanzar();
            NodoAST der = parsearUnario();
            izq = new NodoAST.NodoBinario(izq, op, der, linea);
        }
        return izq;
    }

    private NodoAST parsearUnario() {
        if (esTokenActual(Token.Tipo.T_NOT)) {
            int linea = tokenActual().getLinea();
            avanzar();
            NodoAST operando = parsearPrimario();
            return new NodoAST.NodoUnario("!", operando, linea);
        }
        if (esTokenActual(Token.Tipo.T_RESTA)) {
            int linea = tokenActual().getLinea();
            avanzar();
            NodoAST operando = parsearPrimario();
            return new NodoAST.NodoUnario("-", operando, linea);
        }
        return parsearPrimario();
    }

    private NodoAST parsearPrimario() {
        Token t = tokenActual();

        switch (t.getTipo()) {
            case T_LITERAL_ENTERO:
                avanzar();
                return new NodoAST.NodoLiteralEntero(Integer.parseInt(t.getLexema()), t.getLinea());

            case T_LITERAL_DECIMAL:
                avanzar();
                return new NodoAST.NodoLiteralDecimal(Double.parseDouble(t.getLexema()), t.getLinea());

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
                    + ": expresion invalida, token inesperado '" + t.getLexema() + "'.");
                avanzar(); // recuperacion
                return new NodoAST.NodoLiteralEntero(0, t.getLinea()); // nodo de relleno
        }
    }

    // Metodos auxiliares
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
                + ": se esperaba '" + tipo + "' pero se encontro '"
                + tokenActual().getLexema() + "' (" + tokenActual().getTipo() + ").");
        }
    }

    private void saltarComentarios() {
        while (esTokenActual(Token.Tipo.T_COMENTARIO)) {
            avanzar();
        }
    }

    private boolean esTipoValido() {
        Token.Tipo t = tokenActual().getTipo();
        return t == Token.Tipo.T_INT || t == Token.Tipo.T_DEC
            || t == Token.Tipo.T_STRING || t == Token.Tipo.T_BOOL
            || t == Token.Tipo.T_VOID || t == Token.Tipo.T_OBJECT;
    }

    public List<String> getErrores() { return errores; }
    public boolean tieneErrores() { return !errores.isEmpty(); }
}
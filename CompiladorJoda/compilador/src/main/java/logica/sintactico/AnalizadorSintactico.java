package logica.sintactico;

import java.util.ArrayList;
import java.util.List;

import logica.lexico.Token;

/*
Analizador Sintactico (Parser) de JODA.
CAMBIO v2.2:
  - parsearSelect ahora consume T_DOS_PUNTOS (':') despues del valor del case,
    que es la sintaxis oficial de JODA: "case 1:"
  - Se mantiene compatibilidad con ';' como separador alternativo (legacy).
*/
public class AnalizadorSintactico {

    private final List<Token>  tokens;
    private       int          posicion;
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

        // Recopilar declaraciones globales (object / method) que aparezcan
        // ANTES del bloque entry. JODA permite definir clases antes de entry.
        List<NodoAST> globales = new ArrayList<>();
        while (!esTokenActual(Token.Tipo.T_ENTRY)
                && !esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) {
            saltarComentarios();
            if (esTokenActual(Token.Tipo.T_OBJECT)) {
                globales.add(parsearObject());
            } else if (esTokenActual(Token.Tipo.T_METHOD)) {
                globales.add(parsearMethod());
            } else {
                // Token inesperado antes de entry
                errores.add("Error sintactico en linea " + tokenActual().getLinea()
                    + ": se esperaba 'entry' o una declaracion 'object'/'method', "
                    + "pero se encontro '" + tokenActual().getLexema() + "'.");
                avanzar();
            }
            saltarComentarios();
        }

        if (!esTokenActual(Token.Tipo.T_ENTRY)) {
            errores.add("Error sintactico en linea " + tokenActual().getLinea()
                + ": falta el bloque 'entry' principal del programa.");
            return null;
        }

        NodoAST.NodoEntry entry = parsearEntry();

        // Insertar los nodos globales al INICIO de las instrucciones de entry
        // para que el ejecutor los registre antes de ejecutar el cuerpo.
        if (!globales.isEmpty() && entry != null) {
            globales.addAll(entry.instrucciones);
            return new NodoAST.NodoEntry(globales, entry.getLinea());
        }
        return entry;
    }

    // ---------------------------------------------------------------
    // REGLAS GRAMATICALES
    // ---------------------------------------------------------------
    private NodoAST.NodoEntry parsearEntry() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_ENTRY);
        consumir(Token.Tipo.T_LLAVE_ABRE);
        List<NodoAST> instrucciones = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoEntry(instrucciones, linea);
    }

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
            case T_RETURN:       return parsearReturn();
            default:
                errores.add("Error sintactico en linea " + t.getLinea()
                    + ": instruccion inesperada '" + t.getLexema() + "' (" + t.getTipo() + ").");
                avanzar();
                return null;
        }
    }

    private NodoAST parsearReturn() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_RETURN);
        NodoAST expresion = null;
        if (!esTokenActual(Token.Tipo.T_PUNTO_Y_COMA)) {
            expresion = parsearExpresion();
        }
        consumir(Token.Tipo.T_PUNTO_Y_COMA);
        // Usamos NodoOut como nodo temporal para el return (el ejecutor lo ignora en metodos)
        return expresion != null ? expresion : new NodoAST.NodoLiteralEntero(0, linea);
    }

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

    /*
     * select (variable) {
     *     case valor:
     *         instrucciones
     *     case valor2:
     *         instrucciones
     * }
     * CAMBIO v2.2: despues del valor del case se consume T_DOS_PUNTOS (':').
     * Por compatibilidad, tambien se acepta T_PUNTO_Y_COMA (';') como separador legacy.
     */
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

            // Consumir separador: ':' (correcto) o ';' (legacy)
            if (esTokenActual(Token.Tipo.T_DOS_PUNTOS)) {
                avanzar();
            } else if (esTokenActual(Token.Tipo.T_PUNTO_Y_COMA)) {
                avanzar();
            }
            // Si no hay ninguno simplemente continuamos (tolerante)

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

    private NodoAST.NodoOut parsearOut() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_OUT);
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        NodoAST expresion = parsearExpresion();
        consumir(Token.Tipo.T_PARENTESIS_CIERRA);
        consumir(Token.Tipo.T_PUNTO_Y_COMA);
        return new NodoAST.NodoOut(expresion, linea);
    }

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

    private NodoAST parsearAsignacionOIncrementoPostfijo() {
        int    linea = tokenActual().getLinea();
        String id    = tokenActual().getLexema();
        consumir(Token.Tipo.T_IDENTIFICADOR);

        // *** NUEVO: llamada a metodo -> saludar(); ***
        if (esTokenActual(Token.Tipo.T_PARENTESIS_ABRE)) {
            consumir(Token.Tipo.T_PARENTESIS_ABRE);
            // Consumir argumentos si los hay (por ahora los ignoramos)
            while (!esTokenActual(Token.Tipo.T_PARENTESIS_CIERRA)
                    && !esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) {
                avanzar();
            }
            consumir(Token.Tipo.T_PARENTESIS_CIERRA);
            consumir(Token.Tipo.T_PUNTO_Y_COMA);
            // Reutilizamos NodoOut con un literal vacio como nodo dummy
            // El ejecutor lo ignorara igual que a NodoMethod
            return new NodoAST.NodoLiteralCadena("__llamada__" + id, linea);
        }

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


    private NodoAST.NodoObject parsearObject() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_OBJECT);
        String nombre = tokenActual().getLexema();
        avanzar();
        consumir(Token.Tipo.T_LLAVE_ABRE);
        List<NodoAST> miembros = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoObject(nombre, miembros, linea);
    }

    private NodoAST.NodoMethod parsearMethod() {
        int linea = tokenActual().getLinea();
        consumir(Token.Tipo.T_METHOD);

        // Tipo de retorno opcional: method [tipo] nombre(...)
        // Si el token actual es un tipo valido, lo consumimos y lo ignoramos
        // (soporte futuro; por ahora no se usa en la ejecucion).
        if (esTipoValido()) {
            avanzar(); // consumir tipo de retorno (int, dec, string, bool, void, object)
        }

        String nombre = tokenActual().getLexema();
        avanzar(); // consumir nombre del metodo

        // Parametros: consumir todo entre '(' y ')' ignorando tipos y comas
        // Sintaxis: method [tipo] nombre(tipo param, tipo param, ...)
        consumir(Token.Tipo.T_PARENTESIS_ABRE);
        while (!esTokenActual(Token.Tipo.T_PARENTESIS_CIERRA) && !esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) {
            avanzar(); // saltar tipos, nombres de parametros y comas
        }
        consumir(Token.Tipo.T_PARENTESIS_CIERRA);

        consumir(Token.Tipo.T_LLAVE_ABRE);
        List<NodoAST> cuerpo = parsearBloqueInstrucciones(Token.Tipo.T_LLAVE_CIERRA);
        consumir(Token.Tipo.T_LLAVE_CIERRA);
        return new NodoAST.NodoMethod(nombre, cuerpo, linea);
    }

    // ---------------------------------------------------------------
    // EXPRESIONES
    // ---------------------------------------------------------------
    private NodoAST parsearExpresion()       { return parsearOr(); }

    private NodoAST parsearOr() {
        NodoAST izq = parsearAnd();
        while (esTokenActual(Token.Tipo.T_OR)) {
            int linea = tokenActual().getLinea(); String op = tokenActual().getLexema(); avanzar();
            izq = new NodoAST.NodoBinario(izq, op, parsearAnd(), linea);
        }
        return izq;
    }

    private NodoAST parsearAnd() {
        NodoAST izq = parsearIgualdad();
        while (esTokenActual(Token.Tipo.T_AND)) {
            int linea = tokenActual().getLinea(); String op = tokenActual().getLexema(); avanzar();
            izq = new NodoAST.NodoBinario(izq, op, parsearIgualdad(), linea);
        }
        return izq;
    }

    private NodoAST parsearIgualdad() {
        NodoAST izq = parsearRelacional();
        while (esTokenActual(Token.Tipo.T_IGUAL_IGUAL) || esTokenActual(Token.Tipo.T_DIFERENTE)) {
            int linea = tokenActual().getLinea(); String op = tokenActual().getLexema(); avanzar();
            izq = new NodoAST.NodoBinario(izq, op, parsearRelacional(), linea);
        }
        return izq;
    }

    private NodoAST parsearRelacional() {
        NodoAST izq = parsearSuma();
        while (esTokenActual(Token.Tipo.T_MAYOR) || esTokenActual(Token.Tipo.T_MENOR)
                || esTokenActual(Token.Tipo.T_MAYOR_IGUAL) || esTokenActual(Token.Tipo.T_MENOR_IGUAL)) {
            int linea = tokenActual().getLinea(); String op = tokenActual().getLexema(); avanzar();
            izq = new NodoAST.NodoBinario(izq, op, parsearSuma(), linea);
        }
        return izq;
    }

    private NodoAST parsearSuma() {
        NodoAST izq = parsearMultiplicacion();
        while (esTokenActual(Token.Tipo.T_SUMA) || esTokenActual(Token.Tipo.T_RESTA)) {
            int linea = tokenActual().getLinea(); String op = tokenActual().getLexema(); avanzar();
            izq = new NodoAST.NodoBinario(izq, op, parsearMultiplicacion(), linea);
        }
        return izq;
    }

    private NodoAST parsearMultiplicacion() {
        NodoAST izq = parsearUnario();
        while (esTokenActual(Token.Tipo.T_MULTIPLICACION) || esTokenActual(Token.Tipo.T_DIVISION)
                || esTokenActual(Token.Tipo.T_MODULO)) {
            int linea = tokenActual().getLinea(); String op = tokenActual().getLexema(); avanzar();
            izq = new NodoAST.NodoBinario(izq, op, parsearUnario(), linea);
        }
        return izq;
    }

    private NodoAST parsearUnario() {
        if (esTokenActual(Token.Tipo.T_NOT)) {
            int linea = tokenActual().getLinea(); avanzar();
            return new NodoAST.NodoUnario("!", parsearPrimario(), linea);
        }
        if (esTokenActual(Token.Tipo.T_RESTA)) {
            int linea = tokenActual().getLinea(); avanzar();
            return new NodoAST.NodoUnario("-", parsearPrimario(), linea);
        }
        return parsearPrimario();
    }

    private NodoAST parsearPrimario() {
        Token t = tokenActual();
        NodoAST nodo;

        switch (t.getTipo()) {
            case T_LITERAL_ENTERO:
                avanzar();
                nodo = new NodoAST.NodoLiteralEntero(Integer.parseInt(t.getLexema()), t.getLinea());
                break;
            case T_LITERAL_DECIMAL:
                avanzar();
                nodo = new NodoAST.NodoLiteralDecimal(Double.parseDouble(t.getLexema()), t.getLinea());
                break;
            case T_LITERAL_CADENA:
                avanzar();
                nodo = new NodoAST.NodoLiteralCadena(t.getLexema(), t.getLinea());
                break;
            case T_LITERAL_BOOL: case T_TRUE: case T_FALSE:
                avanzar();
                nodo = new NodoAST.NodoLiteralBool("true".equals(t.getLexema()), t.getLinea());
                break;
            case T_NEW: {
                // new NombreClase(args...)
                // Tratamos la instanciacion como un literal null por ahora;
                // consumimos toda la expresion para no generar errores en cascada.
                avanzar(); // consumir 'new'
                avanzar(); // consumir nombre de clase
                if (esTokenActual(Token.Tipo.T_PARENTESIS_ABRE)) {
                    consumir(Token.Tipo.T_PARENTESIS_ABRE);
                    while (!esTokenActual(Token.Tipo.T_PARENTESIS_CIERRA)
                            && !esTokenActual(Token.Tipo.T_FIN_ARCHIVO)) {
                        avanzar();
                    }
                    consumir(Token.Tipo.T_PARENTESIS_CIERRA);
                }
                nodo = new NodoAST.NodoLiteralCadena("__instancia__", t.getLinea());
                break;
            }
            case T_IDENTIFICADOR: {
                avanzar();
                nodo = new NodoAST.NodoIdentificador(t.getLexema(), t.getLinea());
                break;
            }
            case T_PARENTESIS_ABRE:
                avanzar();
                nodo = parsearExpresion();
                consumir(Token.Tipo.T_PARENTESIS_CIERRA);
                break;
            default:
                errores.add("Error sintactico en linea " + t.getLinea()
                    + ": expresion invalida, token inesperado '" + t.getLexema() + "' (" + t.getTipo() + ").");
                avanzar();
                return new NodoAST.NodoLiteralEntero(0, t.getLinea());
        }

        // Encadenar accesos con '.' : obj.campo  o  obj.metodo(args)
        // Ejemplo: calc.sumar(n1, n2)  ->  se consume y se devuelve nodo dummy
        while (esTokenActual(Token.Tipo.T_PUNTO)) {
            avanzar(); // consumir '.'
            if (esTokenActual(Token.Tipo.T_IDENTIFICADOR)) {
                avanzar(); // consumir nombre del miembro/metodo
            }
            if (esTokenActual(Token.Tipo.T_PARENTESIS_ABRE)) {
                // Llamada a metodo: consumir argumentos
                consumir(Token.Tipo.T_PARENTESIS_ABRE);
                List<NodoAST> args = new ArrayList<>();
                if (!esTokenActual(Token.Tipo.T_PARENTESIS_CIERRA)) {
                    args.add(parsearExpresion());
                    while (esTokenActual(Token.Tipo.T_COMA)) {
                        avanzar();
                        args.add(parsearExpresion());
                    }
                }
                consumir(Token.Tipo.T_PARENTESIS_CIERRA);
                // Retornamos null como valor de la llamada (soporte futuro de OOP)
                nodo = new NodoAST.NodoLiteralEntero(0, t.getLinea());
            }
        }

        return nodo;
    }

    // ---------------------------------------------------------------
    private Token   tokenActual() {
        return (posicion < tokens.size()) ? tokens.get(posicion) : tokens.get(tokens.size() - 1);
    }
    private void    avanzar()     { if (posicion < tokens.size() - 1) posicion++; }
    private boolean esTokenActual(Token.Tipo tipo) { return tokenActual().getTipo() == tipo; }

    private void consumir(Token.Tipo tipo) {
        if (esTokenActual(tipo)) {
            avanzar();
        } else {
            errores.add("Error sintactico en linea " + tokenActual().getLinea()
                + ": se esperaba '" + tipo + "' pero se encontro '"
                + tokenActual().getLexema() + "' (" + tokenActual().getTipo() + ").");
        }
    }

    private void    saltarComentarios() { while (esTokenActual(Token.Tipo.T_COMENTARIO)) avanzar(); }

    private boolean esTipoValido() {
        Token.Tipo t = tokenActual().getTipo();
        return t == Token.Tipo.T_INT || t == Token.Tipo.T_DEC || t == Token.Tipo.T_STRING
            || t == Token.Tipo.T_BOOL || t == Token.Tipo.T_VOID || t == Token.Tipo.T_OBJECT;
    }

    public List<String> getErrores()   { return errores; }
    public boolean      tieneErrores() { return !errores.isEmpty(); }
}
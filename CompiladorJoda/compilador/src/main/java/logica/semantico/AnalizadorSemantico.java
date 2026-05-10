package logica.semantico;

import java.util.ArrayList;
import java.util.List;

import logica.sintactico.NodoAST;

/*
Analizador Semantico del compilador
Lo que hace es recorrer el AST y valida las reglas de contexto:
    -Variables declaradas pro el usuario
    -No se redeclaran variables en el mismo ambito
    -Compatibilidad de tipos en asignaciones
    -Compatibilidad de tipos en operaciones binarias
    -Correcto uso de operacores segun el tipo de operandos.
    
Reglas de tipos de JODA:
    -Operadores aritmeticos solo entre numericos
        Excepcion con "+" para concatenacion de strings
    -Operadores relacionales de orden solo entre numericos
        No se pueden comparar string con int, string con bool, etc.
    -Operadores de igualdad entre tipos identicos o  numeros entre si
        No se puede comparar int con string, bool, etc.
    -Operadores logicos solo entre booleanos
    -Operador NOT solo sobre booleano
    -Incremento/decremento solo sobre numericos
*/
public class AnalizadorSemantico {

    private final TablaSimbolos tablaSimbolos;
    private final List<String> errores;
    private final List<String> advertencias;

    public AnalizadorSemantico() {
        this.tablaSimbolos = new TablaSimbolos();
        this.errores = new ArrayList<>();
        this.advertencias = new ArrayList<>();
    }

    // Registro global de todos los simbolos declarados (para reporte final)
    private final List<EntradaTablaSimbolos> registroGlobal = new ArrayList<>();

    // Alias cortos para legibilidad
    private static final EntradaTablaSimbolos.TipoDato INT    = EntradaTablaSimbolos.TipoDato.INT;
    private static final EntradaTablaSimbolos.TipoDato DEC    = EntradaTablaSimbolos.TipoDato.DEC;
    private static final EntradaTablaSimbolos.TipoDato STRING = EntradaTablaSimbolos.TipoDato.STRING;
    private static final EntradaTablaSimbolos.TipoDato BOOL   = EntradaTablaSimbolos.TipoDato.BOOL;
    private static final EntradaTablaSimbolos.TipoDato VOID   = EntradaTablaSimbolos.TipoDato.VOID;
    private static final EntradaTablaSimbolos.TipoDato DESC   = EntradaTablaSimbolos.TipoDato.DESCONOCIDO;

    // PUNTO DE ENTRADA
    public void analizar(NodoAST.NodoEntry nodoEntry) {
        if (nodoEntry == null) return;
        analizarBloque(nodoEntry.instrucciones);
    }

    public List<EntradaTablaSimbolos> getRegistroGlobal() { return registroGlobal; }

    // ANALISIS DE BLOQUES E INSTRUCCIONES
    private void analizarBloque(List<NodoAST> instrucciones) {
        tablaSimbolos.abrirAmbito();
        for (NodoAST instruccion : instrucciones) {
            analizarNodo(instruccion);
        }
        tablaSimbolos.cerrarAmbito();
    }

    private void analizarNodo(NodoAST nodo) {
        if (nodo == null) return;

        if      (nodo instanceof NodoAST.NodoDefine)             analizarDefine((NodoAST.NodoDefine) nodo);
        else if (nodo instanceof NodoAST.NodoAsignacion)         analizarAsignacion((NodoAST.NodoAsignacion) nodo);
        else if (nodo instanceof NodoAST.NodoIf)                 analizarIf((NodoAST.NodoIf) nodo);
        else if (nodo instanceof NodoAST.NodoLoop)               analizarLoop((NodoAST.NodoLoop) nodo);
        else if (nodo instanceof NodoAST.NodoSelect)             analizarSelect((NodoAST.NodoSelect) nodo);
        else if (nodo instanceof NodoAST.NodoOut)                analizarOut((NodoAST.NodoOut) nodo);
        else if (nodo instanceof NodoAST.NodoInput)              analizarInput((NodoAST.NodoInput) nodo);
        else if (nodo instanceof NodoAST.NodoIncrementoPostfijo) analizarIncrementoPostfijo((NodoAST.NodoIncrementoPostfijo) nodo);
        else if (nodo instanceof NodoAST.NodoObject)             analizarObject((NodoAST.NodoObject) nodo);
        else if (nodo instanceof NodoAST.NodoMethod)             analizarMethod((NodoAST.NodoMethod) nodo);
    }

    // DECLARACION DE VARIABLE
    private void analizarDefine(NodoAST.NodoDefine nodo) {
        EntradaTablaSimbolos.TipoDato tipo = EntradaTablaSimbolos.parsearTipo(nodo.tipo);
        if (tipo == DESC) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": tipo de dato desconocido '" + nodo.tipo + "'.");
        }

        EntradaTablaSimbolos entrada = new EntradaTablaSimbolos(
            nodo.identificador, tipo,
            EntradaTablaSimbolos.CategoriaEntrada.VARIABLE, nodo.getLinea()
        );

        if (!tablaSimbolos.declarar(entrada)) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' ya fue declarada en este ambito.");
            return;
        }

        registroGlobal.add(entrada);

        if (nodo.expresion != null) {
            // Validar tipos en la expresion de inicializacion
            EntradaTablaSimbolos.TipoDato tipoExpr = analizarExpresionConTipo(nodo.expresion);
            if (!sonCompatiblesAsignacion(tipo, tipoExpr)) {
                if (tipo == DEC && tipoExpr == INT) {
                    advertencias.add("Advertencia en linea " + nodo.getLinea()
                        + ": promocion automatica de 'int' a 'dec' para '"
                        + nodo.identificador + "'.");
                } else {
                    errores.add("Error semantico en linea " + nodo.getLinea()
                        + ": tipo incompatible. '" + nodo.identificador
                        + "' es '" + nodo.tipo + "' pero se asigna '"
                        + nombreTipo(tipoExpr) + "'.");
                }
            }
        } else {
            advertencias.add("Advertencia en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' se declara sin valor inicial.");
        }
    }

    // ASIGNACION
    private void analizarAsignacion(NodoAST.NodoAsignacion nodo) {
        EntradaTablaSimbolos entrada = tablaSimbolos.buscar(nodo.identificador);
        if (entrada == null) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' no fue declarada.");
            return;
        }

        EntradaTablaSimbolos.TipoDato tipoExpr = analizarExpresionConTipo(nodo.expresion);
        if (!sonCompatiblesAsignacion(entrada.getTipoDato(), tipoExpr)) {
            if (entrada.getTipoDato() == DEC && tipoExpr == INT) {
                advertencias.add("Advertencia en linea " + nodo.getLinea()
                    + ": promocion de 'int' a 'dec' en asignacion a '"
                    + nodo.identificador + "'.");
            } else {
                errores.add("Error semantico en linea " + nodo.getLinea()
                    + ": tipo incompatible en asignacion a '"
                    + nodo.identificador + "' (tipo '" + nombreTipo(entrada.getTipoDato())
                    + "'): no se puede asignar '" + nombreTipo(tipoExpr) + "'.");
            }
        }
    }

    // ESTRUCTURAS DE CONTROL
    private void analizarIf(NodoAST.NodoIf nodo) {
        EntradaTablaSimbolos.TipoDato tipoCondicion = analizarExpresionConTipo(nodo.condicion);
        // La condicion de un 'if' debe ser bool o una expresion relacional/logica
        if (tipoCondicion != BOOL && tipoCondicion != DESC) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la condicion del 'if' debe ser booleana, pero se encontro '"
                + nombreTipo(tipoCondicion) + "'.");
        }
        analizarBloque(nodo.bloqueThen);
        if (nodo.bloqueElse != null) analizarBloque(nodo.bloqueElse);
    }

    private void analizarLoop(NodoAST.NodoLoop nodo) {
        EntradaTablaSimbolos.TipoDato tipoCondicion = analizarExpresionConTipo(nodo.condicion);
        if (tipoCondicion != BOOL && tipoCondicion != DESC) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la condicion del 'loop' debe ser booleana, pero se encontro '"
                + nombreTipo(tipoCondicion) + "'.");
        }
        analizarBloque(nodo.cuerpo);
    }

    private void analizarSelect(NodoAST.NodoSelect nodo) {
        EntradaTablaSimbolos.TipoDato tipoVar = analizarExpresionConTipo(nodo.variable);
        for (NodoAST.NodoCaso caso : nodo.casos) {
            EntradaTablaSimbolos.TipoDato tipoCaso = analizarExpresionConTipo(caso.valor);
            // El valor del case debe ser del mismo tipo que la variable del select
            if (tipoVar != DESC && tipoCaso != DESC && !sonMismoGrupo(tipoVar, tipoCaso)) {
                errores.add("Error semantico en linea " + caso.getLinea()
                    + ": el valor del 'case' es de tipo '" + nombreTipo(tipoCaso)
                    + "' pero la variable de 'select' es de tipo '"
                    + nombreTipo(tipoVar) + "'.");
            }
            analizarBloque(caso.instrucciones);
        }
    }

    private void analizarOut(NodoAST.NodoOut nodo) {
        analizarExpresionConTipo(nodo.expresion);
    }

    private void analizarInput(NodoAST.NodoInput nodo) {
        if (tablaSimbolos.buscar(nodo.identificador) == null) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' no fue declarada.");
        }
    }

    private void analizarIncrementoPostfijo(NodoAST.NodoIncrementoPostfijo nodo) {
        EntradaTablaSimbolos entrada = tablaSimbolos.buscar(nodo.identificador);
        if (entrada == null) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' no fue declarada.");
            return;
        }
        if (entrada.getTipoDato() != INT && entrada.getTipoDato() != DEC) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": el operador '" + nodo.operador
                + "' solo aplica a tipos numericos (int, dec), pero '"
                + nodo.identificador + "' es de tipo '"
                + nombreTipo(entrada.getTipoDato()) + "'.");
        }
    }

    private void analizarObject(NodoAST.NodoObject nodo) {
        tablaSimbolos.declarar(new EntradaTablaSimbolos(
            nodo.nombre, EntradaTablaSimbolos.TipoDato.OBJECT,
            EntradaTablaSimbolos.CategoriaEntrada.CLASE, nodo.getLinea()
        ));
        analizarBloque(nodo.miembros);
    }

    private void analizarMethod(NodoAST.NodoMethod nodo) {
        tablaSimbolos.declarar(new EntradaTablaSimbolos(
            nodo.nombre, VOID,
            EntradaTablaSimbolos.CategoriaEntrada.METODO, nodo.getLinea()
        ));
        analizarBloque(nodo.cuerpo);
    }

    // ANALISIS DE EXPRESIONES CON INFERENCIA DE TIPO (NUCLEO)
    /*
    Analiza una expresion en profundidad, valida todas sus sub-expresiones
    y retorna el tipo resultante de la expresion completa.
    
    Esta es la pieza central de la mejora: antes, analizarExpresion solo
    verificaba que las variables existieran, pero no validaba la compatibilidad
    de tipos entre los operandos de cada operacion binaria.
    */
    private EntradaTablaSimbolos.TipoDato analizarExpresionConTipo(NodoAST nodo) {
        if (nodo == null) return DESC;

        // --- Literales: su tipo es fijo ---
        if (nodo instanceof NodoAST.NodoLiteralEntero)  return INT;
        if (nodo instanceof NodoAST.NodoLiteralDecimal) return DEC;
        if (nodo instanceof NodoAST.NodoLiteralCadena)  return STRING;
        if (nodo instanceof NodoAST.NodoLiteralBool)    return BOOL;

        // --- Identificador: buscar en tabla de simbolos ---
        if (nodo instanceof NodoAST.NodoIdentificador) {
            String nombre = ((NodoAST.NodoIdentificador) nodo).nombre;
            EntradaTablaSimbolos e = tablaSimbolos.buscar(nombre);
            if (e == null) {
                errores.add("Error semantico en linea " + nodo.getLinea()
                    + ": el identificador '" + nombre + "' no fue declarado.");
                return DESC;
            }
            return e.getTipoDato();
        }

        // --- Expresion unaria ---
        if (nodo instanceof NodoAST.NodoUnario) {
            return analizarUnario((NodoAST.NodoUnario) nodo);
        }

        // --- Expresion binaria: aqui esta la validacion central ---
        if (nodo instanceof NodoAST.NodoBinario) {
            return analizarBinario((NodoAST.NodoBinario) nodo);
        }

        return DESC;
    }

    // VALIDACION DE OPERACION UNARIA
    private EntradaTablaSimbolos.TipoDato analizarUnario(NodoAST.NodoUnario nodo) {
        EntradaTablaSimbolos.TipoDato tipoOperando = analizarExpresionConTipo(nodo.operando);

        switch (nodo.operador) {
            case "!":
                // NOT solo sobre bool
                if (tipoOperando != BOOL && tipoOperando != DESC) {
                    errores.add("Error semantico en linea " + nodo.getLinea()
                        + ": el operador '!' solo aplica a expresiones booleanas, "
                        + "pero se uso sobre '" + nombreTipo(tipoOperando) + "'.");
                }
                return BOOL;

            case "-":
                // Negacion numerica solo sobre int o dec
                if (tipoOperando != INT && tipoOperando != DEC && tipoOperando != DESC) {
                    errores.add("Error semantico en linea " + nodo.getLinea()
                        + ": el operador '-' (negacion) solo aplica a tipos numericos, "
                        + "pero se uso sobre '" + nombreTipo(tipoOperando) + "'.");
                }
                return tipoOperando;

            default:
                return tipoOperando;
        }
    }

    // VALIDACION DE OPERACION BINARIA  <-- MEJORA PRINCIPAL
    /*
    Valida que los dos operandos de una operacion binaria sean compatibles
    con el operador dado, y retorna el tipo resultado de la expresion.
    
    Tabla de reglas:
    
    Operador  | Operandos validos           | Tipo resultado
    ----------+-----------------------------+----------------
    + (num)   | int+int, dec+dec, int+dec   | int o dec
    + (concat)| cualquiera con string       | string
    -, *, /   | int, dec (misma familia)    | int o dec
    %         | int, dec (misma familia)    | int
    >, <,>=,<=| int, dec (misma familia)    | bool  <-- RESTRICCION NUEVA
    ==, !=    | mismo tipo o numericos      | bool  <-- RESTRICCION NUEVA
    &&, ||    | bool + bool                 | bool  <-- RESTRICCION NUEVA
    */
    private EntradaTablaSimbolos.TipoDato analizarBinario(NodoAST.NodoBinario nodo) {
        // Analizar los dos lados primero (recursivo, detecta errores internos)
        EntradaTablaSimbolos.TipoDato tipoIzq = analizarExpresionConTipo(nodo.izquierda);
        EntradaTablaSimbolos.TipoDato tipoDer = analizarExpresionConTipo(nodo.derecha);
        String op = nodo.operador;
        int linea = nodo.getLinea();

        // Si alguno es DESCONOCIDO (error previo), no generar error en cascada
        if (tipoIzq == DESC || tipoDer == DESC) {
            return inferirTipoResultadoSinValidar(op, tipoIzq, tipoDer);
        }

        // ---- OPERADORES LOGICOS: &&  || ----
        if (op.equals("&&") || op.equals("||")) {
            if (tipoIzq != BOOL) {
                errores.add("Error semantico en linea " + linea
                    + ": el operador '" + op + "' requiere operando izquierdo booleano, "
                    + "pero se encontro '" + nombreTipo(tipoIzq) + "'.");
            }
            if (tipoDer != BOOL) {
                errores.add("Error semantico en linea " + linea
                    + ": el operador '" + op + "' requiere operando derecho booleano, "
                    + "pero se encontro '" + nombreTipo(tipoDer) + "'.");
            }
            return BOOL;
        }

        // ---- OPERADORES RELACIONALES DE ORDEN: >  <  >=  <= ----
        if (op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<=")) {
            // Solo se pueden comparar tipos numericos entre si
            if (!esNumerico(tipoIzq) || !esNumerico(tipoDer)) {
                errores.add("Error semantico en linea " + linea
                    + ": el operador '" + op + "' solo compara tipos numericos (int, dec). "
                    + "No se puede comparar '" + nombreTipo(tipoIzq)
                    + "' con '" + nombreTipo(tipoDer) + "'.");
            }
            return BOOL;
        }

        // ---- OPERADORES DE IGUALDAD: ==  != ----
        if (op.equals("==") || op.equals("!=")) {
            // Permitido: mismo tipo, o dos numericos entre si
            boolean ambosNumericos = esNumerico(tipoIzq) && esNumerico(tipoDer);
            boolean mismoTipo      = tipoIzq == tipoDer;
            if (!mismoTipo && !ambosNumericos) {
                errores.add("Error semantico en linea " + linea
                    + ": el operador '" + op + "' no puede comparar '"
                    + nombreTipo(tipoIzq) + "' con '" + nombreTipo(tipoDer)
                    + "'. Solo se pueden comparar valores del mismo tipo o dos numericos.");
            }
            return BOOL;
        }

        // ---- OPERADOR '+': concatenacion o suma ----
        if (op.equals("+")) {
            // Si uno de los dos es string, el resultado es concatenacion (string)
            if (tipoIzq == STRING || tipoDer == STRING) {
                return STRING;
            }
            // Si ambos son numericos: suma normal
            if (esNumerico(tipoIzq) && esNumerico(tipoDer)) {
                return (tipoIzq == DEC || tipoDer == DEC) ? DEC : INT;
            }
            // Cualquier otro caso (ej: bool + int) es error
            errores.add("Error semantico en linea " + linea
                + ": el operador '+' no puede operar sobre '"
                + nombreTipo(tipoIzq) + "' y '" + nombreTipo(tipoDer) + "'.");
            return DESC;
        }

        // ---- OPERADORES ARITMETICOS: -  *  /  % ----
        if (op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
            if (!esNumerico(tipoIzq) || !esNumerico(tipoDer)) {
                errores.add("Error semantico en linea " + linea
                    + ": el operador '" + op + "' solo opera sobre tipos numericos (int, dec). "
                    + "No se puede usar con '" + nombreTipo(tipoIzq)
                    + "' y '" + nombreTipo(tipoDer) + "'.");
                return DESC;
            }
            // Modulo siempre retorna int
            if (op.equals("%")) return INT;
            // Division entre enteros puede dar decimal en JODA
            if (op.equals("/")) return DEC;
            // Suma/resta/multiplicacion: si alguno es dec, resultado es dec
            return (tipoIzq == DEC || tipoDer == DEC) ? DEC : INT;
        }

        // Operador no reconocido
        return DESC;
    }

    // INFERENCIA DE TIPO SIN VALIDACION (para errores previos)
    /*
    Cuando uno de los operandos ya tiene tipo DESCONOCIDO (hubo error previo),
    intentamos inferir el tipo del resultado sin generar errores adicionales
    en cascada, para no "inundar" al usuario con mensajes de error derivados.
    */
    private EntradaTablaSimbolos.TipoDato inferirTipoResultadoSinValidar(
            String op,
            EntradaTablaSimbolos.TipoDato tipoIzq,
            EntradaTablaSimbolos.TipoDato tipoDer) {

        // Operadores que siempre retornan bool
        if (op.equals("==") || op.equals("!=") || op.equals(">") || op.equals("<")
                || op.equals(">=") || op.equals("<=")
                || op.equals("&&") || op.equals("||")) {
            return BOOL;
        }
        // Concatenacion con string conocido
        if (tipoIzq == STRING || tipoDer == STRING) return STRING;
        // Si alguno es dec
        if (tipoIzq == DEC || tipoDer == DEC) return DEC;
        // Por defecto
        return DESC;
    }

    // COMPATIBILIDAD EN ASIGNACION
    /*
    Determina si un tipo puede ser asignado a una variable de otro tipo.
    Solo se permiten las coerciones explicitas de JODA:
    - int -> dec (promocion numerica)
    - mismo tipo
    Todo lo demas es incompatible.
    */
    private boolean sonCompatiblesAsignacion(EntradaTablaSimbolos.TipoDato esperado,
                                              EntradaTablaSimbolos.TipoDato actual) {
        if (actual == DESC || esperado == DESC) return true; // error previo, no acumular
        if (esperado == actual) return true;
        // int se puede asignar a dec (promocion)
        if (esperado == DEC && actual == INT) return true;
        return false;
    }

    // COMPATIBILIDAD DE GRUPO (para select/case)
    /*
    Verifica si dos tipos pertenecen al mismo grupo semantico.
    Numericos (int, dec) se consideran del mismo grupo entre si.
    */
    private boolean sonMismoGrupo(EntradaTablaSimbolos.TipoDato a,
                                   EntradaTablaSimbolos.TipoDato b) {
        if (a == b) return true;
        if (esNumerico(a) && esNumerico(b)) return true;
        return false;
    }

    // UTILIDADES

    /* Retorna true si el tipo es numerico (int o dec). */
    private boolean esNumerico(EntradaTablaSimbolos.TipoDato tipo) {
        return tipo == INT || tipo == DEC;
    }

    /* Retorna el nombre legible del tipo para los mensajes de error. */
    private String nombreTipo(EntradaTablaSimbolos.TipoDato tipo) {
        if (tipo == null) return "desconocido";
        switch (tipo) {
            case INT:         return "int";
            case DEC:         return "dec";
            case STRING:      return "string";
            case BOOL:        return "bool";
            case VOID:        return "void";
            case OBJECT:      return "object";
            case DESCONOCIDO: return "desconocido";
            default:          return tipo.name().toLowerCase();
        }
    }

    private EntradaTablaSimbolos.TipoDato inferirTipo(NodoAST nodo) {
        return analizarExpresionConTipo(nodo);
    }

    // ACCESO A RESULTADOS
    public TablaSimbolos getTablaSimbolos()  { return tablaSimbolos; }
    public List<String>  getErrores()        { return errores; }
    public List<String>  getAdvertencias()   { return advertencias; }
    public boolean       tieneErrores()      { return !errores.isEmpty(); }
}
package logica.sintactico;

import logica.lexico.Token;
import java.util.ArrayList;
import java.util.List;

/*
Analizador sintactico

Valida las siguientes construcciones:
 - Bloque entry { ... }
 - Declaraciones: define tipo id [= valor] ;
 - Salida: out( expr ) ;
 - Entrada: input( id ) ;
 - Condicional: if ( cond ) { } [else { }]
 - Ciclo: loop ( cond ) { }
 - Seleccion: select ( id ) { case val: sentencia; ... }
 - Definicion de objeto: object Nombre { ... }
 - Definicion de metodo: method [tipo] nombre ( params ) { ... }
 - Asignacion: id = expr ;
 - Incremento/Decremento: id++ ; / id-- ;
*/
public class AnalizadorSintactico {

    // Error sintactico con descripcion y linea.
    public static class ErrorSintactico {
        private final String descripcion;
        private final int linea;

        public ErrorSintactico(String descripcion, int linea) {
            this.descripcion = descripcion;
            this.linea = linea;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public int getLinea() {
            return linea;
        }
    }

    // Estado interno
    private List<Token> tokens;
    private int pos;
    private List<ErrorSintactico> errores;

    public List<ErrorSintactico> analizar(List<Token> tokens) {
        this.tokens  = tokens;
        this.pos     = 0;
        this.errores = new ArrayList<>();

        parsearPrograma();
        return errores;
    }

    // ------------------------------------------------------------------ //
    // Reglas gramaticales
    // ------------------------------------------------------------------ //

    // programa -> declaracionTop* EOF
    private void parsearPrograma() {
        while (!esEOF()) {
            // CORRECCIÓN 1: ignorar comentarios a nivel de programa
            if (verActual().getTipo() == Token.Tipo.COMENTARIO) {
                avanzar();
                continue;
            }
            parsearDeclaracionTop();
        }
    }

    /*
     * declaracionTop -> definicionObjeto
     *                 | definicionMetodo
     *                 | bloqueEntry
     *                 | sentencia
     */
    private void parsearDeclaracionTop() {
        Token t = verActual();
        switch (t.getTipo()) {
            case PR_OBJECT: parsearObjeto(); break;
            case PR_METHOD: parsearMetodo(); break;
            case PR_ENTRY:  parsearEntry();  break;
            default:
                parsearSentencia();
                break;
        }
    }

    // entry { bloque }
    private void parsearEntry() {
        consumir(Token.Tipo.PR_ENTRY, "'entry'");
        consumir(Token.Tipo.DEL_LLAVE_A, "'{'  despues de 'entry'");
        parsearBloque();
        consumir(Token.Tipo.DEL_LLAVE_C, "'}'  para cerrar bloque 'entry'");
    }

    // object Nombre { miembroObjeto* }
    private void parsearObjeto() {
        consumir(Token.Tipo.PR_OBJECT, "'object'");
        consumirIdentificador("nombre de clase despues de 'object'");
        consumir(Token.Tipo.DEL_LLAVE_A, "'{'  despues del nombre de objeto");
        while (!esEOF()
                && verActual().getTipo() != Token.Tipo.DEL_LLAVE_C) {
            // CORRECCIÓN 2: ignorar comentarios dentro de object
            if (verActual().getTipo() == Token.Tipo.COMENTARIO) {
                avanzar();
                continue;
            }
            if (verActual().getTipo() == Token.Tipo.PR_DEFINE) {
                parsearDefine();
            } else if (verActual().getTipo() == Token.Tipo.PR_METHOD) {
                parsearMetodo();
            } else {
                errores.add(new ErrorSintactico(
                        "Construccion inesperada dentro de 'object': '"
                                + limpiar(verActual().getLexema()) + "'.",
                        verActual().getLinea()));
                avanzar();
            }
        }
        consumir(Token.Tipo.DEL_LLAVE_C, "'}'  para cerrar bloque 'object'");
    }

    // method [tipo] nombre ( params ) { bloque }
    private void parsearMetodo() {
        consumir(Token.Tipo.PR_METHOD, "'method'");
        // Tipo de retorno opcional
        if (esTipoDato(verActual().getTipo())) {
            avanzar(); // consume tipo
        }
        consumirIdentificador("nombre del metodo");
        consumir(Token.Tipo.DEL_PAREN_A, "'('  en declaracion de metodo");
        parsearParametros();
        consumir(Token.Tipo.DEL_PAREN_C, "')'  en declaracion de metodo");
        consumir(Token.Tipo.DEL_LLAVE_A, "'{'  en cuerpo de metodo");
        parsearBloque();
        consumir(Token.Tipo.DEL_LLAVE_C, "'}'  para cerrar metodo");
    }

    // parametros -> (tipo id (, tipo id)*)?
    private void parsearParametros() {
        if (esTipoDato(verActual().getTipo())) {
            avanzar(); // tipo
            consumirIdentificador("nombre de parametro");
            while (verActual().getTipo() == Token.Tipo.DEL_COMA) {
                avanzar(); // consume ,
                if (esTipoDato(verActual().getTipo())) {
                    avanzar();
                    consumirIdentificador("nombre de parametro");
                } else {
                    errores.add(new ErrorSintactico(
                            "Se esperaba tipo de dato despues de ','.",
                            verActual().getLinea()));
                }
            }
        }
    }

    // bloque -> sentencia*
    private void parsearBloque() {
        while (!esEOF()
                && verActual().getTipo() != Token.Tipo.DEL_LLAVE_C) {
            // CORRECCIÓN 3: ignorar comentarios dentro de cualquier bloque
            if (verActual().getTipo() == Token.Tipo.COMENTARIO) {
                avanzar();
                continue;
            }
            parsearSentencia();
        }
    }

    /*
     * sentencia -> define
     *            | out
     *            | input
     *            | if
     *            | loop
     *            | select
     *            | asignacion
     *            | incremento/decremento
     *            | return
     */
    private void parsearSentencia() {
        Token t = verActual();
        switch (t.getTipo()) {
            // CORRECCIÓN 4: ignorar comentarios en parsearSentencia
            // (defensa extra por si llega un comentario desde parsearDeclaracionTop)
            case COMENTARIO:
                avanzar();
                break;

            case PR_DEFINE: parsearDefine();              break;
            case PR_OUT:    parsearOut();                 break;
            case PR_INPUT:  parsearInput();               break;
            case PR_IF:     parsearIf();                  break;
            case PR_LOOP:   parsearLoop();                break;
            case PR_SELECT: parsearSelect();              break;
            case PR_RETURN: parsearReturn();              break;

            case IDENTIFICADOR:
                parsearAsignacionOIncremento();
                break;

            case DEL_LLAVE_C:
                // Fin de bloque, no consumir aqui
                break;

            case EOF:
                break;

            default:
                errores.add(new ErrorSintactico(
                        "Sentencia inesperada: '" + limpiar(t.getLexema()) + "'.",
                        t.getLinea()));
                avanzar(); // recuperacion
                break;
        }
    }

    // define tipo [[] ] id [= expr] ;
    private void parsearDefine() {
        consumir(Token.Tipo.PR_DEFINE, "'define'");
        if (!esTipoDato(verActual().getTipo())) {
            errores.add(new ErrorSintactico(
                    "Se esperaba tipo de dato despues de 'define'.",
                    verActual().getLinea()));
            recuperar();
            return;
        }
        avanzar(); // consume tipo
        // Arreglo opcional: []
        if (verActual().getTipo() == Token.Tipo.DEL_CORCHETE_A) {
            avanzar();
            consumir(Token.Tipo.DEL_CORCHETE_C, "']'  en declaracion de arreglo");
        }
        consumirIdentificador("nombre de variable en 'define'");
        if (verActual().getTipo() == Token.Tipo.OP_ASIGNACION) {
            avanzar(); // consume =
            parsearExpresion();
        }
        consumir(Token.Tipo.DEL_PUNTO_COMA, "';'  al final de la declaracion 'define'");
    }

    // out ( expr ) ;
    private void parsearOut() {
        consumir(Token.Tipo.PR_OUT, "'out'");
        consumir(Token.Tipo.DEL_PAREN_A, "'('  despues de 'out'");
        parsearExpresion();
        consumir(Token.Tipo.DEL_PAREN_C, "')'  al cerrar 'out'");
        consumir(Token.Tipo.DEL_PUNTO_COMA, "';'  al final de 'out'");
    }

    // input ( id ) ;
    private void parsearInput() {
        consumir(Token.Tipo.PR_INPUT, "'input'");
        consumir(Token.Tipo.DEL_PAREN_A, "'('  despues de 'input'");
        consumirIdentificador("variable en 'input'");
        consumir(Token.Tipo.DEL_PAREN_C, "')'  al cerrar 'input'");
        consumir(Token.Tipo.DEL_PUNTO_COMA, "';'  al final de 'input'");
    }

    // if ( cond ) { bloque } [else { bloque }]
    private void parsearIf() {
        consumir(Token.Tipo.PR_IF, "'if'");
        consumir(Token.Tipo.DEL_PAREN_A, "'('  en condicion 'if'");
        parsearExpresion();
        consumir(Token.Tipo.DEL_PAREN_C, "')'  en condicion 'if'");
        consumir(Token.Tipo.DEL_LLAVE_A, "'{'  en bloque 'if'");
        parsearBloque();
        consumir(Token.Tipo.DEL_LLAVE_C, "'}'  para cerrar bloque 'if'");
        if (verActual().getTipo() == Token.Tipo.PR_ELSE) {
            avanzar(); // consume else
            consumir(Token.Tipo.DEL_LLAVE_A, "'{'  en bloque 'else'");
            parsearBloque();
            consumir(Token.Tipo.DEL_LLAVE_C, "'}'  para cerrar bloque 'else'");
        }
    }

    // loop ( cond ) { bloque }
    private void parsearLoop() {
        consumir(Token.Tipo.PR_LOOP, "'loop'");
        consumir(Token.Tipo.DEL_PAREN_A, "'('  en condicion 'loop'");
        parsearExpresion();
        consumir(Token.Tipo.DEL_PAREN_C, "')'  en condicion 'loop'");
        consumir(Token.Tipo.DEL_LLAVE_A, "'{'  en bloque 'loop'");
        parsearBloque();
        consumir(Token.Tipo.DEL_LLAVE_C, "'}'  para cerrar bloque 'loop'");
    }

    // select ( id ) { (case val : sentencia)+ }
    private void parsearSelect() {
        consumir(Token.Tipo.PR_SELECT, "'select'");
        consumir(Token.Tipo.DEL_PAREN_A, "'('  en 'select'");
        consumirIdentificador("variable en 'select'");
        consumir(Token.Tipo.DEL_PAREN_C, "')'  en 'select'");
        consumir(Token.Tipo.DEL_LLAVE_A, "'{'  en bloque 'select'");
        while (!esEOF()
                && verActual().getTipo() == Token.Tipo.PR_CASE) {
            avanzar(); // consume 'case'
            if (esLiteral(verActual().getTipo())
                    || verActual().getTipo() == Token.Tipo.IDENTIFICADOR) {
                avanzar();
            } else {
                errores.add(new ErrorSintactico(
                        "Se esperaba un valor despues de 'case'.",
                        verActual().getLinea()));
            }
            parsearSentencia();
        }
        consumir(Token.Tipo.DEL_LLAVE_C, "'}'  para cerrar bloque 'select'");
    }

    // return [expr] ;
    private void parsearReturn() {
        consumir(Token.Tipo.PR_RETURN, "'return'");
        if (verActual().getTipo() != Token.Tipo.DEL_PUNTO_COMA) {
            parsearExpresion();
        }
        consumir(Token.Tipo.DEL_PUNTO_COMA, "';'  al final de 'return'");
    }

    // id = expr ; | id++ ; | id-- ; | id.metodo(...) ;
    private void parsearAsignacionOIncremento() {
        Token id = verActual();
        avanzar(); // consume el identificador

        Token.Tipo sig = verActual().getTipo();

        if (sig == Token.Tipo.OP_ASIGNACION) {
            avanzar(); // consume =
            parsearExpresion();
            consumir(Token.Tipo.DEL_PUNTO_COMA, "';'  al final de asignacion");

        } else if (sig == Token.Tipo.OP_INCREMENTO || sig == Token.Tipo.OP_DECREMENTO) {
            avanzar(); // consume ++ o --
            consumir(Token.Tipo.DEL_PUNTO_COMA, "';'  al final de incremento/decremento");

        } else if (sig == Token.Tipo.DEL_PUNTO) {
            // Acceso a metodo: id.metodo(...)
            avanzar(); // consume .
            consumirIdentificador("nombre de metodo despues de '.'");
            consumir(Token.Tipo.DEL_PAREN_A, "'('  en llamada a metodo");
            parsearArgumentos();
            consumir(Token.Tipo.DEL_PAREN_C, "')'  en llamada a metodo");
            consumir(Token.Tipo.DEL_PUNTO_COMA, "';'  al final de llamada a metodo");

        } else if (sig == Token.Tipo.DEL_PAREN_A) {
            // Llamada a funcion: id(...)
            avanzar();
            parsearArgumentos();
            consumir(Token.Tipo.DEL_PAREN_C, "')'  en llamada a funcion");
            consumir(Token.Tipo.DEL_PUNTO_COMA, "';'  al final de llamada a funcion");

        } else {
            errores.add(new ErrorSintactico(
                    "Se esperaba '=', '++', '--' o '.'  despues del identificador '"
                            + limpiar(id.getLexema()) + "'.",
                    id.getLinea()));
            recuperar();
        }
    }

    // argumentos -> (expr (, expr)*)?
    private void parsearArgumentos() {
        if (verActual().getTipo() != Token.Tipo.DEL_PAREN_C
                && !esEOF()) {
            parsearExpresion();
            while (verActual().getTipo() == Token.Tipo.DEL_COMA) {
                avanzar();
                parsearExpresion();
            }
        }
    }

    /*
     * expr -> termino ((op) termino)*
     */
    private void parsearExpresion() {
        parsearTermino();
        while (esOperadorBinario(verActual().getTipo())) {
            avanzar();
            parsearTermino();
        }
    }

    // termino -> literal | identificador | (expr) | !termino
    private void parsearTermino() {
        Token t = verActual();
        switch (t.getTipo()) {
            case LIT_ENTERO:
            case LIT_DECIMAL:
            case LIT_CADENA:
            case PR_TRUE:
            case PR_FALSE:
                avanzar();
                break;

            case IDENTIFICADOR:
                avanzar();
                if (verActual().getTipo() == Token.Tipo.DEL_PUNTO) {
                    avanzar();
                    consumirIdentificador("metodo despues de '.'");
                    if (verActual().getTipo() == Token.Tipo.DEL_PAREN_A) {
                        avanzar();
                        parsearArgumentos();
                        consumir(Token.Tipo.DEL_PAREN_C, "')'");
                    }
                } else if (verActual().getTipo() == Token.Tipo.DEL_PAREN_A) {
                    avanzar();
                    parsearArgumentos();
                    consumir(Token.Tipo.DEL_PAREN_C, "')'");
                } else if (verActual().getTipo() == Token.Tipo.OP_INCREMENTO
                        || verActual().getTipo() == Token.Tipo.OP_DECREMENTO) {
                    avanzar();
                }
                break;

            case DEL_PAREN_A:
                avanzar();
                parsearExpresion();
                consumir(Token.Tipo.DEL_PAREN_C, "')'  cerrando expresion agrupada");
                break;

            case OP_NOT:
            case OP_RESTA:
                avanzar();
                parsearTermino();
                break;

            case PR_NEW:
                avanzar();
                consumirIdentificador("nombre de clase en 'new'");
                consumir(Token.Tipo.DEL_PAREN_A, "'('  en instanciacion 'new'");
                parsearArgumentos();
                consumir(Token.Tipo.DEL_PAREN_C, "')'  en instanciacion 'new'");
                break;

            default:
                break;
        }
    }

    // ------------------------------------------------------------------ //
    // Utilidades de consumo
    // ------------------------------------------------------------------ //

    private Token verActual() {
        if (pos < tokens.size()) return tokens.get(pos);
        return new Token(Token.Tipo.EOF, "", -1);
    }

    private void avanzar() {
        if (pos < tokens.size()) pos++;
    }

    private void consumir(Token.Tipo esperado, String descripcion) {
        if (verActual().getTipo() == esperado) {
            avanzar();
        } else {
            errores.add(new ErrorSintactico(
                    "Se esperaba " + descripcion + " pero se encontro '"
                            + limpiar(verActual().getLexema()) + "'.",
                    verActual().getLinea()));
        }
    }

    private void consumirIdentificador(String contexto) {
        if (verActual().getTipo() == Token.Tipo.IDENTIFICADOR) {
            avanzar();
        } else {
            errores.add(new ErrorSintactico(
                    "Se esperaba un identificador (" + contexto + ") pero se encontro '"
                            + limpiar(verActual().getLexema()) + "'.",
                    verActual().getLinea()));
        }
    }

    // Avanza hasta el siguiente ';' o '}' para recuperarse de un error.
    private void recuperar() {
        while (!esEOF()
                && verActual().getTipo() != Token.Tipo.DEL_PUNTO_COMA
                && verActual().getTipo() != Token.Tipo.DEL_LLAVE_C) {
            avanzar();
        }
        if (verActual().getTipo() == Token.Tipo.DEL_PUNTO_COMA) {
            avanzar();
        }
    }

    private boolean esEOF() {
        return pos >= tokens.size()
                || tokens.get(pos).getTipo() == Token.Tipo.EOF;
    }

    private boolean esTipoDato(Token.Tipo tipo) {
        return tipo == Token.Tipo.PR_INT
                || tipo == Token.Tipo.PR_DEC
                || tipo == Token.Tipo.PR_STRING
                || tipo == Token.Tipo.PR_BOOL
                || tipo == Token.Tipo.PR_VOID;
    }

    private boolean esLiteral(Token.Tipo tipo) {
        return tipo == Token.Tipo.LIT_ENTERO
                || tipo == Token.Tipo.LIT_DECIMAL
                || tipo == Token.Tipo.LIT_CADENA
                || tipo == Token.Tipo.PR_TRUE
                || tipo == Token.Tipo.PR_FALSE;
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
            default:
                return false;
        }
    }

    private String limpiar(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
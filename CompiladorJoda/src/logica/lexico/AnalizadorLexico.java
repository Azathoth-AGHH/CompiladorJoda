package logica.lexico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*Analizador Lexico del compilador
Lee el codigo fuente caracter por carcter y produce una lista de tokens.
*/
public class AnalizadorLexico {

    private final String codigoFuente;
    private int posicion;
    private int lineaActual;
    private final List<Token> tokens;
    private final List<String> errores;

    // Tabla de palabras reservadas del lenguaje JODA
    private static final Map<String, Token.Tipo> PALABRAS_RESERVADAS = new HashMap<>();

    static {
        PALABRAS_RESERVADAS.put("entry",   Token.Tipo.T_ENTRY);
        PALABRAS_RESERVADAS.put("object",  Token.Tipo.T_OBJECT);
        PALABRAS_RESERVADAS.put("method",  Token.Tipo.T_METHOD);
        PALABRAS_RESERVADAS.put("define",  Token.Tipo.T_DEFINE);
        PALABRAS_RESERVADAS.put("int",     Token.Tipo.T_INT);
        PALABRAS_RESERVADAS.put("dec",     Token.Tipo.T_DEC);
        PALABRAS_RESERVADAS.put("string",  Token.Tipo.T_STRING);
        PALABRAS_RESERVADAS.put("bool",    Token.Tipo.T_BOOL);
        PALABRAS_RESERVADAS.put("void",    Token.Tipo.T_VOID);
        PALABRAS_RESERVADAS.put("if",      Token.Tipo.T_IF);
        PALABRAS_RESERVADAS.put("else",    Token.Tipo.T_ELSE);
        PALABRAS_RESERVADAS.put("select",  Token.Tipo.T_SELECT);
        PALABRAS_RESERVADAS.put("case",    Token.Tipo.T_CASE);
        PALABRAS_RESERVADAS.put("loop",    Token.Tipo.T_LOOP);
        PALABRAS_RESERVADAS.put("out",     Token.Tipo.T_OUT);
        PALABRAS_RESERVADAS.put("input",   Token.Tipo.T_INPUT);
        PALABRAS_RESERVADAS.put("true",    Token.Tipo.T_TRUE);
        PALABRAS_RESERVADAS.put("false",   Token.Tipo.T_FALSE);
        PALABRAS_RESERVADAS.put("new",     Token.Tipo.T_NEW);
        PALABRAS_RESERVADAS.put("return",  Token.Tipo.T_RETURN);
    }

    public AnalizadorLexico(String codigoFuente) {
        this.codigoFuente = codigoFuente;
        this.posicion = 0;
        this.lineaActual = 1;
        this.tokens = new ArrayList<>();
        this.errores = new ArrayList<>();
    }

    //Ejecuta el analisis lexico completo del codigo fuente.
    public List<Token> analizar() {
        tokens.clear();
        errores.clear();
        posicion = 0;
        lineaActual = 1;

        while (!esFin()) {
            saltarEspaciosYSaltosDeLinea();
            if (esFin()) break;

            char c = caracterActual();

            if (c == '/' && siguienteCaracter() == '/') {
                procesarComentarioLinea();
            } else if (esLetra(c)) {
                procesarPalabraOIdentificador();
            } else if (esDigito(c)) {
                procesarNumero();
            } else if (c == '"') {
                procesarCadena();
            } else {
                procesarSimbolo();
            }
        }

        tokens.add(new Token(Token.Tipo.T_FIN_ARCHIVO, "EOF", lineaActual));
        return tokens;
    }

    // Metodos de procesamiento de categorias lexicas
    private void procesarComentarioLinea() {
        int lineaInicio = lineaActual;
        StringBuilder sb = new StringBuilder();
        // Consumir los dos '//'
        avanzar(); avanzar();
        while (!esFin() && caracterActual() != '\n') {
            sb.append(caracterActual());
            avanzar();
        }
        tokens.add(new Token(Token.Tipo.T_COMENTARIO, sb.toString().trim(), lineaInicio));
    }

    private void procesarPalabraOIdentificador() {
        int lineaInicio = lineaActual;
        StringBuilder sb = new StringBuilder();

        while (!esFin() && (esLetra(caracterActual()) || esDigito(caracterActual()) || caracterActual() == '_')) {
            sb.append(caracterActual());
            avanzar();
        }

        String lexema = sb.toString();
        Token.Tipo tipo = PALABRAS_RESERVADAS.getOrDefault(lexema, Token.Tipo.T_IDENTIFICADOR);

        // Reclasificar true/false como literales booleanos
        if (tipo == Token.Tipo.T_TRUE || tipo == Token.Tipo.T_FALSE) {
            tokens.add(new Token(Token.Tipo.T_LITERAL_BOOL, lexema, lineaInicio));
        } else {
            tokens.add(new Token(tipo, lexema, lineaInicio));
        }
    }

    private void procesarNumero() {
        int lineaInicio = lineaActual;
        StringBuilder sb = new StringBuilder();
        boolean esDecimal = false;

        while (!esFin() && esDigito(caracterActual())) {
            sb.append(caracterActual());
            avanzar();
        }

        // Verificar si hay punto decimal
        if (!esFin() && caracterActual() == '.' && esDigito(peek(1))) {
            esDecimal = true;
            sb.append('.');
            avanzar(); // consumir '.'
            while (!esFin() && esDigito(caracterActual())) {
                sb.append(caracterActual());
                avanzar();
            }
        }

        Token.Tipo tipo = esDecimal ? Token.Tipo.T_LITERAL_DECIMAL : Token.Tipo.T_LITERAL_ENTERO;
        tokens.add(new Token(tipo, sb.toString(), lineaInicio));
    }

    private void procesarCadena() {
        int lineaInicio = lineaActual;
        StringBuilder sb = new StringBuilder();
        avanzar(); // consumir comilla inicial '"'

        while (!esFin() && caracterActual() != '"' && caracterActual() != '\n') {
            sb.append(caracterActual());
            avanzar();
        }

        if (esFin() || caracterActual() == '\n') {
            errores.add("Error lexico en linea " + lineaInicio + ": cadena de texto no cerrada.");
            tokens.add(new Token(Token.Tipo.T_DESCONOCIDO, sb.toString(), lineaInicio));
        } else {
            avanzar(); // consumir comilla final '"'
            tokens.add(new Token(Token.Tipo.T_LITERAL_CADENA, sb.toString(), lineaInicio));
        }
    }

    private void procesarSimbolo() {
        int lineaInicio = lineaActual;
        char c = caracterActual();
        avanzar();

        switch (c) {
            case ';': tokens.add(new Token(Token.Tipo.T_PUNTO_Y_COMA,    ";",  lineaInicio)); break;
            case '{': tokens.add(new Token(Token.Tipo.T_LLAVE_ABRE,      "{",  lineaInicio)); break;
            case '}': tokens.add(new Token(Token.Tipo.T_LLAVE_CIERRA,    "}",  lineaInicio)); break;
            case '(': tokens.add(new Token(Token.Tipo.T_PARENTESIS_ABRE, "(",  lineaInicio)); break;
            case ')': tokens.add(new Token(Token.Tipo.T_PARENTESIS_CIERRA,")", lineaInicio)); break;
            case '[': tokens.add(new Token(Token.Tipo.T_CORCHETE_ABRE,   "[",  lineaInicio)); break;
            case ']': tokens.add(new Token(Token.Tipo.T_CORCHETE_CIERRA, "]",  lineaInicio)); break;
            case '.': tokens.add(new Token(Token.Tipo.T_PUNTO,           ".",  lineaInicio)); break;
            case ',': tokens.add(new Token(Token.Tipo.T_COMA,            ",",  lineaInicio)); break;
            case '+': tokens.add(new Token(Token.Tipo.T_SUMA,            "+",  lineaInicio)); break;
            case '-': tokens.add(new Token(Token.Tipo.T_RESTA,           "-",  lineaInicio)); break;
            case '*': tokens.add(new Token(Token.Tipo.T_MULTIPLICACION,  "*",  lineaInicio)); break;
            case '/': tokens.add(new Token(Token.Tipo.T_DIVISION,        "/",  lineaInicio)); break;
            case '%': tokens.add(new Token(Token.Tipo.T_MODULO,          "%",  lineaInicio)); break;

            case '=':
                if (!esFin() && caracterActual() == '=') {
                    avanzar();
                    tokens.add(new Token(Token.Tipo.T_IGUAL_IGUAL, "==", lineaInicio));
                } else {
                    tokens.add(new Token(Token.Tipo.T_ASIGNACION, "=", lineaInicio));
                }
                break;

            case '!':
                if (!esFin() && caracterActual() == '=') {
                    avanzar();
                    tokens.add(new Token(Token.Tipo.T_DIFERENTE, "!=", lineaInicio));
                } else {
                    tokens.add(new Token(Token.Tipo.T_NOT, "!", lineaInicio));
                }
                break;

            case '>':
                if (!esFin() && caracterActual() == '=') {
                    avanzar();
                    tokens.add(new Token(Token.Tipo.T_MAYOR_IGUAL, ">=", lineaInicio));
                } else {
                    tokens.add(new Token(Token.Tipo.T_MAYOR, ">", lineaInicio));
                }
                break;

            case '<':
                if (!esFin() && caracterActual() == '=') {
                    avanzar();
                    tokens.add(new Token(Token.Tipo.T_MENOR_IGUAL, "<=", lineaInicio));
                } else {
                    tokens.add(new Token(Token.Tipo.T_MENOR, "<", lineaInicio));
                }
                break;

            case '&':
                if (!esFin() && caracterActual() == '&') {
                    avanzar();
                    tokens.add(new Token(Token.Tipo.T_AND, "&&", lineaInicio));
                } else {
                    errores.add("Error lexico en linea " + lineaInicio + ": se esperaba '&&'.");
                    tokens.add(new Token(Token.Tipo.T_DESCONOCIDO, "&", lineaInicio));
                }
                break;

            case '|':
                if (!esFin() && caracterActual() == '|') {
                    avanzar();
                    tokens.add(new Token(Token.Tipo.T_OR, "||", lineaInicio));
                } else {
                    errores.add("Error lexico en linea " + lineaInicio + ": se esperaba '||'.");
                    tokens.add(new Token(Token.Tipo.T_DESCONOCIDO, "|", lineaInicio));
                }
                break;

            default:
                errores.add("Error lexico en linea " + lineaInicio + ": caracter desconocido '" + c + "'.");
                tokens.add(new Token(Token.Tipo.T_DESCONOCIDO, String.valueOf(c), lineaInicio));
                break;
        }
    }

    // Metodos auxiliares de navegacion
    private void saltarEspaciosYSaltosDeLinea() {
        while (!esFin()) {
            char c = caracterActual();
            if (c == '\n') {
                lineaActual++;
                avanzar();
            } else if (c == ' ' || c == '\t' || c == '\r') {
                avanzar();
            } else {
                break;
            }
        }
    }

    private char caracterActual() {
        return codigoFuente.charAt(posicion);
    }

    private char siguienteCaracter() {
        if (posicion + 1 < codigoFuente.length()) return codigoFuente.charAt(posicion + 1);
        return '\0';
    }

    private char peek(int offset) {
        int idx = posicion + offset;
        if (idx < codigoFuente.length()) return codigoFuente.charAt(idx);
        return '\0';
    }

    private void avanzar() {
        posicion++;
    }

    private boolean esFin() {
        return posicion >= codigoFuente.length();
    }

    private boolean esLetra(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean esDigito(char c) {
        return Character.isDigit(c);
    }

    public List<String> getErrores() {
        return errores;
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }
}
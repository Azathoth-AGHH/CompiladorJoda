package logica.lexico;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AnalizadorLexico {

    private static final Map<String, Token.Tipo> PALABRAS_RESERVADAS = new java.util.HashMap<>(32);

    static {
        PALABRAS_RESERVADAS.put("entry",  Token.Tipo.T_ENTRY);
        PALABRAS_RESERVADAS.put("object", Token.Tipo.T_OBJECT);
        PALABRAS_RESERVADAS.put("method", Token.Tipo.T_METHOD);
        PALABRAS_RESERVADAS.put("define", Token.Tipo.T_DEFINE);
        PALABRAS_RESERVADAS.put("int",    Token.Tipo.T_INT);
        PALABRAS_RESERVADAS.put("dec",    Token.Tipo.T_DEC);
        PALABRAS_RESERVADAS.put("string", Token.Tipo.T_STRING);
        PALABRAS_RESERVADAS.put("bool",   Token.Tipo.T_BOOL);
        PALABRAS_RESERVADAS.put("void",   Token.Tipo.T_VOID);
        PALABRAS_RESERVADAS.put("if",     Token.Tipo.T_IF);
        PALABRAS_RESERVADAS.put("else",   Token.Tipo.T_ELSE);
        PALABRAS_RESERVADAS.put("select", Token.Tipo.T_SELECT);
        PALABRAS_RESERVADAS.put("case",   Token.Tipo.T_CASE);
        PALABRAS_RESERVADAS.put("loop",   Token.Tipo.T_LOOP);
        PALABRAS_RESERVADAS.put("out",    Token.Tipo.T_OUT);
        PALABRAS_RESERVADAS.put("input",  Token.Tipo.T_INPUT);
        PALABRAS_RESERVADAS.put("true",   Token.Tipo.T_TRUE);
        PALABRAS_RESERVADAS.put("false",  Token.Tipo.T_FALSE);
        PALABRAS_RESERVADAS.put("new",    Token.Tipo.T_NEW);
        PALABRAS_RESERVADAS.put("return", Token.Tipo.T_RETURN);
    }

    private final String       fuente;
    private final List<Token>  tokens  = new ArrayList<>(128);
    private final List<String> errores = new ArrayList<>();
    private int pos  = 0;
    private int linea = 1;

    public AnalizadorLexico(String fuente) {
        this.fuente = fuente;
    }

    public List<Token> analizar() {
        tokens.clear();
        errores.clear();
        pos   = 0;
        linea = 1;

        while (!fin()) {
            saltarBlancos();
            if (fin()) break;

            char c = actual();
            if      (c == '/' && sig() == '/')        procesarComentario();
            else if (Character.isLetter(c) || c == '_') procesarPalabraOId();
            else if (Character.isDigit(c))              procesarNumero();
            else if (c == '"')                          procesarCadena();
            else                                        procesarSimbolo();
        }

        tokens.add(new Token(Token.Tipo.T_FIN_ARCHIVO, "EOF", linea));
        return tokens;
    }

    private void procesarComentario() {
        int l = linea;
        avanzar(); avanzar();
        int ini = pos;
        while (!fin() && actual() != '\n') avanzar();
        tokens.add(new Token(Token.Tipo.T_COMENTARIO, fuente.substring(ini, pos).trim(), l));
    }

    private void procesarPalabraOId() {
        int l = linea, ini = pos;
        while (!fin() && (Character.isLetterOrDigit(actual()) || actual() == '_')) avanzar();
        String lex  = fuente.substring(ini, pos);
        Token.Tipo t = PALABRAS_RESERVADAS.getOrDefault(lex, Token.Tipo.T_IDENTIFICADOR);
        if (t == Token.Tipo.T_TRUE || t == Token.Tipo.T_FALSE)
            tokens.add(new Token(Token.Tipo.T_LITERAL_BOOL, lex, l));
        else
            tokens.add(new Token(t, lex, l));
    }

    private void procesarNumero() {
        int l = linea, ini = pos;
        while (!fin() && Character.isDigit(actual())) avanzar();
        boolean decimal = !fin() && actual() == '.' && Character.isDigit(peek(1));
        if (decimal) {
            avanzar();
            while (!fin() && Character.isDigit(actual())) avanzar();
        }
        tokens.add(new Token(
            decimal ? Token.Tipo.T_LITERAL_DECIMAL : Token.Tipo.T_LITERAL_ENTERO,
            fuente.substring(ini, pos), l));
    }

    private void procesarCadena() {
        int l = linea;
        avanzar();
        int ini = pos;
        while (!fin() && actual() != '"' && actual() != '\n') avanzar();
        if (fin() || actual() == '\n') {
            errores.add("Error lexico en linea " + l + ": cadena no cerrada.");
            tokens.add(new Token(Token.Tipo.T_DESCONOCIDO, fuente.substring(ini, pos), l));
        } else {
            tokens.add(new Token(Token.Tipo.T_LITERAL_CADENA, fuente.substring(ini, pos), l));
            avanzar();
        }
    }

    private void procesarSimbolo() {
        int  l = linea;
        char c = actual();
        avanzar();
        switch (c) {
            case ';': emit(Token.Tipo.T_PUNTO_Y_COMA,      ";",  l); break;
            case ':': emit(Token.Tipo.T_DOS_PUNTOS,        ":",  l); break;
            case '{': emit(Token.Tipo.T_LLAVE_ABRE,        "{",  l); break;
            case '}': emit(Token.Tipo.T_LLAVE_CIERRA,      "}",  l); break;
            case '(': emit(Token.Tipo.T_PARENTESIS_ABRE,   "(",  l); break;
            case ')': emit(Token.Tipo.T_PARENTESIS_CIERRA, ")",  l); break;
            case '[': emit(Token.Tipo.T_CORCHETE_ABRE,     "[",  l); break;
            case ']': emit(Token.Tipo.T_CORCHETE_CIERRA,   "]",  l); break;
            case '.': emit(Token.Tipo.T_PUNTO,             ".",  l); break;
            case ',': emit(Token.Tipo.T_COMA,              ",",  l); break;
            case '*': emit(Token.Tipo.T_MULTIPLICACION,    "*",  l); break;
            case '/': emit(Token.Tipo.T_DIVISION,          "/",  l); break;
            case '%': emit(Token.Tipo.T_MODULO,            "%",  l); break;
            case '+': emitDoble('+', Token.Tipo.T_INCREMENTO, "++", Token.Tipo.T_SUMA, "+", l); break;
            case '-': emitDoble('-', Token.Tipo.T_DECREMENTO, "--", Token.Tipo.T_RESTA, "-", l); break;
            case '=': emitDoble('=', Token.Tipo.T_IGUAL_IGUAL, "==", Token.Tipo.T_ASIGNACION, "=", l); break;
            case '!': emitDoble('=', Token.Tipo.T_DIFERENTE, "!=", Token.Tipo.T_NOT, "!", l); break;
            case '>': emitDoble('=', Token.Tipo.T_MAYOR_IGUAL, ">=", Token.Tipo.T_MAYOR, ">", l); break;
            case '<': emitDoble('=', Token.Tipo.T_MENOR_IGUAL, "<=", Token.Tipo.T_MENOR, "<", l); break;
            case '&': emitDual('&', Token.Tipo.T_AND, "&&", l, "se esperaba '&&'"); break;
            case '|': emitDual('|', Token.Tipo.T_OR,  "||", l, "se esperaba '||'"); break;
            default:
                errores.add("Error lexico en linea " + l + ": caracter desconocido '" + c + "'.");
                tokens.add(new Token(Token.Tipo.T_DESCONOCIDO, String.valueOf(c), l));
        }
    }

    private void emit(Token.Tipo tipo, String lex, int l) {
        tokens.add(new Token(tipo, lex, l));
    }

    private void emitDoble(char siguiente, Token.Tipo doble, String lexDoble,
                            Token.Tipo simple, String lexSimple, int l) {
        if (!fin() && actual() == siguiente) { avanzar(); emit(doble, lexDoble, l); }
        else                                               emit(simple, lexSimple, l);
    }

    private void emitDual(char esperado, Token.Tipo tipo, String lex, int l, String msg) {
        if (!fin() && actual() == esperado) { avanzar(); emit(tipo, lex, l); }
        else {
            errores.add("Error lexico en linea " + l + ": " + msg + ".");
            tokens.add(new Token(Token.Tipo.T_DESCONOCIDO, String.valueOf((char)(esperado == '&' ? '&' : '|')), l));
        }
    }

    private void saltarBlancos() {
        while (!fin()) {
            char c = actual();
            if      (c == '\n')                       { linea++; avanzar(); }
            else if (c == ' ' || c == '\t' || c == '\r') avanzar();
            else break;
        }
    }

    private char   actual()      { return fuente.charAt(pos); }
    private char   sig()         { return pos + 1 < fuente.length() ? fuente.charAt(pos + 1) : '\0'; }
    private char   peek(int off) { int i = pos + off; return i < fuente.length() ? fuente.charAt(i) : '\0'; }
    private void   avanzar()     { pos++; }
    private boolean fin()        { return pos >= fuente.length(); }

    public List<String> getErrores()   { return errores; }
    public boolean      tieneErrores() { return !errores.isEmpty(); }
}
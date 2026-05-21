package logica.lexico;

public final class Token {

    public enum Tipo {
        T_ENTRY, T_OBJECT, T_METHOD,
        T_DEFINE, T_INT, T_DEC, T_STRING, T_BOOL, T_VOID,
        T_IF, T_ELSE, T_SELECT, T_CASE, T_LOOP,
        T_OUT, T_INPUT,
        T_LITERAL_ENTERO, T_LITERAL_DECIMAL, T_LITERAL_CADENA, T_LITERAL_BOOL,
        T_IDENTIFICADOR,
        T_SUMA, T_RESTA, T_MULTIPLICACION, T_DIVISION, T_MODULO,
        T_IGUAL_IGUAL, T_DIFERENTE, T_MAYOR, T_MENOR, T_MAYOR_IGUAL, T_MENOR_IGUAL,
        T_AND, T_OR, T_NOT,
        T_ASIGNACION, T_INCREMENTO, T_DECREMENTO,
        T_PUNTO_Y_COMA, T_DOS_PUNTOS,
        T_LLAVE_ABRE, T_LLAVE_CIERRA,
        T_PARENTESIS_ABRE, T_PARENTESIS_CIERRA,
        T_CORCHETE_ABRE, T_CORCHETE_CIERRA,
        T_PUNTO, T_COMA,
        T_NEW, T_RETURN, T_TRUE, T_FALSE,
        T_COMENTARIO, T_FIN_ARCHIVO, T_DESCONOCIDO
    }

    private final Tipo   tipo;
    private final String lexema;
    private final int    linea;

    public Token(Tipo tipo, String lexema, int linea) {
        this.tipo   = tipo;
        this.lexema = lexema;
        this.linea  = linea;
    }

    public Tipo   getTipo()   { return tipo;   }
    public String getLexema() { return lexema; }
    public int    getLinea()  { return linea;  }

    @Override
    public String toString() {
        return "Token{tipo=" + tipo + ", lexema='" + lexema + "', linea=" + linea + "}";
    }
}
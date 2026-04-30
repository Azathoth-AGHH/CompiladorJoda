package logica.lexico;

/*
Representa un token reconocido por el analizador lexico.
Contiene el tipo (categoria), el lexema y la linea de origen.
*/
public class Token {

    public enum Tipo {
        // Palabras reservadas - estructura
        PR_ENTRY, PR_OBJECT, PR_METHOD,

        // Palabras reservadas - definicion de tipo
        PR_DEFINE, PR_INT, PR_DEC, PR_STRING, PR_BOOL, PR_VOID,

        // Palabras reservadas - control
        PR_IF, PR_ELSE, PR_SELECT, PR_CASE, PR_LOOP,

        // Palabras reservadas - entrada/salida
        PR_OUT, PR_INPUT,

        // Palabras reservadas - valores logicos
        PR_TRUE, PR_FALSE,

        // Palabras reservadas - OOP
        PR_NEW, PR_RETURN,

        // Literales
        LIT_ENTERO,
        LIT_DECIMAL,
        LIT_CADENA,

        // Identificadores
        IDENTIFICADOR,

        // Operadores aritmeticos
        OP_SUMA,
        OP_RESTA,
        OP_MULTIPLICACION,
        OP_DIVISION,
        OP_MODULO,

        // Operadores relacionales
        OP_IGUAL,
        OP_DIFERENTE,
        OP_MAYOR,
        OP_MENOR,
        OP_MAYOR_IGUAL,
        OP_MENOR_IGUAL,

        // Operadores logicos
        OP_AND,
        OP_OR,
        OP_NOT,

        // Operadores de asignacion e incremento
        OP_ASIGNACION,
        OP_INCREMENTO,
        OP_DECREMENTO,

        // Delimitadores y agrupadores
        DEL_PUNTO_COMA,
        DEL_LLAVE_A,
        DEL_LLAVE_C,
        DEL_PAREN_A,
        DEL_PAREN_C,
        DEL_CORCHETE_A,
        DEL_CORCHETE_C,
        DEL_COMA,
        DEL_PUNTO,
        
        // Especiales
        COMENTARIO,
        ERROR,
        EOF
    }

    private final Tipo tipo;
    private final String lexema;
    private final int linea;

    public Token(Tipo tipo, String lexema, int linea) {
        this.tipo   = tipo;
        this.lexema = lexema;
        this.linea  = linea;
    }

    public Tipo getTipo(){
        return tipo;
    }
    public String getLexema(){
        return lexema;
    }
    public int getLinea(){
        return linea;
    }

    @Override
    public String toString() {
        return "Token[" + tipo + ", '" + lexema + "', linea=" + linea + "]";
    }
}
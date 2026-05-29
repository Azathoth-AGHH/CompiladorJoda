package logica.documentador;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import logica.lexico.Token;

public final class Documentador implements IDocumentador {

    private static final Map<Token.Tipo, String> DESCRIPCIONES = new EnumMap<>(Token.Tipo.class);

    static {
        DESCRIPCIONES.put(Token.Tipo.T_ENTRY,           "Palabra reservada 'entry': marca el inicio del bloque principal de ejecucion.");
        DESCRIPCIONES.put(Token.Tipo.T_OBJECT,          "Palabra reservada 'object': define una clase en el paradigma orientado a objetos.");
        DESCRIPCIONES.put(Token.Tipo.T_METHOD,          "Palabra reservada 'method': declara una funcion o comportamiento.");
        DESCRIPCIONES.put(Token.Tipo.T_DEFINE,          "Instruccion 'define': reserva memoria para una nueva variable (tipado explicito obligatorio).");
        DESCRIPCIONES.put(Token.Tipo.T_INT,             "Tipo 'int': entero de 32 bits con signo.");
        DESCRIPCIONES.put(Token.Tipo.T_DEC,             "Tipo 'dec': decimal de 64 bits (doble precision IEEE 754).");
        DESCRIPCIONES.put(Token.Tipo.T_STRING,          "Tipo 'string': cadena UTF-8 delimitada por comillas dobles.");
        DESCRIPCIONES.put(Token.Tipo.T_BOOL,            "Tipo 'bool': valor logico binario ('true' o 'false').");
        DESCRIPCIONES.put(Token.Tipo.T_VOID,            "Tipo 'void': indica que un metodo no retorna valor.");
        DESCRIPCIONES.put(Token.Tipo.T_IF,              "Control 'if': inicio de estructura condicional.");
        DESCRIPCIONES.put(Token.Tipo.T_ELSE,            "Control 'else': bloque alternativo al 'if'.");
        DESCRIPCIONES.put(Token.Tipo.T_LOOP,            "Control 'loop': iteracion mientras la condicion sea verdadera.");
        DESCRIPCIONES.put(Token.Tipo.T_SELECT,          "Control 'select': seleccion multiple basada en casos.");
        DESCRIPCIONES.put(Token.Tipo.T_CASE,            "Control 'case': define un caso dentro de 'select'.");
        DESCRIPCIONES.put(Token.Tipo.T_OUT,             "E/S 'out': imprime datos en la consola estandar.");
        DESCRIPCIONES.put(Token.Tipo.T_INPUT,           "E/S 'input': captura valor ingresado por el usuario.");
        DESCRIPCIONES.put(Token.Tipo.T_TRUE,            "Literal booleano 'true': estado verdadero (1).");
        DESCRIPCIONES.put(Token.Tipo.T_FALSE,           "Literal booleano 'false': estado falso (0).");
        DESCRIPCIONES.put(Token.Tipo.T_SUMA,            "Operador '+': suma aritmetica o concatenacion de cadenas.");
        DESCRIPCIONES.put(Token.Tipo.T_RESTA,           "Operador '-': resta aritmetica.");
        DESCRIPCIONES.put(Token.Tipo.T_MULTIPLICACION,  "Operador '*': multiplicacion aritmetica.");
        DESCRIPCIONES.put(Token.Tipo.T_DIVISION,        "Operador '/': division aritmetica (resultado 'dec').");
        DESCRIPCIONES.put(Token.Tipo.T_MODULO,          "Operador '%': residuo de division entera.");
        DESCRIPCIONES.put(Token.Tipo.T_IGUAL_IGUAL,     "Operador '==': igualdad relacional.");
        DESCRIPCIONES.put(Token.Tipo.T_DIFERENTE,       "Operador '!=': desigualdad relacional.");
        DESCRIPCIONES.put(Token.Tipo.T_MAYOR,           "Operador '>': comparacion mayor que.");
        DESCRIPCIONES.put(Token.Tipo.T_MENOR,           "Operador '<': comparacion menor que.");
        DESCRIPCIONES.put(Token.Tipo.T_MAYOR_IGUAL,     "Operador '>=': mayor o igual que.");
        DESCRIPCIONES.put(Token.Tipo.T_MENOR_IGUAL,     "Operador '<=': menor o igual que.");
        DESCRIPCIONES.put(Token.Tipo.T_AND,             "Operador '&&': conjuncion logica AND.");
        DESCRIPCIONES.put(Token.Tipo.T_OR,              "Operador '||': disyuncion logica OR.");
        DESCRIPCIONES.put(Token.Tipo.T_NOT,             "Operador '!': negacion logica NOT.");
        DESCRIPCIONES.put(Token.Tipo.T_ASIGNACION,      "Operador '=': almacena el valor derecho en la variable izquierda.");
        DESCRIPCIONES.put(Token.Tipo.T_INCREMENTO,      "Operador '++': incremento postfijo en 1.");
        DESCRIPCIONES.put(Token.Tipo.T_DECREMENTO,      "Operador '--': decremento postfijo en 1.");
        DESCRIPCIONES.put(Token.Tipo.T_PUNTO_Y_COMA,   "Delimitador ';': fin obligatorio de sentencia.");
        DESCRIPCIONES.put(Token.Tipo.T_DOS_PUNTOS,      "Delimitador ':': separador en 'case valor:'.");
        DESCRIPCIONES.put(Token.Tipo.T_LLAVE_ABRE,      "Delimitador '{': apertura de bloque, nuevo ambito de memoria.");
        DESCRIPCIONES.put(Token.Tipo.T_LLAVE_CIERRA,    "Delimitador '}': cierre de bloque, liberacion de ambito.");
        DESCRIPCIONES.put(Token.Tipo.T_PARENTESIS_ABRE,  "Delimitador '(': agrupacion de expresion o parametros.");
        DESCRIPCIONES.put(Token.Tipo.T_PARENTESIS_CIERRA,"Delimitador ')': cierre de agrupacion.");
        DESCRIPCIONES.put(Token.Tipo.T_CORCHETE_ABRE,   "Delimitador '[': inicio de arreglo indexado.");
        DESCRIPCIONES.put(Token.Tipo.T_CORCHETE_CIERRA,  "Delimitador ']': fin de arreglo indexado.");
        DESCRIPCIONES.put(Token.Tipo.T_PUNTO,           "Operador '.': acceso a miembros, metodos o librerias.");
        DESCRIPCIONES.put(Token.Tipo.T_COMA,            "Delimitador ',': separador en listas de parametros.");
        DESCRIPCIONES.put(Token.Tipo.T_NEW,             "Operador 'new': instancia un objeto en el Heap JVM-J.");
        DESCRIPCIONES.put(Token.Tipo.T_RETURN,          "Control 'return': retorna valor al punto de llamada.");
    }

    @Override
    public String documentar(List<Token> tokens, String codigoFuente) {
        StringBuilder sb = new StringBuilder(tokens.size() * 80);
        sb.append("=== DOCUMENTACION TECNICA - COMPILADOR JODA ===\n");
        sb.append("Narrativa descriptiva del flujo de tokens identificados:\n\n");

        int lineaAnterior = -1;
        for (Token token : tokens) {
            if (token.getTipo() == Token.Tipo.T_FIN_ARCHIVO) break;
            if (token.getLinea() != lineaAnterior) {
                sb.append("\n--- Linea ").append(token.getLinea()).append(" ---\n");
                lineaAnterior = token.getLinea();
            }
            sb.append("  [").append(padRight(token.getTipo().name(), 28)).append("] ");
            sb.append("Lexema: '").append(padRight(token.getLexema(), 20)).append("' -> ");
            sb.append(describirToken(token)).append("\n");
        }

        sb.append("\n=== FIN DE DOCUMENTACION ===\n");
        return sb.toString();
    }

    private String describirToken(Token token) {
        String base = DESCRIPCIONES.get(token.getTipo());
        if (base != null) return base;

        return switch (token.getTipo()) {
            case T_LITERAL_ENTERO  -> "Literal entero: " + token.getLexema() + ".";
            case T_LITERAL_DECIMAL -> "Literal decimal: " + token.getLexema() + ".";
            case T_LITERAL_CADENA  -> "Cadena de texto: \"" + token.getLexema() + "\".";
            case T_LITERAL_BOOL    -> "Literal booleano: " + token.getLexema() + ".";
            case T_IDENTIFICADOR   -> "Identificador: '" + token.getLexema() + "'.";
            case T_COMENTARIO      -> "Comentario de linea: \"" + token.getLexema() + "\".";
            case T_DESCONOCIDO     -> "ADVERTENCIA: token no reconocido '" + token.getLexema() + "'.";
            default                -> "Token " + token.getTipo() + " con lexema '" + token.getLexema() + "'.";
        };
    }

    private static String padRight(String texto, int longitud) {
        if (texto.length() >= longitud) return texto.substring(0, longitud);
        return texto + " ".repeat(longitud - texto.length());
    }
}
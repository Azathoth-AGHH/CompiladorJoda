package logica.documentador;

import java.util.List;

import logica.lexico.Token;

/*Documentador del codigo de JODA
Recorre la lista de tokens producida por el analizador lexico
y a su vez genera una narrativa de cada elemento del codigo */
public class Documentador {
    /*Genera la narrativa a partir de los tokens */
    public String documentar(List<Token> tokens, String codigoFuente) {
        StringBuilder doc = new StringBuilder();
        doc.append("=== DOCUMENTACION GENERADA POR EL COMPILADOR JODA ===\n");
        doc.append("Narrativa descriptiva del flujo de tokens identificados:\n\n");

        int lineaAnterior = -1;

        for (Token token : tokens) {
            if (token.getTipo() == Token.Tipo.T_FIN_ARCHIVO) break;

            if (token.getLinea() != lineaAnterior) {
                doc.append("\n--- Linea ").append(token.getLinea()).append(" ---\n");
                lineaAnterior = token.getLinea();
            }

            String descripcion = describirToken(token);
            doc.append("  [").append(padRight(token.getTipo().name(), 28)).append("] ");
            doc.append("Lexema: '").append(padRight(token.getLexema(), 20)).append("' -> ");
            doc.append(descripcion).append("\n");
        }

        doc.append("\n=== FIN DE DOCUMENTACION ===\n");
        return doc.toString();
    }

    /*
    Retorna una descripcion narrativa segun el tipo de token.
    Se incluyen detalles sobre su funcion en el lenguaje JODA, su sintaxis y su semantica.
     */
    private String describirToken(Token token) {
        switch (token.getTipo()) {

            // Palabras reservadas de estructura
            case T_ENTRY:
                return "Se identifica la palabra reservada 'entry': marca el inicio del bloque principal de ejecucion del programa.";
            case T_OBJECT:
                return "Se identifica 'object': define una nueva clase o estructura para el paradigma orientado a objetos.";
            case T_METHOD:
                return "Se identifica 'method': declara una funcion o comportamiento dentro de un objeto o de forma global.";

            // Palabras reservadas de definicion de variables
            case T_DEFINE:
                return "Se detecta la instruccion 'define': reserva un espacio en memoria para una nueva variable (tipado explicito obligatorio).";
            case T_INT:
                return "Se identifica el tipo de dato 'int': entero de 32 bits con signo (rango: -2,147,483,648 a 2,147,483,647).";
            case T_DEC:
                return "Se identifica el tipo de dato 'dec': decimal de alta fidelidad de 64 bits (doble precision IEEE 754).";
            case T_STRING:
                return "Se identifica el tipo de dato 'string': cadena de caracteres en formato UTF-8, delimitada por comillas dobles.";
            case T_BOOL:
                return "Se identifica el tipo de dato 'bool': valor logico binario, acepta unicamente 'true' o 'false'.";
            case T_VOID:
                return "Se identifica 'void': indica que un metodo no retorna ningun valor.";

            // Control de flujo
            case T_IF:
                return "Se detecta la instruccion 'if': inicio de una estructura condicional para toma de decisiones.";
            case T_ELSE:
                return "Se detecta 'else': bloque alternativo que se ejecuta cuando la condicion del 'if' es falsa.";
            case T_LOOP:
                return "Se detecta 'loop': estructura de repeticion que itera mientras la condicion sea verdadera.";
            case T_SELECT:
                return "Se detecta 'select': estructura de seleccion multiple basada en casos (similar a switch).";
            case T_CASE:
                return "Se detecta 'case': define un caso especifico dentro de una estructura 'select'.";

            // Entrada/salida
            case T_OUT:
                return "Se detecta 'out': comando de salida estandar para imprimir datos en la consola.";
            case T_INPUT:
                return "Se detecta 'input': captura informacion ingresada por el usuario desde el teclado.";

            // Literales
            case T_LITERAL_ENTERO:
                return "Se reconoce un literal entero de valor '" + token.getLexema() + "': constante numerica sin parte decimal.";
            case T_LITERAL_DECIMAL:
                return "Se reconoce un literal decimal de valor '" + token.getLexema() + "': constante numerica con punto decimal (alta fidelidad).";
            case T_LITERAL_CADENA:
                return "Se reconoce una cadena de texto: \"" + token.getLexema() + "\". Se almacena como secuencia de caracteres UTF-8.";
            case T_LITERAL_BOOL:
                return "Se reconoce el literal booleano '" + token.getLexema() + "': valor logico que representa verdadero o falso.";
            case T_TRUE:
                return "Literal booleano 'true': representa el estado verdadero (1) en una variable de tipo bool.";
            case T_FALSE:
                return "Literal booleano 'false': representa el estado falso (0) en una variable de tipo bool.";

            // Identificadores
            case T_IDENTIFICADOR:
                return "Se identifica el nombre de variable o identificador: '" + token.getLexema()
                    + "'. Cumple la regla: inicia en minuscula, seguido de letras, digitos o '_'.";

            // Operadores aritmeticos
            case T_SUMA:
                return "Se detecta el signo '+': operador de suma aritmetica o concatenacion de cadenas (Coercion Inteligente).";
            case T_RESTA:
                return "Se detecta el signo '-': operador de resta aritmetica entre valores numericos.";
            case T_MULTIPLICACION:
                return "Se detecta el signo '*': operador de multiplicacion aritmetica.";
            case T_DIVISION:
                return "Se detecta el signo '/': operador de division aritmetica. El resultado es de tipo 'dec'.";
            case T_MODULO:
                return "Se detecta el signo '%': operador modulo, retorna el residuo de una division entera.";

            // Operadores relacionales
            case T_IGUAL_IGUAL:
                return "Se detecta '==': operador relacional de igualdad. Compara si dos valores son identicos.";
            case T_DIFERENTE:
                return "Se detecta '!=': operador relacional de desigualdad. Retorna 'true' si los valores son distintos.";
            case T_MAYOR:
                return "Se detecta '>': operador relacional 'mayor que'. Retorna 'true' si el operando izquierdo es mayor.";
            case T_MENOR:
                return "Se detecta '<': operador relacional 'menor que'. Retorna 'true' si el operando izquierdo es menor.";
            case T_MAYOR_IGUAL:
                return "Se detecta '>=': operador relacional 'mayor o igual que'.";
            case T_MENOR_IGUAL:
                return "Se detecta '<=': operador relacional 'menor o igual que'.";

            // Operadores logicos
            case T_AND:
                return "Se detecta '&&': operador logico AND (conjuncion). Verdadero solo si ambas condiciones son verdaderas.";
            case T_OR:
                return "Se detecta '||': operador logico OR (disyuncion). Verdadero si al menos una condicion es verdadera.";
            case T_NOT:
                return "Se detecta '!': operador logico NOT (negacion). Invierte el valor booleano del operando.";

            // Operadores de asignacion
            case T_ASIGNACION:
                return "Se detecta el signo '=': operador de asignacion que almacena el valor de la expresion derecha en la variable izquierda.";
            case T_INCREMENTO:
                return "Se detecta '++': operador de incremento postfijo. Aumenta en 1 el valor de la variable numerica.";
            case T_DECREMENTO:
                return "Se detecta '--': operador de decremento postfijo. Reduce en 1 el valor de la variable numerica.";

            // Delimitadores
            case T_PUNTO_Y_COMA:
                return "Se detecta ';': delimitador obligatorio de fin de sentencia (Sintaxis de Precision de JODA).";
            case T_LLAVE_ABRE:
                return "Se detecta '{': apertura de un bloque de codigo. Define un nuevo ambito de memoria en la JVM-J.";
            case T_LLAVE_CIERRA:
                return "Se detecta '}': cierre del bloque de codigo. El ambito de memoria definido por '{' queda liberado.";
            case T_PARENTESIS_ABRE:
                return "Se detecta '(': apertura de agrupacion de expresion o lista de parametros.";
            case T_PARENTESIS_CIERRA:
                return "Se detecta ')': cierre de la agrupacion de expresion o lista de parametros.";
            case T_CORCHETE_ABRE:
                return "Se detecta '[': inicio de definicion o acceso a un arreglo (array) indexado.";
            case T_CORCHETE_CIERRA:
                return "Se detecta ']': fin de la definicion o acceso al arreglo.";
            case T_PUNTO:
                return "Se detecta '.': operador de acceso a miembros, metodos o librerias (ej: Scientific.sqrt).";
            case T_COMA:
                return "Se detecta ',': separador de elementos en listas de parametros o inicializadores de arreglos.";

            // Especiales
            case T_NEW:
                return "Se detecta 'new': crea una nueva instancia de un objeto en el Heap de la JVM-J.";
            case T_RETURN:
                return "Se detecta 'return': retorna un valor desde un metodo al punto de llamada.";
            case T_COMENTARIO:
                return "Se detecta un comentario de linea (//) con contenido: \"" + token.getLexema()
                    + "\". El compilador lo omite del analisis pero lo documenta.";

            case T_DESCONOCIDO:
                return "ADVERTENCIA: token no reconocido '" + token.getLexema()
                    + "'. No pertenece al conjunto de simbolos validos de JODA.";

            default:
                return "Token de tipo " + token.getTipo() + " con lexema '" + token.getLexema() + "'.";
        }
    }

    // Rellena una cadena con espacios hasta la longitud indicada
    private String padRight(String texto, int longitud) {
        if (texto.length() >= longitud) return texto.substring(0, longitud);
        StringBuilder sb = new StringBuilder(texto);
        while (sb.length() < longitud) sb.append(' ');
        return sb.toString();
    }
}
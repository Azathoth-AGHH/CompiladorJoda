package logica.documentador;

import logica.lexico.Token;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera la tabla de documentacion de tokens.
 * Clasifica cada token en su categoria semantica para el reporte.
 */
public class Documentador {

    /** Fila de la tabla de documentacion. */
    public static class FilaDoc {
        private final int    linea;
        private final String lexema;
        private final String categoria;
        private final String detalle;

        public FilaDoc(int linea, String lexema, String categoria, String detalle) {
            this.linea     = linea;
            this.lexema    = lexema;
            this.categoria = categoria;
            this.detalle   = detalle;
        }

        public int    getLinea()     { return linea;     }
        public String getLexema()    { return lexema;    }
        public String getCategoria() { return categoria; }
        public String getDetalle()   { return detalle;   }
    }

    /**
     * Procesa la lista de tokens y retorna las filas de documentacion.
     * Omite comentarios, espacios y EOF.
     */
    public List<FilaDoc> documentar(List<Token> tokens) {
        List<FilaDoc> tabla = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getTipo() == Token.Tipo.COMENTARIO
                    || t.getTipo() == Token.Tipo.EOF) {
                continue;
            }
            tabla.add(new FilaDoc(
                    t.getLinea(),
                    limpiar(t.getLexema()),
                    resolverCategoria(t.getTipo()),
                    resolverDetalle(t.getTipo())
            ));
        }
        return tabla;
    }

    // ------------------------------------------------------------------ //
    //  Metodos privados de clasificacion                                  //
    // ------------------------------------------------------------------ //

    private String resolverCategoria(Token.Tipo tipo) {
        switch (tipo) {
            case PR_ENTRY: case PR_OBJECT: case PR_METHOD:
            case PR_DEFINE: case PR_INT:  case PR_DEC:
            case PR_STRING: case PR_BOOL: case PR_VOID:
            case PR_IF:    case PR_ELSE:  case PR_SELECT:
            case PR_CASE:  case PR_LOOP:  case PR_OUT:
            case PR_INPUT: case PR_TRUE:  case PR_FALSE:
            case PR_NEW:   case PR_RETURN:
                return "Palabra Reservada";

            case LIT_ENTERO:   return "Literal Entero";
            case LIT_DECIMAL:  return "Literal Decimal";
            case LIT_CADENA:   return "Literal Cadena";

            case IDENTIFICADOR: return "Identificador";

            case OP_SUMA: case OP_RESTA: case OP_MULTIPLICACION:
            case OP_DIVISION: case OP_MODULO:
                return "Operador Aritmetico";

            case OP_IGUAL: case OP_DIFERENTE: case OP_MAYOR:
            case OP_MENOR: case OP_MAYOR_IGUAL: case OP_MENOR_IGUAL:
                return "Operador Relacional";

            case OP_AND: case OP_OR: case OP_NOT:
                return "Operador Logico";

            case OP_ASIGNACION: case OP_INCREMENTO: case OP_DECREMENTO:
                return "Operador de Asignacion";

            case DEL_PUNTO_COMA:
                return "Delimitador";
            case DEL_LLAVE_A: case DEL_LLAVE_C:
                return "Bloque";
            case DEL_PAREN_A: case DEL_PAREN_C:
                return "Agrupador";
            case DEL_CORCHETE_A: case DEL_CORCHETE_C:
                return "Arreglo";
            case DEL_COMA:
                return "Separador";
            case DEL_PUNTO:
                return "Acceso";

            case ERROR:
                return "ERROR LEXICO";

            default:
                return "Desconocido";
        }
    }

    private String resolverDetalle(Token.Tipo tipo) {
        switch (tipo) {
            case PR_ENTRY:  return "Punto de entrada del programa";
            case PR_OBJECT: return "Definicion de clase/objeto";
            case PR_METHOD: return "Declaracion de metodo";
            case PR_DEFINE: return "Declaracion de variable";
            case PR_INT:    return "Tipo de dato entero (32 bits)";
            case PR_DEC:    return "Tipo de dato decimal (64 bits IEEE 754)";
            case PR_STRING: return "Tipo de dato cadena (UTF-8)";
            case PR_BOOL:   return "Tipo de dato booleano";
            case PR_VOID:   return "Tipo de retorno nulo";
            case PR_IF:     return "Condicional if";
            case PR_ELSE:   return "Rama alternativa else";
            case PR_SELECT: return "Seleccion multiple";
            case PR_CASE:   return "Caso de seleccion";
            case PR_LOOP:   return "Ciclo de repeticion";
            case PR_OUT:    return "Salida estandar a consola";
            case PR_INPUT:  return "Entrada estandar del usuario";
            case PR_TRUE:   return "Valor logico verdadero";
            case PR_FALSE:  return "Valor logico falso";
            case PR_NEW:    return "Instanciacion de objeto";
            case PR_RETURN: return "Retorno de valor";

            case LIT_ENTERO:    return "Numero sin decimales";
            case LIT_DECIMAL:   return "Numero con punto decimal";
            case LIT_CADENA:    return "Texto entre comillas dobles";

            case IDENTIFICADOR: return "Nombre de variable/objeto (inicia en minuscula)";

            case OP_SUMA:           return "Suma o concatenacion";
            case OP_RESTA:          return "Resta";
            case OP_MULTIPLICACION: return "Multiplicacion";
            case OP_DIVISION:       return "Division";
            case OP_MODULO:         return "Residuo entero";

            case OP_IGUAL:          return "Comparacion de igualdad";
            case OP_DIFERENTE:      return "Comparacion de desigualdad";
            case OP_MAYOR:          return "Comparacion mayor que";
            case OP_MENOR:          return "Comparacion menor que";
            case OP_MAYOR_IGUAL:    return "Comparacion mayor o igual";
            case OP_MENOR_IGUAL:    return "Comparacion menor o igual";

            case OP_AND:            return "Operacion logica AND";
            case OP_OR:             return "Operacion logica OR";
            case OP_NOT:            return "Negacion logica NOT";

            case OP_ASIGNACION:     return "Asignacion de valor";
            case OP_INCREMENTO:     return "Incremento en 1";
            case OP_DECREMENTO:     return "Decremento en 1";

            case DEL_PUNTO_COMA:    return "Fin de sentencia (obligatorio en JODA)";
            case DEL_LLAVE_A:       return "Apertura de bloque";
            case DEL_LLAVE_C:       return "Cierre de bloque";
            case DEL_PAREN_A:       return "Apertura de parametros/condicion";
            case DEL_PAREN_C:       return "Cierre de parametros/condicion";
            case DEL_CORCHETE_A:    return "Apertura de arreglo";
            case DEL_CORCHETE_C:    return "Cierre de arreglo";
            case DEL_COMA:          return "Separador de parametros";
            case DEL_PUNTO:         return "Acceso a metodo o libreria";

            case ERROR:             return "Caracter no reconocido por el lenguaje JODA";

            default: return "";
        }
    }

    /** Elimina caracteres especiales no ASCII. */
    private String limpiar(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
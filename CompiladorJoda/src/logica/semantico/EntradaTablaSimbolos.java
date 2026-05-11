package logica.semantico;

/*
Representa una entrada en la Tabla de Simbolos del compilador JODA.
Almacena informacion sobre cada variable o identificador declarado
en el programa: su nombre, tipo, valor y ambito.
*/
public class EntradaTablaSimbolos {

    // Tipos de dato validos en JODA
    public enum TipoDato {
        INT,
        DEC,
        STRING,
        BOOL,
        VOID,
        OBJECT,
        DESCONOCIDO
    }

    // Tipos de entrada en la tabla
    public enum CategoriaEntrada {
        VARIABLE,
        METODO,
        CLASE
    }

    private final String nombre;
    private TipoDato tipoDato;
    private CategoriaEntrada categoria;
    private Object valor;         // Valor actual (usado por el ejecutor)
    private final int lineaDeclaracion;
    private boolean inicializada;

    public EntradaTablaSimbolos(String nombre, TipoDato tipoDato,
                                 CategoriaEntrada categoria, int lineaDeclaracion) {
        this.nombre = nombre;
        this.tipoDato = tipoDato;
        this.categoria = categoria;
        this.lineaDeclaracion = lineaDeclaracion;
        this.valor = null;
        this.inicializada = false;
    }

    // Getters y Setters
    public String getNombre() { return nombre; }

    public TipoDato getTipoDato() { return tipoDato; }
    public void setTipoDato(TipoDato tipoDato) { this.tipoDato = tipoDato; }

    public CategoriaEntrada getCategoria() { return categoria; }

    public Object getValor() { return valor; }
    public void setValor(Object valor) {
        this.valor = valor;
        this.inicializada = true;
    }

    public int getLineaDeclaracion() { return lineaDeclaracion; }

    public boolean isInicializada() { return inicializada; }

    // Convierte el tipo dado como cadena al enum TipoDato correspondiente.
    public static TipoDato parsearTipo(String tipo) {
        switch (tipo.toLowerCase()) {
            case "int":    return TipoDato.INT;
            case "dec":    return TipoDato.DEC;
            case "string": return TipoDato.STRING;
            case "bool":   return TipoDato.BOOL;
            case "void":   return TipoDato.VOID;
            case "object": return TipoDato.OBJECT;
            default:       return TipoDato.DESCONOCIDO;
        }
    }

    @Override
    public String toString() {
        return String.format("EntradaTablaSimbolos{nombre='%s', tipo=%s, categoria=%s, "
            + "linea=%d, valor=%s, inicializada=%b}",
            nombre, tipoDato, categoria, lineaDeclaracion, valor, inicializada);
    }
}
package logica.semantico;

/**
 * Representa una entrada en la tabla de simbolos del compilador JODA.
 * Almacena el nombre, tipo, ambito, categoria y linea de declaracion.
 */
public class EntradaTablaSimbolos {

    public enum Categoria {
        VARIABLE,
        PARAMETRO,
        METODO,
        CLASE
    }

    private final String    nombre;
    private final String    tipo;       // int, dec, string, bool, void, nombre-de-clase
    private final String    ambito;     // "global", nombre del metodo, nombre de la clase
    private final Categoria categoria;
    private final int       lineaDecl;
    private boolean         inicializada;

    public EntradaTablaSimbolos(String nombre, String tipo, String ambito,
                                Categoria categoria, int lineaDecl) {
        this.nombre       = nombre;
        this.tipo         = tipo;
        this.ambito       = ambito;
        this.categoria    = categoria;
        this.lineaDecl    = lineaDecl;
        this.inicializada = false;
    }

    public String    getNombre()      { return nombre;       }
    public String    getTipo()        { return tipo;         }
    public String    getAmbito()      { return ambito;       }
    public Categoria getCategoria()   { return categoria;    }
    public int       getLineaDecl()   { return lineaDecl;    }
    public boolean   isInicializada() { return inicializada; }

    public void setInicializada(boolean v) { this.inicializada = v; }

    @Override
    public String toString() {
        return nombre + " [" + tipo + "] en ambito='" + ambito
                + "' cat=" + categoria + " linea=" + lineaDecl;
    }
}
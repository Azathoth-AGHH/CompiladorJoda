package logica.nucleo;

import java.util.Collections;
import java.util.List;

import logica.lexico.Token;
import logica.semantico.EntradaTablaSimbolos;
import logica.sintactico.NodoAST;

public final class ResultadoCompilacion {

    private final String                    nombreArchivo;
    private final String                    codigoFuente;
    private final List<Token>               tokens;
    private final List<String>              erroresLexicos;
    private final NodoAST.NodoEntry         arbolSintactico;
    private final List<String>              erroresSintacticos;
    private final List<EntradaTablaSimbolos> tablaSimbolos;
    private final List<String>              erroresSemanticos;
    private final List<String>              advertenciasSemanticas;
    private final String                    documentacion;
    private final String                    rutaPdfDocDescriptiva;
    private final List<String>              salidasEjecucion;
    private final boolean                   exitoCompilacion;
    private final boolean                   exitoEjecucion;

    private ResultadoCompilacion(Builder b) {
        this.nombreArchivo          = b.nombreArchivo;
        this.codigoFuente           = b.codigoFuente;
        this.tokens                 = safe(b.tokens);
        this.erroresLexicos         = safe(b.erroresLexicos);
        this.arbolSintactico        = b.arbolSintactico;
        this.erroresSintacticos     = safe(b.erroresSintacticos);
        this.tablaSimbolos          = safe(b.tablaSimbolos);
        this.erroresSemanticos      = safe(b.erroresSemanticos);
        this.advertenciasSemanticas = safe(b.advertenciasSemanticas);
        this.documentacion          = b.documentacion;
        this.rutaPdfDocDescriptiva  = b.rutaPdfDocDescriptiva;
        this.salidasEjecucion       = safe(b.salidasEjecucion);
        this.exitoCompilacion       = b.exitoCompilacion;
        this.exitoEjecucion         = b.exitoEjecucion;
    }

    private static <T> List<T> safe(List<T> lista) {
        return lista != null ? Collections.unmodifiableList(lista) : Collections.emptyList();
    }

    public boolean tieneErrores() {
        return !erroresLexicos.isEmpty() || !erroresSintacticos.isEmpty() || !erroresSemanticos.isEmpty();
    }

    public String                    getNombreArchivo()          { return nombreArchivo; }
    public String                    getCodigoFuente()           { return codigoFuente; }
    public List<Token>               getTokens()                 { return tokens; }
    public List<String>              getErroresLexicos()         { return erroresLexicos; }
    public NodoAST.NodoEntry         getArbolSintactico()        { return arbolSintactico; }
    public List<String>              getErroresSintacticos()     { return erroresSintacticos; }
    public List<EntradaTablaSimbolos> getTablaSimbolos()         { return tablaSimbolos; }
    public List<String>              getErroresSemanticos()      { return erroresSemanticos; }
    public List<String>              getAdvertenciasSemanticas() { return advertenciasSemanticas; }
    public String                    getDocumentacion()          { return documentacion; }
    public String                    getRutaPdfDocDescriptiva()  { return rutaPdfDocDescriptiva; }
    public List<String>              getSalidasEjecucion()       { return salidasEjecucion; }
    public boolean                   isExitoCompilacion()        { return exitoCompilacion; }
    public boolean                   isExitoEjecucion()          { return exitoEjecucion; }

    public static final class Builder {
        private String                    nombreArchivo;
        private String                    codigoFuente;
        private List<Token>               tokens;
        private List<String>              erroresLexicos;
        private NodoAST.NodoEntry         arbolSintactico;
        private List<String>              erroresSintacticos;
        private List<EntradaTablaSimbolos> tablaSimbolos;
        private List<String>              erroresSemanticos;
        private List<String>              advertenciasSemanticas;
        private String                    documentacion;
        private String                    rutaPdfDocDescriptiva;
        private List<String>              salidasEjecucion;
        private boolean                   exitoCompilacion;
        private boolean                   exitoEjecucion;

        public Builder nombreArchivo(String v)                    { nombreArchivo = v; return this; }
        public Builder codigoFuente(String v)                     { codigoFuente = v; return this; }
        public Builder tokens(List<Token> v)                      { tokens = v; return this; }
        public Builder erroresLexicos(List<String> v)             { erroresLexicos = v; return this; }
        public Builder arbolSintactico(NodoAST.NodoEntry v)       { arbolSintactico = v; return this; }
        public Builder erroresSintacticos(List<String> v)         { erroresSintacticos = v; return this; }
        public Builder tablaSimbolos(List<EntradaTablaSimbolos> v) { tablaSimbolos = v; return this; }
        public Builder erroresSemanticos(List<String> v)          { erroresSemanticos = v; return this; }
        public Builder advertenciasSemanticas(List<String> v)     { advertenciasSemanticas = v; return this; }
        public Builder documentacion(String v)                    { documentacion = v; return this; }
        public Builder rutaPdfDocDescriptiva(String v)            { rutaPdfDocDescriptiva = v; return this; }
        public Builder salidasEjecucion(List<String> v)           { salidasEjecucion = v; return this; }
        public Builder exitoCompilacion(boolean v)                { exitoCompilacion = v; return this; }
        public Builder exitoEjecucion(boolean v)                  { exitoEjecucion = v; return this; }
        public ResultadoCompilacion build()                       { return new ResultadoCompilacion(this); }
    }
}
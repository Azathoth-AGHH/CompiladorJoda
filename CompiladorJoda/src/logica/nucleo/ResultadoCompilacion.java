package logica.nucleo;

import logica.lexico.Token;
import logica.semantico.EntradaTablaSimbolos;
import logica.sintactico.NodoAST;

import java.util.List;

/*
Objeto de valor que encapsula todos los resultados
producidos por las distintas fases del compilador JODA.
La vista_consola accede a este objeto para imprimir los resultados.
*/
public class ResultadoCompilacion {

    //Datos de entrada
    private String nombreArchivo;
    private String codigoFuente;

    //Fase Lexica
    private List<Token> tokens;
    private List<String> erroresLexicos;

    //Fase Sintactica
    private NodoAST.NodoEntry arbolSintactico;
    private List<String> erroresSintacticos;

    //Fase Semantica
    private List<EntradaTablaSimbolos> tablaSimbolos;
    private List<String> erroresSemanticos;
    private List<String> advertenciasSemanticas;

    //Documentacion
    private String documentacion;

    //Ejecucion JVM-J
    private List<String> salidasEjecucion;
    private boolean exitoCompilacion;
    private boolean exitoEjecucion;

    public ResultadoCompilacion() {}

    // Getters y Setters
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getCodigoFuente() { return codigoFuente; }
    public void setCodigoFuente(String codigoFuente) { this.codigoFuente = codigoFuente; }

    public List<Token> getTokens() { return tokens; }
    public void setTokens(List<Token> tokens) { this.tokens = tokens; }

    public List<String> getErroresLexicos() { return erroresLexicos; }
    public void setErroresLexicos(List<String> erroresLexicos) { this.erroresLexicos = erroresLexicos; }

    public NodoAST.NodoEntry getArbolSintactico() { return arbolSintactico; }
    public void setArbolSintactico(NodoAST.NodoEntry arbolSintactico) { this.arbolSintactico = arbolSintactico; }

    public List<String> getErroresSintacticos() { return erroresSintacticos; }
    public void setErroresSintacticos(List<String> erroresSintacticos) { this.erroresSintacticos = erroresSintacticos; }

    public List<EntradaTablaSimbolos> getTablaSimbolos() { return tablaSimbolos; }
    public void setTablaSimbolos(List<EntradaTablaSimbolos> tablaSimbolos) { this.tablaSimbolos = tablaSimbolos; }

    public List<String> getErroresSemanticos() { return erroresSemanticos; }
    public void setErroresSemanticos(List<String> erroresSemanticos) { this.erroresSemanticos = erroresSemanticos; }

    public List<String> getAdvertenciasSemanticas() { return advertenciasSemanticas; }
    public void setAdvertenciasSemanticas(List<String> adv) { this.advertenciasSemanticas = adv; }

    public String getDocumentacion() { return documentacion; }
    public void setDocumentacion(String documentacion) { this.documentacion = documentacion; }

    public List<String> getSalidasEjecucion() { return salidasEjecucion; }
    public void setSalidasEjecucion(List<String> salidasEjecucion) { this.salidasEjecucion = salidasEjecucion; }

    public boolean isExitoCompilacion() { return exitoCompilacion; }
    public void setExitoCompilacion(boolean exitoCompilacion) { this.exitoCompilacion = exitoCompilacion; }

    public boolean isExitoEjecucion() { return exitoEjecucion; }
    public void setExitoEjecucion(boolean exitoEjecucion) { this.exitoEjecucion = exitoEjecucion; }

    public boolean tieneErrores() {
        boolean errL = erroresLexicos != null && !erroresLexicos.isEmpty();
        boolean errS = erroresSintacticos != null && !erroresSintacticos.isEmpty();
        boolean errSem = erroresSemanticos != null && !erroresSemanticos.isEmpty();
        return errL || errS || errSem;
    }
}
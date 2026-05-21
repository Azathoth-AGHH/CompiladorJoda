package logica.nucleo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import logica.documentador.Documentador;
import logica.documentador.DocumentadorLinea;
import logica.documentador.GeneradorPdf_Docdes;
import logica.lexico.AnalizadorLexico;
import logica.lexico.Token;
import logica.semantico.AnalizadorSemantico;
import logica.semantico.EntradaTablaSimbolos;
import logica.sintactico.AnalizadorSintactico;
import logica.sintactico.NodoAST;

public final class CompiladorJoda {

    private Function<String, String> inputCallback;

    public void setInputCallback(Function<String, String> callback) {
        this.inputCallback = callback;
    }

    public ResultadoCompilacion compilarYEjecutar(String rutaArchivo) {
        ResultadoCompilacion.Builder builder = new ResultadoCompilacion.Builder()
            .nombreArchivo(rutaArchivo);

        String codigoFuente;
        try {
            codigoFuente = LectorArchivo.leer(rutaArchivo);
        } catch (IOException e) {
            return builder
                .erroresLexicos(List.of("Error fatal: no se pudo leer '" + rutaArchivo + "'."))
                .exitoCompilacion(false)
                .build();
        }

        builder.codigoFuente(codigoFuente);

        AnalizadorLexico lexico = new AnalizadorLexico(codigoFuente);
        List<Token> tokens = lexico.analizar();
        builder.tokens(tokens).erroresLexicos(lexico.getErrores());

        builder.documentacion(new Documentador().documentar(tokens, codigoFuente));

        if (lexico.tieneErrores()) {
            return builder.exitoCompilacion(false)
                .erroresSintacticos(List.of())
                .erroresSemanticos(List.of())
                .advertenciasSemanticas(List.of())
                .salidasEjecucion(List.of())
                .build();
        }

        AnalizadorSintactico sintactico = new AnalizadorSintactico(tokens);
        NodoAST.NodoEntry arbol = sintactico.parsear();
        builder.arbolSintactico(arbol).erroresSintacticos(sintactico.getErrores());

        if (sintactico.tieneErrores() || arbol == null) {
            return builder.exitoCompilacion(false)
                .erroresSemanticos(List.of())
                .advertenciasSemanticas(List.of())
                .salidasEjecucion(List.of())
                .build();
        }

        AnalizadorSemantico semantico = new AnalizadorSemantico();
        semantico.analizar(arbol);
        builder.erroresSemanticos(semantico.getErrores())
               .advertenciasSemanticas(semantico.getAdvertencias())
               .tablaSimbolos(semantico.getRegistroGlobal());

        if (semantico.tieneErrores()) {
            return builder.exitoCompilacion(false)
                .salidasEjecucion(List.of())
                .build();
        }

        builder.exitoCompilacion(true);
        generarPdf(builder, tokens, codigoFuente, rutaArchivo);
        ejecutar(builder, arbol, semantico);

        return builder.build();
    }

    private void generarPdf(ResultadoCompilacion.Builder builder,
                             List<Token> tokens, String codigoFuente, String rutaArchivo) {
        try {
            List<String> lineasDoc = new DocumentadorLinea().documentarPorLinea(tokens, codigoFuente);
            String rutaPdf = new GeneradorPdf_Docdes().generar(lineasDoc, rutaArchivo);
            builder.rutaPdfDocDescriptiva(rutaPdf);
        } catch (Exception e) {
            System.err.println("Advertencia: no se pudo generar PDF — " + e.getMessage());
        }
    }

    private void ejecutar(ResultadoCompilacion.Builder builder,
                           NodoAST.NodoEntry arbol, AnalizadorSemantico semantico) {
        List<String> salidas = new ArrayList<>();
        EjecutorJoda ejecutor = new EjecutorJoda(semantico.getTablaSimbolos(), salidas);
        if (inputCallback != null) ejecutor.setInputCallback(inputCallback);

        try {
            ejecutor.ejecutar(arbol);
            builder.exitoEjecucion(true);
        } catch (Exception e) {
            salidas.add("[ERROR JVM-J] " + e.getMessage());
            builder.exitoEjecucion(false);
        }

        builder.salidasEjecucion(salidas);
    }
}
package logica.nucleo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

public class CompiladorJoda {

    private Function<String, String> inputCallback;

    public void setInputCallback(Function<String, String> callback) {
        this.inputCallback = callback;
    }


    public ResultadoCompilacion compilarYEjecutar(String rutaArchivo) {

        ResultadoCompilacion resultado = new ResultadoCompilacion();
        resultado.setNombreArchivo(rutaArchivo);

        // ==============================
        // PASO 0: Lectura del archivo
        // ==============================

        String codigoFuente = leerArchivo(rutaArchivo);

        if (codigoFuente == null) {

            resultado.setErroresLexicos(List.of(
                "Error fatal: no se pudo leer el archivo '" + rutaArchivo + "'."
            ));

            resultado.setExitoCompilacion(false);

            return resultado;
        }

        resultado.setCodigoFuente(limpiarCodigo(codigoFuente));

        // ==============================
        // PASO 1: ANALISIS LEXICO
        // ==============================

        AnalizadorLexico analizadorLexico =
                new AnalizadorLexico(resultado.getCodigoFuente());

        List<Token> tokens = analizadorLexico.analizar();

        resultado.setTokens(tokens);
        resultado.setErroresLexicos(analizadorLexico.getErrores());

        // ==============================
        // PASO 2: DOCUMENTACION
        // ==============================

        Documentador documentador = new Documentador();

        String documentacion =
                documentador.documentar(tokens, resultado.getCodigoFuente());

        resultado.setDocumentacion(documentacion);

        // Si hay errores lexicos -> detener
        if (analizadorLexico.tieneErrores()) {

            resultado.setExitoCompilacion(false);

            resultado.setErroresSintacticos(new ArrayList<>());
            resultado.setErroresSemanticos(new ArrayList<>());
            resultado.setAdvertenciasSemanticas(new ArrayList<>());
            resultado.setSalidasEjecucion(new ArrayList<>());

            return resultado;
        }

        // ==============================
        // PASO 3: ANALISIS SINTACTICO
        // ==============================

        AnalizadorSintactico analizadorSintactico =
                new AnalizadorSintactico(tokens);

        NodoAST.NodoEntry arbol = analizadorSintactico.parsear();

        resultado.setArbolSintactico(arbol);

        resultado.setErroresSintacticos(
                analizadorSintactico.getErrores()
        );

        // Si hay errores sintacticos -> detener
        if (analizadorSintactico.tieneErrores() || arbol == null) {

            resultado.setExitoCompilacion(false);

            resultado.setErroresSemanticos(new ArrayList<>());
            resultado.setAdvertenciasSemanticas(new ArrayList<>());
            resultado.setSalidasEjecucion(new ArrayList<>());

            return resultado;
        }

        // ==============================
        // PASO 4: ANALISIS SEMANTICO
        // ==============================

        AnalizadorSemantico analizadorSemantico =
                new AnalizadorSemantico();

        analizadorSemantico.analizar(arbol);

        resultado.setErroresSemanticos(
                analizadorSemantico.getErrores()
        );

        resultado.setAdvertenciasSemanticas(
                analizadorSemantico.getAdvertencias()
        );

        // Tabla de simbolos
        resultado.setTablaSimbolos(
                analizadorSemantico.getRegistroGlobal()
        );

        // Si hay errores semanticos -> detener
        if (analizadorSemantico.tieneErrores()) {

            resultado.setExitoCompilacion(false);

            resultado.setSalidasEjecucion(new ArrayList<>());

            return resultado;
        }

        // ==============================
        // PASO 5: COMPILACION EXITOSA
        // ==============================

        resultado.setExitoCompilacion(true);

        // ==============================
        // GENERAR PDF DESCRIPTIVO
        // ==============================

        try {

            DocumentadorLinea docLinea =
                    new DocumentadorLinea();

            List<String> lineasDoc =
                    docLinea.documentarPorLinea(
                            tokens,
                            resultado.getCodigoFuente()
                    );

            GeneradorPdf_Docdes generadorPdf =
                    new GeneradorPdf_Docdes();

            String rutaPdf =
                    generadorPdf.generar(
                            lineasDoc,
                            rutaArchivo
                    );

            resultado.setRutaPdfDocDescriptiva(rutaPdf);

        } catch (Exception e) {

            System.out.println(
                "Error al generar PDF descriptivo: "
                + e.getMessage()
            );
        }

        // ==============================
        // EJECUCION JVM-J
        // ==============================

        List<String> salidasEjecucion =
                new ArrayList<>();

        EjecutorJoda ejecutor =
                new EjecutorJoda(
                        analizadorSemantico.getTablaSimbolos(),
                        salidasEjecucion
                );

        // Callback input
        if (inputCallback != null) {
            ejecutor.setInputCallback(inputCallback);
        }

        try {

            ejecutor.ejecutar(arbol);

            resultado.setExitoEjecucion(true);

        } catch (Exception e) {

            salidasEjecucion.add(
                "[ERROR JVM-J] Excepcion en ejecucion: "
                + e.getMessage()
            );

            resultado.setExitoEjecucion(false);
        }

        resultado.setSalidasEjecucion(salidasEjecucion);

        return resultado;
    }

    private String leerArchivo(String ruta) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(ruta));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private String limpiarCodigo(String codigo) {
        codigo = codigo.replace("\r\n", "\n").replace("\r", "\n");
        StringBuilder sb = new StringBuilder();
            for (char c : codigo.toCharArray()) {
            if (c == '\n' || c == '\t' || (c >= 32 && c < 127)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
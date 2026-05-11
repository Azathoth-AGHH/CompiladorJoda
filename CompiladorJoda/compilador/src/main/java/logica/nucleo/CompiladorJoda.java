package logica.nucleo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import logica.documentador.Documentador;
import logica.lexico.AnalizadorLexico;
import logica.lexico.Token;
import logica.semantico.AnalizadorSemantico;
import logica.semantico.EntradaTablaSimbolos;
import logica.sintactico.AnalizadorSintactico;
import logica.sintactico.NodoAST;

/*
Compilador principal de JODA.
Coordina todas las fases del modelo hibrido:
    1. Lectura del archivo fuente.
    2. Analisis Lexico -> Lista de Tokens.
    3. Documentacion -> Narrativa del codigo.
    4. Analisis Sintactico -> AST.
    5. Analisis Semantico -> Validacion de tipos y ambitos.
    6. Ejecucion en JVM-J (solo si no hay errores).
 
Retorna un objeto ResultadoCompilacion para que la vista lo interprete.
 */
public class CompiladorJoda {

    //Ejecuta el pipeline completo de compilacion e interpretacion.

    public ResultadoCompilacion compilarYEjecutar(String rutaArchivo) {
        ResultadoCompilacion resultado = new ResultadoCompilacion();
        resultado.setNombreArchivo(rutaArchivo);

        // PASO 0: Lectura del archivo fuente
        String codigoFuente = leerArchivo(rutaArchivo);
        if (codigoFuente == null) {
            resultado.setErroresLexicos(List.of(
                "Error fatal: no se pudo leer el archivo '" + rutaArchivo + "'."));
            resultado.setExitoCompilacion(false);
            return resultado;
        }
        resultado.setCodigoFuente(limpiarCodigo(codigoFuente));

        // PASO 1: Analisis Lexico
        AnalizadorLexico analizadorLexico = new AnalizadorLexico(resultado.getCodigoFuente());
        List<Token> tokens = analizadorLexico.analizar();
        resultado.setTokens(tokens);
        resultado.setErroresLexicos(analizadorLexico.getErrores());

        // PASO 2: Documentacion del codigo
        Documentador documentador = new Documentador();
        String documentacion = documentador.documentar(tokens, resultado.getCodigoFuente());
        resultado.setDocumentacion(documentacion);

        // Si hay errores lexicos, detener pipeline
        if (analizadorLexico.tieneErrores()) {
            resultado.setExitoCompilacion(false);
            resultado.setErroresSintacticos(new ArrayList<>());
            resultado.setErroresSemanticos(new ArrayList<>());
            resultado.setAdvertenciasSemanticas(new ArrayList<>());
            resultado.setSalidasEjecucion(new ArrayList<>());
            return resultado;
        }

        // PASO 3: Analisis Sintactico
        AnalizadorSintactico analizadorSintactico = new AnalizadorSintactico(tokens);
        NodoAST.NodoEntry arbol = analizadorSintactico.parsear();
        resultado.setArbolSintactico(arbol);
        resultado.setErroresSintacticos(analizadorSintactico.getErrores());

        // Si hay errores sintacticos, detener pipeline
        if (analizadorSintactico.tieneErrores() || arbol == null) {
            resultado.setExitoCompilacion(false);
            resultado.setErroresSemanticos(new ArrayList<>());
            resultado.setAdvertenciasSemanticas(new ArrayList<>());
            resultado.setSalidasEjecucion(new ArrayList<>());
            return resultado;
        }

        // PASO 4: Analisis Semantico
        AnalizadorSemantico analizadorSemantico = new AnalizadorSemantico();
        analizadorSemantico.analizar(arbol);
        resultado.setErroresSemanticos(analizadorSemantico.getErrores());
        resultado.setAdvertenciasSemanticas(analizadorSemantico.getAdvertencias());

        // Poblar tabla de simbolos para reporte
        resultado.setTablaSimbolos(analizadorSemantico.getRegistroGlobal());

        // Si hay errores semanticos, detener pipeline
        if (analizadorSemantico.tieneErrores()) {
            resultado.setExitoCompilacion(false);
            resultado.setSalidasEjecucion(new ArrayList<>());
            return resultado;
        }

        // PASO 5: Compilacion exitosa -> Activar JVM-J
        resultado.setExitoCompilacion(true);

        List<String> salidasEjecucion = new ArrayList<>();
        EjecutorJoda ejecutor = new EjecutorJoda(
            analizadorSemantico.getTablaSimbolos(), salidasEjecucion
        );

        try {
            ejecutor.ejecutar(arbol);
            resultado.setExitoEjecucion(true);
        } catch (Exception e) {
            salidasEjecucion.add("[ERROR JVM-J] Excepcion en ejecucion: " + e.getMessage());
            resultado.setExitoEjecucion(false);
        }

        resultado.setSalidasEjecucion(salidasEjecucion);
        return resultado;
    }

    // Metodos auxiliares
    //Lee el contenido de un archivo de texto y lo retorna como String.
    private String leerArchivo(String ruta) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(ruta));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /*
    Elimina caracteres no imprimibles y normaliza saltos de linea.
    Previene errores de visualizacion en la terminal.
    */
    private String limpiarCodigo(String codigo) {
        // Normalizar saltos de linea a '\n'
        codigo = codigo.replace("\r\n", "\n").replace("\r", "\n");
        // Eliminar caracteres nulos y no imprimibles (excepto tabulacion y nueva linea)
        StringBuilder sb = new StringBuilder();
        for (char c : codigo.toCharArray()) {
            if (c == '\n' || c == '\t' || (c >= 32 && c < 127)) {
                sb.append(c);
            }
            // Los caracteres ASCII extendidos (acentos, etc.) se ignoran para evitar problemas de codificacion en la terminal
        }
        return sb.toString();
    }
}
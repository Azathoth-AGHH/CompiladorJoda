package logica.nucleo;

import logica.documentador.Documentador;
import logica.lexico.AnalizadorLexico;
import logica.lexico.Token;
import logica.semantico.AnalizadorSemantico;
import logica.semantico.EntradaTablaSimbolos;
import logica.sintactico.AnalizadorSintactico;

import java.util.List;

/**
 * Orquestador principal del compilador JODA.
 *
 * Fase 1 - Analisis:
 *   a) Lexico    -> tokeniza el codigo fuente
 *   b) Sintactico -> valida la estructura gramatical
 *   c) Semantico  -> valida tipos, ambitos y redeclaraciones
 *
 * Fase 2 - Ejecucion (simulada):
 *   Si no hay errores, se ejecuta el bloque 'entry'.
 *
 * Este paquete NO imprime nada. Devuelve objetos de resultado.
 */
public class CompiladorJoda {

    // ------------------------------------------------------------------ //
    //  Resultado global del compilador                                     //
    // ------------------------------------------------------------------ //

    public static class ResultadoCompilacion {
        private final List<Token>                     tokens;
        private final List<AnalizadorSintactico.ErrorSintactico> erroresSintacticos;
        private final AnalizadorSemantico.ResultadoSemantico     resultadoSemantico;
        private final List<Documentador.FilaDoc>      tablaDoc;
        private final List<String>                    salidaEjecucion;
        private final boolean                         exitoso;

        public ResultadoCompilacion(
                List<Token> tokens,
                List<AnalizadorSintactico.ErrorSintactico> erroresSintacticos,
                AnalizadorSemantico.ResultadoSemantico resultadoSemantico,
                List<Documentador.FilaDoc> tablaDoc,
                List<String> salidaEjecucion,
                boolean exitoso) {
            this.tokens              = tokens;
            this.erroresSintacticos  = erroresSintacticos;
            this.resultadoSemantico  = resultadoSemantico;
            this.tablaDoc            = tablaDoc;
            this.salidaEjecucion     = salidaEjecucion;
            this.exitoso             = exitoso;
        }

        public List<Token>                                    getTokens()             { return tokens;             }
        public List<AnalizadorSintactico.ErrorSintactico>    getErroresSintacticos()  { return erroresSintacticos; }
        public AnalizadorSemantico.ResultadoSemantico        getResultadoSemantico()  { return resultadoSemantico; }
        public List<Documentador.FilaDoc>                    getTablaDoc()            { return tablaDoc;           }
        public List<String>                                  getSalidaEjecucion()     { return salidaEjecucion;    }
        public boolean                                       isExitoso()              { return exitoso;            }
    }

    // ------------------------------------------------------------------ //
    //  Componentes                                                         //
    // ------------------------------------------------------------------ //

    private final AnalizadorLexico      analizadorLexico    = new AnalizadorLexico();
    private final AnalizadorSintactico  analizadorSintact   = new AnalizadorSintactico();
    private final AnalizadorSemantico   analizadorSemant    = new AnalizadorSemantico();
    private final Documentador          documentador        = new Documentador();
    private final EjecutorJoda          ejecutor            = new EjecutorJoda();

    // ------------------------------------------------------------------ //
    //  API publica                                                         //
    // ------------------------------------------------------------------ //

    /**
     * Compila y, si no hay errores, ejecuta el codigo fuente JODA.
     *
     * @param codigoFuente contenido del archivo .joda
     * @return resultado completo con tokens, errores, tabla y salida
     */
    public ResultadoCompilacion compilar(String codigoFuente) {

        // -- FASE 1a: Analisis Lexico --
        List<Token> tokens = analizadorLexico.analizar(codigoFuente);

        // Verificar errores lexicos
        boolean hayErrorLexico = tokens.stream()
                .anyMatch(t -> t.getTipo() == Token.Tipo.ERROR);

        // -- Documentador: tabla de tokens --
        List<Documentador.FilaDoc> tablaDoc = documentador.documentar(tokens);

        // -- FASE 1b: Analisis Sintactico --
        List<AnalizadorSintactico.ErrorSintactico> erroresSintact =
                analizadorSintact.analizar(tokens);

        // -- FASE 1c: Analisis Semantico --
        AnalizadorSemantico.ResultadoSemantico resultSemant =
                analizadorSemantico.analizar(tokens);

        boolean hayErrores = hayErrorLexico
                || !erroresSintact.isEmpty()
                || resultSemant.tieneErrores();

        // -- FASE 2: Ejecucion (solo si no hay errores) --
        List<String> salidaEjecucion = new java.util.ArrayList<>();
        if (!hayErrores) {
            salidaEjecucion = ejecutor.ejecutar(tokens);
        }

        return new ResultadoCompilacion(
                tokens,
                erroresSintact,
                resultSemant,
                tablaDoc,
                salidaEjecucion,
                !hayErrores
        );
    }

    // ------------------------------------------------------------------ //
    //  Acceso directo al semantico (para compatibilidad)                  //
    // ------------------------------------------------------------------ //
    private AnalizadorSemantico analizadorSemantico = new AnalizadorSemantico();
}
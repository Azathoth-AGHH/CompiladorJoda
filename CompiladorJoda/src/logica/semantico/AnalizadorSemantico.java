package logica.semantico;

import logica.sintactico.NodoAST;
import java.util.ArrayList;
import java.util.List;

/*
Analizador Semantico del compilador JODA.
Recorre el AST y valida reglas de contexto:
    - Variables declaradas antes de uso.
    - No se redeclaran variables en el mismo ambito.
    - Compatibilidad de tipos en operaciones.
    - Correcto uso de palabras reservadas.
*/
public class AnalizadorSemantico {

    private final TablaSimbolos tablaSimbolos;
    private final List<String> errores;
    private final List<String> advertencias;

    public AnalizadorSemantico() {
        this.tablaSimbolos = new TablaSimbolos();
        this.errores = new ArrayList<>();
        this.advertencias = new ArrayList<>();
    }

    // Registro global de todos los simbolos declarados (para reporte final)
    private final java.util.List<EntradaTablaSimbolos> registroGlobal = new ArrayList<>();

    //Punto de entrada: analiza el nodo raiz entry.
    public void analizar(NodoAST.NodoEntry nodoEntry) {
        if (nodoEntry == null) return;
        analizarBloque(nodoEntry.instrucciones);
    }

    public java.util.List<EntradaTablaSimbolos> getRegistroGlobal() {
        return registroGlobal;
    }

    // Analisis de bloques e instrucciones
    private void analizarBloque(List<NodoAST> instrucciones) {
        tablaSimbolos.abrirAmbito();
        for (NodoAST instruccion : instrucciones) {
            analizarNodo(instruccion);
        }
        tablaSimbolos.cerrarAmbito();
    }

    private void analizarNodo(NodoAST nodo) {
        if (nodo == null) return;

        if (nodo instanceof NodoAST.NodoDefine) {
            analizarDefine((NodoAST.NodoDefine) nodo);
        } else if (nodo instanceof NodoAST.NodoAsignacion) {
            analizarAsignacion((NodoAST.NodoAsignacion) nodo);
        } else if (nodo instanceof NodoAST.NodoIf) {
            analizarIf((NodoAST.NodoIf) nodo);
        } else if (nodo instanceof NodoAST.NodoLoop) {
            analizarLoop((NodoAST.NodoLoop) nodo);
        } else if (nodo instanceof NodoAST.NodoSelect) {
            analizarSelect((NodoAST.NodoSelect) nodo);
        } else if (nodo instanceof NodoAST.NodoOut) {
            analizarOut((NodoAST.NodoOut) nodo);
        } else if (nodo instanceof NodoAST.NodoInput) {
            analizarInput((NodoAST.NodoInput) nodo);
        } else if (nodo instanceof NodoAST.NodoIncrementoPostfijo) {
            analizarIncrementoPostfijo((NodoAST.NodoIncrementoPostfijo) nodo);
        } else if (nodo instanceof NodoAST.NodoObject) {
            analizarObject((NodoAST.NodoObject) nodo);
        } else if (nodo instanceof NodoAST.NodoMethod) {
            analizarMethod((NodoAST.NodoMethod) nodo);
        }
        // Los nodos de expresion pura son manejados por analizarExpresion
    }

    private void analizarDefine(NodoAST.NodoDefine nodo) {
        EntradaTablaSimbolos.TipoDato tipo = EntradaTablaSimbolos.parsearTipo(nodo.tipo);
        if (tipo == EntradaTablaSimbolos.TipoDato.DESCONOCIDO) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": tipo de dato desconocido '" + nodo.tipo + "'.");
        }

        EntradaTablaSimbolos entrada = new EntradaTablaSimbolos(
            nodo.identificador, tipo,
            EntradaTablaSimbolos.CategoriaEntrada.VARIABLE, nodo.getLinea()
        );

        boolean exitoso = tablaSimbolos.declarar(entrada);
        if (!exitoso) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' ya fue declarada en este ambito.");
            return;
        }

        // Registrar en el reporte global
        registroGlobal.add(entrada);

        if (nodo.expresion != null) {
            EntradaTablaSimbolos.TipoDato tipoExpr = inferirTipo(nodo.expresion);
            if (!sonCompatibles(tipo, tipoExpr)) {
                // Coercion inteligente: int -> dec es valida en JODA
                if (tipo == EntradaTablaSimbolos.TipoDato.DEC
                        && tipoExpr == EntradaTablaSimbolos.TipoDato.INT) {
                    advertencias.add("Advertencia en linea " + nodo.getLinea()
                        + ": promocion automatica de 'int' a 'dec' para la variable '"
                        + nodo.identificador + "'.");
                } else {
                    errores.add("Error semantico en linea " + nodo.getLinea()
                        + ": tipo incompatible. La variable '" + nodo.identificador
                        + "' es de tipo '" + nodo.tipo + "' pero se asigna un valor de tipo '"
                        + tipoExpr.name().toLowerCase() + "'.");
                }
            }
        } else {
            advertencias.add("Advertencia en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' se declara sin valor inicial.");
        }
    }

    private void analizarAsignacion(NodoAST.NodoAsignacion nodo) {
        EntradaTablaSimbolos entrada = tablaSimbolos.buscar(nodo.identificador);
        if (entrada == null) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' no fue declarada.");
            return;
        }

        EntradaTablaSimbolos.TipoDato tipoExpr = inferirTipo(nodo.expresion);
        if (!sonCompatibles(entrada.getTipoDato(), tipoExpr)) {
            if (entrada.getTipoDato() == EntradaTablaSimbolos.TipoDato.DEC
                    && tipoExpr == EntradaTablaSimbolos.TipoDato.INT) {
                advertencias.add("Advertencia en linea " + nodo.getLinea()
                    + ": promocion de 'int' a 'dec' en asignacion a '" + nodo.identificador + "'.");
            } else {
                errores.add("Error semantico en linea " + nodo.getLinea()
                    + ": tipo incompatible al asignar a '" + nodo.identificador + "'.");
            }
        }
    }

    private void analizarIf(NodoAST.NodoIf nodo) {
        analizarExpresion(nodo.condicion);
        analizarBloque(nodo.bloqueThen);
        if (nodo.bloqueElse != null) {
            analizarBloque(nodo.bloqueElse);
        }
    }

    private void analizarLoop(NodoAST.NodoLoop nodo) {
        analizarExpresion(nodo.condicion);
        analizarBloque(nodo.cuerpo);
    }

    private void analizarSelect(NodoAST.NodoSelect nodo) {
        analizarExpresion(nodo.variable);
        for (NodoAST.NodoCaso caso : nodo.casos) {
            analizarExpresion(caso.valor);
            analizarBloque(caso.instrucciones);
        }
    }

    private void analizarOut(NodoAST.NodoOut nodo) {
        analizarExpresion(nodo.expresion);
    }

    private void analizarInput(NodoAST.NodoInput nodo) {
        EntradaTablaSimbolos entrada = tablaSimbolos.buscar(nodo.identificador);
        if (entrada == null) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' no fue declarada.");
        }
    }

    private void analizarIncrementoPostfijo(NodoAST.NodoIncrementoPostfijo nodo) {
        EntradaTablaSimbolos entrada = tablaSimbolos.buscar(nodo.identificador);
        if (entrada == null) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": la variable '" + nodo.identificador + "' no fue declarada.");
            return;
        }
        if (entrada.getTipoDato() != EntradaTablaSimbolos.TipoDato.INT
                && entrada.getTipoDato() != EntradaTablaSimbolos.TipoDato.DEC) {
            errores.add("Error semantico en linea " + nodo.getLinea()
                + ": el operador '" + nodo.operador + "' solo aplica a tipos numericos.");
        }
    }

    private void analizarObject(NodoAST.NodoObject nodo) {
        EntradaTablaSimbolos entrada = new EntradaTablaSimbolos(
            nodo.nombre, EntradaTablaSimbolos.TipoDato.OBJECT,
            EntradaTablaSimbolos.CategoriaEntrada.CLASE, nodo.getLinea()
        );
        tablaSimbolos.declarar(entrada);
        analizarBloque(nodo.miembros);
    }

    private void analizarMethod(NodoAST.NodoMethod nodo) {
        EntradaTablaSimbolos entrada = new EntradaTablaSimbolos(
            nodo.nombre, EntradaTablaSimbolos.TipoDato.VOID,
            EntradaTablaSimbolos.CategoriaEntrada.METODO, nodo.getLinea()
        );
        tablaSimbolos.declarar(entrada);
        analizarBloque(nodo.cuerpo);
    }

    private void analizarExpresion(NodoAST nodo) {
        if (nodo == null) return;

        if (nodo instanceof NodoAST.NodoIdentificador) {
            String nombre = ((NodoAST.NodoIdentificador) nodo).nombre;
            if (tablaSimbolos.buscar(nombre) == null) {
                errores.add("Error semantico en linea " + nodo.getLinea()
                    + ": el identificador '" + nombre + "' no fue declarado.");
            }
        } else if (nodo instanceof NodoAST.NodoBinario) {
            NodoAST.NodoBinario bin = (NodoAST.NodoBinario) nodo;
            analizarExpresion(bin.izquierda);
            analizarExpresion(bin.derecha);
        } else if (nodo instanceof NodoAST.NodoUnario) {
            analizarExpresion(((NodoAST.NodoUnario) nodo).operando);
        }
        // Los literales no requieren validacion adicional
    }

    // Inferencia de tipos
    private EntradaTablaSimbolos.TipoDato inferirTipo(NodoAST nodo) {
        if (nodo instanceof NodoAST.NodoLiteralEntero)  return EntradaTablaSimbolos.TipoDato.INT;
        if (nodo instanceof NodoAST.NodoLiteralDecimal) return EntradaTablaSimbolos.TipoDato.DEC;
        if (nodo instanceof NodoAST.NodoLiteralCadena)  return EntradaTablaSimbolos.TipoDato.STRING;
        if (nodo instanceof NodoAST.NodoLiteralBool)    return EntradaTablaSimbolos.TipoDato.BOOL;

        if (nodo instanceof NodoAST.NodoIdentificador) {
            EntradaTablaSimbolos e = tablaSimbolos.buscar(((NodoAST.NodoIdentificador) nodo).nombre);
            return (e != null) ? e.getTipoDato() : EntradaTablaSimbolos.TipoDato.DESCONOCIDO;
        }

        if (nodo instanceof NodoAST.NodoBinario) {
            NodoAST.NodoBinario bin = (NodoAST.NodoBinario) nodo;
            String op = bin.operador;

            // Operadores relacionales y logicos devuelven bool
            if (op.equals("==") || op.equals("!=") || op.equals(">") || op.equals("<")
                    || op.equals(">=") || op.equals("<=") || op.equals("&&") || op.equals("||")) {
                return EntradaTablaSimbolos.TipoDato.BOOL;
            }

            // '+' con cadenas retorna string (Coercion Inteligente de JODA)
            EntradaTablaSimbolos.TipoDato tipoIzq = inferirTipo(bin.izquierda);
            EntradaTablaSimbolos.TipoDato tipoDer = inferirTipo(bin.derecha);
            if (tipoIzq == EntradaTablaSimbolos.TipoDato.STRING
                    || tipoDer == EntradaTablaSimbolos.TipoDato.STRING) {
                return EntradaTablaSimbolos.TipoDato.STRING;
            }

            // Si alguno es dec, el resultado es dec (promocion)
            if (tipoIzq == EntradaTablaSimbolos.TipoDato.DEC
                    || tipoDer == EntradaTablaSimbolos.TipoDato.DEC) {
                return EntradaTablaSimbolos.TipoDato.DEC;
            }

            return tipoIzq;
        }

        if (nodo instanceof NodoAST.NodoUnario) {
            return inferirTipo(((NodoAST.NodoUnario) nodo).operando);
        }

        return EntradaTablaSimbolos.TipoDato.DESCONOCIDO;
    }

    private boolean sonCompatibles(EntradaTablaSimbolos.TipoDato esperado,
                                    EntradaTablaSimbolos.TipoDato actual) {
        if (esperado == actual) return true;
        if (esperado == EntradaTablaSimbolos.TipoDato.DESCONOCIDO) return true;
        if (actual == EntradaTablaSimbolos.TipoDato.DESCONOCIDO) return true;
        // Coercion: int es compatible donde se espera dec (promocion)
        if (esperado == EntradaTablaSimbolos.TipoDato.DEC
                && actual == EntradaTablaSimbolos.TipoDato.INT) return true;
        // Coercion inteligente: cualquier tipo + string = string
        if (esperado == EntradaTablaSimbolos.TipoDato.STRING) return true;
        return false;
    }

    // Acceso a resultados
    public TablaSimbolos getTablaSimbolos() { return tablaSimbolos; }
    public List<String> getErrores() { return errores; }
    public List<String> getAdvertencias() { return advertencias; }
    public boolean tieneErrores() { return !errores.isEmpty(); }
}
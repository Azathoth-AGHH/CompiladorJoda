package logica.nucleo;

import java.util.List;
import java.util.Scanner;

import logica.semantico.EntradaTablaSimbolos;
import logica.semantico.TablaSimbolos;
import logica.sintactico.NodoAST;

/*
Ejecutor (Interprete) del lenguaje JODA.
Actua como la JVM-J: recorre el AST y ejecuta cada nodo evaluando
expresiones, controlando el flujo y gestionando la tabla de simbolos.
Equivale a la fase de interpretacion del modelo hibrido.
*/
public class EjecutorJoda {

    private final TablaSimbolos memoria;
    private final List<String> salidasConsola;
    private final Scanner scanner;
    public EjecutorJoda(TablaSimbolos memoria, List<String> salidasConsola) {
        this.memoria = memoria;
        this.salidasConsola = salidasConsola;
        this.scanner = new Scanner(System.in);
    }

    // Punto de entrada de ejecucion
    public void ejecutar(NodoAST.NodoEntry nodoEntry) {
        if (nodoEntry == null) return;
        ejecutarBloque(nodoEntry.instrucciones);
    }

    // Ejecucion de bloques e instrucciones
    private void ejecutarBloque(List<NodoAST> instrucciones) {
        memoria.abrirAmbito();
        for (NodoAST instruccion : instrucciones) {
            ejecutarNodo(instruccion);
        }
        memoria.cerrarAmbito();
    }

    private void ejecutarNodo(NodoAST nodo) {
        if (nodo == null) return;

        if (nodo instanceof NodoAST.NodoDefine) {
            ejecutarDefine((NodoAST.NodoDefine) nodo);
        } else if (nodo instanceof NodoAST.NodoAsignacion) {
            ejecutarAsignacion((NodoAST.NodoAsignacion) nodo);
        } else if (nodo instanceof NodoAST.NodoIf) {
            ejecutarIf((NodoAST.NodoIf) nodo);
        } else if (nodo instanceof NodoAST.NodoLoop) {
            ejecutarLoop((NodoAST.NodoLoop) nodo);
        } else if (nodo instanceof NodoAST.NodoSelect) {
            ejecutarSelect((NodoAST.NodoSelect) nodo);
        } else if (nodo instanceof NodoAST.NodoOut) {
            ejecutarOut((NodoAST.NodoOut) nodo);
        } else if (nodo instanceof NodoAST.NodoInput) {
            ejecutarInput((NodoAST.NodoInput) nodo);
        } else if (nodo instanceof NodoAST.NodoIncrementoPostfijo) {
            ejecutarIncrementoPostfijo((NodoAST.NodoIncrementoPostfijo) nodo);
        } else if (nodo instanceof NodoAST.NodoObject) {
            // Los objetos se registran pero no se ejecutan de forma inmediata
        } else if (nodo instanceof NodoAST.NodoMethod) {
            // Los metodos se registran pero no se invocan (soporte futuro)
        }
    }

    // Ejecucion de instrucciones especificas
    private void ejecutarDefine(NodoAST.NodoDefine nodo) {
        EntradaTablaSimbolos.TipoDato tipo = EntradaTablaSimbolos.parsearTipo(nodo.tipo);
        EntradaTablaSimbolos entrada = new EntradaTablaSimbolos(
            nodo.identificador, tipo,
            EntradaTablaSimbolos.CategoriaEntrada.VARIABLE, nodo.getLinea()
        );

        Object valor = null;
        if (nodo.expresion != null) {
            valor = evaluarExpresion(nodo.expresion);
            // Coercion inteligente: int -> dec si el tipo declarado es dec
            if (tipo == EntradaTablaSimbolos.TipoDato.DEC && valor instanceof Integer) {
                valor = ((Integer) valor).doubleValue();
            }
        }

        entrada.setValor(valor);
        memoria.declarar(entrada);
    }

    private void ejecutarAsignacion(NodoAST.NodoAsignacion nodo) {
        Object valor = evaluarExpresion(nodo.expresion);
        EntradaTablaSimbolos entrada = memoria.buscar(nodo.identificador);

        if (entrada != null) {
            // Coercion inteligente
            if (entrada.getTipoDato() == EntradaTablaSimbolos.TipoDato.DEC && valor instanceof Integer) {
                valor = ((Integer) valor).doubleValue();
            }
        }

        boolean exito = memoria.asignar(nodo.identificador, valor);
        if (!exito) {
            // No deberia llegar aqui si el analisis semantico fue exitoso
            agregarSalida("[ERROR] Variable no declarada: " + nodo.identificador);
        }
    }

    private void ejecutarIf(NodoAST.NodoIf nodo) {
        Object condicion = evaluarExpresion(nodo.condicion);
        if (esVerdadero(condicion)) {
            ejecutarBloque(nodo.bloqueThen);
        } else if (nodo.bloqueElse != null) {
            ejecutarBloque(nodo.bloqueElse);
        }
    }

    private void ejecutarLoop(NodoAST.NodoLoop nodo) {
        int iteraciones = 0;
        final int MAX_ITERACIONES = 100_000; // Proteccion contra bucles infinitos

        while (esVerdadero(evaluarExpresion(nodo.condicion))) {
            ejecutarBloque(nodo.cuerpo);
            iteraciones++;
            if (iteraciones > MAX_ITERACIONES) {
                agregarSalida("[ERROR JVM-J] Limite de iteraciones superado. Posible bucle infinito.");
                break;
            }
        }
    }

    private void ejecutarSelect(NodoAST.NodoSelect nodo) {
        Object valorVariable = evaluarExpresion(nodo.variable);
        for (NodoAST.NodoCaso caso : nodo.casos) {
            Object valorCaso = evaluarExpresion(caso.valor);
            if (sonIguales(valorVariable, valorCaso)) {
                ejecutarBloque(caso.instrucciones);
                break; // break implicito como en JODA spec
            }
        }
    }

    private void ejecutarOut(NodoAST.NodoOut nodo) {
        Object valor = evaluarExpresion(nodo.expresion);
        String salida = objetoAString(valor);
        agregarSalida(salida);
    }

    private void ejecutarInput(NodoAST.NodoInput nodo) {
        // En modo automatico (sin terminal interactivo), usamos valor por defecto
        agregarSalida("[JVM-J] Esperando entrada para '" + nodo.identificador + "': ");
        String entrada = "0"; // Valor predeterminado para ejecucion no interactiva

        EntradaTablaSimbolos simbolo = memoria.buscar(nodo.identificador);
        if (simbolo != null) {
            Object valorParsed = parsearEntrada(entrada, simbolo.getTipoDato());
            memoria.asignar(nodo.identificador, valorParsed);
        }
    }

    private void ejecutarIncrementoPostfijo(NodoAST.NodoIncrementoPostfijo nodo) {
        EntradaTablaSimbolos entrada = memoria.buscar(nodo.identificador);
        if (entrada == null) return;

        Object valorActual = entrada.getValor();
        Object nuevoValor;

        if (valorActual instanceof Integer) {
            int v = (Integer) valorActual;
            nuevoValor = nodo.operador.equals("++") ? v + 1 : v - 1;
        } else if (valorActual instanceof Double) {
            double v = (Double) valorActual;
            nuevoValor = nodo.operador.equals("++") ? v + 1.0 : v - 1.0;
        } else {
            return;
        }
        memoria.asignar(nodo.identificador, nuevoValor);
    }

    // Evaluacion de expresiones
    Object evaluarExpresion(NodoAST nodo) {
        if (nodo == null) return null;

        if (nodo instanceof NodoAST.NodoLiteralEntero) {
            return ((NodoAST.NodoLiteralEntero) nodo).valor;
        }
        if (nodo instanceof NodoAST.NodoLiteralDecimal) {
            return ((NodoAST.NodoLiteralDecimal) nodo).valor;
        }
        if (nodo instanceof NodoAST.NodoLiteralCadena) {
            return ((NodoAST.NodoLiteralCadena) nodo).valor;
        }
        if (nodo instanceof NodoAST.NodoLiteralBool) {
            return ((NodoAST.NodoLiteralBool) nodo).valor;
        }
        if (nodo instanceof NodoAST.NodoIdentificador) {
            EntradaTablaSimbolos entrada = memoria.buscar(((NodoAST.NodoIdentificador) nodo).nombre);
            return (entrada != null) ? entrada.getValor() : null;
        }
        if (nodo instanceof NodoAST.NodoBinario) {
            return evaluarBinario((NodoAST.NodoBinario) nodo);
        }
        if (nodo instanceof NodoAST.NodoUnario) {
            return evaluarUnario((NodoAST.NodoUnario) nodo);
        }
        return null;
    }

    private Object evaluarBinario(NodoAST.NodoBinario nodo) {
        Object izq = evaluarExpresion(nodo.izquierda);
        Object der = evaluarExpresion(nodo.derecha);
        String op = nodo.operador;

        // Coercion Inteligente: si alguno es string, concatenar
        if ((izq instanceof String || der instanceof String)
                && (op.equals("+"))) {
            return objetoAString(izq) + objetoAString(der);
        }

        // Operadores logicos
        if (op.equals("&&")) return esVerdadero(izq) && esVerdadero(der);
        if (op.equals("||")) return esVerdadero(izq) || esVerdadero(der);

        // Operadores relacionales sobre cualquier tipo
        if (op.equals("==")) return sonIguales(izq, der);
        if (op.equals("!=")) return !sonIguales(izq, der);

        // Operaciones numericas
        double vi = toDouble(izq);
        double vd = toDouble(der);

        switch (op) {
            case "+":  return esEnteros(izq, der) ? (int)(vi + vd) : (vi + vd);
            case "-":  return esEnteros(izq, der) ? (int)(vi - vd) : (vi - vd);
            case "*":  return esEnteros(izq, der) ? (int)(vi * vd) : (vi * vd);
            case "/":  return vd != 0 ? vi / vd : (agregarSalida("[ERROR JVM-J] Division por cero.") == null ? 0.0 : 0.0);
            case "%":  return vd != 0 ? (int)(vi % vd) : 0;
            case ">":  return vi > vd;
            case "<":  return vi < vd;
            case ">=": return vi >= vd;
            case "<=": return vi <= vd;
            default:
                return null;
        }
    }

    private Object evaluarUnario(NodoAST.NodoUnario nodo) {
        Object operando = evaluarExpresion(nodo.operando);
        switch (nodo.operador) {
            case "!": return !esVerdadero(operando);
            case "-":
                if (operando instanceof Integer) return -(Integer) operando;
                if (operando instanceof Double)  return -(Double) operando;
                return null;
            default:  return operando;
        }
    }

    // Metodos utilitarios de la JVM-J
    private boolean esVerdadero(Object valor) {
        if (valor instanceof Boolean) return (Boolean) valor;
        if (valor instanceof Integer) return (Integer) valor != 0;
        if (valor instanceof Double)  return (Double) valor != 0.0;
        if (valor instanceof String)  return !((String) valor).isEmpty();
        return false;
    }

    private boolean sonIguales(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        // Comparacion numerica con promocion
        if ((a instanceof Number) && (b instanceof Number)) {
            return toDouble(a) == toDouble(b);
        }
        return a.equals(b);
    }

    private double toDouble(Object valor) {
        if (valor instanceof Integer) return ((Integer) valor).doubleValue();
        if (valor instanceof Double)  return (Double) valor;
        if (valor instanceof Boolean) return ((Boolean) valor) ? 1.0 : 0.0;
        return 0.0;
    }

    private boolean esEnteros(Object a, Object b) {
        return (a instanceof Integer) && (b instanceof Integer);
    }

    private String objetoAString(Object valor) {
        if (valor == null)           return "null";
        if (valor instanceof Double) {
            double d = (Double) valor;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        return String.valueOf(valor);
    }

    private Object parsearEntrada(String texto, EntradaTablaSimbolos.TipoDato tipo) {
        try {
            switch (tipo) {
                case INT:    return Integer.parseInt(texto.trim());
                case DEC:    return Double.parseDouble(texto.trim());
                case BOOL:   return Boolean.parseBoolean(texto.trim());
                default:     return texto;
            }
        } catch (NumberFormatException e) {
            return texto;
        }
    }

    private Object agregarSalida(String mensaje) {
        salidasConsola.add(mensaje);
        return null;
    }
}
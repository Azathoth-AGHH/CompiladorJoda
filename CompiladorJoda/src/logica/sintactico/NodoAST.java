package logica.sintactico;

import java.util.List;

/*
Jerarquia de nodos para el Arbol de Sintaxis Abstracta (AST) de JODA.
Cada clase interna representa un tipo de construccion del lenguaje.
*/
public abstract class NodoAST {

    // Linea de origen en el codigo fuente (util para mensajes de error)
    protected int linea;

    public int getLinea() { return linea; }

    // NODO RAIZ DEL PROGRAMA
    // Nodo que representa el bloque principal 'entry { ... }'.
    public static class NodoEntry extends NodoAST {
        public final List<NodoAST> instrucciones;

        public NodoEntry(List<NodoAST> instrucciones, int linea) {
            this.instrucciones = instrucciones;
            this.linea = linea;
        }
    }

    // NODOS DE DECLARACION
    //Nodo para 'define tipo identificador = expresion;'
    public static class NodoDefine extends NodoAST {
        public final String tipo;
        public final String identificador;
        public final NodoAST expresion; // puede ser null si no hay valor inicial

        public NodoDefine(String tipo, String identificador, NodoAST expresion, int linea) {
            this.tipo = tipo;
            this.identificador = identificador;
            this.expresion = expresion;
            this.linea = linea;
        }
    }

    // NODOS DE CONTROL DE FLUJO

    //Nodo para 'if (condicion) { ... } else { ... }'
    public static class NodoIf extends NodoAST {
        public final NodoAST condicion;
        public final List<NodoAST> bloqueThen;
        public final List<NodoAST> bloqueElse; // puede ser null

        public NodoIf(NodoAST condicion, List<NodoAST> bloqueThen, List<NodoAST> bloqueElse, int linea) {
            this.condicion = condicion;
            this.bloqueThen = bloqueThen;
            this.bloqueElse = bloqueElse;
            this.linea = linea;
        }
    }

    //Nodo para 'loop (condicion) { ... }'
    public static class NodoLoop extends NodoAST {
        public final NodoAST condicion;
        public final List<NodoAST> cuerpo;

        public NodoLoop(NodoAST condicion, List<NodoAST> cuerpo, int linea) {
            this.condicion = condicion;
            this.cuerpo = cuerpo;
            this.linea = linea;
        }
    }

    // Nodo para 'select (variable) { case valor: instrucciones }'
    public static class NodoSelect extends NodoAST {
        public final NodoAST variable;
        public final List<NodoCaso> casos;

        public NodoSelect(NodoAST variable, List<NodoCaso> casos, int linea) {
            this.variable = variable;
            this.casos = casos;
            this.linea = linea;
        }
    }

    //Nodo para un 'case valor: instrucciones' dentro de select.
    public static class NodoCaso extends NodoAST {
        public final NodoAST valor;
        public final List<NodoAST> instrucciones;

        public NodoCaso(NodoAST valor, List<NodoAST> instrucciones, int linea) {
            this.valor = valor;
            this.instrucciones = instrucciones;
            this.linea = linea;
        }
    }

    // NODOS DE ENTRADA/SALIDA

    //Nodo para 'out(expresion);'
    public static class NodoOut extends NodoAST {
        public final NodoAST expresion;

        public NodoOut(NodoAST expresion, int linea) {
            this.expresion = expresion;
            this.linea = linea;
        }
    }

    //Nodo para 'input(variable);'
    public static class NodoInput extends NodoAST {
        public final String identificador;

        public NodoInput(String identificador, int linea) {
            this.identificador = identificador;
            this.linea = linea;
        }
    }

    // NODOS DE EXPRESION

    //Nodo para asignacion: 'variable = expresion;'
    public static class NodoAsignacion extends NodoAST {
        public final String identificador;
        public final NodoAST expresion;

        public NodoAsignacion(String identificador, NodoAST expresion, int linea) {
            this.identificador = identificador;
            this.expresion = expresion;
            this.linea = linea;
        }
    }

    //Nodo para operaciones binarias: expresion op expresion
    public static class NodoBinario extends NodoAST {
        public final NodoAST izquierda;
        public final String operador;
        public final NodoAST derecha;

        public NodoBinario(NodoAST izquierda, String operador, NodoAST derecha, int linea) {
            this.izquierda = izquierda;
            this.operador = operador;
            this.derecha = derecha;
            this.linea = linea;
        }
    }

    //Nodo para operaciones unarias: op expresion (ej: !variable, ++)
    public static class NodoUnario extends NodoAST {
        public final String operador;
        public final NodoAST operando;

        public NodoUnario(String operador, NodoAST operando, int linea) {
            this.operador = operador;
            this.operando = operando;
            this.linea = linea;
        }
    }

    //Nodo para incremento/decremento postfijo: variable++  variable--
    public static class NodoIncrementoPostfijo extends NodoAST {
        public final String identificador;
        public final String operador; // "++" o "--"

        public NodoIncrementoPostfijo(String identificador, String operador, int linea) {
            this.identificador = identificador;
            this.operador = operador;
            this.linea = linea;
        }
    }

    // NODOS DE LITERALES
    public static class NodoLiteralEntero extends NodoAST {
        public final int valor;

        public NodoLiteralEntero(int valor, int linea) {
            this.valor = valor;
            this.linea = linea;
        }
    }

    public static class NodoLiteralDecimal extends NodoAST {
        public final double valor;

        public NodoLiteralDecimal(double valor, int linea) {
            this.valor = valor;
            this.linea = linea;
        }
    }

    public static class NodoLiteralCadena extends NodoAST {
        public final String valor;

        public NodoLiteralCadena(String valor, int linea) {
            this.valor = valor;
            this.linea = linea;
        }
    }

    public static class NodoLiteralBool extends NodoAST {
        public final boolean valor;

        public NodoLiteralBool(boolean valor, int linea) {
            this.valor = valor;
            this.linea = linea;
        }
    }

    //Nodo para referencias a variables: solo su nombre.
    public static class NodoIdentificador extends NodoAST {
        public final String nombre;

        public NodoIdentificador(String nombre, int linea) {
            this.nombre = nombre;
            this.linea = linea;
        }
    }

    // NODOS OOP

    //Nodo para definicion de clase: object Nombre { ... }
    public static class NodoObject extends NodoAST {
        public final String nombre;
        public final List<NodoAST> miembros;

        public NodoObject(String nombre, List<NodoAST> miembros, int linea) {
            this.nombre = nombre;
            this.miembros = miembros;
            this.linea = linea;
        }
    }

    //Nodo para declaracion de metodo: method Nombre(params) { ... }
    public static class NodoMethod extends NodoAST {
        public final String nombre;
        public final List<NodoAST> cuerpo;

        public NodoMethod(String nombre, List<NodoAST> cuerpo, int linea) {
            this.nombre = nombre;
            this.cuerpo = cuerpo;
            this.linea = linea;
        }
    }
}
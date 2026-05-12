package logica.semantico;

import java.util.*;

/*
Tabla de Simbolos con soporte de ambitos (scopes) anidados.
Implementa una pila de mapas para gestionar variables locales y globales.
*/
public class TablaSimbolos {

    // Pila de ambitos: cada nivel es un mapa de nombre -> entrada
    private final Deque<Map<String, EntradaTablaSimbolos>> pilaAmbitos;

    public TablaSimbolos() {
        pilaAmbitos = new ArrayDeque<>();
        // Crear el ambito global inicial
        pilaAmbitos.push(new LinkedHashMap<>());
    }

    // Abre un nuevo ambito (al entrar a un bloque { } )
    public void abrirAmbito() {
        pilaAmbitos.push(new LinkedHashMap<>());
    }

    // Cierra el ambito actual (al salir de un bloque { } )
    public void cerrarAmbito() {
        if (pilaAmbitos.size() > 1) {
            pilaAmbitos.pop();
        }
    }

    //Declara un nuevo simbolo en el ambito actual.
    public boolean declarar(EntradaTablaSimbolos entrada) {
        Map<String, EntradaTablaSimbolos> ambitoActual = pilaAmbitos.peek();
        if (ambitoActual != null && ambitoActual.containsKey(entrada.getNombre())) {
            return false; // Redeclaracion en el mismo ambito
        }
        if (ambitoActual != null) {
            ambitoActual.put(entrada.getNombre(), entrada);
        }
        return true;
    }

    // Busca un simbolo recorriendo los ambitos desde el mas interno al mas externo.
    public EntradaTablaSimbolos buscar(String nombre) {
        for (Map<String, EntradaTablaSimbolos> ambito : pilaAmbitos) {
            if (ambito.containsKey(nombre)) {
                return ambito.get(nombre);
            }
        }
        return null;
    }

    //Asigna un valor a un simbolo ya declarado (buscando en todos los ambitos).
    public boolean asignar(String nombre, Object valor) {
        for (Map<String, EntradaTablaSimbolos> ambito : pilaAmbitos) {
            if (ambito.containsKey(nombre)) {
                ambito.get(nombre).setValor(valor);
                return true;
            }
        }
        return false;
    }

    //Retorna todas las entradas de todos los ambitos (para reporte).
    public List<EntradaTablaSimbolos> obtenerTodasLasEntradas() {
        List<EntradaTablaSimbolos> todas = new ArrayList<>();
        // Recorremos desde el ambito mas externo al mas interno
        List<Map<String, EntradaTablaSimbolos>> lista = new ArrayList<>(pilaAmbitos);
        Collections.reverse(lista);
        for (Map<String, EntradaTablaSimbolos> ambito : lista) {
            todas.addAll(ambito.values());
        }
        return todas;
    }

    public int getNivelAmbito() {
        return pilaAmbitos.size() - 1;
    }
}
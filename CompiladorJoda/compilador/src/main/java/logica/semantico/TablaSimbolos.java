package logica.semantico;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TablaSimbolos {

    private final Deque<Map<String, EntradaTablaSimbolos>> pilaAmbitos = new ArrayDeque<>();

    public TablaSimbolos() {
        pilaAmbitos.push(new LinkedHashMap<>());
    }

    public void abrirAmbito()  { pilaAmbitos.push(new LinkedHashMap<>()); }

    public void cerrarAmbito() {
        if (pilaAmbitos.size() > 1) pilaAmbitos.pop();
    }

    public boolean declarar(EntradaTablaSimbolos entrada) {
        Map<String, EntradaTablaSimbolos> actual = pilaAmbitos.peek();
        if (actual == null || actual.containsKey(entrada.getNombre())) return false;
        actual.put(entrada.getNombre(), entrada);
        return true;
    }

    public EntradaTablaSimbolos buscar(String nombre) {
        for (Map<String, EntradaTablaSimbolos> ambito : pilaAmbitos) {
            EntradaTablaSimbolos e = ambito.get(nombre);
            if (e != null) return e;
        }
        return null;
    }

    public boolean asignar(String nombre, Object valor) {
        for (Map<String, EntradaTablaSimbolos> ambito : pilaAmbitos) {
            EntradaTablaSimbolos e = ambito.get(nombre);
            if (e != null) { e.setValor(valor); return true; }
        }
        return false;
    }

    public List<EntradaTablaSimbolos> obtenerTodasLasEntradas() {
        List<EntradaTablaSimbolos> todas = new ArrayList<>();
        List<Map<String, EntradaTablaSimbolos>> lista = new ArrayList<>(pilaAmbitos);
        java.util.Collections.reverse(lista);
        for (Map<String, EntradaTablaSimbolos> ambito : lista) todas.addAll(ambito.values());
        return todas;
    }

    public int getNivelAmbito() { return pilaAmbitos.size() - 1; }
}
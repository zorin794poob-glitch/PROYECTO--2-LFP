package promptzal.semantic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tabla de símbolos: agentes (ámbito global) y variables (ámbito = agente). */
public class TablaSimbolos {
    private final List<Simbolo> simbolos = new ArrayList<>();
    /** ámbito -> (nombre -> símbolo), respetando el orden de declaración. */
    private final Map<String, Map<String, Simbolo>> porAmbito = new LinkedHashMap<>();

    public static final String GLOBAL = "global";

    public boolean declarar(Simbolo s) {
        Map<String, Simbolo> mapa = porAmbito.computeIfAbsent(s.ambito, k -> new LinkedHashMap<>());
        if (mapa.containsKey(s.nombre)) return false;
        mapa.put(s.nombre, s);
        simbolos.add(s);
        return true;
    }

    public Simbolo buscar(String ambito, String nombre) {
        Map<String, Simbolo> m = porAmbito.get(ambito);
        return m == null ? null : m.get(nombre);
    }

    public boolean existe(String ambito, String nombre) { return buscar(ambito, nombre) != null; }

    public List<Simbolo> getSimbolos() { return simbolos; }
}

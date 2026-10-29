package promptzal.semantic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import promptzal.ast.Ast.*;
import promptzal.util.ErrorCompilacion;

public class AnalizadorSemantico {

    private final TablaSimbolos tabla = new TablaSimbolos();
    private final List<ErrorCompilacion> errores = new ArrayList<>();

    public TablaSimbolos getTabla() {
        return tabla;
    }

    public List<ErrorCompilacion> getErrores() {
        return errores;
    }

    public void analizar(Programa prog) {
        // 1) agentes: nombres únicos
        Map<String, Agente> agentesPorNombre = new LinkedHashMap<>();
        for (Agente a : prog.agentes) {
            if (a.nombre.startsWith("<")) {
                continue;            
            }
            Agente previo = agentesPorNombre.get(a.nombre);
            if (previo != null) {
                errores.add(ErrorCompilacion.semantico("Agente '" + a.nombre
                        + "' duplicado (ya declarado en la línea " + previo.fila + ")", a.fila, a.columna));
            } else {
                agentesPorNombre.put(a.nombre, a);
                tabla.declarar(new Simbolo(a.nombre, Simbolo.Tipo.AGENTE, TablaSimbolos.GLOBAL, a.fila));
            }
        }

        // 2) cuerpo de cada agente (ámbito propio)
        Map<Agente, Set<String>> visibles = new IdentityHashMap<>();
        for (Agente a : prog.agentes) {
            Set<String> vars = new HashSet<>();
            visibles.put(a, vars);
            boolean registrar = agentesPorNombre.get(a.nombre) == a;    
            Set<String> declaradasConVariable = new HashSet<>();

            for (VariableDecl v : a.variables) {
                validarExpresion(v.valor, vars, a, false);
                if (!declaradasConVariable.add(v.nombre)) {
                    errores.add(ErrorCompilacion.semantico("La variable '" + v.nombre
                            + "' ya fue declarada en el agente '" + a.nombre + "'", v.fila, v.columna));
                }
                vars.add(v.nombre);
                if (registrar) {
                    tabla.declarar(new Simbolo(v.nombre, Simbolo.Tipo.VARIABLE_DECLARADA, a.nombre, v.fila));
                }
            }

            for (Comando c : a.comandos) {
                validarExpresion(c.argumento, vars, a, c.tipo.equals("GENERAR"));
                for (Conector k : c.conectores) {
                    validarExpresion(k.expresion, vars, a, false);
                }
                if (c.destino != null) {
                    vars.add(c.destino);
                    if (registrar) {
                        tabla.declarar(new Simbolo(c.destino, Simbolo.Tipo.RESULTADO_COMANDO,
                                a.nombre, c.destinoFila));
                    }
                }
            }
        }

        // 3) EJECUTAR
        Set<Agente> ejecutados = new HashSet<>();
        for (Referencia r : prog.ejecuciones) {
            Agente a = agentesPorNombre.get(r.nombre);
            if (a == null) {
                errores.add(ErrorCompilacion.semantico("EJECUTAR hace referencia al agente '" + r.nombre
                        + "', que no existe", r.fila, r.columna));
            } else {
                ejecutados.add(a);
            }
        }

        // 4) EXPORTAR
        for (Referencia r : prog.exportaciones) {
            boolean existe = false;
            for (Agente a : ejecutados) {
                if (visibles.get(a).contains(r.nombre)) {
                    existe = true;
                    break;
                }
            }
            if (!existe) {
                errores.add(ErrorCompilacion.semantico("EXPORTAR hace referencia a '" + r.nombre
                        + "', que no existe en ningún agente ejecutado", r.fila, r.columna));
            }
        }
    }

    private void validarExpresion(Expresion e, Set<String> vars, Agente a, boolean esGenerar) {
        if (e == null) {
            return;
        }
        for (int i = 0; i < e.terminos.size(); i++) {
            Termino t = e.terminos.get(i);
            if (t.clase != Termino.Clase.ID) {
                continue;
            }
            if (esGenerar && i == 0) {
                continue;              
            }
            if (!vars.contains(t.valor)) {
                errores.add(ErrorCompilacion.semantico("El identificador '" + t.valor
                        + "' no está declarado en el agente '" + a.nombre + "' antes de su uso", t.fila, t.columna));
            }
        }
    }
}

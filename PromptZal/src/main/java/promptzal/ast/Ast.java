package promptzal.ast;

import java.util.ArrayList;
import java.util.List;

/** Nodos del árbol de sintaxis abstracta (AST) de PromptZal. */
public final class Ast {
    private Ast() { }

    /** Raíz: directivas, agentes, ejecuciones y exportaciones. */
    public static class Programa {
        public final List<Directiva> directivas = new ArrayList<>();
        public final List<Agente> agentes = new ArrayList<>();
        public final List<Referencia> ejecuciones = new ArrayList<>();
        public final List<Referencia> exportaciones = new ArrayList<>();

        /** Valor de la última directiva con ese nombre (ej. "@modelo") o null. */
        public String directiva(String nombre) {
            String v = null;
            for (Directiva d : directivas) if (d.nombre.equals(nombre)) v = d.valor;
            return v;
        }
    }

    public static class Directiva {
        public final String nombre, valor;
        public final int fila, columna;
        public Directiva(String nombre, String valor, int fila, int columna) {
            this.nombre = nombre; this.valor = valor; this.fila = fila; this.columna = columna;
        }
    }

    public static class Agente {
        public final String nombre;
        public final int fila, columna;
        public String contexto;               // null si no se declaró
        public final List<VariableDecl> variables = new ArrayList<>();
        public final List<Comando> comandos = new ArrayList<>();
        public Agente(String nombre, int fila, int columna) {
            this.nombre = nombre; this.fila = fila; this.columna = columna;
        }
    }

    public static class VariableDecl {
        public final String nombre;
        public final int fila, columna;
        public final Expresion valor;
        public VariableDecl(String nombre, Expresion valor, int fila, int columna) {
            this.nombre = nombre; this.valor = valor; this.fila = fila; this.columna = columna;
        }
    }

    public static class Comando {
        public final String tipo;              // PREGUNTAR, GENERAR, ...
        public final int fila, columna;
        public Expresion argumento;
        public final List<Conector> conectores = new ArrayList<>();
        public String destino;
        public int destinoFila, destinoColumna;
        public Comando(String tipo, int fila, int columna) {
            this.tipo = tipo; this.fila = fila; this.columna = columna;
        }
        /** Primera expresión del conector con ese nombre (SOBRE, DESDE, EN, COMO) o null. */
        public Expresion conector(String nombre) {
            for (Conector c : conectores) if (c.nombre.equals(nombre)) return c.expresion;
            return null;
        }
    }

    public static class Conector {
        public final String nombre;
        public final Expresion expresion;
        public Conector(String nombre, Expresion expresion) { this.nombre = nombre; this.expresion = expresion; }
    }

    /** termino (+ termino)* */
    public static class Expresion {
        public final List<Termino> terminos = new ArrayList<>();
    }

    public static class Termino {
        public enum Clase { CADENA, ID, NUMERO, CARGAR }
        public final Clase clase;
        public final String valor;            // texto de la cadena, nombre del id, número o archivo
        public final String unidad;           // solo NUMERO (puede ser null)
        public final int fila, columna;
        public Termino(Clase clase, String valor, String unidad, int fila, int columna) {
            this.clase = clase; this.valor = valor; this.unidad = unidad; this.fila = fila; this.columna = columna;
        }
    }

    /** Nombre usado en EJECUTAR o EXPORTAR, con su posición. */
    public static class Referencia {
        public final String nombre;
        public final int fila, columna;
        public Referencia(String nombre, int fila, int columna) {
            this.nombre = nombre; this.fila = fila; this.columna = columna;
        }
    }
}

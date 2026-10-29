package promptzal.semantic;

/**
 * Entrada de la tabla de símbolos.
 */
public class Simbolo {

    public enum Tipo {
        AGENTE("Agente"),
        VARIABLE_DECLARADA("Variable declarada"),
        RESULTADO_COMANDO("Resultado de comando");

        public final String etiqueta;

        Tipo(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }

    public final String nombre;
    public final Tipo tipo;
    public final String ambito;    
    public final int linea;

    public Simbolo(String nombre, Tipo tipo, String ambito, int linea) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.ambito = ambito;
        this.linea = linea;
    }
}

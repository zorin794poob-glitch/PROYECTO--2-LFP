package promptzal.util;

/** Error detectado en cualquier fase de la compilación (léxica, sintáctica o semántica). */
public final class ErrorCompilacion {

    public enum Tipo {
        LEXICO("Léxico"), SINTACTICO("Sintáctico"), SEMANTICO("Semántico");

        public final String etiqueta;

        Tipo(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }

    public final Tipo tipo;
    public final String descripcion;
    /** Lexema o token encontrado (léxico y sintáctico). Puede ser vacío. */
    public final String encontrado;
    /** Lo que la gramática esperaba (solo sintáctico). Puede ser vacío. */
    public final String esperado;
    public final int fila;
    public final int columna;

    private ErrorCompilacion(Tipo tipo, String descripcion, String encontrado, String esperado, int fila, int columna) {
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.encontrado = encontrado == null ? "" : encontrado;
        this.esperado = esperado == null ? "" : esperado;
        this.fila = fila;
        this.columna = columna;
    }

    public static ErrorCompilacion lexico(String descripcion, String lexema, int fila, int columna) {
        return new ErrorCompilacion(Tipo.LEXICO, descripcion, lexema, "", fila, columna);
    }

    public static ErrorCompilacion sintactico(String encontrado, String esperado, int fila, int columna) {
        String desc = "Token inesperado: se encontró " + encontrado + " y se esperaba " + esperado;
        return new ErrorCompilacion(Tipo.SINTACTICO, desc, encontrado, esperado, fila, columna);
    }

    public static ErrorCompilacion semantico(String descripcion, int fila, int columna) {
        return new ErrorCompilacion(Tipo.SEMANTICO, descripcion, "", "", fila, columna);
    }

    @Override
    public String toString() {
        return "[" + tipo.etiqueta + "] " + descripcion + " (fila " + fila + ", columna " + columna + ")";
    }
}

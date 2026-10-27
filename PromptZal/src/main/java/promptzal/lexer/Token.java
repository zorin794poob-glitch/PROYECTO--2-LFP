package promptzal.lexer;

/** Token entregado por el analizador léxico (fila y columna inician en 1). */
public class Token {
    public final TipoToken tipo;
    public final String lexema;
    public final int fila;
    public final int columna;

    public Token(TipoToken tipo, String lexema, int fila, int columna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.fila = fila;
        this.columna = columna;
    }

    /** Valor de una cadena sin comillas y con las secuencias de escape resueltas. */
    public String valor() {
        if (tipo != TipoToken.CADENA || lexema.length() < 2) return lexema;
        String s = lexema.substring(1, lexema.length() - 1);
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    @Override
    public String toString() {
        return tipo + "('" + lexema + "' " + fila + ":" + columna + ")";
    }
}

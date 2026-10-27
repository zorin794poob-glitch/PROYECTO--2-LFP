package promptzal.compiler;

import java.util.ArrayList;
import java.util.List;
import promptzal.ast.Ast.Programa;
import promptzal.lexer.Token;
import promptzal.semantic.TablaSimbolos;
import promptzal.util.ErrorCompilacion;

/** Todo lo que produce una compilación. */
public class ResultadoCompilacion {
    public final List<Token> tokens = new ArrayList<>();
    public final List<ErrorCompilacion> errores = new ArrayList<>();
    public Programa programa;
    public TablaSimbolos tabla = new TablaSimbolos();
    public String prompt = "";

    public int contar(ErrorCompilacion.Tipo tipo) {
        int n = 0;
        for (ErrorCompilacion e : errores) if (e.tipo == tipo) n++;
        return n;
    }
}

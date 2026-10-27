package promptzal.compiler;

import java.io.IOException;
import java.io.StringReader;
import promptzal.generator.GeneradorPrompt;
import promptzal.lexer.Lexer;
import promptzal.lexer.TipoToken;
import promptzal.lexer.Token;
import promptzal.parser.Parser;
import promptzal.semantic.AnalizadorSemantico;
import promptzal.util.ErrorCompilacion;

/** Ejecuta todas las fases: léxico (JFlex) -> sintáctico -> semántico -> promptzal. */
public class Compilador {

    public ResultadoCompilacion compilar(String fuente) {
        ResultadoCompilacion r = new ResultadoCompilacion();

        // 1) Léxico (JFlex)
//        Lexer lexer = new Lexer(new StringReader(fuente));
//        Token t;
//        do {
//            t = lexer.siguiente();
//            r.tokens.add(t);
//        } while (t.tipo != TipoToken.EOF);
//        r.errores.addAll(lexer.errores);

        // 2) Sintáctico (parser descendente recursivo a mano)
        if (r.tokens.isEmpty() || r.tokens.get(r.tokens.size() - 1).tipo != TipoToken.EOF) {
            r.tokens.add(new Token(TipoToken.EOF, "EOF", 1, 1));
        }
        Parser parser = new Parser(r.tokens);
        r.programa = parser.programa();
        r.errores.addAll(parser.getErrores());

        // 3) Semántico + tabla de símbolos
        AnalizadorSemantico sem = new AnalizadorSemantico();
        sem.analizar(r.programa);
        r.tabla = sem.getTabla();
        r.errores.addAll(sem.getErrores());

        // 4) Generación del promptzal (se genera aunque existan errores)
        r.prompt = new GeneradorPrompt().generar(r.programa);

        return r;
    }
}

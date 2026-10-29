package promptzal.parser;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import promptzal.ast.Ast.*;
import promptzal.lexer.TipoToken;
import promptzal.lexer.Token;
import promptzal.util.ErrorCompilacion;


public class Parser {

    private static class ErrorSintactico extends RuntimeException {
        final ErrorCompilacion error;
        final int indiceToken;
        ErrorSintactico(ErrorCompilacion error, int indiceToken) {
            super(error.descripcion, null, false, false);
            this.error = error;
            this.indiceToken = indiceToken;
        }
    }

    // Tokens de sincronización (modo pánico). EOF siempre detiene el descarte.
    private static final Set<TipoToken> SINCRONIZACION = EnumSet.of(
            TipoToken.AGENTE, TipoToken.LLAVE_C, TipoToken.COMANDO_IA,
            TipoToken.VARIABLE, TipoToken.EJECUTAR, TipoToken.EXPORTAR,
            TipoToken.DIRECTIVA, TipoToken.EOF);

    private final List<Token> tokens;
    private final List<ErrorCompilacion> errores = new ArrayList<>();
    private int p = 0;
    private int ultimoErrorIndice = -1;
    private Programa programa;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<ErrorCompilacion> getErrores() { return errores; }

    //  utilidades

    private Token actual() { return tokens.get(Math.min(p, tokens.size() - 1)); }

    private boolean es(TipoToken t) { return actual().tipo == t; }

    private Token match(TipoToken esperado) {
        return match(esperado, esperado.name());
    }

    private Token match(TipoToken esperado, String descripcionEsperado) {
        if (es(esperado)) {
            Token t = actual();
            if (t.tipo != TipoToken.EOF) p++;
            return t;
        }
        throw error(descripcionEsperado);
    }

    private ErrorSintactico error(String esperado) {
        Token t = actual();
        String encontrado = t.tipo == TipoToken.EOF ? "EOF" : t.tipo + " '" + t.lexema + "'";
        return new ErrorSintactico(ErrorCompilacion.sintactico(encontrado, esperado, t.fila, t.columna), p);
    }

    // Registra el error (evita duplicar errores sobre el mismo token).
    private void registrar(ErrorSintactico e) {
        if (e.indiceToken != ultimoErrorIndice) {
            errores.add(e.error);
            ultimoErrorIndice = e.indiceToken;
        }
    }

    // Modo pánico: descarta tokens hasta uno de sincronización. 
    private void sincronizar() {
        while (!SINCRONIZACION.contains(actual().tipo)) p++;
    }

    //  gramática

    /** &lt;programa&gt; -> &lt;directivas&gt; &lt;agentes&gt; &lt;ejecuciones&gt; &lt;exportar&gt; EOF */
    public Programa programa() {
        programa = new Programa();
        directivas();
        agentes();
        ejecuciones();
        exportar();
        if (!es(TipoToken.EOF)) {
            ErrorSintactico e = error("EOF");
            registrar(e);
        }
        return programa;
    }

    /** &lt;directivas&gt; -> DIRECTIVA CADENA &lt;directivas&gt; | ε */
    private void directivas() {
        while (es(TipoToken.DIRECTIVA)) {
            try {
                Token d = match(TipoToken.DIRECTIVA);
                Token valor = match(TipoToken.CADENA);
                programa.directivas.add(new Directiva(d.lexema, valor.valor(), d.fila, d.columna));
            } catch (ErrorSintactico e) {
                registrar(e);
                sincronizar();
            }
        }
    }

    /** &lt;agentes&gt; -> &lt;agente&gt; &lt;mas_agentes&gt; */
    private void agentes() {
        agente();
        masAgentes();
    }

    /** &lt;mas_agentes&gt; -> &lt;agente&gt; &lt;mas_agentes&gt; | ε */
    private void masAgentes() {
        while (es(TipoToken.AGENTE)) {
            agente();
        }
    }

    /** &lt;agente&gt; -> AGENTE ID LLAVE_A &lt;contexto&gt; &lt;variables&gt; &lt;comandos&gt; LLAVE_C */
    private void agente() {
        Agente ag = null;
        try {
            Token kw = match(TipoToken.AGENTE);
            Token id = match(TipoToken.ID, "ID (nombre del agente)");
            ag = new Agente(id.lexema, id.fila, id.columna);
            programa.agentes.add(ag);
            match(TipoToken.LLAVE_A);
        } catch (ErrorSintactico e) {
            registrar(e);
            sincronizar();
            if (es(TipoToken.AGENTE) || es(TipoToken.EJECUTAR)
                    || es(TipoToken.EXPORTAR) || es(TipoToken.EOF)) {
                return;
            }
            if (ag == null) {                       // cabecera inválida: se analiza el cuerpo igual
                Token t = actual();
                ag = new Agente("<sin_nombre>", t.fila, t.columna);
                programa.agentes.add(ag);
            }
        }
        try {
            contexto(ag);
            variables(ag);
            comandos(ag);
            match(TipoToken.LLAVE_C);
        } catch (ErrorSintactico e) {
            registrar(e);
            recuperarCuerpo(ag);
        }
    }

    /**
     * Continúa el análisis del cuerpo de un agente después de un error:
     * acepta variables y comandos hasta encontrar la llave de cierre.
     */
    private void recuperarCuerpo(Agente ag) {
        sincronizar();
        while (true) {
            try {
                switch (actual().tipo) {
                    case VARIABLE:
                        ag.variables.add(variable());
                        break;
                    case COMANDO_IA:
                        ag.comandos.add(comando());
                        break;
                    case LLAVE_C:
                        p++;
                        return;
                    default:        // AGENTE, EJECUTAR, EXPORTAR, DIRECTIVA, EOF
                        return;
                }
            } catch (ErrorSintactico e) {
                registrar(e);
                sincronizar();
            }
        }
    }

    /** &lt;contexto&gt; -> CONTEXTO IGUAL CADENA */
    private void contexto(Agente ag) {
        match(TipoToken.CONTEXTO);
        match(TipoToken.IGUAL);
        ag.contexto = match(TipoToken.CADENA).valor();
    }

    /** &lt;variables&gt; -> VARIABLE ID IGUAL &lt;expresion&gt; &lt;variables&gt; | ε */
    private void variables(Agente ag) {
        while (es(TipoToken.VARIABLE)) {
            ag.variables.add(variable());
        }
    }

    /** Una sola declaración: VARIABLE ID IGUAL &lt;expresion&gt; */
    private VariableDecl variable() {
        match(TipoToken.VARIABLE);
        Token id = match(TipoToken.ID, "ID (nombre de la variable)");
        match(TipoToken.IGUAL);
        Expresion e = expresion();
        return new VariableDecl(id.lexema, e, id.fila, id.columna);
    }

    /** &lt;comandos&gt; -> &lt;comando&gt; &lt;mas_comandos&gt; */
    private void comandos(Agente ag) {
        ag.comandos.add(comando());
        masComandos(ag);
    }

    /** &lt;mas_comandos&gt; -> &lt;comando&gt; &lt;mas_comandos&gt; | ε */
    private void masComandos(Agente ag) {
        while (es(TipoToken.COMANDO_IA)) {
            ag.comandos.add(comando());
        }
    }

    /** &lt;comando&gt; -> COMANDO_IA &lt;expresion&gt; &lt;conectores&gt; FLECHA ID */
    private Comando comando() {
        Token c = match(TipoToken.COMANDO_IA);
        Comando cmd = new Comando(c.lexema, c.fila, c.columna);
        cmd.argumento = expresion();
        conectores(cmd);
        match(TipoToken.FLECHA);
        Token destino = match(TipoToken.ID, "ID (variable destino)");
        cmd.destino = destino.lexema;
        cmd.destinoFila = destino.fila;
        cmd.destinoColumna = destino.columna;
        return cmd;
    }

    /** &lt;conectores&gt; -> CONECTOR &lt;expresion&gt; &lt;conectores&gt; | ε */
    private void conectores(Comando cmd) {
        while (es(TipoToken.CONECTOR)) {
            Token k = match(TipoToken.CONECTOR);
            Expresion e = expresion();
            cmd.conectores.add(new Conector(k.lexema, e));
        }
    }

    /** &lt;expresion&gt; -> &lt;termino&gt; &lt;mas_terminos&gt; */
    private Expresion expresion() {
        Expresion e = new Expresion();
        e.terminos.add(termino());
        masTerminos(e);
        return e;
    }

    /** &lt;mas_terminos&gt; -> MAS &lt;termino&gt; &lt;mas_terminos&gt; | ε */
    private void masTerminos(Expresion e) {
        while (es(TipoToken.MAS)) {
            match(TipoToken.MAS);
            e.terminos.add(termino());
        }
    }

    /** &lt;termino&gt; -> CADENA | ID | NUMERO &lt;unidad&gt; | &lt;cargar&gt; */
    private Termino termino() {
        Token t = actual();
        switch (t.tipo) {
            case CADENA:
                p++;
                return new Termino(Termino.Clase.CADENA, t.valor(), null, t.fila, t.columna);
            case ID:
                p++;
                return new Termino(Termino.Clase.ID, t.lexema, null, t.fila, t.columna);
            case NUMERO:
                p++;
                String u = unidad();
                return new Termino(Termino.Clase.NUMERO, t.lexema, u, t.fila, t.columna);
            case CARGAR:
                return cargar();
            default:
                throw error("CADENA, ID, NUMERO o CARGAR");
        }
    }

    /** &lt;unidad&gt; -> ID | ε  (devuelve null si es vacía) */
    private String unidad() {
        if (es(TipoToken.ID)) {
            return tokens.get(p++).lexema;
        }
        return null;
    }

    /** &lt;cargar&gt; -> CARGAR PAR_A CADENA PAR_C */
    private Termino cargar() {
        Token c = match(TipoToken.CARGAR);
        match(TipoToken.PAR_A);
        Token archivo = match(TipoToken.CADENA);
        match(TipoToken.PAR_C);
        return new Termino(Termino.Clase.CARGAR, archivo.valor(), null, c.fila, c.columna);
    }

    /** &lt;ejecuciones&gt; -> EJECUTAR ID &lt;mas_ejecuciones&gt; */
    private void ejecuciones() {
        ejecucion();
        masEjecuciones();
    }

    /** &lt;mas_ejecuciones&gt; -> EJECUTAR ID &lt;mas_ejecuciones&gt; | ε */
    private void masEjecuciones() {
        while (es(TipoToken.EJECUTAR)) {
            ejecucion();
        }
    }

    private void ejecucion() {
        try {
            match(TipoToken.EJECUTAR);
            Token id = match(TipoToken.ID, "ID (nombre del agente)");
            programa.ejecuciones.add(new Referencia(id.lexema, id.fila, id.columna));
        } catch (ErrorSintactico e) {
            registrar(e);
            sincronizar();
        }
    }

    /** &lt;exportar&gt; -> EXPORTAR ID &lt;mas_ids&gt;      &lt;mas_ids&gt; -> COMA ID &lt;mas_ids&gt; | ε */
    private void exportar() {
        try {
            match(TipoToken.EXPORTAR);
            Token id = match(TipoToken.ID, "ID (variable a exportar)");
            programa.exportaciones.add(new Referencia(id.lexema, id.fila, id.columna));
            masIds();
        } catch (ErrorSintactico e) {
            registrar(e);
            sincronizar();
        }
    }

    /** &lt;mas_ids&gt; -> COMA ID &lt;mas_ids&gt; | ε */
    private void masIds() {
        while (es(TipoToken.COMA)) {
            match(TipoToken.COMA);
            Token id = match(TipoToken.ID, "ID (variable a exportar)");
            programa.exportaciones.add(new Referencia(id.lexema, id.fila, id.columna));
        }
    }
}

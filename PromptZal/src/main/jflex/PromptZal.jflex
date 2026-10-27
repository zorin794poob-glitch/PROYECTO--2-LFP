/* ==========================================================================
 *  PromptZal - Analizador léxico (Proyecto 2, LFP)
 *  Generado con JFlex.  Produce Token(tipo, lexema, fila, columna) con fila y
 *  columna iniciando en 1.  Espacios y comentarios se descartan.
 *  Los errores léxicos se acumulan en la lista "errores" y el análisis continúa.
 * ========================================================================== */
package promptzal.lexer;

import java.util.ArrayList;
import java.util.List;
import promptzal.util.ErrorCompilacion;

%%

%public
%class Lexer
%unicode
%line
%column
%type Token
%function siguiente

%{
    /** Errores léxicos encontrados durante el análisis. */
    public final List<ErrorCompilacion> errores = new ArrayList<>();

    private int comFila, comCol;

    private Token tk(TipoToken tipo) {
        return new Token(tipo, yytext(), yyline + 1, yycolumn + 1);
    }

    private void error(String descripcion) {
        errores.add(ErrorCompilacion.lexico(descripcion, yytext(), yyline + 1, yycolumn + 1));
    }
%}

LETRA     = [\p{L}_]
ALNUM     = [\p{L}\p{Nd}_]
DIGITO    = [0-9]
ESPACIO   = [ \t\r\n\f]

%state COMENTARIO

%%

<YYINITIAL> {

    /* ---------- se descartan ---------- */
    {ESPACIO}+                      { /* ignorar */ }
    "//" [^\r\n]*                   { /* comentario de línea */ }
    "/*"                            { comFila = yyline + 1; comCol = yycolumn + 1; yybegin(COMENTARIO); }

    /* ---------- directivas ---------- */
    "@modelo" | "@rol" | "@formato" { return tk(TipoToken.DIRECTIVA); }
    "@" {ALNUM}*                    { error("Directiva desconocida"); }

    /* ---------- palabras reservadas de estructura ---------- */
    "AGENTE"                        { return tk(TipoToken.AGENTE); }
    "contexto"                      { return tk(TipoToken.CONTEXTO); }
    "variable"                      { return tk(TipoToken.VARIABLE); }
    "EJECUTAR"                      { return tk(TipoToken.EJECUTAR); }
    "EXPORTAR"                      { return tk(TipoToken.EXPORTAR); }

    /* ---------- comandos de IA (agregar aquí cualquier comando nuevo, p. ej. "CODIFICAR") ---------- */
    "PREGUNTAR" | "GENERAR" | "RESUMIR" | "ANALIZAR" |
    "TRADUCIR"  | "CLASIFICAR" | "EXTRAER"
                                    { return tk(TipoToken.COMANDO_IA); }

    /* ---------- función del sistema ---------- */
    "CARGAR"                        { return tk(TipoToken.CARGAR); }

    /* ---------- conectores ---------- */
    "SOBRE" | "DESDE" | "EN" | "COMO"
                                    { return tk(TipoToken.CONECTOR); }

    /* ---------- símbolos ---------- */
    "->"                            { return tk(TipoToken.FLECHA); }
    "="                             { return tk(TipoToken.IGUAL); }
    "+"                             { return tk(TipoToken.MAS); }
    "{"                             { return tk(TipoToken.LLAVE_A); }
    "}"                             { return tk(TipoToken.LLAVE_C); }
    "("                             { return tk(TipoToken.PAR_A); }
    ")"                             { return tk(TipoToken.PAR_C); }
    ","                             { return tk(TipoToken.COMA); }

    /* ---------- literales ---------- */
    \" ( [^\"\\\r\n] | \\ [^\r\n] )* \"
                                    { return tk(TipoToken.CADENA); }
    \" [^\"\r\n]*                   { error("Cadena sin cerrar"); }
    {DIGITO}+ ( "." {DIGITO}+ )?    { return tk(TipoToken.NUMERO); }

    /* ---------- identificadores (después de las palabras reservadas) ---------- */
    {LETRA} {ALNUM}*                { return tk(TipoToken.ID); }

    /* ---------- cualquier otro carácter: error léxico ---------- */
    [^]                             { error("Carácter no reconocido"); }
}

<COMENTARIO> {
    "*/"                            { yybegin(YYINITIAL); }
    [^]                             { /* contenido del comentario */ }
}

/* ---------- fin de archivo ---------- */
<YYINITIAL> <<EOF>>                 { return new Token(TipoToken.EOF, "EOF", yyline + 1, yycolumn + 1); }

<COMENTARIO> <<EOF>>                {
                                        errores.add(ErrorCompilacion.lexico("Comentario de bloque sin cerrar", "/*", comFila, comCol));
                                        yybegin(YYINITIAL);
                                        return new Token(TipoToken.EOF, "EOF", yyline + 1, yycolumn + 1);
                                    }

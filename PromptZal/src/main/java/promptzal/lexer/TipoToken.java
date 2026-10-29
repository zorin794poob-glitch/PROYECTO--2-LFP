package promptzal.lexer;

/**
 * Terminales de la gramática de PromptZal (coinciden con el manual técnico).
 */
public enum TipoToken {
    DIRECTIVA,
    AGENTE,
    CONTEXTO,
    VARIABLE,
    EJECUTAR,
    EXPORTAR,
    COMANDO_IA,
    CARGAR,
    CONECTOR,
    FLECHA,
    IGUAL,
    MAS,
    LLAVE_A,
    LLAVE_C,
    PAR_A,
    PAR_C,
    COMA,
    ID,
    CADENA,
    NUMERO,
    EOF
}

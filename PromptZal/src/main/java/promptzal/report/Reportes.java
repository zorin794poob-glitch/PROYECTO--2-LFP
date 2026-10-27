package promptzal.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import promptzal.ast.Ast.*;
import promptzal.compiler.ResultadoCompilacion;
import promptzal.semantic.Simbolo;
import promptzal.util.ErrorCompilacion;

/** Generación de los reportes HTML y del AST en texto con sangrías. */
public final class Reportes {
    private Reportes() { }

    private static final String CSS =
        "body{font-family:Segoe UI,Arial,sans-serif;background:#1e1f22;color:#dfe1e5;margin:32px}"
        + "h1{color:#6fb1fc;margin-bottom:4px}p.sub{color:#8c8f94;margin-top:0}"
        + "table{border-collapse:collapse;width:100%;margin-top:16px}"
        + "th{background:#2b2d30;color:#6fb1fc;text-align:left;padding:8px 12px;border-bottom:2px solid #3c3f41}"
        + "td{padding:7px 12px;border-bottom:1px solid #393b40}tr:hover td{background:#2b2d30}"
        + ".LEXICO{color:#e5c07b}.SINTACTICO{color:#e06c75}.SEMANTICO{color:#c678dd}"
        + ".ok{color:#98c379;font-size:18px;margin-top:24px}pre{background:#2b2d30;padding:16px;border-radius:6px;"
        + "font-family:Consolas,monospace;line-height:1.5;overflow:auto}";

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String pagina(String titulo, String cuerpo) {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><title>" + esc(titulo)
                + "</title><style>" + CSS + "</style></head><body><h1>" + esc(titulo)
                + "</h1><p class=\"sub\">PromptZal &middot; generado el " + fecha + "</p>" + cuerpo + "</body></html>";
    }

    // ------------------------------------------------------------ errores

    public static String erroresHtml(ResultadoCompilacion r) {
        StringBuilder sb = new StringBuilder();
        if (r.errores.isEmpty()) {
            sb.append("<p class=\"ok\">&#10004; La compilación no produjo errores (léxicos, sintácticos ni semánticos).</p>");
        } else {
            sb.append("<table><tr><th>#</th><th>Tipo</th><th>Descripción</th><th>Encontrado</th><th>Esperado</th>"
                    + "<th>Fila</th><th>Columna</th></tr>");
            int n = 1;
            for (ErrorCompilacion e : r.errores) {
                sb.append("<tr><td>").append(n++).append("</td><td class=\"").append(e.tipo).append("\">")
                  .append(nombreTipo(e.tipo)).append("</td><td>").append(esc(e.descripcion)).append("</td><td>")
                  .append(esc(e.encontrado)).append("</td><td>").append(esc(e.esperado)).append("</td><td>")
                  .append(e.fila).append("</td><td>").append(e.columna).append("</td></tr>");
            }
            sb.append("</table>");
        }
        return pagina("Reporte de errores", sb.toString());
    }

    public static String nombreTipo(ErrorCompilacion.Tipo t) {
        switch (t) {
            case LEXICO: return "Léxico";
            case SINTACTICO: return "Sintáctico";
            default: return "Semántico";
        }
    }

    // ------------------------------------------------------------ tabla de símbolos

    public static String simbolosHtml(ResultadoCompilacion r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><tr><th>#</th><th>Nombre</th><th>Tipo</th><th>Ámbito</th><th>Línea de declaración</th></tr>");
        int n = 1;
        for (Simbolo s : r.tabla.getSimbolos()) {
            sb.append("<tr><td>").append(n++).append("</td><td>").append(esc(s.nombre)).append("</td><td>")
              .append(s.tipo.etiqueta).append("</td><td>").append(esc(s.ambito)).append("</td><td>")
              .append(s.linea).append("</td></tr>");
        }
        sb.append("</table>");
        if (r.tabla.getSimbolos().isEmpty()) sb.append("<p>No se registró ningún símbolo.</p>");
        return pagina("Reporte de tabla de símbolos", sb.toString());
    }

    // ------------------------------------------------------------ AST

    public static String astTexto(Programa p) {
        StringBuilder sb = new StringBuilder("Programa\n");
        if (p == null) return sb.toString();
        for (Directiva d : p.directivas) sb.append("  Directiva ").append(d.nombre).append(" = \"").append(d.valor).append("\"\n");
        for (Agente a : p.agentes) {
            sb.append("  Agente ").append(a.nombre).append(" (línea ").append(a.fila).append(")\n");
            if (a.contexto != null) sb.append("    Contexto: \"").append(a.contexto).append("\"\n");
            for (VariableDecl v : a.variables) {
                sb.append("    Variable ").append(v.nombre).append(" (línea ").append(v.fila).append(")\n");
                sb.append("      Valor: ").append(expr(v.valor)).append('\n');
            }
            for (Comando c : a.comandos) {
                sb.append("    Comando ").append(c.tipo).append(" (línea ").append(c.fila).append(")\n");
                sb.append("      Argumento: ").append(expr(c.argumento)).append('\n');
                for (Conector k : c.conectores) {
                    sb.append("      Conector ").append(k.nombre).append(": ").append(expr(k.expresion)).append('\n');
                }
                sb.append("      Destino: ").append(c.destino).append('\n');
            }
        }
        for (Referencia e : p.ejecuciones) sb.append("  Ejecutar ").append(e.nombre).append('\n');
        if (!p.exportaciones.isEmpty()) {
            sb.append("  Exportar\n");
            for (Referencia e : p.exportaciones) sb.append("    ").append(e.nombre).append('\n');
        }
        return sb.toString();
    }

    private static String expr(Expresion e) {
        if (e == null) return "(vacía)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < e.terminos.size(); i++) {
            Termino t = e.terminos.get(i);
            if (i > 0) sb.append(" + ");
            switch (t.clase) {
                case CADENA: sb.append("Cadena(\"").append(t.valor).append("\")"); break;
                case ID: sb.append("Id(").append(t.valor).append(')'); break;
                case CARGAR: sb.append("Cargar(\"").append(t.valor).append("\")"); break;
                default:
                    sb.append("Numero(").append(t.valor);
                    if (t.unidad != null) sb.append(' ').append(t.unidad);
                    sb.append(')');
            }
        }
        return sb.toString();
    }

    public static String astHtml(Programa p) {
        return pagina("Reporte del AST", "<pre>" + esc(astTexto(p)) + "</pre>");
    }

    // ------------------------------------------------------------ escritura

    public static void guardar(Path destino, String contenido) throws IOException {
        Files.write(destino, contenido.getBytes(StandardCharsets.UTF_8));
    }

    /** Genera los tres reportes HTML en la carpeta indicada y devuelve las rutas. */
    public static List<Path> generarTodos(Path carpeta, ResultadoCompilacion r) throws IOException {
        Files.createDirectories(carpeta);
        Path e = carpeta.resolve("reporte_errores.html");
        Path s = carpeta.resolve("reporte_tabla_simbolos.html");
        Path a = carpeta.resolve("reporte_ast.html");
        guardar(e, erroresHtml(r));
        guardar(s, simbolosHtml(r));
        guardar(a, astHtml(r.programa));
        return List.of(e, s, a);
    }
}

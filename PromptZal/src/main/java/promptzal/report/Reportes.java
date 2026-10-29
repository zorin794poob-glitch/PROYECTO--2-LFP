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

public final class Reportes {

    private Reportes() {
    }

    private static final String CSS
            = ":root{color-scheme:light;--navy:#214564;--navy2:#193953;--line:#d8e1e8;--muted:#617386;--bg:#f2f5f8;--red:#b42318;--redbg:#fff3f1;}"
            + "*{box-sizing:border-box}body{font-family:Segoe UI,Arial,sans-serif;background:var(--bg);color:#172b3d;margin:0;font-size:14px}"
            + ".top{background:linear-gradient(120deg,var(--navy),var(--navy2));color:#fff;padding:26px max(24px,calc((100% - 1100px)/2));}"
            + "h1{font-size:25px;margin:0 0 8px;font-weight:700;color:#fff}.sub{color:#d8e7f2;margin:0 0 18px;font-size:13px}"
            + ".nav{display:flex;gap:8px;flex-wrap:wrap}.nav a{color:#fff;text-decoration:none;border:1px solid #6b879e;border-radius:22px;padding:8px 13px;font-size:13px}"
            + ".nav a:hover,.nav a.active{background:#ffffff24;border-color:#9bb5ca}"
            + "main{max-width:1100px;margin:22px auto;padding:0 18px 36px}.alert{border:1px solid #f2b5ad;background:var(--redbg);color:var(--red);border-radius:10px;padding:16px;margin-bottom:12px}"
            + ".alert strong{display:block;margin-bottom:5px}.tools{display:flex;gap:8px;margin-bottom:12px}.search{flex:1;min-width:120px;border:1px solid var(--line);border-radius:9px;padding:11px 13px;background:#fff;font:inherit;color:#172b3d}"
            + "button{border:1px solid var(--line);background:#fff;border-radius:9px;padding:10px 14px;font:inherit;cursor:pointer;color:#172b3d}button:hover{background:#eaf0f5}"
            + ".tablewrap{background:#fff;border:1px solid var(--line);border-radius:11px;overflow:auto;box-shadow:0 1px 3px #1939530b}"
            + "table{border-collapse:collapse;width:100%;margin:0;background:#fff}th{background:#edf2f6;color:#142c40;text-align:left;padding:12px;border-bottom:1px solid var(--line);white-space:nowrap}"
            + "td{padding:11px 12px;border-bottom:1px solid var(--line);vertical-align:top}tr:last-child td{border-bottom:0}tbody tr:hover td{background:#f8fafc}"
            + "code{font-family:Consolas,monospace;background:#edf1f4;border-radius:5px;padding:3px 5px;color:#173b57;white-space:pre-wrap;overflow-wrap:anywhere}"
            + ".badge{display:inline-block;border-radius:20px;padding:4px 8px;font-size:11px;font-weight:700;background:#f9e8e6;color:#a52a1a;white-space:nowrap}"
            + ".badge.ok{background:#e8f3ee;color:#16704a}.badge.token{background:#e8f3ee;color:#16704a}"
            + ".ok{border:1px solid #a8d8c0;background:#effaf4;color:#176b43;border-radius:10px;padding:16px;margin:0}"
            + "pre{background:#fff;color:#172b3d;border:1px solid var(--line);padding:18px;border-radius:10px;line-height:1.55;overflow:auto;white-space:pre-wrap}"
            + ".empty{padding:20px;color:var(--muted)}@media(max-width:650px){.top{padding:22px 18px}main{margin:14px auto}.tools{flex-direction:column}h1{font-size:22px}}"
            + "@media print{body{background:#fff}.top{background:#fff;color:#000;padding:0 0 14px;border-bottom:2px solid #333}h1,.sub,.nav a{color:#000}.nav{display:none}main{margin:12px auto;padding:0}.tools{display:none}.tablewrap{box-shadow:none;border:0}th{background:#eee!important;-webkit-print-color-adjust:exact;print-color-adjust:exact}}";

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String pagina(String titulo, String cuerpo) {
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String actual = titulo.toLowerCase().contains("token") ? "tokens"
                : titulo.toLowerCase().contains("error") ? "errores"
                : titulo.toLowerCase().contains("símbolos") || titulo.toLowerCase().contains("simbolos") ? "simbolos"
                : titulo.toLowerCase().contains("estad") ? "estadisticas" : "ast";
        String nav = "<nav class=\"nav\">"
                + enlace("tokens", "Tokens", actual) + enlace("errores", "Errores", actual)
                + enlace("estadisticas", "Estadísticas", actual) + enlace("simbolos", "Tabla de símbolos", actual)
                + enlace("ast", "AST", actual) + "</nav>";
        String js = "<script>function filtrar(){const q=document.getElementById('buscar');if(!q)return;const v=q.value.toLowerCase();document.querySelectorAll('tbody tr').forEach(r=>r.style.display=r.innerText.toLowerCase().includes(v)?'':'none');}</script>";
        String tools = "<div class=\"tools\"><input id=\"buscar\" class=\"search\" placeholder=\"🔎 Buscar...\" oninput=\"filtrar()\"><button onclick=\"window.print()\">▣ Imprimir</button></div>";
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>" + esc(titulo)
                + "</title><style>" + CSS + "</style></head><body><header class=\"top\"><h1>PromptZal</h1><p class=\"sub\">" + esc(titulo)
                + " · generado el " + fecha + "</p>" + nav + "</header><main>" + tools + cuerpo + js + "</main></body></html>";
    }

    private static String enlace(String archivo, String texto, String actual) {
        String destino = archivo.equals("simbolos") ? "reporte_tabla_simbolos.html" : "reporte_" + archivo + ".html";
        return "<a href=\"" + destino + "\" class=\""
                + (archivo.equals(actual) ? "active" : "") + "\">" + texto + "</a>";
    }

    //   errores
    public static String erroresHtml(ResultadoCompilacion r) {
        StringBuilder sb = new StringBuilder();
        if (r.errores.isEmpty()) {
            sb.append("<p class=\"ok\">&#10004; La compilación no produjo errores (léxicos, sintácticos ni semánticos).</p>");
        } else {
            sb.append("<div class=\"alert\"><strong>⚠ " + r.errores.size() + " errores encontrados</strong>El analizador continuó para recuperar los errores posibles.</div><div class=\"tablewrap\"><table><thead><tr><th>#</th><th>Tipo</th><th>Descripción</th><th>Encontrado</th><th>Esperado</th>"
                    + "<th>Fila</th><th>Columna</th></tr></thead><tbody>");
            int n = 1;
            for (ErrorCompilacion e : r.errores) {
                sb.append("<tr><td>").append(n++).append("</td><td class=\"").append(e.tipo).append("\">")
                        .append(nombreTipo(e.tipo)).append("</td><td>").append(esc(e.descripcion)).append("</td><td>")
                        .append(esc(e.encontrado)).append("</td><td>").append(esc(e.esperado)).append("</td><td>")
                        .append(e.fila).append("</td><td>").append(e.columna).append("</td></tr>");
            }
            sb.append("</tbody></table></div>");
        }
        return pagina("Reporte de errores", sb.toString());
    }

    public static String nombreTipo(ErrorCompilacion.Tipo t) {
        switch (t) {
            case LEXICO:
                return "Léxico";
            case SINTACTICO:
                return "Sintáctico";
            default:
                return "Semántico";
        }
    }

    // tabla de símbolos
    public static String simbolosHtml(ResultadoCompilacion r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"tablewrap\"><table><thead><tr><th>#</th><th>Nombre</th><th>Tipo</th><th>Ámbito</th><th>Línea de declaración</th></tr></thead><tbody>");
        int n = 1;
        for (Simbolo s : r.tabla.getSimbolos()) {
            sb.append("<tr><td>").append(n++).append("</td><td>").append(esc(s.nombre)).append("</td><td>")
                    .append(s.tipo.etiqueta).append("</td><td>").append(esc(s.ambito)).append("</td><td>")
                    .append(s.linea).append("</td></tr>");
        }
        sb.append("</tbody></table></div>");
        if (r.tabla.getSimbolos().isEmpty()) {
            sb.append("<p>No se registró ningún símbolo.</p>");
        }
        return pagina("Reporte de tabla de símbolos", sb.toString());
    }

    //  AST
    public static String astTexto(Programa p) {
        StringBuilder sb = new StringBuilder("Programa\n");
        if (p == null) {
            return sb.toString();
        }
        for (Directiva d : p.directivas) {
            sb.append("  Directiva ").append(d.nombre).append(" = ").append(d.valor).append("\n");
        }
        for (Agente a : p.agentes) {
            sb.append("  Agente ").append(a.nombre).append(" (línea ").append(a.fila).append(")\n");
            if (a.contexto != null) {
                sb.append("    Contexto: ").append(a.contexto).append("\n");
            }
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
        for (Referencia e : p.ejecuciones) {
            sb.append("  Ejecutar ").append(e.nombre).append('\n');
        }
        if (!p.exportaciones.isEmpty()) {
            sb.append("  Exportar\n");
            for (Referencia e : p.exportaciones) {
                sb.append("    ").append(e.nombre).append('\n');
            }
        }
        return sb.toString();
    }

    private static String expr(Expresion e) {
        if (e == null) {
            return "(vacía)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < e.terminos.size(); i++) {
            Termino t = e.terminos.get(i);
            if (i > 0) {
                sb.append(" + ");
            }
            switch (t.clase) {
                case CADENA:
                    sb.append("Cadena(").append(t.valor).append(")");
                    break;
                case ID:
                    sb.append("Id(").append(t.valor).append(')');
                    break;
                case CARGAR:
                    sb.append("Cargar(").append(t.valor).append(")");
                    break;
                default:
                    sb.append("Numero(").append(t.valor);
                    if (t.unidad != null) {
                        sb.append(' ').append(t.unidad);
                    }
                    sb.append(')');
            }
        }
        return sb.toString();
    }

    public static String astHtml(Programa p) {
        return pagina("Reporte del AST", "<pre>" + esc(astTexto(p)) + "</pre>");
    }

    // Reporte de tokens: las cadenas se muestran sin comillas.
    public static String tokensHtml(ResultadoCompilacion r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"tablewrap\"><table><thead><tr><th>#</th><th>Lexema</th><th>Tipo</th><th>Fila</th><th>Columna</th></tr></thead><tbody>");
        int n = 1;
        for (promptzal.lexer.Token t : r.tokens) {
            if (t.tipo == promptzal.lexer.TipoToken.EOF) {
                continue;
            }
            String lexema = t.tipo == promptzal.lexer.TipoToken.CADENA ? t.valor() : t.lexema;
            sb.append("<tr><td>").append(n++).append("</td><td><code>").append(esc(lexema))
                    .append("</code></td><td><span class=\"badge token\">").append(t.tipo)
                    .append("</span></td><td>").append(t.fila).append("</td><td>").append(t.columna).append("</td></tr>");
        }
        sb.append("</tbody></table></div>");
        if (r.tokens.isEmpty()) {
            sb.append("<p class=\"empty\">No se generaron tokens.</p>");
        }
        return pagina("Reporte de tokens", sb.toString());
    }

    public static String estadisticasHtml(ResultadoCompilacion r) {
        int cadenas = 0, ids = 0, nums = 0;
        for (promptzal.lexer.Token t : r.tokens) {
            if (t.tipo == promptzal.lexer.TipoToken.CADENA) {
                cadenas++;
            }
            if (t.tipo == promptzal.lexer.TipoToken.ID) {
                ids++;
            }
            if (t.tipo == promptzal.lexer.TipoToken.NUMERO) {
                nums++;
            }
        }
        String cuerpo = "<div class=\"tablewrap\"><table><thead><tr><th>Métrica</th><th>Cantidad</th></tr></thead><tbody>"
                + "<tr><td>Tokens reconocidos</td><td>" + r.tokens.size() + "</td></tr>"
                + "<tr><td>Cadenas de texto</td><td>" + cadenas + "</td></tr><tr><td>Identificadores</td><td>" + ids + "</td></tr>"
                + "<tr><td>Números</td><td>" + nums + "</td></tr><tr><td>Errores totales</td><td>" + r.errores.size() + "</td></tr>"
                + "<tr><td>Símbolos registrados</td><td>" + r.tabla.getSimbolos().size() + "</td></tr></tbody></table></div>";
        return pagina("Reporte de estadísticas", cuerpo);
    }

    // escritura
    public static void guardar(Path destino, String contenido) throws IOException {
        Files.write(destino, contenido.getBytes(StandardCharsets.UTF_8));
    }

    // Genera los tres reportes HTML en la carpeta indicada y devuelve las rutas.
    public static List<Path> generarTodos(Path carpeta, ResultadoCompilacion r) throws IOException {
        Files.createDirectories(carpeta);
        Path t = carpeta.resolve("reporte_tokens.html");
        Path e = carpeta.resolve("reporte_errores.html");
        Path st = carpeta.resolve("reporte_estadisticas.html");
        Path s = carpeta.resolve("reporte_tabla_simbolos.html");
        Path a = carpeta.resolve("reporte_ast.html");
        guardar(t, tokensHtml(r));
        guardar(e, erroresHtml(r));
        guardar(st, estadisticasHtml(r));
        guardar(s, simbolosHtml(r));
        guardar(a, astHtml(r.programa));
        return List.of(t, e, st, s, a);
    }
}

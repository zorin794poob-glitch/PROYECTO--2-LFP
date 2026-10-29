package promptzal.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import promptzal.ast.Ast.*;

/**
 * Recorre el AST y produce el promptzal.
 */
public class GeneradorPrompt {

    public String generar(Programa prog) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Prompt generado con PromptZal\n");
        String modelo = prog.directiva("@modelo");
        String rol = prog.directiva("@rol");
        String formato = prog.directiva("@formato");
        if (modelo != null) {
            sb.append("Modelo destino: ").append(modelo).append('\n');
        }
        if (rol != null) {
            sb.append("Rol general: ").append(rol).append('\n');
        }
        if (formato != null) {
            sb.append("Formato de respuesta: ").append(formato).append('\n');
        }

        Set<Agente> yaEmitidos = new HashSet<>();
        for (Referencia ej : prog.ejecuciones) {
            Agente ag = buscarAgente(prog, ej.nombre);
            if (ag == null) {
                continue;                
            }
            sb.append("\n## Agente: ").append(ag.nombre).append('\n');
            if (ag.contexto != null) {
                sb.append("Contexto: ").append(ag.contexto).append('\n');
            }

            if (!ag.variables.isEmpty()) {
                sb.append("Variables disponibles:\n");
                for (VariableDecl v : ag.variables) {
                    sb.append("- ").append(v.nombre).append(": ").append(origen(v.valor)).append('\n');
                }
            }
            if (!ag.comandos.isEmpty()) {
                sb.append("Instrucciones:\n");
                int n = 1;
                for (Comando c : ag.comandos) {
                    sb.append(n++).append(". ").append(instruccion(c).replace("\n", "\n   ")).append('\n');
                }
            }
        }

        sb.append("\n## Resultado solicitado\n");
        for (Referencia r : prog.exportaciones) {
            sb.append("- ").append(r.nombre).append('\n');
        }
        return sb.toString();
    }

    private Agente buscarAgente(Programa prog, String nombre) {
        for (Agente a : prog.agentes) {
            if (a.nombre.equals(nombre)) {
                return a;
            }
        }
        return null;
    }

    // origen de variables
    private String origen(Expresion e) {
        List<String> partes = new ArrayList<>();
        for (Termino t : e.terminos) {
            switch (t.clase) {
                case CARGAR:
                    partes.add("contenido del archivo \"" + t.valor + "\" (adjuntar el archivo)");
                    break;
                case CADENA:
                    partes.add("\"" + t.valor + "\"");
                    break;
                case ID:
                    partes.add("valor de " + t.valor);
                    break;
                default:
                    partes.add(numero(t));
                    break;
            }
        }
        return String.join(" + ", partes);
    }

    // instrucciones
    private String instruccion(Comando c) {
        String arg = texto(c.argumento);
        Set<String> usados = new HashSet<>(Arrays.asList("COMO"));
        String base;
        String guarda = "Guarda el resultado como " + c.destino + ".";

        switch (c.tipo) {
            case "PREGUNTAR": {
                String sobre = textoConector(c, "SOBRE", usados);
                base = sobre != null ? "Basándote en " + sobre + ", responde: " + arg + "." : "Responde: " + arg + ".";
                guarda = "Guarda la respuesta como " + c.destino + ".";
                break;
            }
            case "GENERAR": {
                String desde = textoConector(c, "DESDE", usados);
                base = desde != null ? "Genera " + arg + " a partir de: " + desde + "." : "Genera " + arg + ".";
                break;
            }
            case "RESUMIR": {
                String en = textoConector(c, "EN", usados);
                base = en != null ? "Resume " + arg + " en un máximo de " + en + "." : "Resume " + arg + ".";
                break;
            }
            case "ANALIZAR": {
                String sobre = textoConector(c, "SOBRE", usados);
                base = sobre != null ? "Analiza " + arg + " considerando: " + sobre + "." : "Analiza " + arg + ".";
                break;
            }
            case "TRADUCIR": {
                String en = textoConector(c, "EN", usados);
                base = en != null ? "Traduce " + arg + " al idioma " + en + "." : "Traduce " + arg + ".";
                break;
            }
            case "CLASIFICAR": {
                String en = textoConector(c, "EN", usados);
                base = en != null ? "Clasifica " + arg + " en las categorías: " + en + "." : "Clasifica " + arg + ".";
                break;
            }
            case "EXTRAER": {
                String desde = textoConector(c, "DESDE", usados);
                base = desde != null ? "Extrae " + arg + " a partir de " + desde + "." : "Extrae " + arg + ".";
                break;
            }
            default:  
                base = "Aplica " + c.tipo + " a " + arg + ".";
        }

        base = base.replaceAll("([?!.])\\.$", "$1");  
        StringBuilder sb = new StringBuilder(base);
        // conectores no usados por la plantilla del comando
        for (Conector k : c.conectores) {
            if (!usados.contains(k.nombre)) {
                sb.append(" Considera también (").append(k.nombre).append("): ").append(texto(k.expresion)).append('.');
                usados.add(k.nombre);
            }
        }
        sb.append('\n').append(guarda);
        Expresion como = c.conector("COMO");
        if (como != null) {
            sb.append(" Entrega el resultado en formato ").append(texto(como)).append('.');
        }
        return sb.toString();
    }

    private String textoConector(Comando c, String nombre, Set<String> usados) {
        Expresion e = c.conector(nombre);
        if (e == null) {
            return null;
        }
        usados.add(nombre);
        return texto(e);
    }

     // Texto legible de una expresión
    private String texto(Expresion e) {
        if (e == null) {
            return "";
        }
        List<String> partes = new ArrayList<>();
        for (Termino t : e.terminos) {
            switch (t.clase) {
                case CADENA:
                    partes.add(t.valor);
                    break;
                case ID:
                    partes.add(t.valor);
                    break;
                case CARGAR:
                    partes.add("el contenido del archivo \"" + t.valor + "\"");
                    break;
                default:
                    partes.add(numero(t));
                    break;
            }
        }
        return String.join(" ", partes);
    }

    private String numero(Termino t) {
        return t.unidad == null ? t.valor : t.valor + " " + t.unidad;
    }
}

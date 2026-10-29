package promptzal.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import promptzal.compiler.Compilador;
import promptzal.compiler.ResultadoCompilacion;
import promptzal.lexer.Token;
import promptzal.report.Reportes;
import promptzal.semantic.Simbolo;
import promptzal.util.ErrorCompilacion;


public class MainFrame extends JFrame {

    private static final String EJEMPLO
            = "@modelo \"claude-sonnet-4-6\"\n"
            + "@rol \"analista de datos\"\n"
            + "@formato \"markdown\"\n\n"
            + "// Agente que prepara el analisis de un archivo de ventas\n"
            + "AGENTE analista {\n"
            + "    contexto = \"Eres un analista de datos experto\"\n"
            + "    variable ventas = CARGAR(\"ventas.csv\")\n"
            + "    PREGUNTAR \"Cuales son las 3 tendencias principales?\" SOBRE ventas -> tendencias\n"
            + "    RESUMIR tendencias EN 100 palabras -> resumen\n"
            + "}\n\n"
            + "EJECUTAR analista\n"
            + "EXPORTAR resumen\n";

    private final EditorPane editor = new EditorPane();
    private final JTextArea panelPrompt = new JTextArea();
    private final JTextArea panelAst = new JTextArea();
    private final JTextArea consola = new JTextArea();
    private final DefaultTableModel modeloErrores = modelo("#", "Tipo", "Descripción", "Encontrado", "Esperado", "Fila", "Columna");
    private final DefaultTableModel modeloSimbolos = modelo("#", "Nombre", "Tipo", "Ámbito", "Línea");
    private final DefaultTableModel modeloTokens = modelo("#", "Tipo", "Lexema", "Fila", "Columna");
    private final JTabbedPane pestanasInferiores = new JTabbedPane();
    private final JLabel estadoPosicion = new JLabel("Ln 1, Col 1");
    private final JLabel estadoResumen = new JLabel("Listo");
    private final JLabel nombreArchivo = new JLabel("sin_titulo.pz");

    private Path archivoActual;
    private boolean modificado;
    private ResultadoCompilacion ultimo;
    private final Compilador compilador = new Compilador();

    public MainFrame() {
        super("PromptZal IDE");
        Tema.instalar();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Tema.FONDO);
        setLayout(new BorderLayout());

        construirMenu();
        add(construirBarra(), BorderLayout.NORTH);
        add(construirCentro(), BorderLayout.CENTER);
        add(construirEstado(), BorderLayout.SOUTH);

        editor.setTexto(EJEMPLO);
        modificado = false;
        actualizarTitulo();

        editor.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                marcarModificado();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                marcarModificado();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        editor.getTextPane().addCaretListener(e -> {
            int pos = editor.getTextPane().getCaretPosition();
            javax.swing.text.Element raiz = editor.getTextPane().getDocument().getDefaultRootElement();
            int linea = raiz.getElementIndex(pos);
            int col = pos - raiz.getElement(linea).getStartOffset();
            estadoPosicion.setText("Ln " + (linea + 1) + ", Col " + (col + 1));
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salir();
            }
        });
    }

    //   construcción de la UI
    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private void construirMenu() {
        JMenuBar barra = new JMenuBar();
        barra.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE));

        JMenu archivo = new JMenu("Archivo");
        archivo.add(item("Nuevo", KeyEvent.VK_N, true, e -> nuevo()));
        archivo.add(item("Abrir...", KeyEvent.VK_O, true, e -> abrir()));
        archivo.addSeparator();
        archivo.add(item("Guardar", KeyEvent.VK_S, true, e -> guardar(false)));
        archivo.add(item("Guardar como...", 0, false, e -> guardar(true)));
        archivo.addSeparator();
        archivo.add(item("Salir", 0, false, e -> salir()));

        JMenu ejecutar = new JMenu("Compilar");
        ejecutar.add(item("Compilar", KeyEvent.VK_F5, false, e -> compilar()));
        ejecutar.add(item("Guardar promptzal...", 0, false, e -> guardarPrompt()));

        JMenu reportes = new JMenu("Reportes");
        reportes.add(item("Reporte de errores (HTML)", 0, false, e -> reporte("errores")));
        reportes.add(item("Reporte de tabla de símbolos (HTML)", 0, false, e -> reporte("simbolos")));
        reportes.add(item("Reporte del AST (HTML)", 0, false, e -> reporte("ast")));
        reportes.addSeparator();
        reportes.add(item("Generar todos en una carpeta...", 0, false, e -> reportesTodos()));

        JMenu ayuda = new JMenu("Ayuda");
        ayuda.add(item("Acerca de", 0, false, e -> JOptionPane.showMessageDialog(this,
                "PromptZal IDE - Proyecto 2\nLenguajes Formales y de Programación\n"
                + "Léxico: JFlex | Sintáctico: descendente recursivo a mano",
                "Acerca de", JOptionPane.INFORMATION_MESSAGE)));

        barra.add(archivo);
        barra.add(ejecutar);
        barra.add(reportes);
        barra.add(ayuda);
        setJMenuBar(barra);
    }

    private JMenuItem item(String texto, int tecla, boolean ctrl, java.awt.event.ActionListener accion) {
        JMenuItem it = new JMenuItem(texto);
        if (tecla != 0) {
            int mod = ctrl ? java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() : 0;
            it.setAccelerator(KeyStroke.getKeyStroke(tecla, mod));
        }
        it.addActionListener(accion);
        return it;
    }

    private JToolBar construirBarra() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        tb.add(boton("Nuevo", Tema.PANEL, e -> nuevo()));
        tb.add(boton("Abrir", Tema.PANEL, e -> abrir()));
        tb.add(boton("Guardar", Tema.PANEL, e -> guardar(false)));
        tb.addSeparator(new Dimension(12, 0));
        JButton compilar = boton("▶  Compilar", new Color(0x2F7D4F), e -> compilar());
        compilar.setFont(compilar.getFont().deriveFont(Font.BOLD));
        tb.add(compilar);
        tb.addSeparator(new Dimension(12, 0));
        tb.add(boton("Reportes HTML", Tema.PANEL, e -> reportesTodos()));
        tb.add(javax.swing.Box.createHorizontalGlue());
        nombreArchivo.setForeground(Tema.TEXTO_SUAVE);
        tb.add(nombreArchivo);
        return tb;
    }

    private JButton boton(String texto, Color fondo, java.awt.event.ActionListener accion) {
        JButton b = new JButton(texto);
        b.setBackground(fondo);
        b.setForeground(Tema.TEXTO);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE), BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(accion);
        return b;
    }

    private Component construirCentro() {
        // ---- panel del promptzal (derecha)
        configurarAreaTexto(panelPrompt, 14);
        JPanel prompt = new JPanel(new BorderLayout());
        prompt.add(titulo("PROMPTZAL GENERADO", "Guardar .md/.txt", e -> guardarPrompt()), BorderLayout.NORTH);
        prompt.add(desplazable(panelPrompt), BorderLayout.CENTER);

        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.add(titulo("EDITOR", null, null), BorderLayout.NORTH);
        izquierda.add(editor, BorderLayout.CENTER);

        JSplitPane superior = dividir(JSplitPane.HORIZONTAL_SPLIT, izquierda, prompt, 0.58);

        //  panel inferior con pestañas
        JTable tErrores = tabla(modeloErrores, true);
        tErrores.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int f = tErrores.getSelectedRow();
                    if (f >= 0) {
                        int fila = Integer.parseInt(modeloErrores.getValueAt(f, 5).toString());
                        int col = Integer.parseInt(modeloErrores.getValueAt(f, 6).toString());
                        editor.irA(fila, col);
                    }
                }
            }
        });
        configurarAreaTexto(panelAst, 13);
        configurarAreaTexto(consola, 13);

        pestanasInferiores.addTab("Errores", desplazable(tErrores));
        pestanasInferiores.addTab("Tabla de símbolos", desplazable(tabla(modeloSimbolos, false)));
        pestanasInferiores.addTab("AST", desplazable(panelAst));
        pestanasInferiores.addTab("Tokens", desplazable(tabla(modeloTokens, false)));
        pestanasInferiores.addTab("Consola", desplazable(consola));
        pestanasInferiores.setBackground(Tema.PANEL);
        pestanasInferiores.setForeground(Tema.TEXTO);

        JSplitPane principal = dividir(JSplitPane.VERTICAL_SPLIT, superior, pestanasInferiores, 0.68);
        return principal;
    }

    private JSplitPane dividir(int orientacion, Component a, Component b, double peso) {
        JSplitPane sp = new JSplitPane(orientacion, a, b);
        sp.setResizeWeight(peso);
        sp.setBorder(null);
        sp.setDividerSize(5);
        sp.setContinuousLayout(true);
        sp.setBackground(Tema.FONDO);
        sp.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(java.awt.Graphics g) {
                        g.setColor(Tema.BORDE);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });
        sp.setBorder(null);
        return sp;
    }

    private JPanel titulo(String texto, String botonTexto, java.awt.event.ActionListener accion) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.PANEL);
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE));
        JLabel l = new JLabel(texto);
        l.setForeground(Tema.TEXTO_SUAVE);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        l.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        p.add(l, BorderLayout.WEST);
        if (botonTexto != null) {
            JPanel d = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
            d.setOpaque(false);
            JButton b = boton(botonTexto, Tema.PANEL, accion);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Tema.BORDE), BorderFactory.createEmptyBorder(2, 10, 2, 10)));
            d.add(b);
            p.add(d, BorderLayout.EAST);
        }
        return p;
    }

    private void configurarAreaTexto(JTextArea a, int tam) {
        a.setEditable(false);
        a.setFont(Tema.monoespaciada(tam));
        a.setBackground(Tema.FONDO);
        a.setForeground(Tema.TEXTO);
        a.setCaretColor(Tema.TEXTO);
        a.setSelectionColor(Tema.SELECCION);
        a.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    private JScrollPane desplazable(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Tema.FONDO);
        return sp;
    }

    private JTable tabla(DefaultTableModel m, boolean colorearErrores) {
        JTable t = new JTable(m);
        t.setBackground(Tema.FONDO);
        t.setForeground(Tema.TEXTO);
        t.setGridColor(Tema.BORDE);
        t.setRowHeight(24);
        t.setShowVerticalLines(false);
        t.setFillsViewportHeight(true);
        t.setFont(t.getFont().deriveFont(13f));
        t.getTableHeader().setBackground(Tema.PANEL);
        t.getTableHeader().setForeground(Tema.TEXTO_SUAVE);
        t.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tb, v, sel, foc, row, col);
                c.setBackground(sel ? Tema.SELECCION : Tema.FONDO);
                c.setForeground(Tema.TEXTO);
                if (colorearErrores && col == 1) {
                    String s = String.valueOf(v);
                    c.setForeground(s.startsWith("Léx") ? Tema.AVISO : s.startsWith("Sint") ? Tema.ERROR : Tema.SEMANTICO);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        };
        t.setDefaultRenderer(Object.class, r);
        return t;
    }

    private JPanel construirEstado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE), BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        estadoResumen.setForeground(Tema.TEXTO_SUAVE);
        estadoPosicion.setForeground(Tema.TEXTO_SUAVE);
        estadoPosicion.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(estadoResumen, BorderLayout.WEST);
        p.add(estadoPosicion, BorderLayout.EAST);
        return p;
    }

    //   archivos
    private void marcarModificado() {
        if (!modificado) {
            modificado = true;
            actualizarTitulo();
        }
    }

    private void actualizarTitulo() {
        String n = archivoActual == null ? "sin_titulo.pz" : archivoActual.getFileName().toString();
        nombreArchivo.setText(n + (modificado ? " *" : ""));
        setTitle("PromptZal IDE - " + n + (modificado ? " *" : ""));
    }

    private boolean confirmarDescarte() {
        if (!modificado) {
            return true;
        }
        int r = JOptionPane.showConfirmDialog(this, "Hay cambios sin guardar. ¿Guardarlos?", "PromptZal",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (r == JOptionPane.CANCEL_OPTION || r == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (r == JOptionPane.YES_OPTION) {
            return guardar(false);
        }
        return true;
    }

    private void nuevo() {
        if (!confirmarDescarte()) {
            return;
        }
        archivoActual = null;
        editor.setTexto("");
        modificado = false;
        limpiarResultados();
        actualizarTitulo();
    }

    private void abrir() {
        if (!confirmarDescarte()) {
            return;
        }
        JFileChooser fc = new JFileChooser(archivoActual != null ? archivoActual.getParent().toFile() : null);
        fc.setFileFilter(new FileNameExtensionFilter("Archivos PromptZal (*.pz)", "pz"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path p = fc.getSelectedFile().toPath();
            editor.setTexto(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
            archivoActual = p;
            modificado = false;
            limpiarResultados();
            actualizarTitulo();
        } catch (IOException ex) {
            error("No se pudo abrir el archivo: " + ex.getMessage());
        }
    }

    private boolean guardar(boolean como) {
        if (archivoActual == null || como) {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Archivos PromptZal (*.pz)", "pz"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            Path p = fc.getSelectedFile().toPath();
            if (!p.getFileName().toString().toLowerCase().endsWith(".pz")) {
                p = p.resolveSibling(p.getFileName() + ".pz");
            }
            archivoActual = p;
        }
        try {
            Files.write(archivoActual, editor.getTexto().getBytes(StandardCharsets.UTF_8));
            modificado = false;
            actualizarTitulo();
            estadoResumen.setText("Archivo guardado: " + archivoActual);
            return true;
        } catch (IOException ex) {
            error("No se pudo guardar el archivo: " + ex.getMessage());
            return false;
        }
    }

    private void salir() {
        if (confirmarDescarte()) {
            dispose();
            System.exit(0);
        }
    }

    private void error(String m) {
        JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
    }

    //  compilación
    private void limpiarResultados() {
        ultimo = null;
        modeloErrores.setRowCount(0);
        modeloSimbolos.setRowCount(0);
        modeloTokens.setRowCount(0);
        panelPrompt.setText("");
        panelAst.setText("");
        editor.marcarErrores(null);
        estadoResumen.setText("Listo");
        pestanasInferiores.setTitleAt(0, "Errores");
    }

    private void compilar() {
        ResultadoCompilacion r = compilador.compilar(editor.getTexto());
        ultimo = r;

        modeloErrores.setRowCount(0);
        Set<Integer> lineas = new HashSet<>();
        int n = 1;
        for (ErrorCompilacion e : r.errores) {
            modeloErrores.addRow(new Object[]{n++, Reportes.nombreTipo(e.tipo), e.descripcion, e.encontrado,
                e.esperado, e.fila, e.columna});
            lineas.add(e.fila);
        }
        editor.marcarErrores(lineas);

        modeloSimbolos.setRowCount(0);
        n = 1;
        for (Simbolo s : r.tabla.getSimbolos()) {
            modeloSimbolos.addRow(new Object[]{n++, s.nombre, s.tipo.etiqueta, s.ambito, s.linea});
        }
        modeloTokens.setRowCount(0);
        n = 1;
        for (Token t : r.tokens) {
            modeloTokens.addRow(new Object[]{n++, t.tipo, t.lexema, t.fila, t.columna});
        }

        panelAst.setText(Reportes.astTexto(r.programa));
        panelAst.setCaretPosition(0);
        panelPrompt.setText(r.prompt);
        panelPrompt.setCaretPosition(0);

        int lex = r.contar(ErrorCompilacion.Tipo.LEXICO), sin = r.contar(ErrorCompilacion.Tipo.SINTACTICO),
                sem = r.contar(ErrorCompilacion.Tipo.SEMANTICO);
        String resumen = r.errores.isEmpty()
                ? "Compilación exitosa: 0 errores"
                : "Compilación con errores: " + lex + " léxico(s), " + sin + " sintáctico(s), " + sem + " semántico(s)";
        estadoResumen.setText(resumen);
        estadoResumen.setForeground(r.errores.isEmpty() ? Tema.OK : Tema.ERROR);
        pestanasInferiores.setTitleAt(0, "Errores (" + r.errores.size() + ")");
        pestanasInferiores.setSelectedIndex(0);

        consola.setText("");
        consola.append(resumen + "\n");
        consola.append("Tokens: " + r.tokens.size() + " | Símbolos: " + r.tabla.getSimbolos().size() + "\n");
        for (ErrorCompilacion e : r.errores) {
            consola.append(e + "\n");
        }
        if (!r.errores.isEmpty()) {
            consola.append("\nEl promptzal se generó con la información que se pudo reconocer.\n");
        }
    }

    private void guardarPrompt() {
        if (ultimo == null) {
            compilar();
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("promptzal.md"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Markdown (*.md)", "md"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Texto (*.txt)", "txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path p = fc.getSelectedFile().toPath();
        String nombre = p.getFileName().toString().toLowerCase();
        if (!nombre.endsWith(".md") && !nombre.endsWith(".txt")) {
            p = p.resolveSibling(p.getFileName() + ".md");
        }
        try {
            Reportes.guardar(p, ultimo.prompt);
            estadoResumen.setText("Promptzal guardado en " + p);
        } catch (IOException ex) {
            error("No se pudo guardar el promptzal: " + ex.getMessage());
        }
    }

    // ================================================================= reportes
    private Path carpetaReportes() {
        Path base = archivoActual != null && archivoActual.getParent() != null
                ? archivoActual.getParent() : Paths.get(System.getProperty("user.home"));
        return base.resolve("reportes_promptzal");
    }

    private void reporte(String cual) {
        if (ultimo == null) {
            compilar();
        }
        try {
            Path dir = carpetaReportes();
            Files.createDirectories(dir);
            Path destino;
            switch (cual) {
                case "errores":
                    destino = dir.resolve("reporte_errores.html");
                    Reportes.guardar(destino, Reportes.erroresHtml(ultimo));
                    break;
                case "simbolos":
                    destino = dir.resolve("reporte_tabla_simbolos.html");
                    Reportes.guardar(destino, Reportes.simbolosHtml(ultimo));
                    break;
                default:
                    destino = dir.resolve("reporte_ast.html");
                    Reportes.guardar(destino, Reportes.astHtml(ultimo.programa));
            }
            abrirEnNavegador(destino);
            estadoResumen.setText("Reporte generado: " + destino);
        } catch (IOException ex) {
            error("No se pudo generar el reporte: " + ex.getMessage());
        }
    }

    private void reportesTodos() {
        if (ultimo == null) {
            compilar();
        }
        JFileChooser fc = new JFileChooser(carpetaReportes().getParent() == null ? null : carpetaReportes().getParent().toFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Carpeta destino de los reportes");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            List<Path> rutas = Reportes.generarTodos(fc.getSelectedFile().toPath(), ultimo);
            estadoResumen.setText("Reportes generados en " + fc.getSelectedFile());
            abrirEnNavegador(rutas.get(0));
        } catch (IOException ex) {
            error("No se pudieron generar los reportes: " + ex.getMessage());
        }
    }

    private void abrirEnNavegador(Path p) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(p.toUri());
            }
        } catch (IOException | UnsupportedOperationException ignorada) {
        }
    }
}

package promptzal.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class EditorPane extends JPanel {

    private static final Pattern PATRON = Pattern.compile(
            "(?<com>//[^\\n]*|/\\*.*?(\\*/|\\z))"
            + "|(?<cad>\"(?:[^\"\\\\\\n]|\\\\.)*\"?)"
            + "|(?<dir>@\\p{L}+)"
            + "|(?<cmd>\\b(?:PREGUNTAR|GENERAR|RESUMIR|ANALIZAR|TRADUCIR|CLASIFICAR|EXTRAER|CARGAR)\\b)"
            + "|(?<kw>\\b(?:AGENTE|EJECUTAR|EXPORTAR|contexto|variable)\\b)"
            + "|(?<con>\\b(?:SOBRE|DESDE|EN|COMO)\\b)"
            + "|(?<num>\\b\\d+(?:\\.\\d+)?\\b)"
            + "|(?<fle>->)", Pattern.DOTALL);

    private final JTextPane texto = new JTextPane() {
        @Override
        public boolean getScrollableTracksViewportWidth() {   // sin salto de línea automático
            return getUI().getPreferredSize(this).width <= getParent().getWidth();
        }
    };
    private final Timer temporizador;
    private Set<Integer> lineasConError = new HashSet<>();

    private final SimpleAttributeSet normal = estilo(Tema.TEXTO, false, false);
    private final SimpleAttributeSet sCom = estilo(new Color(0x7A7E85), false, true);
    private final SimpleAttributeSet sCad = estilo(new Color(0x6A9955).brighter(), false, false);
    private final SimpleAttributeSet sDir = estilo(Tema.AVISO, true, false);
    private final SimpleAttributeSet sCmd = estilo(new Color(0x56A8F5), true, false);
    private final SimpleAttributeSet sKw = estilo(new Color(0xCF8E6D), true, false);
    private final SimpleAttributeSet sCon = estilo(Tema.SEMANTICO, false, false);
    private final SimpleAttributeSet sNum = estilo(new Color(0x2AACB8), false, false);
    private final SimpleAttributeSet sFle = estilo(Tema.ERROR, true, false);

    public EditorPane() {
        super(new BorderLayout());
        texto.setFont(Tema.monoespaciada(15));
        texto.setBackground(Tema.FONDO);
        texto.setForeground(Tema.TEXTO);
        texto.setCaretColor(Color.WHITE);
        texto.setSelectionColor(Tema.SELECCION);
        texto.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JScrollPane scroll = new JScrollPane(texto);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Tema.FONDO);
        scroll.setRowHeaderView(new Margen());
        add(scroll, BorderLayout.CENTER);

        texto.addCaretListener(e -> repaint());
        temporizador = new Timer(120, e -> resaltar());
        temporizador.setRepeats(false);
        texto.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                temporizador.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                temporizador.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    private static SimpleAttributeSet estilo(Color c, boolean negrita, boolean cursiva) {
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setForeground(s, c);
        StyleConstants.setBold(s, negrita);
        StyleConstants.setItalic(s, cursiva);
        return s;
    }

    public JTextPane getTextPane() {
        return texto;
    }

    public String getTexto() {
        return texto.getText().replace("\r\n", "\n");
    }

    public void setTexto(String t) {
        texto.setText(t);
        texto.setCaretPosition(0);
        resaltar();
    }

    public void addDocumentListener(DocumentListener l) {
        texto.getDocument().addDocumentListener(l);
    }

    public void marcarErrores(Set<Integer> lineas) {
        lineasConError = lineas == null ? Collections.emptySet() : lineas;
        repaint();
    }

    /**
     * Coloca el cursor en la fila y columna indicadas (inician en 1).
     */
    public void irA(int fila, int columna) {
        Element raiz = texto.getDocument().getDefaultRootElement();
        int idx = Math.max(0, Math.min(fila - 1, raiz.getElementCount() - 1));
        Element linea = raiz.getElement(idx);
        int pos = Math.min(linea.getStartOffset() + Math.max(0, columna - 1), linea.getEndOffset() - 1);
        texto.requestFocusInWindow();
        texto.setCaretPosition(Math.max(0, pos));
    }

    private void resaltar() {
        StyledDocument doc = texto.getStyledDocument();
        String s;
        try {
            s = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }
        doc.setCharacterAttributes(0, s.length(), normal, true);
        Matcher m = PATRON.matcher(s);
        while (m.find()) {
            SimpleAttributeSet a = null;
            if (m.group("com") != null) {
                a = sCom;
            } else if (m.group("cad") != null) {
                a = sCad;
            } else if (m.group("dir") != null) {
                a = sDir;
            } else if (m.group("cmd") != null) {
                a = sCmd;
            } else if (m.group("kw") != null) {
                a = sKw;
            } else if (m.group("con") != null) {
                a = sCon;
            } else if (m.group("num") != null) {
                a = sNum;
            } else if (m.group("fle") != null) {
                a = sFle;
            }
            if (a != null) {
                doc.setCharacterAttributes(m.start(), m.end() - m.start(), a, true);
            }
        }
    }

    /**
     * Columna de números de línea.
     */
    private class Margen extends JComponent {

        Margen() {
            setFont(Tema.monoespaciada(13));
            setOpaque(true);
            texto.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    actualizar();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    actualizar();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
        }

        private void actualizar() {
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int lineas = texto.getDocument().getDefaultRootElement().getElementCount();
            int digitos = Math.max(3, String.valueOf(lineas).length());
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.charWidth('0') * digitos + 26, texto.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Tema.PANEL);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Tema.BORDE);
            g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            Rectangle clip = g.getClipBounds();
            Element raiz = texto.getDocument().getDefaultRootElement();
            int ini = raiz.getElementIndex(texto.viewToModel2D(new Point(0, clip.y)));
            int fin = raiz.getElementIndex(texto.viewToModel2D(new Point(0, clip.y + clip.height)));
            int actual = raiz.getElementIndex(texto.getCaretPosition());
            for (int i = ini; i <= fin; i++) {
                try {
                    Rectangle2D r = texto.modelToView2D(raiz.getElement(i).getStartOffset());
                    if (r == null) {
                        continue;
                    }
                    int y = (int) r.getY() + fm.getAscent() + (int) ((r.getHeight() - fm.getHeight()) / 2);
                    boolean error = lineasConError.contains(i + 1);
                    if (error) {
                        g.setColor(Tema.ERROR);
                        g.fillOval(5, (int) r.getY() + (int) (r.getHeight() / 2) - 3, 6, 6);
                    }
                    g.setColor(i == actual ? Tema.TEXTO : Tema.TEXTO_SUAVE);
                    String n = String.valueOf(i + 1);
                    g.drawString(n, getWidth() - 10 - fm.stringWidth(n), y);
                } catch (BadLocationException ignorada) {
                }
            }
        }
    }
}

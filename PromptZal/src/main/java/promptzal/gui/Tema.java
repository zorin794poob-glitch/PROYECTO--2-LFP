package promptzal.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

/**
 * Colores y fuentes del tema oscuro (estilo IDE).
 */
public final class Tema {

    private Tema() {
    }

    public static final Color FONDO = new Color(0x1E1F22);
    public static final Color PANEL = new Color(0x2B2D30);
    public static final Color BORDE = new Color(0x393B40);
    public static final Color TEXTO = new Color(0xDFE1E5);
    public static final Color TEXTO_SUAVE = new Color(0x8C8F94);
    public static final Color ACENTO = new Color(0x3574F0);
    public static final Color SELECCION = new Color(0x2E436E);
    public static final Color ERROR = new Color(0xE06C75);
    public static final Color AVISO = new Color(0xE5C07B);
    public static final Color SEMANTICO = new Color(0xC678DD);
    public static final Color OK = new Color(0x98C379);

    public static Font monoespaciada(int tam) {
        String[] fuentes = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String f : new String[]{"JetBrains Mono", "Consolas", "Cascadia Mono", "Menlo", "DejaVu Sans Mono"}) {
            if (Arrays.asList(fuentes).contains(f)) {
                return new Font(f, Font.PLAIN, tam);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, tam);
    }

    /**
     * Ajusta los valores por defecto de Swing al tema oscuro (llamar antes de
     * crear componentes).
     */
    public static void instalar() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignorada) {
        }
        ColorUIResource fondo = new ColorUIResource(FONDO), panel = new ColorUIResource(PANEL),
                texto = new ColorUIResource(TEXTO), borde = new ColorUIResource(BORDE),
                acento = new ColorUIResource(ACENTO), sel = new ColorUIResource(SELECCION);
        for (String k : new String[]{"Panel.background", "SplitPane.background", "ScrollPane.background",
            "Viewport.background", "TabbedPane.background", "TabbedPane.contentAreaColor", "TabbedPane.unselectedBackground",
            "ToolBar.background", "MenuBar.background", "Menu.background", "MenuItem.background",
            "PopupMenu.background", "OptionPane.background", "Table.background", "TableHeader.background",
            "ScrollBar.background", "ScrollBar.track", "TabbedPane.light", "TabbedPane.highlight"}) {
            UIManager.put(k, k.startsWith("Table") || k.startsWith("Viewport") ? fondo : panel);
        }
        UIManager.put("TableHeader.background", panel);
        for (String k : new String[]{"Label.foreground", "TabbedPane.foreground", "Menu.foreground", "MenuItem.foreground",
            "MenuBar.foreground", "Button.foreground", "Table.foreground", "TableHeader.foreground",
            "OptionPane.messageForeground", "TextArea.foreground", "TextPane.foreground", "ToolTip.foreground",
            "CheckBoxMenuItem.foreground"}) {
            UIManager.put(k, texto);
        }
        UIManager.put("Button.background", panel);
        UIManager.put("ScrollBar.thumb", borde);
        UIManager.put("ScrollBar.thumbShadow", borde);
        UIManager.put("ScrollBar.thumbHighlight", borde);
        UIManager.put("ScrollBar.thumbDarkShadow", borde);
        UIManager.put("ScrollBar.darkShadow", borde);
        UIManager.put("ScrollBar.shadow", borde);
        UIManager.put("ScrollBar.highlight", borde);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("TabbedPane.selected", fondo);
        UIManager.put("TabbedPane.selectHighlight", acento);
        UIManager.put("TabbedPane.darkShadow", borde);
        UIManager.put("TabbedPane.shadow", borde);
        UIManager.put("TabbedPane.focus", fondo);
        UIManager.put("Table.selectionBackground", sel);
        UIManager.put("Table.selectionForeground", texto);
        UIManager.put("Table.gridColor", borde);
        UIManager.put("Menu.selectionBackground", sel);
        UIManager.put("MenuItem.selectionBackground", sel);
        UIManager.put("MenuItem.selectionForeground", texto);
        UIManager.put("Menu.selectionForeground", texto);
        UIManager.put("MenuItem.acceleratorForeground", new ColorUIResource(TEXTO_SUAVE));
        UIManager.put("SplitPane.dividerFocusColor", panel);
        UIManager.put("ToolTip.background", panel);
        UIManager.put("OptionPane.background", panel);
        UIManager.put("Button.select", sel);
        UIManager.put("Button.focus", new ColorUIResource(ACENTO));
        UIManager.put("textHighlight", sel);
    }
}

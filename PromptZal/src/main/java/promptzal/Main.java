package promptzal;

import javax.swing.SwingUtilities;
import promptzal.gui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}

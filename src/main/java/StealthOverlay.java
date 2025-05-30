import javax.swing.*;
import java.awt.*;

public class StealthOverlay {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("StealthOverlay");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 100);
            frame.setLocationRelativeTo(null);

            JLabel label = new JLabel("Injected JFrame running!", SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 14));
            frame.add(label);

            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
        });
    }
}

import javax.swing.*;
import java.awt.*;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.Platform;

public class StealthOverlay {

    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        boolean SetWindowDisplayAffinity(WinDef.HWND hWnd, int dwAffinity);

        int GWL_EXSTYLE = -20;
        int WS_EX_LAYERED = 0x80000;
        int WS_EX_TRANSPARENT = 0x00000020;
        int WS_EX_NOREDIRECTIONBITMAP = 0x00200000;

        WinDef.HWND FindWindowA(String lpClassName, String lpWindowName);

        long GetWindowLongPtrW(WinDef.HWND hWnd, int nIndex);
        long SetWindowLongPtrW(WinDef.HWND hWnd, int nIndex, long dwNewLong);

        int GetWindowLong(WinDef.HWND hWnd, int nIndex);
        int SetWindowLong(WinDef.HWND hWnd, int nIndex, int dwNewLong);

        boolean InvalidateRect(WinDef.HWND hWnd, Object lpRect, boolean bErase);
        boolean UpdateWindow(WinDef.HWND hWnd);
    }

    public static long getWindowLong(WinDef.HWND hwnd, int index) {
        if (Platform.is64Bit()) {
            return User32.INSTANCE.GetWindowLongPtrW(hwnd, index);
        } else {
            return User32.INSTANCE.GetWindowLong(hwnd, index);
        }
    }

    public static long setWindowLong(WinDef.HWND hwnd, int index, long newValue) {
        if (Platform.is64Bit()) {
            return User32.INSTANCE.SetWindowLongPtrW(hwnd, index, newValue);
        } else {
            return User32.INSTANCE.SetWindowLong(hwnd, index, (int) newValue);
        }
    }

    public static void applySecurityProperties(String windowTitle) {
        WinDef.HWND hwnd = User32.INSTANCE.FindWindowA(null, windowTitle);
        if (hwnd == null) {
            System.out.println("Window not found for screen-hide protection");
            return;
        }

        boolean result = User32.INSTANCE.SetWindowDisplayAffinity(hwnd, 0x00000011); // WDA_EXCLUDE
        System.out.println("SetWindowDisplayAffinity: " + result);

        long exStyle = getWindowLong(hwnd, User32.GWL_EXSTYLE);
        long newStyle = exStyle
                | User32.WS_EX_LAYERED
                | User32.WS_EX_TRANSPARENT
                | User32.WS_EX_NOREDIRECTIONBITMAP
                | 0x00000080; // WS_EX_TOOLWINDOW

        setWindowLong(hwnd, User32.GWL_EXSTYLE, newStyle);
        User32.INSTANCE.InvalidateRect(hwnd, null, true);
        User32.INSTANCE.UpdateWindow(hwnd);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SecureFrame");

            frame.setAlwaysOnTop(true);
            frame.setSize(600, 300);
            frame.setLocation(1000, 600);
            frame.setUndecorated(false);
            frame.setBackground(new Color(0, 0, 0, 255));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JLabel label = new JLabel("🔒 Hidden from screen share", SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 18));
            label.setForeground(Color.GREEN);
            frame.add(label);

            frame.setVisible(true);

            // wait a second to let OS register the window
//            new Thread(() -> {
//                try {
//                    Thread.sleep(1000);
//                    applySecurityProperties("SecureFrame");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }).start();
        });
    }
}

package com.hl.main;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class SecureFrame implements Runnable{
    static HWND hwnd;
    private static final int WDA_EXCLUDE = 0x00000011;  // updated to use WDA_EXCLUDE
    private static final int WDA_MONITOR = 0x00000001;
    static JWindow frame;  // Changed from JFrame to JWindow
    static JLabel titleLabel;
    static Color textColor;
    static JTextArea contentArea;
    static JPanel contentContainer;
    // setup the JNA calls
    public interface User32 extends StdCallLibrary {

        User32 INSTANCE = Native.load("user32", User32.class);

        boolean SetWindowDisplayAffinity(WinDef.HWND hWnd, int dwAffinity);
        int GWL_EXSTYLE = -20;
        int WS_EX_LAYERED = 0x80000;
        int WS_EX_TRANSPARENT = 0x00000020;
        int WS_EX_NOREDIRECTIONBITMAP = 0x00200000;

        HWND FindWindowA(String lpClassName, String lpWindowName);
        HWND GetForegroundWindow();
        HWND GetActiveWindow();
        int GetWindowThreadProcessId(HWND hWnd, int[] lpdwProcessId);

        // exported func
        long GetWindowLongPtrW(HWND hWnd, int nIndex);
        long SetWindowLongPtrW(HWND hWnd, int nIndex, long dwNewLong);
        // 32-bit
        int GetWindowLong(HWND hWnd, int nIndex);
        int SetWindowLong(HWND hWnd, int nIndex, int dwNewLong);
        boolean ShowWindow(HWND hWnd, int nCmdShow);
        boolean UpdateWindow(HWND hWnd);
        boolean InvalidateRect(HWND hWnd, Object lpRect, boolean bErase);

        // additional methods for finding JWindow
        boolean EnumWindows(WndEnumProc lpEnumFunc, int lParam);
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
        int GetClassNameA(HWND hWnd, byte[] lpClassName, int nMaxCount);
    }

    // Kernel32 interface for process functions
    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        int GetCurrentProcessId();
        HWND GetConsoleWindow();
        boolean ShowWindow(HWND hWnd, int nCmdShow);

        // constants for ShowWindow
        int SW_HIDE = 0;
    }

    public static void frameSetup() throws Exception {
        textColor = new Color(255, 255, 200);
        // Hide console window if running from command line
        hideConsoleWindow();

        // init jframe
        frame = new JWindow();
        frame.setAlwaysOnTop(true);
        frame.setSize(600, 400);
        applyRoundedCorners();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - frame.getWidth();
        int y = screenSize.height - frame.getHeight();

        setupContent();
        frame.setLocation(x, y);
        frame.setVisible(true);

        // let OS update new frame
        Thread.sleep(1000);
    }

    public static void setupContent() {
        JPanel panel = createTransparentPanel(new BorderLayout());

        // title label
        titleLabel = new JLabel("Press Up or Down to slide thorugh (+/- to execute)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(textColor);
        titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.BLACK));

        contentArea = new JTextArea(Main.pt.getStringArr()[0]);
        setAreaProperties(contentArea);

        JPanel titleContainer = createColoredPanel(new Color(151, 120, 255, 160));
        contentContainer = createColoredPanel(new Color(153, 153, 255, 160));

        titleContainer.add(titleLabel, BorderLayout.CENTER);
        contentContainer.add(contentArea, BorderLayout.CENTER);

        panel.add(titleContainer, BorderLayout.NORTH);
        panel.add(contentContainer, BorderLayout.CENTER);

        frame.getContentPane().add(panel);
        frame.setBackground(new Color(0, 0, 0, 65));
    }

    public static void changeContent(String content) {
        // clear the text area completely
        contentArea.setText("");

        // force a complete repaint of the entire frame to clear ghost text
        frame.repaint();

        // small delay to ensure the clear operation completes
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        contentArea.setText(content);

        // refresh the layout and repaint
        contentArea.revalidate();
        contentContainer.revalidate();
        frame.repaint();
    }


    private static JPanel createTransparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel createColoredPanel(Color bgColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(bgColor);
        return panel;
    }
    private static void setAreaProperties(JTextArea contentArea){
        contentArea.setFont(new Font("Segoe UI", Font.BOLD, 18));
        contentArea.setForeground(textColor);
        contentArea.setOpaque(false);
        contentArea.setEditable(false);
        contentArea.setWrapStyleWord(true);
        contentArea.setLineWrap(true);
        contentArea.setHighlighter(null);
        contentArea.setFocusable(false);
        contentArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private static void applyRoundedCorners() {
        frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 40, 40));
    }






    /**
     * Interface for window enumeration
     * */
    public interface WndEnumProc extends StdCallLibrary.StdCallCallback {
        boolean callback(HWND hWnd, int lParam);
    }

    public static long getWindowLong(HWND hwnd, int index) {
        if (Platform.is64Bit()) {
            return User32.INSTANCE.GetWindowLongPtrW(hwnd, index);
        } else {
            return User32.INSTANCE.GetWindowLong(hwnd, index);
        }
    }

    public static long setWindowLong(HWND hwnd, int index, long newValue) {
        if (Platform.is64Bit()) {
            return User32.INSTANCE.SetWindowLongPtrW(hwnd, index, newValue);
        } else {
            return User32.INSTANCE.SetWindowLong(hwnd, index, (int) newValue);
        }
    }

    /**
     * Method to find JWindow handle since it doesn't have a title
     * */
    public static HWND findJWindowHandle() {
        final HWND[] foundWindow = {null};
        final int currentProcessId = Kernel32.INSTANCE.GetCurrentProcessId();

        WndEnumProc enumProc = new WndEnumProc() {
            @Override
            public boolean callback(HWND hWnd, int lParam) {
                int[] processId = new int[1];
                User32.INSTANCE.GetWindowThreadProcessId(hWnd, processId);

                // Check if window belongs to our process
                if (processId[0] == currentProcessId) {
                    byte[] className = new byte[256];
                    int classNameLength = User32.INSTANCE.GetClassNameA(hWnd, className, className.length);
                    if (classNameLength > 0) {
                        String classNameStr = new String(className, 0, classNameLength);
                        // JWindow typically has class name starting with "SunAwtFrame" or similar
                        if (classNameStr.contains("SunAwtFrame") || classNameStr.contains("SunAwtWindow")) {
                            // verify window dimensions match our JWindow
                            foundWindow[0] = hWnd;
                            return false; // Stop enumeration
                        }
                    }
                }
                return true; // Continue enumeration
            }
        };

        User32.INSTANCE.EnumWindows(enumProc, 0);
        return foundWindow[0];
    }

    /**
     *  Method to hide console window
     *  */
    public static void hideConsoleWindow() {
        HWND consoleWindow = Kernel32.INSTANCE.GetConsoleWindow();
        if (consoleWindow != null) {
            Kernel32.INSTANCE.ShowWindow(consoleWindow, Kernel32.SW_HIDE);
            System.out.println("Console window hidden");
        }
    }

    /**
     * change process name using system properties (less effective but safer)
     * */
    public static void changeProcessName(String newName) {
        System.setProperty("java.awt.headless", "false");
        // This doesn't actually change the process name but can help with some detection
        System.setProperty("sun.java.command", newName);
        System.setProperty("sun.java2d.noddraw", "true");
    }

    /**
     * This will change the properties of the window, don't change UI beyond here
     * */
    public static void changeProperties(){
        // Try multiple methods to find the JWindow handle
        hwnd = User32.INSTANCE.FindWindowA(null, "SecureFrame"); // This will likely fail for JWindow
        if (hwnd == null) {
            hwnd = findJWindowHandle(); // Use our custom method
        }
        if (hwnd == null) {
            hwnd = User32.INSTANCE.GetForegroundWindow(); // get current foreground window
        }

        if (hwnd == null) {
            System.out.println("frame doesn't exist");
            return;
        }

        /**
         * set up Window Display Affinity (primary method)
         * */
        boolean affinityResult = User32.INSTANCE.SetWindowDisplayAffinity(hwnd, WDA_EXCLUDE);
        System.out.println("SetWindowDisplayAffinity result: " + affinityResult);

        // set extended window styles
        long exStyle = getWindowLong(hwnd, User32.GWL_EXSTYLE);
        long newStyle = exStyle
                | User32.WS_EX_LAYERED
                | User32.WS_EX_TRANSPARENT
                | User32.WS_EX_NOREDIRECTIONBITMAP
//                        | 0x00200000 // WS_EX_NOREDIRECTIONBITMAP
                | 0x00000080; // WS_EX_TOOLWINDOW
        long setResult = setWindowLong(hwnd, User32.GWL_EXSTYLE, newStyle);
        System.out.println("result: " + setResult);

        // force window update
        User32.INSTANCE.InvalidateRect(hwnd, null, true);
        User32.INSTANCE.UpdateWindow(hwnd);

        // allow clickability through

        System.out.println("window is now protected from screen capture");
        System.out.println("extended style changed from: 0x" + Long.toHexString(exStyle) +
                " to: 0x" + Long.toHexString(newStyle));

    }


    /**
     * failsafe if accidentally brick an entire application
     * note: it has happened, don't test on external project
     * This method attempts multiple approaches to undo stealth modifications
     */
    public static void undoChangedProperties(){
        if (hwnd == null) {
            System.out.println("frame doesn't exist");
            return;
        }

        // Method 1: try to restore Window Display Affinity to normal monitoring
        boolean affinityResult1 = User32.INSTANCE.SetWindowDisplayAffinity(hwnd, WDA_MONITOR);
        System.out.println("Method 1 - SetWindowDisplayAffinity(WDA_MONITOR) result: " + affinityResult1);

        // Method 2: try setting affinity to 0 (default/none)
        boolean affinityResult2 = User32.INSTANCE.SetWindowDisplayAffinity(hwnd, 0);
        System.out.println("Method 2 - SetWindowDisplayAffinity(0) result: " + affinityResult2);

        // get current extended window style
        long exStyle = getWindowLong(hwnd, User32.GWL_EXSTYLE);
        System.out.println("Current extended style: 0x" + Long.toHexString(exStyle));

        // remove stealth-related extended window styles
        long newStyle = exStyle & ~(
                User32.WS_EX_TRANSPARENT |       // Remove transparency (click-through)
                        User32.WS_EX_NOREDIRECTIONBITMAP | // Remove no-redirection bitmap
                        0x00000080                       // Remove WS_EX_TOOLWINDOW
        );

        // apply the restored style
        long setResult = setWindowLong(hwnd, User32.GWL_EXSTYLE, newStyle);
        System.out.println("SetWindowLong result: " + setResult);

        // force multiple window updates
        User32.INSTANCE.InvalidateRect(hwnd, null, true);
        User32.INSTANCE.UpdateWindow(hwnd);
        User32.INSTANCE.ShowWindow(hwnd, 5); // SW_SHOW

        // try hiding and showing the window to reset its state
        User32.INSTANCE.ShowWindow(hwnd, 0); // SW_HIDE
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        User32.INSTANCE.ShowWindow(hwnd, 5); // SW_SHOW

        // restore normal window behavior at Java level
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);  // show again to reset
            frame.toFront();
            frame.repaint();
        });

        // wait and force final updates
        try {
            Thread.sleep(200);
            User32.INSTANCE.InvalidateRect(hwnd, null, true);
            User32.INSTANCE.UpdateWindow(hwnd);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Extended style changed from: 0x" + Long.toHexString(exStyle) +
                " to: 0x" + Long.toHexString(newStyle));
    }


    public static void changeIcon() throws MalformedURLException {
        URL url = new File("C:\\Users\\justi\\IdeaProjects\\Honorlock\\configs\\walk-0.png").toURI().toURL();

        Toolkit kit = Toolkit.getDefaultToolkit();
        Image img = kit.createImage(url);
        frame.setIconImage(img);
    }
    @Override
    public void run() {
        try {
            System.out.println("hit the entry");

            // add propreties
            changeProcessName("SystemService");
            frameSetup();
            changeProperties();
            changeIcon();

            // uncomment if you want to remove the stealth gui
//            Thread.sleep(2500);
//            undoChangedProperties();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
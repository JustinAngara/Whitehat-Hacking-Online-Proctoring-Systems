package com.hl.main;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;

import javax.swing.*;
import java.awt.*;

public class SecureFrame implements Runnable{
    static HWND hwnd;
    private static final int WDA_EXCLUDE = 0x00000011;  // updated to use WDA_EXCLUDE
    private static final int WDA_MONITOR = 0x00000001;
    static JWindow frame;  // Changed from JFrame to JWindow
    static JLabel titleLabel;
    static JLabel contentLabel;

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

        // Additional methods for finding JWindow
        boolean EnumWindows(WndEnumProc lpEnumFunc, int lParam);
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
        int GetClassNameA(HWND hWnd, byte[] lpClassName, int nMaxCount);
    }

    // Kernel32 interface for process functions
    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        int GetCurrentProcessId();
    }

    public static void frameSetup() throws Exception{
        frame = new JWindow();  // Using JWindow instead of JFrame

        frame.setAlwaysOnTop(true);
        frame.setLocation(0, 0);
        frame.setSize(0, 0);
        frame.setLocationRelativeTo(null);
        frame.setBackground(new Color(0, 0, 0, 255));
        // JWindow doesn't have setDefaultCloseOperation or setResizable or setUndecorated
        setupContent();
        frame.setVisible(true);
        // let OS update new frame
        Thread.sleep(1000);
    }

    public static void setupContent() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        titleLabel = new JLabel("hidden from screnshare", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.RED);
        panel.setLayout(new BorderLayout());
        panel.add(titleLabel, BorderLayout.CENTER);

        frame.getContentPane().add(panel);

        contentLabel = new JLabel("Press Up or Down to slide thorugh");
        panel.add(contentLabel, BorderLayout.NORTH);
        contentLabel.setFont(new Font("Arial", Font.BOLD, 20));
        contentLabel.setForeground(Color.RED);
    }

    /*
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

    /*
    * method to find JWindow handle since it doesn't have a title
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
                            // Additional check: verify window dimensions match our JWindow
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

    /*
     * This will change the properties of the window, don't change UI beyond here
     * */
    public static void changeProperties(){
        // Try multiple methods to find the JWindow handle
        hwnd = User32.INSTANCE.FindWindowA(null, "SecureFrame"); // This will likely fail for JWindow
        if (hwnd == null) {
            hwnd = findJWindowHandle(); // Use our custom method
        }
        if (hwnd == null) {
            hwnd = User32.INSTANCE.GetForegroundWindow(); // Last resort - get current foreground window
        }

        if (hwnd == null) {
            System.out.println("frame doesn't exist");
            return;
        }

        // set up Window Display Affinity (primary method)
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

        // this will allow to display the frame and it's entirety
        frame.setSize(600, 300);
    }

    /*
     * failsafe if accidentally brick an entire application
     * note: it has happened, don't test on external project
     */
    public static void undoChangedProperties(){
        if (hwnd == null) {
            System.out.println("frame doesn't exist");
            return;
        }

        boolean affinityResult = User32.INSTANCE.SetWindowDisplayAffinity(hwnd, WDA_MONITOR);
        System.out.println("Restore SetWindowDisplayAffinity result: " + affinityResult);

        long exStyle = getWindowLong(hwnd, User32.GWL_EXSTYLE);
        long newStyle = exStyle & ~(User32.WS_EX_TRANSPARENT | User32.WS_EX_NOREDIRECTIONBITMAP);
        setWindowLong(hwnd, User32.GWL_EXSTYLE, newStyle);

        User32.INSTANCE.InvalidateRect(hwnd, null, true);
        User32.INSTANCE.UpdateWindow(hwnd);

        System.out.println("Window protection removed");
    }

    @Override
    public void run() {
        try {
            System.out.println("hit the entry");
            frameSetup();
            changeProperties();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
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
    static JFrame frame;
    static JLabel label;
    public static void frameSetup() throws Exception{
        frame = new JFrame("SecureFrame");

        frame.setAlwaysOnTop(true);
        frame.setLocation(0, 0);
        frame.setSize(0, 0);
        frame.setLocationRelativeTo(null);
        frame.setBackground(new Color(0, 0, 0, 255));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setUndecorated(true);
        setupPanel();
        frame.setVisible(true);
        // let OS update new frame
        Thread.sleep(1000);


    }

    public static void setupPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        label = new JLabel("hidden from screnshare", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        label.setForeground(Color.RED);
        panel.setLayout(new BorderLayout());
        panel.add(label, BorderLayout.CENTER);

        frame.getContentPane().add(panel);

        JLabel lblNewLabel = new JLabel("Press Up or Down to slide thorugh");
        panel.add(lblNewLabel, BorderLayout.NORTH);
        lblNewLabel.setFont(new Font("Arial", Font.BOLD, 20));
        lblNewLabel.setForeground(Color.RED);
    }




    // setup the JNA calls
    public interface User32 extends StdCallLibrary {

        User32 INSTANCE = Native.load("user32", User32.class);

        boolean SetWindowDisplayAffinity(WinDef.HWND hWnd, int dwAffinity);
        int GWL_EXSTYLE = -20;
        int WS_EX_LAYERED = 0x80000;
        int WS_EX_TRANSPARENT = 0x00000020;

        int WS_EX_NOREDIRECTIONBITMAP = 0x00200000;

        HWND FindWindowA(String lpClassName, String lpWindowName);
        // exported func
        long GetWindowLongPtrW(HWND hWnd, int nIndex);
        long SetWindowLongPtrW(HWND hWnd, int nIndex, long dwNewLong);
        // 32-bit
        int GetWindowLong(HWND hWnd, int nIndex);

        int SetWindowLong(HWND hWnd, int nIndex, int dwNewLong);
        boolean ShowWindow(HWND hWnd, int nCmdShow);
        boolean UpdateWindow(HWND hWnd);
        boolean InvalidateRect(HWND hWnd, Object lpRect, boolean bErase);

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
    * This will change the properties of the window, don't change UI beyond here
    * */
    public static void changeProperties(){
        hwnd = User32.INSTANCE.FindWindowA(null, "SecureFrame");
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
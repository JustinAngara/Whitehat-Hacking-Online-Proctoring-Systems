package com.hl.main;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static com.sun.jna.platform.win32.Win32VK.VK_LEFT;
import static com.sun.jna.platform.win32.Win32VK.VK_RIGHT;

public class SecureFrame implements Runnable {

    // ===== Win32/JNA =====
    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        boolean SetWindowDisplayAffinity(HWND hWnd, int dwAffinity);

        int GWL_EXSTYLE = -20;
        int WS_EX_LAYERED = 0x00080000;
        int WS_EX_TRANSPARENT = 0x00000020;
        int WS_EX_TOOLWINDOW = 0x00000080;
        int WS_EX_TOPMOST = 0x00000008;
        int WS_EX_NOACTIVATE = 0x08000000;

        HWND FindWindowA(String lpClassName, String lpWindowName);
        HWND GetForegroundWindow();
        HWND GetActiveWindow();
        int GetWindowThreadProcessId(HWND hWnd, DWORDByReference lpdwProcessId);

        long GetWindowLongPtrW(HWND hWnd, int nIndex);
        long SetWindowLongPtrW(HWND hWnd, int nIndex, long dwNewLong);
        int GetWindowLong(HWND hWnd, int nIndex);
        int SetWindowLong(HWND hWnd, int nIndex, int dwNewLong);
        boolean ShowWindow(HWND hWnd, int nCmdShow);
        boolean UpdateWindow(HWND hWnd);
        boolean InvalidateRect(HWND hWnd, Object lpRect, boolean bErase);

        boolean EnumWindows(WndEnumProc lpEnumFunc, int lParam);
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
        int GetClassNameA(HWND hWnd, byte[] lpClassName, int nMaxCount);
    }

    public interface User32Extra extends com.sun.jna.platform.win32.User32 {
        User32Extra INSTANCE = Native.load("user32", User32Extra.class);
        short GetAsyncKeyState(int vKey);
    }

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        int GetCurrentProcessId();
        HWND GetConsoleWindow();
        boolean ShowWindow(HWND hWnd, int nCmdShow);
        int SW_HIDE = 0;
    }

    public interface WndEnumProc extends StdCallLibrary.StdCallCallback {
        boolean callback(HWND hWnd, int lParam);
    }

    // ===== Constants =====
    private static final int WDA_EXCLUDE = 0x00000011; // WDA_EXCLUDEFROMCAPTURE
    private static final int WDA_MONITOR = 0x00000001; // unused but kept for reference

    // Map overlay scroll to arrow keys (intentionally LEFT=up, RIGHT=down per prior behavior)
    private static final int VK_UP = VK_LEFT.code;
    private static final int VK_DOWN = VK_RIGHT.code;

    // ===== UI Fields =====
    static JWindow frame;
    static JLabel titleLabel;
    static JTextPane contentArea;
    private static JScrollPane scrollPaneRef;
    static JPanel contentContainer;
    static Color textColor = new Color(255, 255, 200);

    // Native window handle
    static HWND hwnd;

    // ===== Public API =====
    @Override
    public void run() {
        try {
            changeProcessName("SystemService"); // disguise process title a bit
            frameSetup();
            startHoverScrollThread();
            changeProperties();
            changeIcon(); // safe no-op if icon missing
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== Window + Overlay =====
    public static void frameSetup() throws Exception {
        hideConsoleWindow();

        frame = new JWindow();
        frame.setAlwaysOnTop(true);
        frame.setSize(680, 420);
        frame.setFocusable(false);
        frame.setFocusableWindowState(false);
        try {
            frame.setAutoRequestFocus(false);
        } catch (Throwable ignored) {}

        applyRoundedCorners();
        setupContent();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screen.width - frame.getWidth() - 24;
        int y = screen.height - frame.getHeight() - 48;
        frame.setLocation(x, y);

        // Transparent background so rounded corners look proper
        frame.setBackground(new Color(0, 0, 0, 0));
        frame.setVisible(true);
    }

    public static void setupContent() {
        JPanel root = createTransparentPanel(new BorderLayout());

        // Header (gradient + border)
        titleLabel = new JLabel("Press LEFT to scroll ↑   |   RIGHT to scroll ↓   (overlay is click-through)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(230, 235, 255));
        titleLabel.setFocusable(false);

        JPanel titleContainer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                GradientPaint paint = new GradientPaint(0, 0, new Color(60, 55, 105), 0, h, new Color(40, 38, 78));
                g2.setPaint(paint);
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        titleContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(30, 30, 50)));
        titleContainer.setFocusable(false);
        titleContainer.add(titleLabel, BorderLayout.CENTER);

        // Code pane
        contentArea = new JTextPane();
        configureCodePane(contentArea);
        contentArea.setText("Overlay initialized...");
        applySyntaxColors(contentArea);

        // Scroll pane
        scrollPaneRef = new JScrollPane(
                contentArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPaneRef.setBorder(BorderFactory.createEmptyBorder());
        scrollPaneRef.setFocusable(false);
        scrollPaneRef.getViewport().setOpaque(true);
        scrollPaneRef.getViewport().setBackground(new Color(22, 22, 28));

        // Line numbers
        LineNumberView lineNumbers = new LineNumberView(contentArea);
        scrollPaneRef.setRowHeaderView(lineNumbers);

        // Content container
        contentContainer = new JPanel(new BorderLayout());
        contentContainer.setOpaque(true);
        contentContainer.setBackground(new Color(22, 22, 28));
        contentContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(35, 35, 50)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        contentContainer.setFocusable(false);
        contentContainer.add(scrollPaneRef, BorderLayout.CENTER);

        root.add(titleContainer, BorderLayout.NORTH);
        root.add(contentContainer, BorderLayout.CENTER);
        root.setFocusable(false);

        frame.getContentPane().removeAll();
        frame.getContentPane().add(root);
        frame.revalidate();
        frame.repaint();
    }

    public static void changeContent(String content) {
        contentArea.setText(content == null ? "" : content);
        applySyntaxColors(contentArea);
        contentArea.setCaretPosition(0);
        frame.repaint();
    }

    private static JPanel createTransparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        panel.setFocusable(false);
        return panel;
    }

    private static void applyRoundedCorners() {
        frame.setShape(new RoundRectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight(), 40, 40));
    }

    // ===== Hover + Scroll (key polled only when mouse over frame) =====
    private static void startHoverScrollThread() {
        new Thread(() -> {
            while (true) {
                try {
                    if (frame == null || !frame.isShowing()) {
                        Thread.sleep(100);
                        continue;
                    }
                    Point mousePos = MouseInfo.getPointerInfo().getLocation();
                    Rectangle bounds = frame.getBounds();
                    Point frameLoc = frame.getLocationOnScreen();
                    bounds.setLocation(frameLoc);

                    if (bounds.contains(mousePos) && scrollPaneRef != null) {
                        JScrollBar v = scrollPaneRef.getVerticalScrollBar();
                        int step = v.getUnitIncrement() + 10;

                        // Scroll down
                        if ((User32Extra.INSTANCE.GetAsyncKeyState(VK_DOWN) & 0x8000) != 0) {
                            v.setValue(v.getValue() + step);
                            Thread.sleep(50);
                        }
                        // Scroll up
                        if ((User32Extra.INSTANCE.GetAsyncKeyState(VK_UP) & 0x8000) != 0) {
                            v.setValue(v.getValue() - step);
                            Thread.sleep(50);
                        }
                    }
                    Thread.sleep(10);
                } catch (Exception e) {
                    // keep overlay resilient
                }
            }
        }, "HoverScrollThread").start();
    }

    // ===== Window Style helpers =====
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

    public static HWND findJWindowHandle() {
        final HWND[] foundWindow = {null};
        final int currentPid = Kernel32.INSTANCE.GetCurrentProcessId();

        WndEnumProc enumProc = (hWnd, lParam) -> {
            DWORDByReference pidRef = new DWORDByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hWnd, pidRef);

            if (pidRef.getValue().intValue() == currentPid) {
                byte[] className = new byte[256];
                int n = User32.INSTANCE.GetClassNameA(hWnd, className, className.length);
                if (n > 0) {
                    String cls = new String(className, 0, n);
                    // JWindow/Frame classes on Windows typically match these:
                    if (cls.contains("SunAwtWindow") || cls.contains("SunAwtFrame")) {
                        foundWindow[0] = hWnd;
                        return false;
                    }
                }
            }
            return true;
        };

        User32.INSTANCE.EnumWindows(enumProc, 0);
        return foundWindow[0];
    }

    public static void changeProperties() {
        hwnd = findJWindowHandle();
        if (hwnd == null) {
            System.out.println("Could not locate window handle.");
            return;
        }

        // Exclude from screen capture
        User32.INSTANCE.SetWindowDisplayAffinity(hwnd, WDA_EXCLUDE);

        long exStyle = getWindowLong(hwnd, User32.GWL_EXSTYLE);
        long newStyle = exStyle
                | User32.WS_EX_LAYERED
                | User32.WS_EX_TRANSPARENT
                | User32.WS_EX_TOOLWINDOW
                | User32.WS_EX_NOACTIVATE;

        long prev = setWindowLong(hwnd, User32.GWL_EXSTYLE, newStyle);
        System.out.println("Old style: 0x" + Long.toHexString(prev) + " New style: 0x" + Long.toHexString(newStyle));

        User32.INSTANCE.InvalidateRect(hwnd, null, true);
        User32.INSTANCE.UpdateWindow(hwnd);
    }

    public static void hideConsoleWindow() {
        HWND console = Kernel32.INSTANCE.GetConsoleWindow();
        if (console != null) {
            Kernel32.INSTANCE.ShowWindow(console, Kernel32.SW_HIDE);
        }
    }

    public static void changeProcessName(String newName) {
        System.setProperty("java.awt.headless", "false");
        System.setProperty("sun.java.command", newName);
        System.setProperty("sun.java2d.noddraw", "true");
    }

    public static void changeIcon() throws MalformedURLException {
        // Optional: set app icon in taskbar if available
        File f = new File("C:\\Users\\justi\\IdeaProjects\\Honorlock\\configs\\walk-0.png");
        if (!f.exists()) return;
        URL url = f.toURI().toURL();
        Image img = Toolkit.getDefaultToolkit().createImage(url);
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar.getTaskbar().setIconImage(img);
            }
        } catch (Throwable ignored) {
            // Not supported on this platform/JRE
        }
    }

    // ===== Syntax Highlighting =====
    private static void configureCodePane(JTextPane pane) {
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setOpaque(true);
        pane.setBackground(new Color(22, 22, 28));
        pane.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        pane.putClientProperty("caretAspectRatio", 0.2f);

        // Monospace font choices
        Font mono = new Font("JetBrains Mono", Font.PLAIN, 16);
        if (mono.getFamily().equals("Dialog")) mono = new Font("Consolas", Font.PLAIN, 16);
        if (mono.getFamily().equals("Dialog")) mono = new Font("Cascadia Mono", Font.PLAIN, 16);
        if (mono.getFamily().equals("Dialog")) mono = new Font("Courier New", Font.PLAIN, 16);
        pane.setFont(mono);

        // Tweak selection colors
        UIManager.put("TextPane.selectionBackground", new Color(60, 65, 90));
        pane.setSelectedTextColor(new Color(240, 240, 255));
    }

    private static final String[] JAVA_KEYWORDS = new String[]{
            "abstract","assert","boolean","break","byte","case","catch","char","class","const","continue","default",
            "do","double","else","enum","extends","final","finally","float","for","goto","if","implements","import",
            "instanceof","int","interface","long","native","new","package","private","protected","public","return",
            "short","static","strictfp","super","switch","synchronized","this","throw","throws","transient","try",
            "void","volatile","while","var","record","sealed","permits","non-sealed"
    };

    private static SimpleAttributeSet ATTR_BASE, ATTR_KW, ATTR_TYPE, ATTR_NUM, ATTR_STR, ATTR_COM, ATTR_ANN;

    static {
        ATTR_BASE = attr(new Color(220, 220, 235), Font.PLAIN);
        ATTR_KW   = attr(new Color(137, 180, 250), Font.BOLD);   // keywords
        ATTR_TYPE = attr(new Color(180, 220, 140), Font.PLAIN);  // class/interface names
        ATTR_NUM  = attr(new Color(244, 184, 113), Font.PLAIN);  // numbers
        ATTR_STR  = attr(new Color(144, 221, 245), Font.PLAIN);  // strings/char
        ATTR_COM  = attr(new Color(120, 125, 145), Font.ITALIC); // comments
        ATTR_ANN  = attr(new Color(198, 146, 234), Font.PLAIN);  // annotations
    }

    private static SimpleAttributeSet attr(Color fg, int style) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, fg);
        StyleConstants.setBold(a, (style & Font.BOLD) != 0);
        StyleConstants.setItalic(a, (style & Font.ITALIC) != 0);
        return a;
    }

    private static void applySyntaxColors(JTextPane pane) {
        StyledDocument doc = pane.getStyledDocument();
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (Exception e) {
            return;
        }

        // Base
        doc.setCharacterAttributes(0, text.length(), ATTR_BASE, true);

        // Block comments
        applyRegex(doc, text, "/\\*.*?\\*/", ATTR_COM, true);
        // Line comments
        applyRegex(doc, text, "//.*?$", ATTR_COM, true);
        // Strings
        applyRegex(doc, text, "\"([^\"\\\\]|\\\\.)*\"", ATTR_STR, true);
        applyRegex(doc, text, "'([^'\\\\]|\\\\.)*'", ATTR_STR, true);
        // Numbers
        applyRegex(doc, text, "\\b(0x[0-9a-fA-F]+|\\d+(?:_\\d+)*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\b", ATTR_NUM, false);
        // Annotations
        applyRegex(doc, text, "@\\w+", ATTR_ANN, false);
        // Keywords
        String kw = String.join("|", JAVA_KEYWORDS);
        applyRegex(doc, text, "\\b(" + kw + ")\\b", ATTR_KW, false);
        // Naive type names
        applyRegex(doc, text, "\\b(?:class|interface|enum|record)\\s+([A-Za-z_][A-Za-z0-9_]*)", ATTR_TYPE, false);
    }

    private static void applyRegex(StyledDocument doc, String text, String pattern, AttributeSet attr, boolean dotAll) {
        int flags = java.util.regex.Pattern.MULTILINE;
        if (dotAll) flags |= java.util.regex.Pattern.DOTALL;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern, flags).matcher(text);
        while (m.find()) {
            int start = m.start();
            int len = m.end() - m.start();
            doc.setCharacterAttributes(start, len, attr, false);
            if (m.groupCount() >= 1 && m.group(1) != null) {
                int gStart = m.start(1);
                int gLen = m.end(1) - m.start(1);
                doc.setCharacterAttributes(gStart, gLen, attr, false);
            }
        }
    }

    // ===== Line Number Gutter =====
    @SuppressWarnings("serial")
    private static class LineNumberView extends JComponent implements DocumentListener, CaretListener {
        private final JTextPane text;
        private final Font font = new Font("Consolas", Font.PLAIN, 13);
        private int lastDigits = 2;

        LineNumberView(JTextPane text) {
            this.text = text;
            setOpaque(true);
            setBackground(new Color(18, 18, 24));
            setForeground(new Color(120, 125, 145));
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(35, 35, 50)));
            text.getDocument().addDocumentListener(this);
            text.addCaretListener(this);
            setPreferredWidth();
        }

        private void setPreferredWidth() {
            int lineCount = Math.max(1, text.getDocument().getDefaultRootElement().getElementCount());
            int digits = Math.max(2, String.valueOf(lineCount).length());
            if (digits != lastDigits) {
                lastDigits = digits;
                FontMetrics fm = getFontMetrics(font);
                int width = 12 + fm.charWidth('0') * digits;
                setPreferredSize(new Dimension(width, Integer.MAX_VALUE));
                revalidate();
            }
        }

        @Override public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setFont(font);
            FontMetrics fm = getFontMetrics(font);

            Rectangle clip = g.getClipBounds();
            int base = clip.y;
            int end = clip.y + clip.height;

            int startOffset = text.viewToModel(new Point(0, base));
            int endOffset = text.viewToModel(new Point(0, end));

            Element root = text.getDocument().getDefaultRootElement();
            int startLine = root.getElementIndex(startOffset);
            int endLine = root.getElementIndex(endOffset);

            for (int line = startLine; line <= endLine; line++) {
                Element e = root.getElement(line);
                try {
                    Rectangle r = text.modelToView(e.getStartOffset());
                    if (r == null) continue;
                    String num = String.valueOf(line + 1);
                    int x = getWidth() - 6 - g.getFontMetrics().stringWidth(num);
                    int y = r.y + r.height - fm.getDescent();
                    g.setColor(getForeground());
                    g.drawString(num, x, y);
                } catch (Exception ignored) {}
            }
        }

        // Listeners
        @Override public void insertUpdate(DocumentEvent e) { setPreferredWidth(); repaint(); }
        @Override public void removeUpdate(DocumentEvent e) { setPreferredWidth(); repaint(); }
        @Override public void changedUpdate(DocumentEvent e) { setPreferredWidth(); repaint(); }
        @Override public void caretUpdate(CaretEvent e) { repaint(); }
    }

}

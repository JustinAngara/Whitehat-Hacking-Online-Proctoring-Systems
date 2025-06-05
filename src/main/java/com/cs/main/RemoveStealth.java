/*
* This class and package is a rebuttal against those stealthy GUIs!
* I am in progress of creating an anti virus/cheat to remove stealth related GUIs!
* Currently experimenting on how to approach this problem
* */

package com.cs.main;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RemoveStealth {
    private static final int WDA_MONITOR = 0x00000001;

    // User32 interface for Windows API calls
    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);

        boolean SetWindowDisplayAffinity(HWND hWnd, int dwAffinity);
        int GWL_EXSTYLE = -20;
        int WS_EX_LAYERED = 0x80000;
        int WS_EX_TRANSPARENT = 0x00000020;
        int WS_EX_NOREDIRECTIONBITMAP = 0x00200000;

        HWND FindWindowA(String lpClassName, String lpWindowName);
        HWND GetForegroundWindow();
        HWND GetActiveWindow();
        int GetWindowThreadProcessId(HWND hWnd, int[] lpdwProcessId);

        // 64-bit and 32-bit window long functions
        long GetWindowLongPtrW(HWND hWnd, int nIndex);
        long SetWindowLongPtrW(HWND hWnd, int nIndex, long dwNewLong);
        int GetWindowLong(HWND hWnd, int nIndex);
        int SetWindowLong(HWND hWnd, int nIndex, int dwNewLong);

        boolean ShowWindow(HWND hWnd, int nCmdShow);
        boolean UpdateWindow(HWND hWnd);
        boolean InvalidateRect(HWND hWnd, Object lpRect, boolean bErase);

        // Window enumeration functions
        boolean EnumWindows(WndEnumProc lpEnumFunc, int lParam);
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
        int GetClassNameA(HWND hWnd, byte[] lpClassName, int nMaxCount);
        boolean IsWindowVisible(HWND hWnd);
    }

    // Kernel32 interface for process functions
    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        int GetCurrentProcessId();
        HWND GetConsoleWindow();
        boolean ShowWindow(HWND hWnd, int nCmdShow);

        // Constants for ShowWindow
        int SW_HIDE = 0;
        int SW_SHOW = 5;
    }

    // Interface for window enumeration callback
    public interface WndEnumProc extends StdCallLibrary.StdCallCallback {
        boolean callback(HWND hWnd, int lParam);
    }

    // Helper methods for 32/64-bit compatibility
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
     * Remove stealth properties from all windows belonging to a specific process name
     * @param processName The name of the process (e.g., "notepad.exe", "chrome.exe")
     * @return Number of windows that had stealth removed
     */
    public static int removeStealthByProcessName(String processName) {
        System.out.println("Searching for windows belonging to process: " + processName);

        List<HWND> targetWindows = findWindowsByProcessName(processName);

        if (targetWindows.isEmpty()) {
            System.out.println("No windows found for process: " + processName);
            return 0;
        }

        System.out.println("Found " + targetWindows.size() + " windows for process: " + processName);

        int successCount = 0;
        for (int i = 0; i < targetWindows.size(); i++) {
            HWND windowHandle = targetWindows.get(i);
            System.out.println("Attempting to remove stealth from window " + (i + 1) + "...");

            boolean success = removeStealthFromWindow(windowHandle);
            if (success) {
                successCount++;
                System.out.println("Successfully removed stealth from window " + (i + 1));
            } else {
                System.out.println("Failed to remove stealth from window " + (i + 1));
            }
        }

        System.out.println("Stealth removal completed. Success: " + successCount + "/" + targetWindows.size());
        return successCount;
    }

    /**
     * Find all windows belonging to a specific process name
     * @param processName The process name to search for
     * @return List of window handles belonging to the process
     */
    private static List<HWND> findWindowsByProcessName(String processName) {
        final List<HWND> foundWindows = new ArrayList<>();
        final String targetProcessName = processName.toLowerCase();

        WndEnumProc enumProc = new WndEnumProc() {
            @Override
            public boolean callback(HWND hWnd, int lParam) {
                try {
                    // Get process ID for this window
                    int[] processId = new int[1];
                    User32.INSTANCE.GetWindowThreadProcessId(hWnd, processId);

                    // Get process name from process ID
                    String currentProcessName = getProcessNameById(processId[0]);

                    if (currentProcessName != null &&
                            currentProcessName.toLowerCase().equals(targetProcessName)) {

                        // Check if window is visible (has some content)
                        if (isWindowVisible(hWnd)) {
                            foundWindows.add(hWnd);
                            System.out.println("Found target window - Process: " + currentProcessName +
                                    ", Handle: " + hWnd.getPointer());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing window: " + e.getMessage());
                }

                return true; // Continue enumeration
            }
        };

        User32.INSTANCE.EnumWindows(enumProc, 0);
        return foundWindows;
    }

    /**
     * Get process name by process ID using additional Windows API calls
     * @param processId The process ID
     * @return Process name or null if not found
     */
    private static String getProcessNameById(int processId) {
        try {
            // This is a simplified approach - you might need to add more robust process name detection
            // For now, we'll use a different approach by checking window titles and class names
            return "unknown"; // Placeholder - see alternative method below
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Alternative method: Find windows by executable name in window title or class
     * @param executableName The executable name (e.g., "notepad", "chrome")
     * @return List of matching window handles
     */
    public static List<HWND> findWindowsByExecutableName(String executableName) {
        final List<HWND> foundWindows = new ArrayList<>();
        final String targetName = executableName.toLowerCase();

        WndEnumProc enumProc = new WndEnumProc() {
            @Override
            public boolean callback(HWND hWnd, int lParam) {
                try {
                    // Get window title
                    byte[] titleBuffer = new byte[512];
                    int titleLength = User32.INSTANCE.GetWindowTextA(hWnd, titleBuffer, titleBuffer.length);
                    String windowTitle = "";
                    if (titleLength > 0) {
                        windowTitle = new String(titleBuffer, 0, titleLength).toLowerCase();
                    }

                    // Get class name
                    byte[] classBuffer = new byte[256];
                    int classLength = User32.INSTANCE.GetClassNameA(hWnd, classBuffer, classBuffer.length);
                    String className = "";
                    if (classLength > 0) {
                        className = new String(classBuffer, 0, classLength).toLowerCase();
                    }

                    // Check if window title or class contains the target executable name
                    if ((windowTitle.contains(targetName) || className.contains(targetName)) &&
                            isWindowVisible(hWnd)) {
                        foundWindows.add(hWnd);
                        System.out.println("Found window - Title: '" + windowTitle +
                                "', Class: '" + className + "'");
                    }

                } catch (Exception e) {
                    System.err.println("Error processing window: " + e.getMessage());
                }

                return true; // Continue enumeration
            }
        };

        User32.INSTANCE.EnumWindows(enumProc, 0);
        return foundWindows;
    }

    /**
     * Check if a window is visible and has content
     * @param hWnd Window handle
     * @return true if window appears to be visible
     */
    private static boolean isWindowVisible(HWND hWnd) {
        // Check if window is actually visible using Windows API
        boolean isVisible = User32.INSTANCE.IsWindowVisible(hWnd);

        // Also check if it has a title (additional verification)
        byte[] titleBuffer = new byte[256];
        int titleLength = User32.INSTANCE.GetWindowTextA(hWnd, titleBuffer, titleBuffer.length);
        boolean hasTitle = titleLength > 0;

        return isVisible || hasTitle; // Return true if either condition is met
    }

    /**
     * Remove stealth properties from a specific window handle
     * @param windowHandle The window handle to modify
     * @return true if successful, false otherwise
     */
    private static boolean removeStealthFromWindow(HWND windowHandle) {
        try {
            System.out.println("Attempting to remove stealth from window handle: " + windowHandle.getPointer());

            // Method 1: Try to restore Window Display Affinity
            boolean affinityResult1 = User32.INSTANCE.SetWindowDisplayAffinity(windowHandle, WDA_MONITOR);
            System.out.println("SetWindowDisplayAffinity(WDA_MONITOR) result: " + affinityResult1);

            // Method 2: Try setting affinity to 0 (default)
            boolean affinityResult2 = User32.INSTANCE.SetWindowDisplayAffinity(windowHandle, 0);
            System.out.println("SetWindowDisplayAffinity(0) result: " + affinityResult2);

            // Get and modify extended window styles
            long exStyle = getWindowLong(windowHandle, User32.GWL_EXSTYLE);
            System.out.println("Current extended style: 0x" + Long.toHexString(exStyle));

            // Remove stealth-related styles
            long newStyle = exStyle & ~(
                    User32.WS_EX_TRANSPARENT |
                            User32.WS_EX_NOREDIRECTIONBITMAP |
                            0x00000080 // WS_EX_TOOLWINDOW
            );

            long setResult = setWindowLong(windowHandle, User32.GWL_EXSTYLE, newStyle);
            System.out.println("SetWindowLong result: " + setResult);

            // Force window updates
            User32.INSTANCE.InvalidateRect(windowHandle, null, true);
            User32.INSTANCE.UpdateWindow(windowHandle);

            // Try hide/show cycle
            User32.INSTANCE.ShowWindow(windowHandle, 0); // Hide
            Thread.sleep(100);
            User32.INSTANCE.ShowWindow(windowHandle, 5); // Show

            System.out.println("Stealth removal attempted for window: " + windowHandle.getPointer());
            return affinityResult1 || affinityResult2; // Success if either affinity call worked

        } catch (Exception e) {
            System.err.println("Error removing stealth from window: " + e.getMessage());
            return false;
        }
    }

    /**
     * Convenience method to remove stealth by partial executable name match
     * @param partialName Partial name to match (e.g., "notepad" will match "notepad.exe")
     * @return Number of windows that had stealth removed
     */
    public static int removeStealthByPartialName(String partialName) {
        System.out.println("Searching for windows with partial name: " + partialName);

        List<HWND> targetWindows = findWindowsByExecutableName(partialName);

        if (targetWindows.isEmpty()) {
            System.out.println("No windows found matching: " + partialName);
            return 0;
        }

        int successCount = 0;
        for (HWND windowHandle : targetWindows) {
            if (removeStealthFromWindow(windowHandle)) {
                successCount++;
            }
        }

        return successCount;
    }

    /**
     * Find and remove stealth from windows with specific window title
     * @param windowTitle Exact window title to match
     * @return Number of windows that had stealth removed
     */
    public static int removeStealthByWindowTitle(String windowTitle) {
        System.out.println("Searching for windows with title: " + windowTitle);

        final List<HWND> foundWindows = new ArrayList<>();
        final String targetTitle = windowTitle.toLowerCase();

        WndEnumProc enumProc = new WndEnumProc() {
            @Override
            public boolean callback(HWND hWnd, int lParam) {
                try {
                    byte[] titleBuffer = new byte[512];
                    int titleLength = User32.INSTANCE.GetWindowTextA(hWnd, titleBuffer, titleBuffer.length);

                    if (titleLength > 0) {
                        String currentTitle = new String(titleBuffer, 0, titleLength);
                        if (currentTitle.toLowerCase().equals(targetTitle)) {
                            foundWindows.add(hWnd);
                            System.out.println("Found window with exact title: " + currentTitle);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing window: " + e.getMessage());
                }
                return true;
            }
        };

        User32.INSTANCE.EnumWindows(enumProc, 0);

        int successCount = 0;
        for (HWND windowHandle : foundWindows) {
            if (removeStealthFromWindow(windowHandle)) {
                successCount++;
            }
        }

        return successCount;
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        // Example usage:
        System.out.println("=== RemoveStealth Utility ===");
        // Remove stealth from Java applications
        int removed = removeStealthByPartialName("java");
        System.out.println("Removed stealth from " + removed + " Java windows");
    }
}
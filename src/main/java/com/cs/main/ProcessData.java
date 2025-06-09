package com.cs.main;
import com.sun.jna.platform.win32.WinDef;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a Windows process with its windows and display affinity information
 */
public class ProcessData {
    private final int processId;
    private final String processName;
    private final String executablePath;
    private final LocalDateTime discoveredAt;
    private final List<WindowData> windows;

    public ProcessData(int processId, String processName, String executablePath) {
        this.processId = processId;
        this.processName = processName;
        this.executablePath = executablePath;
        this.discoveredAt = LocalDateTime.now();
        this.windows = new ArrayList<>();
    }

    // Getters
    public int getProcessId() { return processId; }
    public String getProcessName() { return processName; }
    public String getExecutablePath() { return executablePath; }
    public LocalDateTime getDiscoveredAt() { return discoveredAt; }
    public List<WindowData> getWindows() { return new ArrayList<>(windows); }

    // Window management
    public void addWindow(WindowData window) {
        windows.add(window);
    }

    public void removeWindow(WindowData window) {
        windows.remove(window);
    }

    public List<WindowData> getExcludedWindows() {
        return windows.stream()
                .filter(w -> w.isExcludedFromCapture())
                .toList();
    }

    public boolean hasExcludedWindows() {
        return windows.stream().anyMatch(WindowData::isExcludedFromCapture);
    }

    @Override
    public String toString() {
        return String.format("ProcessData{pid=%d, name='%s', windows=%d, excluded=%d}",
                processId, processName, windows.size(), getExcludedWindows().size());
    }

    /**
     * Inner class to represent window data
     */
    public static class WindowData {
        private final long hwndValue;
        private final WinDef.HWND hwnd;
        private final String title;
        private final boolean visible;
        private int displayAffinity;
        private final LocalDateTime discoveredAt;
        private LocalDateTime lastChecked;

        public WindowData(WinDef.HWND hwnd, String title, boolean visible) {
            this.hwnd = hwnd;
            this.hwndValue = com.sun.jna.Pointer.nativeValue(hwnd.getPointer());
            this.title = title;
            this.visible = visible;
            this.displayAffinity = -1; // Unknown
            this.discoveredAt = LocalDateTime.now();
            this.lastChecked = LocalDateTime.now();
        }

        // Getters
        public WinDef.HWND getHwnd() { return hwnd; }
        public long getHwndValue() { return hwndValue; }
        public String getTitle() { return title; }
        public boolean isVisible() { return visible; }
        public int getDisplayAffinity() { return displayAffinity; }
        public LocalDateTime getDiscoveredAt() { return discoveredAt; }
        public LocalDateTime getLastChecked() { return lastChecked; }

        // Setters
        public void setDisplayAffinity(int affinity) {
            this.displayAffinity = affinity;
            this.lastChecked = LocalDateTime.now();
        }

        // Convenience methods
        public boolean isExcludedFromCapture() {
            return displayAffinity == 0x11; // WDA_EXCLUDEFROMCAPTURE
        }

        public boolean isNormalCapture() {
            return displayAffinity == 0x01; // WDA_MONITOR
        }

        public String getAffinityDescription() {
            return switch (displayAffinity) {
                case 0x00 -> "None (default)";
                case 0x01 -> "Monitor (normal capture)";
                case 0x11 -> "Excluded from capture";
                default -> "Unknown (" + displayAffinity + ")";
            };
        }

        @Override
        public String toString() {
            return String.format("WindowData{hwnd=0x%x, title='%s', affinity=%s}",
                    hwndValue, title, getAffinityDescription());
        }
    }
}
package com.cs.main;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcessHandler {
    private List<ProcessData> processList;
    private List<ProcessData> excludedFromCapture;

    // Display Affinity Constants
    public static final int WDA_NONE = 0x00;
    public static final int WDA_MONITOR = 0x01;
    public static final int WDA_EXCLUDEFROMCAPTURE = 0x11;

    // Extended User32 interface with missing methods
    public interface ExtendedUser32 extends StdCallLibrary {
        ExtendedUser32 INSTANCE = Native.load("user32", ExtendedUser32.class);

        boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);
        int GetWindowThreadProcessId(WinDef.HWND hWnd, IntByReference lpdwProcessId);
        int GetWindowTextA(WinDef.HWND hWnd, byte[] lpString, int nMaxCount);
        int GetWindowTextW(WinDef.HWND hWnd, char[] lpString, int nMaxCount);
        int GetWindowTextLengthW(WinDef.HWND hWnd);
        boolean GetWindowDisplayAffinity(WinDef.HWND hWnd, IntByReference pdwAffinity);
        boolean SetWindowDisplayAffinity(WinDef.HWND hWnd, int dwAffinity);
        boolean IsWindowVisible(WinDef.HWND hWnd);
        boolean IsWindow(WinDef.HWND hWnd);
        int GetLastError();
    }

    public interface Psapi extends Library {
        Psapi INSTANCE = Native.load("psapi", Psapi.class);
        int GetModuleBaseNameW(WinNT.HANDLE hProcess, WinDef.HMODULE hModule, char[] lpBaseName, int nSize);
    }

    public ProcessHandler() {
        this.processList = new ArrayList<>();
    }

    public void getProcessName() {
        processList.clear();

        Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));

        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry)) {
                String exeFile = Native.toString(processEntry.szExeFile);
                int pid = processEntry.th32ProcessID.intValue();

                ProcessData process = new ProcessData(pid, exeFile, exeFile);
                processList.add(process);

                System.out.println("PID: " + pid + " | Process: " + exeFile);

                if (process.hasExcludedWindows()) {
                    System.out.println("Process has excluded windows:");
                    for (ProcessData.WindowData w : process.getExcludedWindows()) {
                        System.out.println("  " + w.getTitle());
                    }
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }

        System.out.println("Total processes stored: " + processList.size());
    }

    public void checkProcessDisplayAffinity(String targetProcess) {
        Set<Integer> matchingPIDs = new HashSet<>();

        Tlhelp32.PROCESSENTRY32.ByReference entry = new Tlhelp32.PROCESSENTRY32.ByReference();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));

        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, entry)) {
                String exe = Native.toString(entry.szExeFile);
                if (exe.equalsIgnoreCase(targetProcess)) {
                    matchingPIDs.add(entry.th32ProcessID.intValue());
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }

        if (matchingPIDs.isEmpty()) {
            System.out.println("No running processes named " + targetProcess);
            return;
        }

        System.out.println("Found PIDs for " + targetProcess + ": " + matchingPIDs);
        System.out.println("Checking display affinity for all windows...\n");

        final int[] windowCount = {0};
        final int[] excludedCount = {0};

        // Use ExtendedUser32 instead of User32
        ExtendedUser32.INSTANCE.EnumWindows((hWnd, data) -> {
            try {
                if (!ExtendedUser32.INSTANCE.IsWindow(hWnd)) {
                    return true;
                }

                IntByReference pidRef = new IntByReference();
                ExtendedUser32.INSTANCE.GetWindowThreadProcessId(hWnd, pidRef);
                int pid = pidRef.getValue();

                if (matchingPIDs.contains(pid)) {
                    windowCount[0]++;

                    char[] windowTextW = new char[512];
                    int titleLength = ExtendedUser32.INSTANCE.GetWindowTextW(hWnd, windowTextW, 512);
                    String title = titleLength > 0 ? Native.toString(windowTextW) : "<No Title>";

                    boolean isVisible = ExtendedUser32.INSTANCE.IsWindowVisible(hWnd);

                    System.out.println("Window: \"" + title + "\"");
                    System.out.println("  -> PID: " + pid);
                    System.out.println("  -> HWND: 0x" + Long.toHexString(Pointer.nativeValue(hWnd.getPointer())));
                    System.out.println("  -> Visible: " + isVisible);

                    IntByReference affinity = new IntByReference();
                    boolean result = ExtendedUser32.INSTANCE.GetWindowDisplayAffinity(hWnd, affinity);

                    if (result) {
                        int value = affinity.getValue();
                        System.out.println("  -> Display Affinity: 0x" + Integer.toHexString(value));

                        switch (value) {
                            case ProcessHandler.WDA_NONE:
                                System.out.println("     -> NONE (default)");
                                break;
                            case ProcessHandler.WDA_MONITOR:
                                System.out.println("     -> MONITOR (normal capture allowed)");
                                break;
                            case ProcessHandler.WDA_EXCLUDEFROMCAPTURE:
                                System.out.println("     -> EXCLUDED FROM CAPTURE");
                                excludedCount[0]++;
                                break;
                            default:
                                System.out.println("     -> UNKNOWN flag: " + value);
                                break;
                        }
                    } else {
                        int error = ExtendedUser32.INSTANCE.GetLastError();
                        System.out.println("  -> Could not get affinity (Error: " + error + ")");

                        switch (error) {
                            case 5:
                                System.out.println("     -> Access denied - may need elevated privileges");
                                break;
                            case 6:
                                System.out.println("     -> Invalid window handle");
                                break;
                            case 120:
                                System.out.println("     -> Function not supported on this Windows version");
                                break;
                            default:
                                System.out.println("     -> Unknown error");
                                break;
                        }
                    }
                    System.out.println();
                }
            } catch (Exception e) {
                System.err.println("Error processing window: " + e.getMessage());
            }

            return true;
        }, null);

        System.out.println("Summary:");
        System.out.println("  Total windows found for " + targetProcess + ": " + windowCount[0]);
        System.out.println("  Windows excluded from capture: " + excludedCount[0]);
    }

    // Other methods remain the same...
    public List<ProcessData> getProcessList() {
        return new ArrayList<>(processList);
    }

    public List<ProcessData> findProcessesByName(String processName) {
        return processList.stream()
                .filter(p -> p.getProcessName().equalsIgnoreCase(processName))
                .toList();
    }

    public ProcessData findProcessByPid(int pid) {
        return processList.stream()
                .filter(p -> p.getProcessId() == pid)
                .findFirst()
                .orElse(null);
    }

    public int getProcessCount() {
        return processList.size();
    }

    public void printStoredProcesses() {
        System.out.println("=== Stored Processes ===");
        for (ProcessData process : processList) {
            System.out.println(process);
        }
    }
}
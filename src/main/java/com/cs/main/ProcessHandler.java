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
    // Store the list of processes as a class field
    private List<ProcessData> processList;
    private List<ProcessData> excludedFromCapture;

    // Display Affinity Constants
    public static final int WDA_NONE = 0x00;
    public static final int WDA_MONITOR = 0x01;
    public static final int WDA_EXCLUDEFROMCAPTURE = 0x11;
    public interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class);
        boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);
        int GetWindowThreadProcessId(WinDef.HWND hWnd, IntByReference lpdwProcessId);
        int GetWindowTextA(WinDef.HWND hWnd, byte[] lpString, int nMaxCount);
        int GetWindowTextW(WinDef.HWND hWnd, char[] lpString, int nMaxCount);
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

    public void getProcessName(){
        // Clear existing list before scanning
        processList.clear();

        Tlhelp32.PROCESSENTRY32.ByReference processEntry = new Tlhelp32.PROCESSENTRY32.ByReference();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));

        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, processEntry)) {
                String exeFile = Native.toString(processEntry.szExeFile);
                int pid = processEntry.th32ProcessID.intValue();

                // Create ProcessData object and add to list
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

    // Getter for the process list
    public List<ProcessData> getProcessList() {
        return new ArrayList<>(processList); // Return copy to prevent external modification
    }

    // Find processes by name
    public List<ProcessData> findProcessesByName(String processName) {
        return processList.stream()
                .filter(p -> p.getProcessName().equalsIgnoreCase(processName))
                .toList();
    }

    // Find process by PID
    public ProcessData findProcessByPid(int pid) {
        return processList.stream()
                .filter(p -> p.getProcessId() == pid)
                .findFirst()
                .orElse(null);
    }



    public void checkProcessDisplayAffinity(String targetProcess){
        Set<Integer> matchingPIDs = new HashSet<>();


        // Step 1: Get all PIDs matching target process name
        Tlhelp32.PROCESSENTRY32.ByReference entry = new Tlhelp32.PROCESSENTRY32.ByReference();
        WinNT.HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));


        try {
            while (Kernel32.INSTANCE.Process32Next(snapshot, entry)) {
                String exe = Native.toString(entry.szExeFile);
                if (exe.equalsIgnoreCase(targetProcess)) {
                    // adds relevant process with same name to the hashset
                    matchingPIDs.add(entry.th32ProcessID.intValue());
                }
            }
        } finally {
            // ends pointer
            Kernel32.INSTANCE.CloseHandle(snapshot);
        }


        if (matchingPIDs.isEmpty()) {
            System.out.println("No running processes named " + targetProcess);
            return;
        }


        System.out.println("Found PIDs for " + targetProcess + ": " + matchingPIDs);
        System.out.println("Checking display affinity for all windows...\n");

        // start processing the process, keep track of the window count vs the ones that are excluded via windows api
        final int[] windowCount = {0};
        final int[] excludedCount = {0};


        // Enumerate all windows and match HWND to one of those PIDs
        User32.INSTANCE.EnumWindows((hWnd, data) -> {
            try {
                // verify the window handle is valid
                if (!User32.INSTANCE.IsWindow(hWnd)) {
                    return true;
                }


                IntByReference pidRef = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hWnd, pidRef);
                int pid = pidRef.getValue();


                if (matchingPIDs.contains(pid)) {
                    windowCount[0]++;


                    // get window title using Unicode version for better compatibility
                    char[] windowTextW = new char[512];
                    int titleLength = User32.INSTANCE.GetWindowTextW(hWnd, windowTextW, 512);
                    String title = titleLength > 0 ? Native.toString(windowTextW) : "<No Title>";


                    // check if window is visible
                    boolean isVisible = User32.INSTANCE.IsWindowVisible(hWnd);


                    System.out.println("Window: \"" + title + "\"");
                    System.out.println("  -> PID: " + pid);
                    System.out.println("  -> HWND: 0x" + Long.toHexString(Pointer.nativeValue(hWnd.getPointer())));
                    System.out.println("  -> Visible: " + isVisible);


                    // check display affinity
                    IntByReference affinity = new IntByReference();
                    boolean result = User32.INSTANCE.GetWindowDisplayAffinity(hWnd, affinity);


                    if (result) {
                        int value = affinity.getValue();
                        System.out.println("  -> Display Affinity: 0x" + Integer.toHexString(value));


                        switch (value) {
                            case WDA_NONE:
                                System.out.println("     -> NONE (default)");
                                break;
                            case WDA_MONITOR:
                                System.out.println("     -> MONITOR (normal capture allowed)");
                                break;
                            case WDA_EXCLUDEFROMCAPTURE:
                                System.out.println("     -> EXCLUDED FROM CAPTURE");
                                excludedCount[0]++;
                                break;
                            default:
                                System.out.println("     -> UNKNOWN flag: " + value);
                                break;
                        }
                    } else {
                        int error = User32.INSTANCE.GetLastError();
                        System.out.println("  -> Could not get affinity (Error: " + error + ")");


                        // Common error codes
                        switch (error) {
                            case 5: // ERROR_ACCESS_DENIED
                                System.out.println("     -> Access denied - may need elevated privileges");
                                break;
                            case 6: // ERROR_INVALID_HANDLE
                                System.out.println("     -> Invalid window handle");
                                break;
                            case 120: // ERROR_CALL_NOT_IMPLEMENTED
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


            return true; // continue enumeration
        }, null);


        System.out.println("Summary:");
        System.out.println("  Total windows found for " + targetProcess + ": " + windowCount[0]);
        System.out.println("  Windows excluded from capture: " + excludedCount[0]);
    }

    // Get count of processes
    public int getProcessCount() {
        return processList.size();
    }

    // Print all stored processes
    public void printStoredProcesses() {
        System.out.println("=== Stored Processes ===");
        for (ProcessData process : processList) {
            System.out.println(process);
        }
    }


    public static void main(String[] args) {
        ProcessHandler handler = new ProcessHandler();

        // Scan and store all processes
        handler.getProcessName();

        handler.checkProcessDisplayAffinity("cluely.exe");
        handler.checkProcessDisplayAffinity("java.exe");
        // Access the stored list
        /*
        List<ProcessData> allProcesses = handler.getProcessList();
        System.out.println("Retrieved " + allProcesses.size() + " processes");

        // Find specific processes
        List<ProcessData> notepadProcesses = handler.findProcessesByName("notepad.exe");
        if (!notepadProcesses.isEmpty()) {
            System.out.println("Found notepad processes:");
            for (ProcessData process : notepadProcesses) {
                System.out.println("  " + process);
            }
        }



        // Print all stored processes
        handler.printStoredProcesses();

         */
    }
}
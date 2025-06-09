package com.cs.main;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

import java.util.ArrayList;
import java.util.List;

public class ProcessHandler {
    // Store the list of processes as a class field
    private List<ProcessData> processList;

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
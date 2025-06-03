 package com.hl.main; // Your package structure

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
/*
public class Injector {

    // Define Kernel32 library methods not in JNA's Kernel32 interface or for more control
    interface CustomKernel32 extends Kernel32 {
        CustomKernel32 INSTANCE = Native.load("kernel32", CustomKernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        @Override
        HANDLE OpenProcess(int dwDesiredAccess, boolean bInheritHandle, int dwProcessId);

        @Override
        Pointer VirtualAllocEx(HANDLE hProcess, Pointer lpAddress, SIZE_T dwSize, int flAllocationType, int flProtect);

        @Override
        boolean WriteProcessMemory(HANDLE hProcess, Pointer lpBaseAddress, Pointer lpBuffer, SIZE_T nSize, IntByReference lpNumberOfBytesWritten);
        // JNA's default WriteProcessMemory takes a byte[] buffer, Memory object is more flexible here

        @Override
        HANDLE CreateRemoteThread(HANDLE hProcess, Pointer lpThreadAttributes, SIZE_T dwStackSize, Pointer lpStartAddress, Pointer lpParameter, int dwCreationFlags, IntByReference lpThreadId);

        @Override
        int GetLastError();

        @Override
        boolean CloseHandle(HANDLE hObject);

        // GetProcAddress is used to find LoadLibraryA
        @Override
        FARPROC GetProcAddress(HMODULE hModule, String lpProcName);

        // GetModuleHandle is used to get a handle to kernel32.dll
        @Override
        HMODULE GetModuleHandle(String lpModuleName);

    }

    // Define necessary constants from WinNT.h if not readily available or for clarity
    public static final int PROCESS_VM_OPERATION = 0x0008;
    public static final int PROCESS_VM_WRITE = 0x0020;
    public static final int PROCESS_VM_READ = 0x0010; // Though not strictly needed for basic injection
    public static final int PROCESS_CREATE_THREAD = 0x0002;
    public static final int PROCESS_QUERY_INFORMATION = 0x0400; // For some operations
    public static final int PROCESS_ALL_ACCESS = WinNT.PROCESS_ALL_ACCESS;//0x1F0FFF;


    public static final int MEM_COMMIT = 0x1000;
    public static final int MEM_RESERVE = 0x2000;
    public static final int PAGE_READWRITE = 0x04;
    public static final int PAGE_EXECUTE_READWRITE = 0x40; // Often used, but PAGE_READWRITE for DLL path is fine


    public static int getProcessIdByName(String processName) {
        CustomKernel32 kernel32 = CustomKernel32.INSTANCE;
        Tlhelp32.PROCESSENTRY32 processEntry = new Tlhelp32.PROCESSENTRY32();
        processEntry.dwSize = new WinDef.DWORD(processEntry.size()); // Set dwSize before use

        HANDLE snapshot = kernel32.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, new WinDef.DWORD(0));
        if (INVALID_HANDLE_VALUE.equals(snapshot)) {
            System.err.println("Failed to create snapshot: " + kernel32.GetLastError());
            return 0;
        }

        try {
            if (kernel32.Process32First(snapshot, processEntry)) {
                do {
                    String currentProcessName = Native.toString(processEntry.szExeFile);
                    if (processName.equalsIgnoreCase(currentProcessName)) {
                        return processEntry.th32ProcessID.intValue();
                    }
                } while (kernel32.Process32Next(snapshot, processEntry));
            } else {
                System.err.println("Failed to get first process: " + kernel32.GetLastError());
            }
        } finally {
            kernel32.CloseHandle(snapshot);
        }
        System.err.println("Process '" + processName + "' not found.");
        return 0;
    }


    public static void main(String[] args) {
        // --- Configuration ---
        String dllPath = "C:\\Users\\justi\\IdeaProjects\\Honorlock\\src\\main\\c\\payload.dll"; // Ensure this DLL matches JVM architecture
        String targetProcessName = "Spotify.exe"; // Or any other process like "notepad.exe" for testing
        // --- End Configuration ---

        File dllFile = new File(dllPath);
        if (!dllFile.exists() || !dllFile.isFile()) {
            System.err.println("DLL not found at path: " + dllPath);
            return;
        }
        String absoluteDllPath = dllFile.getAbsolutePath();
        System.out.println("Attempting to inject: " + absoluteDllPath);


        CustomKernel32 k32 = CustomKernel32.INSTANCE;

        // 1. Get Process ID by name
        int processId = getProcessIdByName(targetProcessName);
        if (processId == 0) {
            // Optionally, allow PID as argument:
            // if (args.length > 0) { try { processId = Integer.parseInt(args[0]); } catch (NumberFormatException e) { } }
            // if (processId == 0) { System.err.println("Target process not found and no valid PID provided."); return; }
            return;
        }
        System.out.println("Target process '" + targetProcessName + "' found with PID: " + processId);

        // 2. Open the target process
        // PROCESS_ALL_ACCESS might be too broad and denied; specific rights are better.
        // For injection, we need: VM_OPERATION, VM_WRITE, CREATE_THREAD, QUERY_INFORMATION (sometimes)
        int desiredAccess = PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION | PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ;
        HANDLE hProcess = k32.OpenProcess(desiredAccess, false, processId);

        if (hProcess == null || hProcess.getPointer() == Pointer.NULL) {
            System.err.println("Failed to open target process. PID: " + processId + ", Error: " + k32.GetLastError());
            System.err.println("Try running this application as an Administrator.");
            return;
        }
        System.out.println("Successfully opened target process. Handle: " + hProcess.getPointer());

        try {
            // 3. Get address of LoadLibraryA in kernel32.dll
            // LoadLibraryA is in kernel32.dll, which is loaded in every process at the same relative address (usually).
            // We need its absolute address within the target process's address space.
            // GetModuleHandle and GetProcAddress operate on the current process, but the address of LoadLibraryA
            // is the same across processes (for the same instance of kernel32.dll).
            WinDef.HMODULE hKernel32 = k32.GetModuleHandle("kernel32.dll");
            if (hKernel32 == null) {
                System.err.println("Failed to get handle for kernel32.dll: " + k32.GetLastError());
                return;
            }
            Pointer loadLibraryAddr = k32.GetProcAddress(hKernel32, "LoadLibraryA").getPointer();
            if (loadLibraryAddr == null || loadLibraryAddr == Pointer.NULL) {
                System.err.println("Failed to get address of LoadLibraryA: " + k32.GetLastError());
                return;
            }
            System.out.println("Address of LoadLibraryA: " + loadLibraryAddr);

            // 4. Allocate memory in the target process for the DLL path
            byte[] dllPathBytes = (absoluteDllPath + "\0").getBytes(StandardCharsets.US_ASCII); // Null-terminated ASCII string
            WinDef.SIZE_T dllPathSize = new WinDef.SIZE_T(dllPathBytes.length);
            Pointer remoteMem = k32.VirtualAllocEx(hProcess, null, dllPathSize, MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);
            if (remoteMem == null || remoteMem == Pointer.NULL) {
                System.err.println("Failed to allocate memory in target process. Error: " + k32.GetLastError());
                return;
            }
            System.out.println("Memory allocated in target process at: " + remoteMem);

            // 5. Write the DLL path to the allocated memory
            Memory dllPathBuffer = new Memory(dllPathBytes.length);
            dllPathBuffer.write(0, dllPathBytes, 0, dllPathBytes.length);
            IntByReference bytesWritten = new IntByReference();
            boolean writeSuccess = k32.WriteProcessMemory(hProcess, remoteMem, dllPathBuffer, dllPathSize, bytesWritten);
            if (!writeSuccess || bytesWritten.getValue() != dllPathBytes.length) {
                System.err.println("Failed to write DLL path to target process memory. Error: " + k32.GetLastError() + ", Bytes written: " + bytesWritten.getValue());
                k32.VirtualFreeEx(hProcess, remoteMem, new WinDef.SIZE_T(0), WinNT.MEM_RELEASE); // Free allocated memory
                return;
            }
            System.out.println("DLL path successfully written to target process.");

            // 6. Create a remote thread in the target process to call LoadLibraryA with our DLL path
            IntByReference threadId = new IntByReference();
            HANDLE hRemoteThread = k32.CreateRemoteThread(hProcess, null, new WinDef.SIZE_T(0), loadLibraryAddr, remoteMem, 0, threadId);
            if (hRemoteThread == null || hRemoteThread.getPointer() == Pointer.NULL) {
                System.err.println("Failed to create remote thread in target process. Error: " + k32.GetLastError());
                k32.VirtualFreeEx(hProcess, remoteMem, new WinDef.SIZE_T(0), WinNT.MEM_RELEASE); // Free allocated memory
                return;
            }
            System.out.println("Remote thread created successfully. Thread ID: " + threadId.getValue());

            // 7. Wait for the remote thread to finish (optional, but good practice)
            k32.WaitForSingleObject(hRemoteThread, Kernel32.INFINITE);

            // 8. Get exit code of the thread (optional, LoadLibraryA returns HMODULE of loaded lib, 0 on failure)
            IntByReference threadExitCode = new IntByReference();
            k32.GetExitCodeThread(hRemoteThread, threadExitCode);
            System.out.println("Remote thread finished. Exit code (HMODULE of loaded DLL, or 0 on fail): " + Long.toHexString(threadExitCode.getValue()));
            if (threadExitCode.getValue() == 0) {
                System.err.println("LoadLibraryA likely failed in the remote process.");
            } else {
                System.out.println("DLL successfully loaded into target process!");
            }


            // 9. Clean up: free the allocated memory and close handles
            k32.VirtualFreeEx(hProcess, remoteMem, new WinDef.SIZE_T(0), WinNT.MEM_RELEASE); // Size must be 0 for MEM_RELEASE
            k32.CloseHandle(hRemoteThread);
            System.out.println("Cleaned up remote memory and thread handle.");

        } catch (Exception e) {
            System.err.println("An unexpected error occurred during injection:");
            e.printStackTrace();
        } finally {
            if (hProcess != null && hProcess.getPointer() != Pointer.NULL) {
                k32.CloseHandle(hProcess);
                System.out.println("Closed target process handle.");
            }
        }
        System.out.println("Injection attempt finished.");
    }

}*/
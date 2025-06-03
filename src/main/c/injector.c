// c/injector.c
#include <windows.h>
#include <stdio.h>
#include <tlhelp32.h> // For Process32First/Next
#include <string.h>   // For _stricmp (case-insensitive string compare)

// Function to get the Process ID (PID) by process name
DWORD GetProcessIdByName(const char *processName) {
    PROCESSENTRY32 entry;
    entry.dwSize = sizeof(PROCESSENTRY32);
    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    DWORD pid = 0;

    if (snapshot == INVALID_HANDLE_VALUE) {
        printf("[-] Failed to create snapshot: %lu\n", GetLastError());
        return 0;
    }

    if (Process32First(snapshot, &entry)) {
        do {
            if (_stricmp(entry.szExeFile, processName) == 0) { // Case-insensitive comparison
                pid = entry.th32ProcessID;
                break;
            }
        } while (Process32Next(snapshot, &entry));
    } else {
        printf("[-] Failed to get first process: %lu\n", GetLastError());
    }

    CloseHandle(snapshot);
    if (pid == 0) {
        printf("[-] Process \"%s\" not found.\n", processName);
    } else {
        printf("[+] Process \"%s\" found with PID: %lu\n", processName, pid);
    }
    return pid;
}


int main(int argc, char *argv[]) {
    if (argc < 2 || argc > 3) {
        printf("Usage: injector.exe <ProcessName | PID> [PathToDll]\n");
        printf("Example: injector.exe Spotify.exe\n");
        printf("Example: injector.exe Spotify.exe C:\\path\\to\\payload.dll\n");
        printf("Example: injector.exe 1234 C:\\path\\to\\payload.dll\n");
        return 1;
    }

    DWORD pid = 0;
    // Try to convert first argument to PID, if fails, assume it's a process name
    pid = (DWORD)atoi(argv[1]);
    if (pid == 0) { // atoi failed or returned 0, so it's probably a name
        pid = GetProcessIdByName(argv[1]);
        if (pid == 0) {
            return 1; // GetProcessIdByName will print an error
        }
    } else {
         printf("[+] Using target PID: %lu\n", pid);
    }


    char dllPath[MAX_PATH];
    if (argc == 3) {
        strncpy_s(dllPath, MAX_PATH, argv[2], _TRUNCATE);
    } else {
        // Get full path to payload.dll assuming it's in the same directory as injector.exe
        // Or in a "c" subfolder relative to where build_all.bat is run if injector is in root.
        // For simplicity, let's assume build_all.bat places payload.dll in the same dir as injector.exe
        // A more robust solution would be to get the injector's directory.
        // Let's assume payload.dll is in the current working directory OR a fixed relative path.
        // If build_all.bat output injector.exe and payload.dll to root:
        GetCurrentDirectory(MAX_PATH, dllPath);
        strcat_s(dllPath, MAX_PATH, "\\payload.dll"); // Default name
        // If build_all.bat output them to c/
        // GetCurrentDirectory(MAX_PATH, dllPath);
        // strcat_s(dllPath, MAX_PATH, "\\c\\payload.dll");
    }
    printf("[*] Using DLL Path: %s\n", dllPath);

    // Check if DLL exists
    if (GetFileAttributesA(dllPath) == INVALID_FILE_ATTRIBUTES) {
        printf("[-] DLL not found at path: %s. Error: %lu\n", dllPath, GetLastError());
        printf("Please ensure payload.dll is compiled and in the correct location.\n");
        printf("Expected location based on execution: current directory or specified path.\n");
        printf("If using build_all.bat, it should be in the same directory as injector.exe or in './c/'.\n");
        return 1;
    }


    HANDLE hProcess = OpenProcess(PROCESS_ALL_ACCESS, FALSE, pid);
    if (hProcess == NULL) {
        printf("[-] Failed to open target process. PID: %lu, Error: %lu\n", pid, GetLastError());
        printf("    Try running the injector as an administrator.\n");
        return 1;
    }
    printf("[+] Opened target process successfully.\n");

    // Allocate memory in the target process for the DLL path
    LPVOID pDllPathRemote = VirtualAllocEx(hProcess, NULL, strlen(dllPath) + 1,
                                           MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);
    if (pDllPathRemote == NULL) {
        printf("[-] Failed to allocate memory in target process. Error: %lu\n", GetLastError());
        CloseHandle(hProcess);
        return 1;
    }
    printf("[+] Memory allocated in target process at %p\n", pDllPathRemote);

    // Write the DLL path to the allocated memory
    if (!WriteProcessMemory(hProcess, pDllPathRemote, dllPath, strlen(dllPath) + 1, NULL)) {
        printf("[-] Failed to write DLL path to target process memory. Error: %lu\n", GetLastError());
        VirtualFreeEx(hProcess, pDllPathRemote, 0, MEM_RELEASE);
        CloseHandle(hProcess);
        return 1;
    }
    printf("[+] DLL path written to target process memory.\n");

    // Get the address of LoadLibraryA in kernel32.dll
    // (LoadLibraryA is the same in both 32-bit and 64-bit processes, its address will be different though)
    HMODULE hKernel32 = GetModuleHandleA("kernel32.dll");
    if (hKernel32 == NULL) {
        printf("[-] Failed to get handle for kernel32.dll. Error: %lu\n", GetLastError());
        VirtualFreeEx(hProcess, pDllPathRemote, 0, MEM_RELEASE);
        CloseHandle(hProcess);
        return 1;
    }
    FARPROC pLoadLibraryA = GetProcAddress(hKernel32, "LoadLibraryA");
    if (pLoadLibraryA == NULL) {
        printf("[-] Failed to get address of LoadLibraryA. Error: %lu\n", GetLastError());
        VirtualFreeEx(hProcess, pDllPathRemote, 0, MEM_RELEASE);
        CloseHandle(hProcess);
        return 1;
    }
    printf("[+] Address of LoadLibraryA: %p\n", pLoadLibraryA);

    // Create a remote thread in the target process to call LoadLibraryA with our DLL path
    HANDLE hRemoteThread = CreateRemoteThread(hProcess, NULL, 0,
                                             (LPTHREAD_START_ROUTINE)pLoadLibraryA,
                                             pDllPathRemote, 0, NULL);
    if (hRemoteThread == NULL) {
        printf("[-] Failed to create remote thread in target process. Error: %lu\n", GetLastError());
        VirtualFreeEx(hProcess, pDllPathRemote, 0, MEM_RELEASE);
        CloseHandle(hProcess);
        return 1;
    }
    printf("[+] Remote thread created successfully. Waiting for it to finish...\n");

    // Wait for the remote thread to finish
    WaitForSingleObject(hRemoteThread, INFINITE);
    printf("[+] Remote thread finished.\n");

    // Get exit code of the thread (optional, can tell if LoadLibrary succeeded)
    DWORD threadExitCode = 0;
    GetExitCodeThread(hRemoteThread, &threadExitCode);
    if (threadExitCode == 0) { // For LoadLibrary, 0 means failure
        printf("[-] LoadLibraryA likely failed in the remote process (exit code 0 for thread).\n");
    } else {
        printf("[+] LoadLibraryA seems to have succeeded in remote process (module handle: %p).\n", (void*)threadExitCode);
    }


    // Clean up: free the allocated memory and close handles
    VirtualFreeEx(hProcess, pDllPathRemote, 0, MEM_RELEASE);
    CloseHandle(hRemoteThread);
    CloseHandle(hProcess);

    printf("[+] Injection attempt finished. Check target process (e.g., Spotify.exe) for a message box from payload.dll.\n");
    printf("[+] If JNI was enabled in payload, check for JNI message boxes and Java console output (if visible).\n");

    return 0;
}
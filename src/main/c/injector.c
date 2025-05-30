#include <windows.h>
#include <tlhelp32.h>
#include <stdio.h>

DWORD GetProcessIdByName(const char* procName) {
    PROCESSENTRY32 entry;
    entry.dwSize = sizeof(PROCESSENTRY32);
    DWORD pid = 0;

    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (Process32First(snapshot, &entry)) {
        do {
            if (strcmp(entry.szExeFile, procName) == 0) {
                pid = entry.th32ProcessID;
                break;
            }
        } while (Process32Next(snapshot, &entry));
    }
    CloseHandle(snapshot);
    return pid;
}

int main(int argc, char* argv[]) {
    if (argc < 3) {
        printf("Usage: injector.exe <dll path> <process name>\n");
        return 1;
    }

    const char* dllPath = argv[1];
    const char* procName = argv[2];
    DWORD pid = GetProcessIdByName(procName);
    if (pid == 0) {
        printf("Process not found.\n");
        return 1;
    }

    HANDLE hProc = OpenProcess(PROCESS_ALL_ACCESS, FALSE, pid);
    if (!hProc) return 1;

    LPVOID alloc = VirtualAllocEx(hProc, NULL, strlen(dllPath)+1, MEM_COMMIT, PAGE_READWRITE);
    WriteProcessMemory(hProc, alloc, dllPath, strlen(dllPath)+1, NULL);

    HANDLE thread = CreateRemoteThread(hProc, NULL, 0, 
        (LPTHREAD_START_ROUTINE) GetProcAddress(GetModuleHandleA("kernel32.dll"), "LoadLibraryA"),
        alloc, 0, NULL);

    WaitForSingleObject(thread, INFINITE);
    CloseHandle(hProc);
    return 0;
}

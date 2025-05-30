#include <windows.h>

BOOL APIENTRY DllMain(HMODULE hModule, DWORD reason, LPVOID lpReserved) {
    if (reason == DLL_PROCESS_ATTACH) {
        MessageBox(NULL, "Injected into Spotify!", "DLL Message", MB_OK);
    }
    return TRUE;
}

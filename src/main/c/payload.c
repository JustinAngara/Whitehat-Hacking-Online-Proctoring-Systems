#include <windows.h>
#include <jni.h>

DWORD WINAPI Run(LPVOID lpParam) {
    MessageBoxA(NULL, "Spotify DLL Thread Started", "STEP 0", MB_OK);

    JavaVM *jvm;
    JNIEnv *env;

    JavaVMInitArgs vm_args;
    JavaVMOption options[1];

    // 🔥 Use your absolute classpath here
    options[0].optionString = "-Djava.class.path=C:\\Users\\justi\\IdeaProjects\\Honorlock\\target\\classes";

    vm_args.version = JNI_VERSION_1_8;  // Or use JNI_VERSION_10 if you're sure you're on JDK 24
    vm_args.nOptions = 1;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = JNI_FALSE;

    MessageBoxA(NULL, "Creating JVM for Spotify", "STEP 1", MB_OK);
    jint res = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
    if (res != JNI_OK) {
        MessageBoxA(NULL, "Failed to create JVM", "ERROR STEP 1", MB_OK | MB_ICONERROR);
        return 1;
    }

    MessageBoxA(NULL, "JVM successfully created", "STEP 1 SUCCESS", MB_OK);

    jclass cls = (*env)->FindClass(env, "StealthOverlay");
    if (!cls) {
        MessageBoxA(NULL, "Could not find StealthOverlay class", "ERROR STEP 2", MB_OK | MB_ICONERROR);
        return 1;
    }

    MessageBoxA(NULL, "Found StealthOverlay class", "STEP 2 SUCCESS", MB_OK);

    jmethodID mid = (*env)->GetStaticMethodID(env, cls, "main", "([Ljava/lang/String;)");
    if (!mid) {
        MessageBoxA(NULL, "Could not find main() method", "ERROR STEP 3", MB_OK | MB_ICONERROR);
        return 1;
    }

    MessageBoxA(NULL, "Found main() method", "STEP 3 SUCCESS", MB_OK);

    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    jobjectArray args = (*env)->NewObjectArray(env, 0, stringClass, NULL);

    MessageBoxA(NULL, "Calling StealthOverlay.main()", "STEP 4", MB_OK);
    (*env)->CallStaticVoidMethod(env, cls, mid, args);

    MessageBoxA(NULL, "Java class executed", "STEP 4 SUCCESS", MB_OK);
    return 0;
}

BOOL APIENTRY DllMain(HMODULE hModule, DWORD reason, LPVOID lpReserved) {
    if (reason == DLL_PROCESS_ATTACH) {
        MessageBoxA(NULL, "Injected into Spotify!", "DLL LOADED", MB_OK);
        CreateThread(NULL, 0, Run, NULL, 0, NULL);
    }
    return TRUE;
}

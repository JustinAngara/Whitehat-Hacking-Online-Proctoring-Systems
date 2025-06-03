// c/payload.c
#include <windows.h>
#include <stdio.h> // For snprintf

// Optional: Define if you want to attempt JNI integration (much more complex)
// #define ENABLE_JNI_CALL

#ifdef ENABLE_JNI_CALL
#include <jni.h>
#endif

#ifdef ENABLE_JNI_CALL
void StartJava(HINSTANCE hinstDLL) {
    JavaVM *jvm = NULL;
    JNIEnv *env = NULL;
    JavaVMInitArgs vm_args;
    JavaVMOption options[1]; // Increase if more options are needed

    // Path to your JDK's jvm.dll. This needs to be correct for your system.
    // Or hope it's in the PATH or the target process can find it.
    // This is a major point of failure/complexity.
    // HMODULE hJVMDLL = LoadLibrary("C:\\Program Files\\Java\\jdk-17\\bin\\server\\jvm.dll"); // Example path
    // if (hJVMDLL == NULL) {
    //     MessageBoxA(NULL, "Failed to load jvm.dll. Ensure path is correct.", "JNI Error", MB_OK | MB_ICONERROR);
    //     return;
    // }
    // typedef jint (JNICALL *JNI_CreateJavaVM_t)(JavaVM**, void**, void*);
    // JNI_CreateJavaVM_t JNI_CreateJavaVM_func = (JNI_CreateJavaVM_t)GetProcAddress(hJVMDLL, "JNI_CreateJavaVM");
    // if (JNI_CreateJavaVM_func == NULL) {
    //     MessageBoxA(NULL, "Failed to find JNI_CreateJavaVM in jvm.dll.", "JNI Error", MB_OK | MB_ICONERROR);
    //     return;
    // }

    // Setup JVM options
    // You need to point to your compiled Main.class.
    // Assuming build_all.bat places Main.class in a 'bin' or 'java' directory accessible from the target.
    // The classpath should be relative to where the target process is running, or an absolute path.
    options[0].optionString = "-Djava.class.path=C:\\path\\to\\your\\project\\java"; // VERY IMPORTANT: Update this path
    // options[0].optionString = "-Djava.class.path=.\\java"; // If Main.class is in a 'java' subdir relative to target process CWD

    vm_args.version = JNI_VERSION_1_8;
    vm_args.nOptions = 1;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = FALSE;

    // Create the JVM
    // jint res = JNI_CreateJavaVM_func(&jvm, (void**)&env, &vm_args);
    // For simplicity if jvm.dll is in PATH (less reliable for injection)
    jint res = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);

    if (res < 0 || jvm == NULL || env == NULL) {
        char errorMsg[256];
        snprintf(errorMsg, sizeof(errorMsg), "Failed to create JVM. Error code: %d", res);
        MessageBoxA(NULL, errorMsg, "JNI Error", MB_OK | MB_ICONERROR);
        // if (hJVMDLL) FreeLibrary(hJVMDLL);
        return;
    }

    // Find the Main class
    // The class name format is "packageName/ClassName" or "java/Main" if your Main.java has "package java;"
    // and is in a folder named "java" relative to the classpath root.
    jclass mainClass = (*env)->FindClass(env, "java/Main");
    if (mainClass == NULL) {
        MessageBoxA(NULL, "Failed to find Java Main class. Check classpath and class name (e.g., java/Main).", "JNI Error - FindClass", MB_OK | MB_ICONERROR);
        if ((*env)->ExceptionOccurred(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        (*jvm)->DestroyJavaVM(jvm);
        // if (hJVMDLL) FreeLibrary(hJVMDLL);
        return;
    }

    // Get the static main method
    jmethodID mainMethod = (*env)->GetStaticMethodID(env, mainClass, "main", "([Ljava/lang/String;)V");
    if (mainMethod == NULL) {
        MessageBoxA(NULL, "Failed to find Java Main.main method.", "JNI Error - GetStaticMethodID", MB_OK | MB_ICONERROR);
        if ((*env)->ExceptionOccurred(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
        (*jvm)->DestroyJavaVM(jvm);
        // if (hJVMDLL) FreeLibrary(hJVMDLL);
        return;
    }

    // Create an empty String array for args (if needed)
    jobjectArray mainArgs = (*env)->NewObjectArray(env, 0, (*env)->FindClass(env, "java/lang/String"), NULL);

    // Call the main method
    MessageBoxA(NULL, "Attempting to call Java Main.main() via JNI...", "JNI Info", MB_OK | MB_ICONINFORMATION);
    (*env)->CallStaticVoidMethod(env, mainClass, mainMethod, mainArgs);

    if ((*env)->ExceptionOccurred(env)) {
        MessageBoxA(NULL, "Exception occurred during Java Main.main() execution.", "JNI Execution Error", MB_OK | MB_ICONERROR);
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    } else {
        MessageBoxA(NULL, "Java Main.main() called successfully via JNI (check console if applicable).", "JNI Success", MB_OK | MB_ICONINFORMATION);
    }

    // Optional: Call another static method like showMessage
    jmethodID showMessageMethod = (*env)->GetStaticMethodID(env, mainClass, "showMessage", "(Ljava/lang/String;)I");
    if (showMessageMethod != NULL) {
        jstring javaString = (*env)->NewStringUTF(env, "Hello from Injected C DLL!");
        (*env)->CallStaticIntMethod(env, mainClass, showMessageMethod, javaString);
        (*env)->DeleteLocalRef(env, javaString); // Clean up local reference
         if ((*env)->ExceptionOccurred(env)) {
            MessageBoxA(NULL, "Exception occurred during Java showMessage() execution.", "JNI Execution Error", MB_OK | MB_ICONERROR);
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    } else {
         MessageBoxA(NULL, "Could not find showMessage method.", "JNI Info", MB_OK | MB_ICONWARNING);
    }


    // Clean up
    (*jvm)->DestroyJavaVM(jvm);
    // if (hJVMDLL) FreeLibrary(hJVMDLL);
    MessageBoxA(NULL, "JVM Destroyed. JNI attempt finished.", "JNI Info", MB_OK | MB_ICONINFORMATION);
}
#endif // ENABLE_JNI_CALL

// DllMain is the entry point for the DLL
BOOL APIENTRY DllMain(HMODULE hModule, // Handle to DLL module
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved) {
    switch (ul_reason_for_call) {
        case DLL_PROCESS_ATTACH:
            // This code runs when the DLL is loaded into the process
            MessageBoxA(NULL, "Payload DLL Injected Successfully!", "Injected DLL", MB_OK | MB_ICONINFORMATION);

#ifdef ENABLE_JNI_CALL
            // Create a new thread to start Java to avoid blocking DllMain
            CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)StartJava, hModule, 0, NULL);
#endif
            break;

        case DLL_THREAD_ATTACH:
            // Code here runs when a new thread is created in the process
            break;

        case DLL_THREAD_DETACH:
            // Code here runs when a thread exits cleanly
            break;

        case DLL_PROCESS_DETACH:
            // Code here runs when the DLL is unloaded from the process
#ifdef ENABLE_JNI_CALL
            // If JVM was started and not cleaned up properly, this is a place for last-resort cleanup,
            // but it's tricky because the process might be terminating.
            // Typically, the JVM should be destroyed by the thread that created it.
#endif
            MessageBoxA(NULL, "Payload DLL Unloaded.", "Injected DLL", MB_OK | MB_ICONINFORMATION);
            break;
    }
    return TRUE; // Successful
}

// You can export other functions if you want the injector to call them via GetProcAddress + CreateRemoteThread
// For example:
// __declspec(dllexport) void HelloWorld() {
//     MessageBoxA(NULL, "Hello from exported function!", "Payload DLL", MB_OK);
// }
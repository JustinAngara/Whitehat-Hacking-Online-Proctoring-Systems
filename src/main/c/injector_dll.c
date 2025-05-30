#include <jni.h>
#include <windows.h>

JNIEnv* create_vm(JavaVM **jvm) {
    JNIEnv *env;
    JavaVMInitArgs vm_args;
    JavaVMOption options[1];

    options[0].optionString = "-Djava.class.path=C:\\Users\\justi\\IdeaProjects\\Honorlock\\target\\classes;C:\\Users\\justi\\IdeaProjects\\Honorlock\\lib\\jna-5.13.0.jar;C:\\Users\\justi\\IdeaProjects\\Honorlock\\lib\\jna-platform-5.13.0.jar";

    vm_args.version = JNI_VERSION_1_8;
    vm_args.nOptions = 1;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = JNI_FALSE;

    jint res = JNI_CreateJavaVM(jvm, (void**)&env, &vm_args);
    if (res < 0 || !env) {
        MessageBoxA(NULL, "❌ Failed to create JVM", "Injector", MB_OK);
        return NULL;
    }

    MessageBoxA(NULL, "✅ JVM created", "Injector", MB_OK);
    return env;
}

DWORD WINAPI RunJavaOverlay(LPVOID lpParam) {
    JavaVM *jvm;
    JNIEnv *env = create_vm(&jvm);
    if (!env) return 1;

    jclass cls = (*env)->FindClass(env, "StealthOverlay");
    if (!cls) {
        MessageBoxA(NULL, "❌ StealthOverlay class not found", "Injector", MB_OK);
        return 1;
    }

    jmethodID mid = (*env)->GetStaticMethodID(env, cls, "main", "([Ljava/lang/String;)V");
    if (!mid) {
        MessageBoxA(NULL, "❌ main method not found", "Injector", MB_OK);
        return 1;
    }

    jobjectArray args = (*env)->NewObjectArray(env, 0, (*env)->FindClass(env, "java/lang/String"), NULL);
    (*env)->CallStaticVoidMethod(env, cls, mid, args);
    return 0;
}

BOOL APIENTRY DllMain(HMODULE hModule, DWORD reason, LPVOID lpReserved) {
    if (reason == DLL_PROCESS_ATTACH) {
        CreateThread(NULL, 0, RunJavaOverlay, NULL, 0, NULL);
    }
    return TRUE;
}

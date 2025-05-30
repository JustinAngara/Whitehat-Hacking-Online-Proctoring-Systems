@echo off
setlocal

echo [*] Setting JAVA_HOME...
set JAVA_HOME=C:\Program Files\Java\jdk-24

echo [*] Compiling injector.exe...
gcc -o injector.exe injector.c

echo [*] Compiling payload.dll...
gcc -shared -o payload.dll injector_dll.c -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -L"%JAVA_HOME%\lib" -ljvm

echo [*] Native build complete.
pause

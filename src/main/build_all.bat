@echo off
setlocal

:: -------- CONFIG --------
set JAVA_HOME=C:\Program Files\Java\jdk-24
set LIB_DIR=%~dp0..\..\lib
set CLASS_DIR=%~dp0..\..\target\classes
set CDIR=%~dp0c
set JAVADIR=%~dp0java

:: -------- Compile Java --------
echo [*] Compiling StealthOverlay.java with JNA...
javac -cp "%LIB_DIR%\jna-5.13.0.jar;%LIB_DIR%\jna-platform-5.13.0.jar" -d "%CLASS_DIR%" "%JAVADIR%\StealthOverlay.java"
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Java compilation failed.
    pause
    exit /b
)

:: -------- Compile DLL --------
echo [*] Compiling payload.dll from injector_dll.c...
cd /d "%CDIR%"
gcc -shared -o payload.dll injector_dll.c ^
  -I"%JAVA_HOME%\include" ^
  -I"%JAVA_HOME%\include\win32" ^
  -L"%JAVA_HOME%\lib" -ljvm

if %ERRORLEVEL% NEQ 0 (
    echo ❌ DLL compilation failed.
    pause
    exit /b
)

echo [✔] All components built successfully
pause

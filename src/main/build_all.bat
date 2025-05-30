@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-24
setlocal

echo [*] Compiling Java...
javac -d target\classes java\StealthOverlay.java

echo [*] Building payload.dll...
gcc -shared -o c\payload.dll c\payload.c ^
  -I"%JAVA_HOME%\include" ^
  -I"%JAVA_HOME%\include\win32" ^
  -L"%JAVA_HOME%\lib" -ljvm

echo [*] Building injector.exe...
gcc -o c\injector.exe c\injector.c

echo [*] Launching Notepad...
start notepad.exe
timeout /t 2 >nul

echo [*] Injecting payload.dll into Notepad...
c\injector.exe

endlocal
pause

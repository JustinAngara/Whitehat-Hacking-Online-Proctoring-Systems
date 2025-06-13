**Demo: https://www.youtube.com/shorts/G9aQb5mFjbo**
# Whitehat Hacking Online Proctoring Systems
[![Ask DeepWiki](https://devin.ai/assets/askdeepwiki.png)](https://deepwiki.com/JustinAngara/Whitehat-Hacking-Online-Proctoring-Systems)

## 🎯 Purpose

This repository explores techniques for interacting with and bypassing online proctoring systems, focusing on local-to-remote interaction models. It demonstrates a full-stack application combining a Java-based backend with JNA for low-level system hooks and a React + TypeScript frontend for remote interaction via WebSockets. The primary goal is to highlight limitations in browser-based proctoring tools.

The system integrates:
- Low-level system input (via Java and JNA for key events and window manipulation).
- An AWT-based local GUI (`JFrame`/`JWindow`) rendered stealthily to evade screen capture.
- Real-time WebSocket messaging between the Java backend and a browser-based React interface.

## ⚠️ Security Implication
Browser-based proctoring tools like Honorlock are fundamentally limited by the browser's sandbox. They generally cannot fully monitor or block system-level applications, especially those that:
- Do not directly interact with the DOM.
- Do not rely on overlays or typical screen capture methods susceptible to screenshare detection.
- Avoid traditional process naming conventions or employ techniques to hide their presence.

This project demonstrates that such Online Proctoring Systems (OPS) detection capabilities can be bypassed by:
- Running a native Java application in parallel with the proctored exam.
- Leveraging input events or automation from the system level (e.g., global hotkeys, pseudo-typing).
- Communicating information (like answers) through standard WebSockets to interface with a browser-based dashboard, which then informs the local stealth application.

No browser extension or JavaScript-based client can reliably block these system-level approaches.

## ✅ Ethical Use Disclaimer
This project was created strictly for educational and ethical research purposes. It is **not** intended to encourage or condone academic dishonesty. The primary intent is to raise awareness about the vulnerabilities and limitations of browser-only proctoring tools and to promote the development of more transparent, accountable, and robust test-taking environments.

## ✨ Features

-   **Stealth GUI:** A Java-based GUI (`SecureFrame`) that utilizes JNA (Windows `SetWindowDisplayAffinity`) to make itself invisible to screen capture and screen sharing.
-   **Low-Level Keyboard Listener:** Global hotkey detection using JNA (`GetAsyncKeyState`) for controlling the application (e.g., toggling GUI visibility, triggering actions).
-   **Pseudo-Typer:** Emulates human-like typing of pre-loaded text (from `configs/CopyCFG.txt`) into any active window, triggered by hotkeys.
-   **GUI Control:** Hotkeys allow toggling visibility (F10) and repositioning (Ctrl + Numpad keys) of the stealth GUI.
-   **WebSocket Communication:**
    -   A Java Spring Boot backend provides a WebSocket server (`ws://localhost:8080/ws`).
    -   A React frontend application connects to this server for real-time, two-way communication.
    -   The frontend can act as a dashboard to receive and display information (e.g., answers to quiz questions), which can then be manually or semi-automatically inputted using the pseudo-typer.
-   **Process Analysis Tools:** Includes Java components (`com.cs.main.*`) for listing system processes and identifying windows with screen capture exclusion flags, and tools to attempt to remove stealth properties from other applications.
-   **DLL Injection Utilities:** C-based tools (`injector.exe`, `payload.dll`) for experimenting with DLL injection techniques.

## 🔧 Tech Stack

| Layer                   | Technology                                      |
| ----------------------- | ----------------------------------------------- |
| Core Stealth App        | Java, JNA (User32.dll, Kernel32.dll)            |
| WebSocket Backend       | Java, Spring Boot (WebSocket)                   |
| Frontend                | Vite, React, TypeScript                         |
| GUI                     | Java AWT/Swing (`JWindow`)                      |
| Low-Level C Utilities   | C, Windows API                                  |
| Build                   | Maven (Java), GCC (C), npm (Frontend)           |

## 🚀 Running the Project

### Prerequisites
-   JDK 17+
-   Apache Maven
-   Node.js and npm
-   GCC (MinGW for Windows if building C components)

The application consists of three main parts that can be run: the Stealth GUI & Key Listener, the WebSocket Server, and the React Frontend.

### 1. Backend - Stealth GUI & Key Listener (Core Java Application)
This component includes the stealthy GUI (`SecureFrame`), pseudo-typing (`PseudoType`), and global key listeners (`KeyListener`).
The main entry point for this part is `com.hl.main.Main`.

-   **To Run:**
    1.  Compile the Java sources:
        ```bash
        mvn compile
        ```
    2.  Run the `com.hl.main.Main` class from your IDE or via the command line with the appropriate classpath.
        Example for running from the command line after compiling (may need adjustment based on your environment and Maven version):
        ```bash
        # On Linux/macOS
        # java -cp "target/classes:$(mvn exec:exec -Dexec.executable=echo -Dexec.args='%classpath' -q)" com.hl.main.Main

        # On Windows (PowerShell)
        # java -cp "target/classes;$(mvn exec:exec -Dexec.executable=echo -Dexec.args='%classpath' -q)" com.hl.main.Main
        ```
        Alternatively, set up a run configuration in your IDE for the `com.hl.main.Main` class.

    *Note: As of the current code in `com.hl.main.Main`, the Spring Boot application (WebSocket server) is not started by default from this class (the relevant lines are commented out). This means the Stealth GUI will operate standalone without direct WebSocket integration unless the code is modified.*

### 2. Backend - WebSocket Server (Spring Boot)
This server handles WebSocket connections from the React frontend.

-   **To Build and Run:**
    1.  Navigate to the project root directory (where `pom.xml` is).
    2.  Package the application:
        ```bash
        mvn clean package
        ```
    3.  Run the JAR file:
        ```bash
        java -jar target/websocket-server-1.0-SNAPSHOT.jar
        ```
    -   The server will start, and WebSockets will be available at `ws://localhost:8080/ws`.

### 3. Frontend (React + TypeScript + Vite)
This is the browser-based interface that communicates with the WebSocket server.

-   **To Run:**
    1.  Navigate to the frontend directory:
        ```bash
        cd src/main/javascript/react-websocket
        ```
    2.  Install dependencies:
        ```bash
        npm install
        ```
    3.  Start the development server:
        ```bash
        npm run dev
        ```
    -   Access the frontend via the URL provided by Vite (typically `http://localhost:5173`).

### Workflow Example
1.  Start the **WebSocket Server** (Step 2).
2.  Start the **React Frontend** (Step 3). The frontend will connect to the WebSocket server.
3.  Start the **Stealth GUI & Key Listener** application (Step 1).
4.  Use the React frontend as a dashboard to receive/display information.
5.  Use the hotkeys (e.g., `~` (tilde/backtick), arrow keys) provided by the Stealth GUI application to select and type text into the target proctored exam window.
6.  Use F10 to toggle the visibility of the Stealth GUI and Ctrl + Numpad keys to move it.

## 🛠️ DLL Injection Utilities
The repository also contains C code for DLL injection (`src/main/c/`):
-   `injector.c`: A simple program to inject a DLL into a running process by name or PID.
-   `payload.c`: A sample DLL that, when injected, displays a message box. It includes commented-out experimental JNI code to potentially interact with a JVM from the injected DLL.
-   `src/main/build_all.bat`: A batch script to compile `injector.exe` and `payload.dll` using GCC, and then demonstrate injecting the payload into Notepad.exe.

**To use these utilities (on Windows with MinGW/GCC):**
1.  Ensure GCC is installed and in your PATH.
2.  Modify `build_all.bat` if necessary (e.g., `JAVA_HOME` path for JNI headers if `ENABLE_JNI_CALL` in `payload.c` is used).
3.  Run `build_all.bat` from the `src/main/` directory.
    ```bash
    cd src/main
    build_all.bat
    ```
This will compile `injector.exe` and `payload.dll` into the `src/main/c/` directory and attempt to inject `payload.dll` into a newly started `notepad.exe` process.

## 📄 Configuration Files
-   `configs/CopyCFG.txt`: Contains text snippets for the PseudoType feature. Each line or block separated by `<.ENDLINE>` is a distinct message that can be typed out.

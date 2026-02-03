package com.hl.main;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;

import java.awt.*;

import static com.sun.jna.platform.win32.Win32VK.*;

public class KeyListener implements Runnable {
    static volatile boolean writeOn;
    private volatile boolean lastLeftDown = false; // for edge detection

    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class);
        short GetAsyncKeyState(int vKey);
        boolean GetCursorPos(WinDef.POINT lpPoint);
    }
    
    public KeyListener() {}

    @Override
    public void run() {
        writeOn = false;
        int d = 100;

        Rectangle originalBound = SecureFrame.frame.getBounds();
        Rectangle hiddenBounds = new Rectangle(-10000, 0,
                (int) originalBound.getWidth(), (int) originalBound.getHeight());

        // create a scheduler/executenior

        while (true) {
            // F9 -> your Gemini call
            if (isPressed(VK_NUMPAD9.code, 250)) {
                // no validation check, if this does get ran multiple times, there isn't enough api calls. therefore put it in its own thread
//                Main.executor.execute(Main.ga.displayPrompt());
                Main.s.changeContent("New Loading "+Math.random()*1000.0);
                Main.ga.displayPrompt();
            }

            // F10 -> toggle visibility (move off-screen / restore)
            if (isPressed(VK_F10.code, 250)) {
                boolean restore = !SecureFrame.frame.getBounds().equals(originalBound);
                Rectangle bound = restore ? originalBound : hiddenBounds;
                SecureFrame.frame.setBounds(bound);
            }

            if (isPressed(VK_NUMPAD1.code, 250)) {
                AudioListener.start();
            }

            if (isPressed(VK_NUMPAD3.code, 250)) {
                AudioListener.close();
            }

            if (isPressed(VK_NUMPAD4.code, 250)) {
                // perform api call
                String content = Main.s.getContent();
                if(!AudioListener.isRunning && !content.isEmpty()){
                    Main.s.changeContent(content + "\nLoading1337");
                    Main.ga.displayTransPrompt();


                }
            }

            // Ctrl held? then allow numpad move OR ctrl+click move
            if (isPressed(VK_CONTROL.code)) {
                maybeMoveFrameToCtrlClick();
            }
        }
    }

    /** On Ctrl + Left Click (edge), move frame to cursor (centered). */
    private void maybeMoveFrameToCtrlClick() {
        boolean leftDown = (User32.INSTANCE.GetAsyncKeyState(VK_LBUTTON.code) & 0x8000) != 0;

        if (leftDown && !lastLeftDown) { // edge-triggered on press
            WinDef.POINT pt = new WinDef.POINT();
            if (User32.INSTANCE.GetCursorPos(pt)) {
                // Center the frame on the click position. Change to false for top-left anchoring.
                moveFrameTo(pt.x, pt.y, /*center=*/true);
            }
        }
        lastLeftDown = leftDown;
    }

    private void moveFrameTo(int mouseX, int mouseY, boolean center) {
        Rectangle bounds = SecureFrame.frame.getBounds();
        int w = bounds.width;
        int h = bounds.height;

        int targetX = center ? mouseX - (w / 2) : mouseX;
        int targetY = center ? mouseY - (h / 2) : mouseY;

//        // Optional: keep fully on the primary screen
//        Rectangle screen = GraphicsEnvironment
//                .getLocalGraphicsEnvironment()
//                .getDefaultScreenDevice()
//                .getDefaultConfiguration()
//                .getBounds();
//
//        targetX = Math.max(screen.x, Math.min(targetX, screen.x + screen.width - w));
//        targetY = Math.max(screen.y, Math.min(targetY, screen.y + screen.height - h));

        final int finalTargetX = targetX;
        final int finalTargetY = targetY;
        EventQueue.invokeLater(() -> SecureFrame.frame.setLocation(finalTargetX, finalTargetY));
    }

    public void changeFramePosition() {
        Rectangle current = SecureFrame.frame.getBounds();
        int step = 100;
        int newX = current.x;
        int newY = current.y;
        boolean moved = false;

        if (isPressed(VK_NUMPAD8.code)) {
            newY -= step; moved = true;
        } else if (isPressed(VK_NUMPAD2.code)) {
            newY += step; moved = true;
        } else if (isPressed(VK_NUMPAD4.code)) {
            newX -= step; moved = true;
        } else if (isPressed(VK_NUMPAD6.code)) {
            newX += step; moved = true;
        }

        if (moved) {
            SecureFrame.frame.setLocation(newX, newY);
            waitForKeyRelease();
        }
    }

    public void waitForKeyRelease() {
        try {
            while (isPressed(VK_NUMPAD8.code) ||
                    isPressed(VK_NUMPAD2.code) ||
                    isPressed(VK_NUMPAD4.code) ||
                    isPressed(VK_NUMPAD6.code)) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void delay(int d) {
        try { Thread.sleep(d); } catch (InterruptedException e) { throw new RuntimeException(e); }
    }

    public static boolean isPressed(int vkCode) {
        return (User32.INSTANCE.GetAsyncKeyState(vkCode) & 0x8000) != 0;
    }

    public boolean isPressed(int vkCode, int d) {
        boolean t = isPressed(vkCode);
        if (t) delay(d);
        return t;
    }
}

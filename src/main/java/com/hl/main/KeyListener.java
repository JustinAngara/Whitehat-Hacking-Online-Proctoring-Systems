package com.hl.main;
import com.sun.jna.Library;
import com.sun.jna.Native;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.sun.jna.platform.win32.Win32VK.*;

public class KeyListener implements Runnable {
    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class);

        short GetAsyncKeyState(int vKey);
    }


    static int currentIndex = 0;
    static volatile boolean isTriggerOn;

    public KeyListener() {}


    @Override
    public void run() {

        isTriggerOn = true;
        int vkCodeC = 0x43;
        Rectangle originalBound = SecureFrame.frame.getBounds();
        Rectangle hiddenBounds = new Rectangle(-10000,0, (int) originalBound.getWidth(), (int) originalBound.getHeight());
        // continuously check if a keypress is hit
        while (isTriggerOn) {



            // checks for visibility
            if(isPressed(VK_F10.code)){
                // makes it so you can toggle the visiility of the jframe
                boolean reverse = !SecureFrame.frame.getBounds().equals(originalBound);
                Rectangle bound = reverse ? originalBound : hiddenBounds;
                SecureFrame.frame.setBounds(bound);
                delay();
            }

            // start typing
            else if(isPressed(VK_OEM_PLUS.code)){
                System.out.println("pressed plus");
                delay();
            }


            // now user is pressing control, so if user presses numpad up(8), numpad left(4), ...
            // move the frame by 100 px increments respectively to their direction
            else if(isPressed(VK_CONTROL.code)) {


                Rectangle current = SecureFrame.frame.getBounds();
                int step = 100;
                int newX = current.x;
                int newY = current.y;
                boolean moved = false;

                if (isPressed(VK_NUMPAD8.code)) {
                    newY -= step;
                    moved = true;
                } else if (isPressed(VK_NUMPAD2.code)) {
                    newY += step;
                    moved = true;
                } else if (isPressed(VK_NUMPAD4.code)) {
                    newX -= step;
                    moved = true;
                } else if (isPressed(VK_NUMPAD6.code)) {
                    newX += step;
                    moved = true;
                }

                if (moved) {
                    SecureFrame.frame.setLocation(newX, newY);
                    waitForKeyRelease(); // prevent key repeat noise
                }
            }

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


    public void delay(){
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean isPressed(int vkCode){
        boolean t = (User32.INSTANCE.GetAsyncKeyState(vkCode) & 0x8000) != 0;
//        if (t) delay();
        return t;
    }
    public boolean runPseudoType(int vkCode) throws InterruptedException {
        boolean t = isPressed(vkCode);
        if(t){
            // will put the indecie to recieve from
            isTriggerOn = false;
            PseudoType.write(0);
            isTriggerOn = true;
        }

        return t;
    }


}
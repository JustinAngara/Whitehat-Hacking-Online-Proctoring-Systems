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

    // VK_CODE, Indecie of text to point to
    Map<Integer, Integer> map;

    static int currentIndex = 0;
    static volatile boolean isTriggerOn;

    // Up Down respectively changes the toggle to paste
    // - will assign the end, and PLUS will assign the start
    int[] hotkeys = new int[]{VK_UP.code, VK_DOWN.code, VK_OEM_PLUS.code, VK_OEM_MINUS.code};

    public KeyListener() {
        map = new HashMap<>();
        for(int i = 0; i < hotkeys.length; i++){
            map.put(hotkeys[i], i);
        }
    }
    public void run(){}
/*
    @Override
    public void run() {

        isTriggerOn = true;
        int vkCodeC = 0x43;
        Rectangle originalBound = SecureFrame.frame.getBounds();
        Rectangle hiddenBounds = new Rectangle(-10000,0, (int) originalBound.getWidth(), (int) originalBound.getHeight());
        // continuously check if a keypress is hit
        while (isTriggerOn) {
            // checks for visibility
            if((User32.INSTANCE.GetAsyncKeyState(VK_F10.code) & 0x8000) != 0){
                // makes it so you can toggle the visiility of the jframe
                boolean reverse = !SecureFrame.frame.getBounds().equals(originalBound);
                Rectangle bound = reverse ? originalBound : hiddenBounds;
                SecureFrame.frame.setBounds(bound);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if((User32.INSTANCE.GetAsyncKeyState(VK_OEM_PLUS.code) & 0x8000) != 0){
                try {
                    Main.handler.replyMessage("test from java");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }


            // iterates through the hashmap
//            for(Map.Entry<Integer, Integer> entry : map.entrySet()){
//                // chceks if a button from the hotkey is pressed
//                try {
//
//                    if(runPseudoType(entry.getKey())){
//                         try {
//                             Thread.sleep(100);
//                         } catch (InterruptedException e) {
//                             throw new RuntimeException(e);
//                         }
//                    }
//
//
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }



        }
    }
    public boolean isPressed(int vkCode){
        return (User32.INSTANCE.GetAsyncKeyState(vkCode) & 0x8000) != 0;
    }
    public boolean runPseudoType(int vkCode) throws InterruptedException {
        boolean t = isPressed(vkCode);
        if(t){
            // will put the indecie to recieve from
            isTriggerOn = false;
            PseudoType.write(map.get(vkCode));
            isTriggerOn = true;
        }

        return t;
    }
*/

}
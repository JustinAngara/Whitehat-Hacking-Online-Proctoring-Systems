package com.hl.main;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


import static com.sun.jna.platform.win32.Win32VK.VK_OEM_MINUS;
import static com.sun.jna.platform.win32.WinUser.INPUT.INPUT_KEYBOARD;
import static com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_KEYUP;

public class PseudoType {

    String fileLoc ="C:\\Users\\justi\\IdeaProjects\\Honorlock\\configs\\CopyCFG.txt";
    private int index = 0;
    private String[] stringsArr;

    public interface User32 extends StdCallLibrary {
        User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

        int SendInput(int nInputs, Pointer pInputs, int cbSize);

    }
    public void setup() throws FileNotFoundException, InterruptedException {
        populate();
    }

    /*
    * This method is used to increment the index in which we want to grab the string
    * */
    public int increment(int delta){
        int length = stringsArr.length;

        // wrap around using modular arithmetic
        index = (index + delta) % length;

        // ensure positive index if delta is negative
        if (index < 0) {
            index += length;
        }
//        System.out.println("new index :"+index);
        // i want to update the content array
        SecureFrame.changeContent(stringsArr[index]);
        return index;
    }


    public void populate() throws FileNotFoundException {
        // go to file location and add
        BufferedReader reader;
        String line, z = "";
        try {
            reader = new BufferedReader(new FileReader(fileLoc));
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                z+=line;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        stringsArr = z.split("<.ENDLINE>");

    }

    public String[] getStringArr(){
        return stringsArr;
    }

    /*
    * This method is used to remove any escape character that interfere
    * with the type configuration
    * */
    public static String removeExternalities(String s){
        // edit the string somehow

        return s;
    }

    public void write() throws InterruptedException {
        String sentence = stringsArr[index];
        // now individually press each one,
        for(int z = 0; z < sentence.length(); z++){
            if(KeyListener.isPressed(VK_OEM_MINUS.code)) return;


            int vkCode = charToVirtualKey(sentence.charAt(z));
            int random = (int)(Math.random()*100);
            Thread.sleep(random);
            pressKey(vkCode);
        }
    }
    public static int charToVirtualKey(char ch) {
        if (ch >= 'A' && ch <= 'Z') return ch; // A-Z
        if (ch >= 'a' && ch <= 'z') return ch - 32; // a-z -> A-Z
        if (ch >= '0' && ch <= '9') return ch; // 0-9
        if (ch == ' ') return 0x20; // space
        return -1; // unsupported char
    }

    public static void pressKey(int vkCode) {
        WinUser.INPUT press = new WinUser.INPUT();
        press.type = new WinDef.DWORD(INPUT_KEYBOARD);
        press.input.setType("ki");
        press.input.ki.wVk = new WinDef.WORD(vkCode);
        press.input.ki.dwFlags = new WinDef.DWORD(0); // Key down

        WinUser.INPUT inputRelease = new WinUser.INPUT();
        inputRelease.type = new WinDef.DWORD(INPUT_KEYBOARD);
        inputRelease.input.setType("ki");
        inputRelease.input.ki.wVk = new WinDef.WORD(vkCode);
        inputRelease.input.ki.dwFlags = new WinDef.DWORD(KEYEVENTF_KEYUP);

        int inputSize = press.size();
        Memory memory = new Memory(inputSize * 2L);

        // write into memory
        press.write();
        inputRelease.write();

        press.getPointer().write(0, press.getPointer().getByteArray(0, inputSize), 0, inputSize);
        inputRelease.getPointer().write(0, inputRelease.getPointer().getByteArray(0, inputSize), 0, inputSize);

        memory.write(0, press.getPointer().getByteArray(0, inputSize), 0, inputSize);
        memory.write(inputSize, inputRelease.getPointer().getByteArray(0, inputSize), 0, inputSize);

        // call SendInput with that memory buffer
        int sent = User32Ext.INSTANCE.SendInput(2, memory, inputSize);

        if (sent == 0) {
            System.err.println(Native.getLastError());
        }
    }

}

import com.sun.jna.Library;
import com.sun.jna.Native;

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


//    static volatile boolean isTriggerOn;
    int[] hotkeys = new int[]{VK_C.code,VK_UP.code, VK_DOWN.code, VK_OEM_MINUS.code, VK_OEM_PLUS.code};

    public KeyListener() {
        map = new HashMap<>();
        for(int i = 0; i < hotkeys.length; i++){
            map.put(hotkeys[i], i);
        }
    }

    @Override
    public void run() {
        int vkCodeC = 0x43;

        // continuously check if a keypress is hit
        while (true) {
            // iterates through the hashmap
            for(Map.Entry<Integer, Integer> entry : map.entrySet()){
                // chceks if a button from the hotkey is pressed
                try {
                    if(isPressed(entry.getKey())){
                         try {
                             Thread.sleep(100);
                         } catch (InterruptedException e) {
                             throw new RuntimeException(e);
                         }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean isPressed(int vkCode) throws InterruptedException {
        boolean t = (User32.INSTANCE.GetAsyncKeyState(vkCode) & 0x8000) != 0;
        if(t){
            // will put the indecie to recieve from
            PseudoType.write(map.get(vkCode));
        }

        return t;
    }
}
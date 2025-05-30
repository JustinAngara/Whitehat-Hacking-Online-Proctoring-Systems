import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.sun.jna.platform.win32.Win32VK.VK_OEM_MINUS;

public class Main {
    public static void main(String[] args) throws Exception {
        PseudoType.pressKey(VK_OEM_MINUS.code);
        PseudoType.setup();
        // creates frame
        SecureFrame s = new SecureFrame();
        s.run();

        // put in multithread-emptyso this
        KeyListener k = new KeyListener();



        // this will run KeyListener
        int coreCount = Runtime.getRuntime().availableProcessors();
        System.out.println(coreCount);

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(coreCount);

        // submit tasks to the thread pool
        executor.execute(k);

        // no longer point
        executor.shutdown();
    }
}

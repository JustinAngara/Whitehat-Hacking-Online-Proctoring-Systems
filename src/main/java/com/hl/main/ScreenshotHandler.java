package com.hl.main;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ScreenshotHandler {

    public static BufferedImage takeFullScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            return robot.createScreenCapture(screenRect);
        } catch (AWTException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void sendToClients() {
        BufferedImage screenshot = takeFullScreenshot();
        if (screenshot != null) {
            WebSocketHandler.sendScreenshotToAll(screenshot);
        }
    }
}

package com.hl.main;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;


public class InteractGemini {
    public static void main(String[] args) {

        String apiKey = "";
        String message = "evaluate this screenshot";
        BufferedImage bf = takeScreenshot();
        sendGeminiImagePrompt(bf, apiKey, message);

    }

    public static BufferedImage takeScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            return robot.createScreenCapture(screenRect);
        } catch (AWTException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void sendGeminiImagePrompt(BufferedImage image, String apiKey, String promptText) {
        try {
            // Convert BufferedImage to Base64 PNG string
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            baos.close();

            // Prepare JSON request
            String jsonInput = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "%s"
                    },
                    {
                      "inlineData": {
                        "mimeType": "image/png",
                        "data": "%s"
                      }
                    }
                  ]
                }
              ]
            }
            """.formatted(promptText.replace("\"", "\\\""), base64Image);

            // Set up connection
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:generateContent?key=" + apiKey;

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send JSON body
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("utf-8"));
            }

            // Read response
            int status = conn.getResponseCode();
            InputStream responseStream = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, "utf-8"))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
                System.out.println("Gemini Response: " + response);
            }

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.hl.main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class InteractGPT {

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

    public static void sendChatGPTImagePrompt(BufferedImage image, String apiKey, String promptText) {
        try {
            // convert BufferedImage to Base64 PNG string
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            baos.close();

            // setup JSON request
            String jsonInput = """
            {
              "model": "gpt-4o",
              "messages": [
                {
                  "role": "user",
                  "content": [
                    {
                      "type": "text",
                      "text": "%s"
                    },
                    {
                      "type": "image_url",
                      "image_url": {
                        "url": "data:image/png;base64,%s"
                      }
                    }
                  ]
                }
              ]
            }
            """.formatted(promptText.replace("\"", "\\\""), base64Image);

            // setup connection
            String endpoint = "https://api.openai.com/v1/chat/completions";

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // send JSON body
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("utf-8"));
            }

            // get response
            int status = conn.getResponseCode();
            InputStream responseStream = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, "utf-8"))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
                System.out.println("ChatGPT Response: " + response);
                // return this shit
            }

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String apiKey = ""; // Insert your OpenAI API key
        String message = "evaluate this screenshot";
        BufferedImage bf = takeScreenshot();
        sendChatGPTImagePrompt(bf, apiKey, message);
    }
}

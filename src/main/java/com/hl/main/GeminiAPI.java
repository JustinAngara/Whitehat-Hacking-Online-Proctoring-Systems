package com.hl.main;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import javax.imageio.ImageIO;

public class GeminiAPI {

    private static final String API_KEY = APIHandler.getGeminiKey();
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    private HttpClient httpClient;

    public GeminiAPI() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String runQuery(BufferedImage bi, String prompt) {
        try {
            String base64Image = convertImageToBase64(bi);
            String jsonPayload = createJsonPayload(base64Image, prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseGeminiResponse(response.body());

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public String runScreenshotQuery(String prompt) {
        BufferedImage bi = ScreenshotHandler.takeFullScreenshot();
        if (bi == null) {
            return "Failed to capture screenshot";
        }
        return runQuery(bi, prompt);
    }

    private String convertImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }




    private String createJsonPayload(String base64Image, String prompt) {
        return String.format("""
            {
              "contents": [{
                "parts": [
                  {"text": "%s"},
                  {
                    "inline_data": {
                      "mime_type": "image/png",
                      "data": "%s"
                    }
                  }
                ]
              }]
            }
            """, prompt, base64Image);
    }

    public void displayPrompt(){
        // now this will give us the stuff
        String answer = runScreenshotQuery(APIHandler.PROMPT);

        // display it to the frame
        Main.s.changeContent(answer);
    }

    private String parseGeminiResponse(String responseBody) {
        int textStart = responseBody.indexOf("\"text\":");
        if (textStart != -1) {
            textStart += 8;
            int textEnd = responseBody.indexOf("\"", textStart);
            if (textEnd != -1) {
                return responseBody.substring(textStart, textEnd)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
        }
        return "No response found";
    }

}
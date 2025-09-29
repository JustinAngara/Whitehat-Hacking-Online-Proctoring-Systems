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

import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiAPI {

    private static final String API_KEY = APIHandler.getGeminiKey();
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final HttpClient httpClient;

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
                    .header("x-goog-api-key", API_KEY) // API key in header
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

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
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String createJsonPayload(String base64Image, String prompt) {
        String escapedPrompt = prompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        return String.format("""
            {
              "contents": [{
                "role": "user",
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
            """, escapedPrompt, base64Image);
    }

    public void displayPrompt() {
        String answer = runScreenshotQuery(APIHandler.PROMPT);
        Main.s.changeContent(answer);
    }

    private String parseGeminiResponse(String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);

            if (!root.has("candidates")) {
                return "No candidates found\nRaw: " + responseBody;
            }

            JSONArray candidates = root.getJSONArray("candidates");
            if (candidates.isEmpty()) {
                return "No candidates in response";
            }

            JSONObject first = candidates.getJSONObject(0);
            if (!first.has("content")) {
                return "No content in first candidate\nRaw: " + responseBody;
            }

            JSONObject content = first.getJSONObject("content");
            if (!content.has("parts")) {
                return "No parts in content\nRaw: " + responseBody;
            }

            JSONArray parts = content.getJSONArray("parts");
            for (int i = 0; i < parts.length(); i++) {
                JSONObject part = parts.getJSONObject(i);
                if (part.has("text")) {
                    return part.getString("text");
                }
            }

            return "No text found in parts\nRaw: " + responseBody;
        } catch (Exception e) {
            return "Parse error: " + e.getMessage() + "\nRaw: " + responseBody;
        }
    }
}

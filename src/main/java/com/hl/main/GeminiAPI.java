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

    // Reuse your existing key loader. Ensure it returns your OpenAI API key.
    private static final String API_KEY = APIHandler.getGeminiKey();

    // ChatGPT (OpenAI) Chat Completions endpoint
    private static final String OPENAI_CHAT_COMPLETIONS_URL =
            "https://api.openai.com/v1/chat/completions";

    // Use a vision-capable, coding-strong model
    private static final String MODEL = "o3";

    private final HttpClient httpClient;

    public GeminiAPI() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String runQuery(BufferedImage bi, String prompt) {

        try {
            String base64Image = convertImageToBase64(bi);
            String jsonPayload = createJsonPayload(base64Image, prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_CHAT_COMPLETIONS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = null;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                // swallow like original, but return a meaningful message below
                return "Network error: " + e.getMessage();
            }

            return parseGeminiResponse(response != null ? response.body() : null);

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
        // Build payload as JSON to avoid escaping pitfalls
        JSONObject root = new JSONObject();
        root.put("model", MODEL);
//        root.put("temperature", 0.1); // coding-friendly determinism

        // Optional: steer for coding quality
        JSONObject systemMsg = new JSONObject()
                .put("role", "system")
                .put("content", "You are a senior software engineer specializing in LeetCode-style problems. Prefer concise, correct answers that are relevant to coding interview questions. When code is requested, return the code first, then minimal notes. " +
                        "Default language is C++ Unless context of screenshot says otherwise. Furthermore, focus on memory space optimization IF the algorithm in choice is considered as a 'top' pick");

        // User content with text + image (vision)
        JSONObject textPart = new JSONObject()
                .put("type", "text")
                .put("text", prompt);

        // OpenAI expects image as a data URL for image inputs in chat content
        JSONObject imageUrl = new JSONObject()
                .put("url", "data:image/png;base64," + base64Image);

        JSONObject imagePart = new JSONObject()
                .put("type", "image_url")
                .put("image_url", imageUrl);

        JSONArray userContent = new JSONArray()
                .put(textPart)
                .put(imagePart);

        JSONObject userMsg = new JSONObject()
                .put("role", "user")
                .put("content", userContent);

        JSONArray messages = new JSONArray()
                .put(systemMsg)
                .put(userMsg);

        root.put("messages", messages);

        return root.toString();
    }

    public Runnable displayPrompt() {
        try {
            String answer = runScreenshotQuery(APIHandler.PROMPT);
            Main.s.changeContent(answer); // preserve original side-effect
        } catch (Exception e) {
            // match original swallow
        }
        return null;
    }

    public Runnable displayTransPrompt() {
        try {
            String answer = runTextQuery(APIHandler.TRANS_PROMPT + "\n\nUser Question: "+AudioListener.content);
            Main.s.changeContent(answer);

        } catch (Exception e) {
            // match original swallow
        }
        return null;
    }


    public String runTextQuery(String prompt) {
        try {
            // Build text-only JSON payload
            JSONObject root = new JSONObject();
            root.put("model", MODEL);
            root.put("temperature", 0.1); // deterministic for coding

            JSONObject systemMsg = new JSONObject()
                    .put("role", "system")
                    .put("content",
                            "You are a senior software engineer specializing in system-level and algorithmic reasoning. "
                                    + "When asked a question, assume it is coding-related unless clearly not. "
                                    + "Default to **C++** unless syntax suggests another language. "
                                    + "Provide runnable code first, followed by a low-level explanation covering stack/heap, "
                                    + "runtime behavior, and compiler-level effects where relevant.");

            JSONObject userMsg = new JSONObject()
                    .put("role", "user")
                    .put("content", new JSONArray()
                            .put(new JSONObject()
                                    .put("type", "text")
                                    .put("text", prompt)));

            JSONArray messages = new JSONArray()
                    .put(systemMsg)
                    .put(userMsg);

            root.put("messages", messages);

            String jsonPayload = root.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_CHAT_COMPLETIONS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = null;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                return "Network error: " + e.getMessage();
            }

            return parseGeminiResponse(response != null ? response.body() : null);

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }


    private String parseGeminiResponse(String responseBody) {
        // Adapted to OpenAI chat.completions JSON format
        if (responseBody == null) return "";
        try {
            JSONObject root = new JSONObject(responseBody);

            if (!root.has("choices")) {
                return "No choices found\nRaw: " + responseBody;
            }

            JSONArray choices = root.getJSONArray("choices");
            if (choices.isEmpty()) {
                return "No choices in response";
            }

            JSONObject first = choices.getJSONObject(0);

            // Newer OpenAI responses: choices[0].message.content (string)
            if (first.has("message")) {
                JSONObject message = first.getJSONObject("message");
                if (message.has("content")) {
                    return message.getString("content");
                }
            }

            // Fallbacks (older/alternative shapes)
            if (first.has("text")) {
                return first.getString("text");
            }

            return "No text content found in response\nRaw: " + responseBody;

        } catch (Exception e) {
            return "Parse error: " + e.getMessage() + "\nRaw: " + responseBody;
        }
    }
}

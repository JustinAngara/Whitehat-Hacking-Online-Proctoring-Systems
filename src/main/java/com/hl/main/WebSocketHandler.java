package com.hl.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private String content;
    private WebSocketSession session;
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("Session connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        System.out.println("Session closed: " + session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String incoming = message.getPayload();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(incoming);

            // If the client sends: { "action": "screenshot" }
            if (jsonNode.has("action") && jsonNode.get("action").asText().equals("screenshot")) {
                ScreenshotHandler.sendToClients();
                return;
            }

            // If the client sends: { "content": "..." }
            if (jsonNode.has("content")) {
                content = jsonNode.get("content").asText();
                updateContent();
                return;
            }

        } catch (Exception e) {
            // Fallback: treat it as raw plain text
            content = incoming;
            updateContent();
        }
    }


    public void updateContent() {
        SecureFrame.changeContent(content);
    }

    public void replyMessage(String message) throws IOException {
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    // Called by ScreenshotHandler to push images to all clients
    public static void sendScreenshotToAll(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            BinaryMessage binary = new BinaryMessage(imageBytes);

            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(binary);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to send screenshot: " + e.getMessage());
        }
    }
}

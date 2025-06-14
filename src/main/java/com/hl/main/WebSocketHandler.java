package com.hl.main;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private String content;
    private WebSocketSession session;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

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
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String incoming = message.getPayload();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(incoming);
            content = jsonNode.get("content").asText();
            updateContent();

        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }

    public void updateContent(){
        SecureFrame.changeContent(content);
    }

    public void replyMessage(String message) throws IOException {
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {

                session.sendMessage(new TextMessage(message));
            }
        }
    }

}

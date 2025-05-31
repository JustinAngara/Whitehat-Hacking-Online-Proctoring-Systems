package com.hl.main;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class WebSocketHandler extends TextWebSocketHandler {
    private WebSocketSession session;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        System.out.println("Connection established");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String incoming = message.getPayload();
        System.out.println("Received from React: " + incoming);

        // Example response:
        String reply = "{\"msg\":\"Hello from Java!\"}";
        session.sendMessage(new TextMessage(reply));
    }
}

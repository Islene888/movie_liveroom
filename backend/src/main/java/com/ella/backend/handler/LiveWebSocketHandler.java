package com.ella.backend.handler;

import com.ella.backend.service.KafkaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(LiveWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Autowired
    private KafkaService kafkaService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("✅ 新的WebSocket连接已建立, Session ID: {}", session.getId());
        session.sendMessage(new TextMessage("✅ WebSocket 已连接"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
        log.info("❌ WebSocket连接已断开, Session ID: {}, Status: {}", session.getId(), status);
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        log.info("💬 收到来自 {} 的消息: {}", session.getId(), payload);

        kafkaService.sendEvent(payload);

        for (WebSocketSession ws : sessions) {
            if (ws.isOpen()) {
                synchronized (ws) {
                    ws.sendMessage(new TextMessage(payload));
                }
            }
        }
    }


}
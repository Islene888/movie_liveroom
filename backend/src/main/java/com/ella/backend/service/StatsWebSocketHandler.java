package com.ella.backend.service;

import com.ella.backend.dto.StatsDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class StatsWebSocketHandler extends TextWebSocketHandler {

    private static final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    // 这里是累计统计数据
    private int userJoinTotal = 0;
    private int likeTotal = 0;
    private int commentTotal = 0;
    private int giftTotal = 0;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        // 新连接推送一次当前数据
        try {
            session.sendMessage(new TextMessage(currentStatsJson()));
        } catch (Exception e) {
            // 日志
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    // 更新累计数据并推送（你在 Service 或 Flink聚合后直接调用这个）
    // StatsWebSocketHandler.java

//flink 自带的state

    public synchronized void addAndBroadcast(int userJoin, int like, int comment, int sendGift) {
        userJoinTotal += userJoin;
        likeTotal += like;
        commentTotal += comment;
        giftTotal += sendGift;
        StatsDto stats = new StatsDto();
        stats.setUserJoin(userJoinTotal);
        stats.setLike(likeTotal);
        stats.setComment(commentTotal);
        stats.setSendGift(giftTotal);
        try {
            String json = new ObjectMapper().writeValueAsString(stats);
            broadcast(json);
        } catch (Exception e) { e.printStackTrace(); }
    }


    // 广播给所有前端
    public void broadcast(String json) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    // 日志
                }
            }
        }
    }

    // 当前统计的 JSON
    private String currentStatsJson() {
        try {
            StatsDto stats = new StatsDto();
            stats.setUserJoin(userJoinTotal);
            stats.setLike(likeTotal);
            stats.setComment(commentTotal);
            stats.setSendGift(giftTotal);
            return objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            return "{}";
        }
    }
}

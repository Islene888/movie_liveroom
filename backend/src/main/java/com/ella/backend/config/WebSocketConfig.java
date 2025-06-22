package com.ella.backend.config;


import com.ella.backend.handler.LiveWebSocketHandler;
import com.ella.backend.service.StatsWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private LiveWebSocketHandler liveWebSocketHandler;

    @Autowired
    private StatsWebSocketHandler statsWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 主弹幕通道
        registry.addHandler(liveWebSocketHandler, "/ws")
                .setAllowedOrigins("*");

        // 聚合统计通道
        registry.addHandler(statsWebSocketHandler, "/stats-ws")
                .setAllowedOrigins("*");

}
}


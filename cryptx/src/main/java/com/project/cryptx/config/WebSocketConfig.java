package com.project.cryptx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.project.cryptx.websocket.BinanceWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BinanceWebSocketHandler binanceWebSocketHandler;

    public WebSocketConfig(BinanceWebSocketHandler binanceWebSocketHandler) {
        this.binanceWebSocketHandler = binanceWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Mapping for MVC WebSocket
        registry.addHandler(binanceWebSocketHandler, "/ws/crypto/{symbol}")
                .setAllowedOrigins("*"); // Allow CORS
    }
}

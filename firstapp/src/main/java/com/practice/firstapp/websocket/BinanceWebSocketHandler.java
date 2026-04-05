package com.practice.firstapp.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BinanceWebSocketHandler extends TextWebSocketHandler {

    private final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/";
    
    // Track Binance connections per browser session to close them later
    private final Map<String, Disposable> binanceConnections = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        String symbol = path.substring(path.lastIndexOf('/') + 1);

        if (symbol == null || symbol.isEmpty()) {
            session.close();
            return;
        }

        String binanceUrl = BINANCE_WS_URL + symbol.toLowerCase() + "@ticker";
        System.out.println("BRIDGE ATTEMPT: " + symbol + " -> " + binanceUrl);

        Disposable connection = HttpClient.create()
                .websocket(WebsocketClientSpec.builder().build())
                .uri(binanceUrl)
                .handle((inbound, outbound) -> {
                    System.out.println("BRIDGE CONNECTED to Binance for: " + symbol);
                    return inbound.receive()
                            .asString()
                            .doOnNext(message -> {
                                try {
                                    if (session.isOpen()) {
                                        session.sendMessage(new TextMessage(message));
                                    }
                                } catch (Exception e) {
                                    System.err.println("Forward Error: " + e.getMessage());
                                }
                            })
                            .doOnTerminate(() -> System.out.println("Binance Stream Terminated for: " + symbol))
                            .then();
                })
                .doOnError(err -> System.err.println("Binance Connection FAILED for " + symbol + ": " + err.getMessage()))
                .subscribe();

        binanceConnections.put(session.getId(), connection);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Disposable connection = binanceConnections.remove(session.getId());
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
            System.out.println("Closed bridge for session: " + session.getId());
        }
    }
}

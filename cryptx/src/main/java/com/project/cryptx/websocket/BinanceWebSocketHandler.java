package com.project.cryptx.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.project.cryptx.config.SingletonLogger;

import reactor.core.Disposable;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.util.retry.Retry;
import io.netty.resolver.DefaultAddressResolverGroup;
import java.time.Duration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BinanceWebSocketHandler extends TextWebSocketHandler {

    @Value("${crypto-base-url.binance-websocket}")
    private String binanceWebSocketUrl;

    private static final SingletonLogger log = SingletonLogger.log();

    // private final String BINANCE_WS_URL = ch"wss://stream.binance.com:9443/ws/";

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

        String binanceUrl = binanceWebSocketUrl + symbol.toLowerCase() + "@ticker";
        log.info("BRIDGE ATTEMPT: {} -> {}", symbol, binanceUrl);

        Disposable connection = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .websocket(WebsocketClientSpec.builder().build())
                .uri(binanceUrl)
                .handle((inbound, outbound) -> {
                    log.info("BRIDGE CONNECTED to Binance for: {}", symbol);
                    return inbound.receive()
                            .asString()
                            .doOnNext(message -> {
                                try {
                                    if (session.isOpen()) {
                                        session.sendMessage(new TextMessage(message));
                                    }
                                } catch (Exception e) {
                                    log.error("Forward Error: {}", e.getMessage(), e);
                                }
                            })
                            .doOnTerminate(() -> log.info("Binance Stream Terminated for: {}", symbol))
                            .then();
                })
                .doOnError(
                        err -> log.error("Binance Connection FAILED for {}: {}", symbol, err.getMessage(), err))
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(2))
                        .doBeforeRetry(
                                retrySignal -> log.info("Retrying Binance connection for {}...", symbol)))
                .subscribe();

        binanceConnections.put(session.getId(), connection);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Disposable connection = binanceConnections.remove(session.getId());
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
            log.info("Closed bridge for session: {}", session.getId());
        }
    }
}
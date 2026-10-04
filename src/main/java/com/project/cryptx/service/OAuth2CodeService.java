package com.project.cryptx.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.ServiceUnavailableException;

import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OAuth2CodeService {

    private static class CodeData {
        Long userId;
        Instant expiry;

        CodeData(Long userId, Instant expiry) {
            this.userId = userId;
            this.expiry = expiry;
        }
    }

    private final Map<String, CodeData> codeStore = new ConcurrentHashMap<>();

    public void storeCode(String code, Long userId) {
        log.info("Storing OAuth2 authorization code for user: {}", userId);
        // Codes expire in 60 seconds
        codeStore.put(code, new CodeData(userId, Instant.now().plusSeconds(60)));
    }

    @CircuitBreaker(name = "consumeCode", fallbackMethod = "consumeCodeFallback")
    public Long consumeCode(String code) {
        log.info("Consuming OAuth2 authorization code");
        CodeData data = codeStore.remove(code); // Atomic read-and-remove ensures single-use
        if (data != null && data.expiry.isAfter(Instant.now())) {
            log.info("Successfully consumed OAuth2 authorization code for user: {}", data.userId);
            return data.userId;
        }
        log.warn("OAuth2 authorization code was invalid or expired");
        return null;
    }

    public Long consumeCodeFallback(String code, Exception e) throws ServiceUnavailableException {
        log.error("consumeCode fallback triggered. Error: {}", e.getMessage());
        throw new ServiceUnavailableException("Authorization service temporarily unavailable");
    }
}

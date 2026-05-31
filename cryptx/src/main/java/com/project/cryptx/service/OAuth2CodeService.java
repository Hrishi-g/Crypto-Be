package com.project.cryptx.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

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
        // Codes expire in 60 seconds
        codeStore.put(code, new CodeData(userId, Instant.now().plusSeconds(60)));
    }

    public Long consumeCode(String code) {
        CodeData data = codeStore.remove(code); // Atomic read-and-remove ensures single-use
        if (data != null && data.expiry.isAfter(Instant.now())) {
            return data.userId;
        }
        return null;
    }
}

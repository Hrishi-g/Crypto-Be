package com.practice.firstapp.config;

import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

        @Bean
        public CacheManager cacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager();
                cacheManager.setAsyncCacheMode(true); // Required for Mono/Flux

                // 5-minute cache for crypto prices to avoid 429 Too Many Requests
                Caffeine<Object, Object> fiveMin = Caffeine.newBuilder()
                                .recordStats()
                                .expireAfterWrite(300, TimeUnit.SECONDS)
                                .maximumSize(100);

                // 1-hour cache for historical charts (24h trend)
                Caffeine<Object, Object> historicalCache = Caffeine.newBuilder()
                                .recordStats()
                                .expireAfterWrite(3600, TimeUnit.SECONDS)
                                .maximumSize(50);

                // Register them separately with their own timers
                cacheManager.registerCustomCache("crypto-data", fiveMin.buildAsync());
                cacheManager.registerCustomCache("historical-data", historicalCache.buildAsync());

                return cacheManager;
        }
}

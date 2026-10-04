package com.project.cryptx.config;

import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAsyncCacheMode(false);

        Caffeine<Object, Object> userCache = Caffeine.newBuilder()
                .expireAfterWrite(600, TimeUnit.SECONDS)
                .maximumSize(200);

        cacheManager.registerCustomCache("user", userCache.build());
        cacheManager.registerCustomCache("portfolio", userCache.build());
        return cacheManager;
    }

    @Bean(name = "asyncCacheManager")
    public CacheManager asyncCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAsyncCacheMode(true);

        Caffeine<Object, Object> fiveMin = Caffeine.newBuilder()
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .maximumSize(100);

        Caffeine<Object, Object> historicalCache = Caffeine.newBuilder()
                .expireAfterWrite(900, TimeUnit.SECONDS) // last 24hrs data cache 15 min and store max 50 items
                .maximumSize(50);

        cacheManager.registerCustomCache("crypto-data", fiveMin.buildAsync());
        cacheManager.registerCustomCache("historical-data", historicalCache.buildAsync());
        cacheManager.registerCustomCache("exchange-rate", fiveMin.buildAsync());

        return cacheManager;
    }
}

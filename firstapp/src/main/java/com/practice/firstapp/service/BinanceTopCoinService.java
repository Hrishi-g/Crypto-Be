package com.practice.firstapp.service;

import com.practice.firstapp.dto.BinanceTickerDto;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class BinanceTopCoinService {

    private final WebClient webClient;
    // Thread-safe way to store the cached list in memory
    private final AtomicReference<List<BinanceTickerDto>> topCoinsCache = new AtomicReference<>(
            Collections.emptyList());

    public BinanceTopCoinService() {
        // Create a new WebClient pointing to Binance manually to avoid Spring bean
        // conflict
        this.webClient = WebClient.create("https://api.binance.com");
    }

    @PostConstruct // Runs immediately when the app starts so the cache is never empty
    @Scheduled(fixedRate = 300000) // Runs every 5 minutes (300,000 milliseconds)
    public void fetchAndCacheTopCoins() {
        System.out.println("Fetching 24hr ticker from Binance to update top 10 cache...");

        webClient.get()
                .uri("/api/v3/ticker/24hr")
                .retrieve()
                .bodyToFlux(BinanceTickerDto.class)
                // Filter for USDT pairs only
                .filter(ticker -> ticker.getSymbol() != null && ticker.getSymbol().endsWith("USDT"))
                // Sort descending by quoteVolume
                .sort(Comparator.comparingDouble(BinanceTickerDto::getQuoteVolume).reversed())
                // Take only the top 10
                .take(12)
                .collectList()
                .subscribe(
                        // On Success: Update our in-memory cache
                        top10 -> {
                            topCoinsCache.set(top10);
                            System.out.println("Successfully updated top 10 Binance coins cache.");
                        },
                        // On Error: Log it, keep old cache
                        error -> System.err.println("Failed to fetch Binance tickers: " + error.getMessage()));
    }

    // This returns the cached top 12 list instantly
    public List<BinanceTickerDto> getBinanceTopCoins() {
        return topCoinsCache.get();
    }
}

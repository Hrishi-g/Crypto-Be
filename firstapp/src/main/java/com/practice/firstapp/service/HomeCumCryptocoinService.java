package com.practice.firstapp.service;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.practice.firstapp.dto.BinanceTickerDto;
import com.practice.firstapp.dto.CryptoDto;
import com.practice.firstapp.dto.SearchResponseDto;

import jakarta.annotation.PostConstruct;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class HomeCumCryptocoinService {

    private final WebClient webClient;

    public HomeCumCryptocoinService(WebClient webClient) {
        this.webClient = webClient;
    }

    private final AtomicReference<List<BinanceTickerDto>> topCoinsCache = new AtomicReference<>(
            Collections.emptyList());

    @Cacheable(value = "crypto-data", key = "'all-crypto-page-' + #page + '-size-' + #perPage", cacheManager = "asyncCacheManager")
    public Mono<List<CryptoDto>> getAllCrptoData(int page, int perPage) {
        System.out.println("Fetching real crypto data from CoinGecko (Page: " + page + ", Size: " + perPage + ")...");
        String url = String.format(
                "https://api.coingecko.com/api/v3/coins/markets?vs_currency=inr&order=market_cap_desc&per_page=%d&page=%d",
                perPage,
                page);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(CryptoDto.class)
                .collectList()
                .timeout(Duration.ofSeconds(10))
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .doOnSuccess(list -> System.out
                        .println("Successfully fetched " + (list != null ? list.size() : 0) + " coins"))
                .doOnError(error -> System.err.println("CoinGecko API Error: " + error.getMessage()))
                .onErrorResume(error -> {
                    System.out.println("Falling back to empty list due to API error");
                    return Mono.just(List.of());
                });
    }

    @Cacheable(value = "crypto-data", key = "'search-' + #query", cacheManager = "asyncCacheManager")
    public Mono<List<CryptoDto>> searchCrypto(String query) {
        System.out.println("Searching for crypto with query: " + query);
        String searchUrl = "https://api.coingecko.com/api/v3/search?query=" + query;
        return webClient.get()
                .uri(searchUrl)
                .retrieve()
                .bodyToMono(SearchResponseDto.class)
                .flatMap(searchResponse -> {
                    if (searchResponse == null || searchResponse.getCoins() == null
                            || searchResponse.getCoins().isEmpty()) {
                        return Mono.just(List.<CryptoDto>of());
                    }
                    // Get top 10 matching coin IDs
                    List<String> ids = searchResponse.getCoins().stream()
                            .limit(10)
                            .map(SearchResponseDto.SearchCoinDto::getId)
                            .toList();
                    if (ids.isEmpty()) {
                        return Mono.just(List.<CryptoDto>of());
                    }
                    String idsJoined = String.join(",", ids);
                    String marketsUrl = String.format("/coins/markets?vs_currency=inr&ids=%s&order=market_cap_desc",
                            idsJoined);
                    return webClient.get()
                            .uri(marketsUrl)
                            .retrieve()
                            .bodyToFlux(CryptoDto.class)
                            .collectList();
                })
                .timeout(Duration.ofSeconds(15))
                .onErrorResume(error -> {
                    System.err.println("Search/Markets Error: " + error.getMessage());
                    return Mono.just(List.of());
                });
    }

    // @Cacheable(value = "exchange-rate", key = "'usd-inr'", cacheManager =
    // "asyncCacheManager")
    public Mono<Double> getUsdToInrRate() {
        return webClient.get()
                .uri("https://api.frankfurter.dev/v2/rate/USD/INR")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode json = mapper.readTree(response);
                        return json.get("rate").asDouble();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .onErrorResume(ex -> {
                    ex.printStackTrace();
                    return Mono.just(95.00);
                });
    }

    @Cacheable(value = "historical-data", key = "'historical-v3-' + #symbol + '-' + #interval + '-' + #limit", cacheManager = "asyncCacheManager")
    public Mono<Map<String, Object>> getHistoricalData(String symbol, String interval, int limit) {
        String istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return webClient.get()
                .uri("https://api.binance.com/api/v3/klines?symbol={symbol}&interval={interval}&limit={limit}",
                        symbol.toUpperCase(), interval, limit)
                .retrieve()
                .bodyToMono(Object.class)
                .map(data -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("tickerData", data);
                    response.put("timestamp", istTime);
                    return response;
                })
                .timeout(Duration.ofSeconds(15))
                .onErrorResume(error -> {
                    System.out.println("Historical Data Error: " + error.getMessage());
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("tickerData", null);
                    errorMap.put("timestamp", istTime);
                    return Mono.just(errorMap);
                })
                .cache(); // IMPORTANT: Converts the cold Mono into a hot one for actual reactive caching
    }

    @PostConstruct // Runs immediately when the app starts so the cache is never empty
    @Scheduled(fixedRate = 3600000) // Runs every 1 hour (3,600,000 milliseconds)
    public void fetchAndCacheTopCoins() {
        System.out.println("Fetching crypto markets to show on home page using postConstruct");
        webClient.get()
                .uri("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=250&page=1")
                .retrieve()
                .bodyToFlux(CryptoDto.class)
                .collectMap(coin -> coin.getSymbol().toUpperCase())
                .flatMap(coinGeckoMap -> {
                    // Fetch top volume pairs from Binance
                    return WebClient.create().get()
                            .uri("https://api.binance.com/api/v3/ticker/24hr")
                            .retrieve()
                            .bodyToFlux(BinanceTickerDto.class)
                            .filter(ticker -> ticker.getSymbol() != null && ticker.getSymbol().endsWith("USDT"))
                            .sort(Comparator.comparingDouble(BinanceTickerDto::getQuoteVolume).reversed())
                            .take(12)
                            .map(ticker -> {
                                String baseSymbol = ticker.getSymbol().replace("USDT", "");
                                CryptoDto cgCoin = coinGeckoMap.get(baseSymbol);
                                if (cgCoin != null) {
                                    ticker.setName(cgCoin.getName());
                                    ticker.setImage(cgCoin.getImage());
                                } else {
                                    ticker.setName(baseSymbol);
                                    ticker.setImage("");
                                }
                                return ticker;
                            })
                            .collectList();
                })
                .subscribe(
                        top12 -> {
                            topCoinsCache.set(top12);
                            System.out.println(
                                    "Successfully updated top 12 Binance coins cache with CoinGecko images and names.");
                        },
                        error -> System.err.println("Failed to fetch Top Coins data: " + error.getMessage()));
    }

    public List<BinanceTickerDto> getBinanceTopCoins() {
        return topCoinsCache.get();
    }
}

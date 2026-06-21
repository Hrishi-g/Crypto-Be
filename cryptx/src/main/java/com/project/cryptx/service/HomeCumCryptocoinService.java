package com.project.cryptx.service;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
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
import com.project.cryptx.dto.BinanceTickerDto;
import com.project.cryptx.dto.CryptoDto;
import com.project.cryptx.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class HomeCumCryptocoinService {

    @Value("${crypto.logo-dev.image-token}")
    private String imageToken;

    @Value("${crypto-base-url.binance}")
    private String binanceBaseUrl;

    @Value("${crypto-base-url.coingecko}")
    private String coingeckoBaseUrl;

    @Value("${crypto-base-url.exchange-rate}")
    private String exchangeRateBaseUrl;

    @Value("${crypto-base-url.image}")
    private String imageBaseUrl;

    private final WebClient webClient;

    public HomeCumCryptocoinService(WebClient webClient) {
        this.webClient = webClient;
    }

    private final AtomicReference<List<BinanceTickerDto>> topCoinsCache = new AtomicReference<>(
            Collections.emptyList());

    private final AtomicReference<List<CryptoDto>> allCryptoCache = new AtomicReference<>(
            Collections.emptyList());

    private static final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<Double> usdToInrRateCache = new AtomicReference<>(92.5);

    @EventListener(ApplicationReadyEvent.class)
    public void initCryptoCache() {
        fetchAndCacheAllCryptoData().subscribe(
                null,
                err -> log.error("Failed to initialize crypto cache on startup: {}", err.getMessage()));
    }

    @Scheduled(fixedRate = 600000)
    public void scheduledCryptoFetch() {
        fetchAndCacheAllCryptoData().subscribe(
                null,
                err -> log.error("Failed to fetch crypto cache on schedule: {}", err.getMessage()));
    }

    @CircuitBreaker(name = "fetchAndCacheAllCryptoData", fallbackMethod = "fetchAndCacheAllCryptoDataFallback")
    @Retry(name = "fetchAndCacheAllCryptoData")
    public Mono<Void> fetchAndCacheAllCryptoData() {
        log.info("Fetching top 250 crypto data from CoinGecko...");
        String url = String.format(
                "%s/api/v3/coins/markets?vs_currency=inr&order=market_cap_desc&per_page=250&page=1",
                coingeckoBaseUrl);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(CryptoDto.class)
                .collectList()
                .timeout(Duration.ofSeconds(10))
                .doOnNext(list -> {
                    if (list != null && !list.isEmpty()) {
                        allCryptoCache.set(list);
                        log.info("Successfully cached {} coins", list.size());
                    }
                })
                .then();
    }

    public Mono<Void> fetchAndCacheAllCryptoDataFallback(Throwable e) {
        log.error("CoinGecko unavailable: {}", e.getMessage());
        // keep old cache
        return Mono.empty();
    }

    public Mono<List<CryptoDto>> getAllCrptoData(int page, int perPage) {
        List<CryptoDto> allCoins = allCryptoCache.get();
        int start = (page - 1) * perPage;
        if (start >= allCoins.size()) {
            return Mono.just(Collections.emptyList());
        }
        int end = Math.min(start + perPage, allCoins.size());
        return Mono.just(allCoins.subList(start, end));
    }

    public Mono<List<CryptoDto>> searchCrypto(String query) {
        String lowerQuery = query.toLowerCase();
        List<CryptoDto> filtered = allCryptoCache.get().stream()
                .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(lowerQuery)) ||
                        (c.getSymbol() != null && c.getSymbol().toLowerCase().contains(lowerQuery)))
                .toList();
        return Mono.just(filtered);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initExchangeRateCache() {
        fetchAndCacheExchangeRate().subscribe(
                null,
                err -> log.error("Failed to initialize exchange rate cache on startup: {}", err.getMessage()));
    }

    @Scheduled(fixedRate = 120000) // 2 minutes
    public void scheduledExchangeRateFetch() {
        fetchAndCacheExchangeRate().subscribe(
                null,
                err -> log.error("Failed to fetch exchange rate cache on schedule: {}", err.getMessage()));
    }

    @CircuitBreaker(name = "fetchAndCacheExchangeRate", fallbackMethod = "fetchAndCacheExchangeRateFallback")
    @Retry(name = "fetchAndCacheExchangeRate")
    public Mono<Void> fetchAndCacheExchangeRate() {
        log.info("Fetching USD to INR exchange rate...");
        return webClient.get()
                .uri(exchangeRateBaseUrl + "/v2/rate/USD/INR")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode json = mapper.readTree(response);
                        return json.get("rate").asDouble();
                    } catch (Exception e) {
                        throw new ExternalServiceException("Failed to parse exchange rate", e);
                    }
                })
                .timeout(Duration.ofSeconds(10))
                .doOnNext(rate -> {
                    if (rate != null) {
                        usdToInrRateCache.set(rate);
                        log.info("Successfully cached USD INR rate: {}", rate);
                    }
                })
                .then();
    }

    public Mono<Void> fetchAndCacheExchangeRateFallback(Throwable e) {
        log.error("Exchange rate service unavailable: {}", e.getMessage());
        return Mono.empty();
    }

    public Mono<Double> getUsdToInrRate() {
        Double rate = usdToInrRateCache.get();
        return Mono.just(rate);
    }

    @CircuitBreaker(name = "getHistoricalData", fallbackMethod = "getHistoricalDataFallback")
    @Retry(name = "getHistoricalData")
    @Cacheable(value = "historical-data", key = "'historical-v3-' + #symbol + '-' + #interval + '-' + #limit", cacheManager = "asyncCacheManager")
    public Mono<Map<String, Object>> getHistoricalData(String symbol, String interval, int limit) {
        return webClient.get()
                .uri(binanceBaseUrl + "/api/v3/klines?symbol={symbol}&interval={interval}&limit={limit}",
                        symbol.toUpperCase(), interval, limit)
                .retrieve()
                .bodyToMono(Object.class)
                .map(data -> {
                    String istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    Map<String, Object> response = new HashMap<>();
                    response.put("tickerData", data);
                    response.put("timestamp", istTime);
                    return response;
                })
                .timeout(Duration.ofSeconds(15))
                .cache(); // IMPORTANT: Converts the cold Mono into a hot one for actual reactive caching
    }

    public Mono<Map<String, Object>> getHistoricalDataFallback(String symbol, String interval, int limit,
            Throwable error) {
        log.error("Historical Data Error: {}", error.getMessage());
        String istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("tickerData", null);
        errorMap.put("timestamp", istTime);
        return Mono.just(errorMap);
    }

    @EventListener(ApplicationReadyEvent.class) // Runs immediately when the app starts so the cache is never empty
    public void initTopCoinsCache() {
        fetchAndCacheTopCoins().subscribe(
                null,
                err -> log.error("Failed to initialize top coins cache on startup: {}", err.getMessage()));
    }

    @Scheduled(fixedRate = 3600000) // Runs every 1 hour (3,600,000 milliseconds)
    public void scheduledTopCoinsFetch() {
        fetchAndCacheTopCoins().subscribe(
                null,
                err -> log.error("Failed to fetch top coins cache on schedule: {}", err.getMessage()));
    }

    @CircuitBreaker(name = "fetchAndCacheTopCoins", fallbackMethod = "fetchAndCacheTopCoinsFallback")
    @Retry(name = "fetchAndCacheTopCoins")
    public Mono<Void> fetchAndCacheTopCoins() {
        log.info("Fetching crypto markets to show on home page using postConstruct");
        return webClient.get()
                .uri(binanceBaseUrl + "/api/v3/ticker/24hr")
                .retrieve()
                .bodyToFlux(BinanceTickerDto.class)
                .filter(ticker -> ticker.getSymbol() != null && ticker.getSymbol().endsWith("USDT"))
                .sort(Comparator.comparingDouble(BinanceTickerDto::getQuoteVolume).reversed())
                .take(12)
                .map(ticker -> {
                    String baseSymbol = ticker.getSymbol().replace("USDT", "");
                    ticker.setName(baseSymbol);
                    String formattedImageUri = imageBaseUrl + baseSymbol.toLowerCase()
                            + "?token=" + imageToken;
                    ticker.setImage(formattedImageUri);
                    return ticker;
                })
                .collectList()
                .timeout(Duration.ofSeconds(15))
                .doOnNext(top12 -> {
                    topCoinsCache.set(top12);
                    log.info("Successfully updated homepage cache using pure Binance + CDN routing");
                })
                .then();
    }

    public Mono<Void> fetchAndCacheTopCoinsFallback(Throwable error) {
        log.error("Failed to fetch Top Coins data: {}", error.getMessage());
        return Mono.empty();
    }

    public List<BinanceTickerDto> getBinanceTopCoins() {
        return topCoinsCache.get();
    }
}

package com.project.cryptx.service;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
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
import com.project.cryptx.config.SingletonLogger;
import com.project.cryptx.dto.BinanceTickerDto;
import com.project.cryptx.dto.CryptoDto;
import com.project.cryptx.dto.SearchResponseDto;
import com.project.cryptx.exception.ExternalServiceException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class HomeCumCryptocoinService {

    private static final SingletonLogger log = SingletonLogger.log();

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

    @PostConstruct
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void fetchAndCacheAllCryptoData() {
        log.info("Scheduled task: Fetching top 250 crypto data from CoinGecko...");
        String url = String.format(
                "%s/api/v3/coins/markets?vs_currency=inr&order=market_cap_desc&per_page=250&page=1",
                coingeckoBaseUrl);
        webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(CryptoDto.class)
                .collectList()
                .timeout(Duration.ofSeconds(10))
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .doOnSuccess(list -> {
                    if (list != null && !list.isEmpty()) {
                        allCryptoCache.set(list);
                        log.info("Successfully fetched and cached {} coins", list.size());
                    }
                })
                .doOnError(error -> log.error("CoinGecko API Error for all crypto: {}", error.getMessage(), error))
                .subscribe();
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

    // @Cacheable(value = "exchange-rate", key = "'usd-inr'", cacheManager =
    // "asyncCacheManager")
    public Mono<Double> getUsdToInrRate() {
        return webClient.get()
                .uri(exchangeRateBaseUrl + "/v2/rate/USD/INR")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode json = mapper.readTree(response);
                        return json.get("rate").asDouble();
                    } catch (Exception e) {
                        throw new ExternalServiceException("Failed to read exchange rate payload", e);
                    }
                })
                .onErrorResume(ex -> {
                    log.error("Error fetching USD to INR exchange rate: {}", ex.getMessage(), ex);
                    return Mono.just(95.00);
                });
    }

    @Cacheable(value = "historical-data", key = "'historical-v3-' + #symbol + '-' + #interval + '-' + #limit", cacheManager = "asyncCacheManager")
    public Mono<Map<String, Object>> getHistoricalData(String symbol, String interval, int limit) {
        String istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return webClient.get()
                .uri(binanceBaseUrl + "/api/v3/klines?symbol={symbol}&interval={interval}&limit={limit}",
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
                    log.error("Historical Data Error: {}", error.getMessage(), error);
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
        log.info("Fetching crypto markets to show on home page using postConstruct");
        webClient.get()
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
                .subscribe(
                        top12 -> {
                            topCoinsCache.set(top12);
                            log.info("Successfully updated homepage cache using pure Binance + CDN routing");
                        },
                        error -> log.error("Failed to fetch Top Coins data: {}", error.getMessage(), error));
    }

    public List<BinanceTickerDto> getBinanceTopCoins() {
        return topCoinsCache.get();
    }
}

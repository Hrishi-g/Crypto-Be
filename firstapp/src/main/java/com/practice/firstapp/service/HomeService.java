package com.practice.firstapp.service;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.practice.firstapp.dto.CryptoDto;
import com.practice.firstapp.dto.SearchResponseDto;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class HomeService {

    private final WebClient webClient;

    public HomeService(WebClient webClient) {
        this.webClient = webClient;
    }

    @Cacheable(value = "crypto-data", key = "'all-crypto-page-' + #page + '-size-' + #perPage", cacheManager = "asyncCacheManager")
    public Mono<List<CryptoDto>> getAllCrptoData(int page, int perPage) {
        System.out.println("Fetching real crypto data from CoinGecko (Page: " + page + ", Size: " + perPage + ")...");
        String url = String.format("/coins/markets?vs_currency=inr&order=market_cap_desc&per_page=%d&page=%d", perPage,
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
        String searchUrl = "/search?query=" + query;

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

    @Cacheable(value = "exchange-rate", key = "'usd-inr'", cacheManager = "asyncCacheManager")
    public Mono<Double> getUsdToInrRate() {
        // Using Tether (USDT) as it mirrors the Binance pairs exactly
        return webClient.get()
                .uri("/simple/price?ids=tether&vs_currencies=inr")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .map(json -> json.path("tether").path("inr").asDouble())
                .onErrorResume(ex -> Mono.just(92.50));
    }

    @Cacheable(value = "historical-data", key = "'historical-v2-' + #coinId + '-' + #days", cacheManager = "asyncCacheManager")
    public Mono<Map<String, Object>> getHistoricalData(String coinId, int days) {
        String url = String.format("/coins/%s/market_chart?vs_currency=inr&days=%d", coinId, days);
        // Captured snapshot time in IST
        String istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return webClient.get()
                .uri(url)
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
                    System.err.println("Historical Data Error: " + error.getMessage());
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("tickerData", null);
                    errorMap.put("timestamp", istTime);
                    return Mono.just(errorMap);
                });
    }
}

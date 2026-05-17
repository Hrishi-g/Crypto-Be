package com.practice.firstapp.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.practice.firstapp.dto.CryptoDto;
import com.practice.firstapp.dto.BinanceTickerDto;
import com.practice.firstapp.service.HomeCumCryptocoinService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/home/crypto")
public class HomeCumCryptocoinController {

    private final HomeCumCryptocoinService homeService;

    public HomeCumCryptocoinController(HomeCumCryptocoinService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/top-crypto")
    public ResponseEntity<List<BinanceTickerDto>> getBinanceTopCoins() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "http://localhost:5173")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .body(homeService.getBinanceTopCoins());
    }

    @GetMapping("/all-crypto")
    public Mono<ResponseEntity<List<CryptoDto>>> getAllCrptoData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage,
            @RequestParam(required = false) String query) {

        Mono<List<CryptoDto>> dataMono;

        if (query != null && !query.isEmpty()) {
            dataMono = homeService.searchCrypto(query);
        } else {
            dataMono = homeService.getAllCrptoData(page, perPage);
        }

        return dataMono.map(data -> ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "http://localhost:5173")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .body(data));
    }

    @GetMapping("/exchange-rate")
    public Mono<ResponseEntity<Double>> getExchangeRate() {
        return homeService.getUsdToInrRate()
                .map(rate -> ResponseEntity.ok()
                        .header("Access-Control-Allow-Origin", "http://localhost:5173")
                        .header("Access-Control-Allow-Credentials", "true")
                        .body(rate));
    }

    @GetMapping("/historical-data")
    public Mono<ResponseEntity<Object>> getHistoricalData(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "100") int limit) {
        return homeService.getHistoricalData(symbol, interval, limit)
                .map(data -> ResponseEntity.ok()
                        .header("Access-Control-Allow-Origin", "http://localhost:5173")
                        .header("Access-Control-Allow-Credentials", "true")
                        .body(data));
    }
}

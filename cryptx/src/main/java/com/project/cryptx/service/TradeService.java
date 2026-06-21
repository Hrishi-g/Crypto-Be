package com.project.cryptx.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import com.project.cryptx.dto.IdompotencyDto;
import com.project.cryptx.dto.TradeRequestDto;
import com.project.cryptx.dto.UserProfileDto;
import com.project.cryptx.exception.ExternalServiceException;
import com.project.cryptx.exception.InsufficientBalanceException;
import com.project.cryptx.repo.IdompotencyRepo;
import com.project.cryptx.repo.TransactionRepo;
import com.project.cryptx.repo.WalletRepo;
import com.project.cryptx.vo.Idompotency;
import com.project.cryptx.vo.Transaction;
import com.project.cryptx.vo.Wallet;
import com.project.cryptx.vo.enums.TransactionStatus;
import com.project.cryptx.vo.enums.TransactionType;

import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Slf4j
@Service
public class TradeService {

    @Value("${crypto-base-url.binance}")
    private String binanceBaseUrl;
    private final WalletRepo walletRepo;
    private final TransactionRepo transactionRepo;
    private final PortFolioService profileService;
    private final HomeCumCryptocoinService homeService;
    private final IdompotencyRepo idompotencyRepo;
    private final UserService userService;
    private final WebClient webClient;
    private final TradeService self;

    public TradeService(WalletRepo walletRepo, TransactionRepo transactionRepo, PortFolioService profileService,
            HomeCumCryptocoinService homeService, UserService userService, WebClient webClient,
            IdompotencyRepo idompotencyRepo, @Lazy TradeService self) {
        this.walletRepo = walletRepo;
        this.transactionRepo = transactionRepo;
        this.profileService = profileService;
        this.homeService = homeService;
        this.userService = userService;
        this.webClient = webClient;
        this.idompotencyRepo = idompotencyRepo;
        this.self = self;
    }

    @CircuitBreaker(name = "fetchLivePrice", fallbackMethod = "fetchLivePriceFallback")
    @Retry(name = "fetchLivePrice")
    public BigDecimal fetchLivePrice(String asset) {
        log.info("Fetching live price for asset: {}", asset);
        Double inrRate = homeService.getUsdToInrRate().block();
        if (inrRate == null) {
            throw new ExternalServiceException("INR rate unavailable");
        }
        String symbol = asset.toUpperCase();
        if (!symbol.endsWith("USDT")) {
            symbol += "USDT";
        }
        Map<?, ?> response = webClient.get().uri(binanceBaseUrl + "/api/v3/ticker/price?symbol=" + symbol)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (response == null || !response.containsKey("price")) {
            throw new ExternalServiceException("Price unavailable");
        }
        BigDecimal usdPrice = new BigDecimal(String.valueOf(response.get("price")));
        BigDecimal inrPrice = usdPrice.multiply(BigDecimal.valueOf(inrRate));
        log.info("Live price for {}: USD={}, INR={}", asset, usdPrice, inrPrice);
        return inrPrice;
    }

    public BigDecimal fetchLivePriceFallback(String asset, Throwable t) {
        log.error("fetchLivePrice fallback triggered for asset {}. Error: {}", asset, t.getMessage());
        throw new ExternalServiceException("Live price service is currently unavailable. Please try again later.", t);
    }

    @Bulkhead(name = "tradeService", fallbackMethod = "tradeFallback")
    public ResponseEntity<?> trade(String idempotencyKey, TradeRequestDto request) {
        log.info("Initiating trade request: User={}, Asset={}, Amount={}, Type={}", 
                request.getUserId(), request.getAsset(), request.getAmount(), request.getType());
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty() ||
                "null".equalsIgnoreCase(idempotencyKey) || "undefined".equalsIgnoreCase(idempotencyKey)) {
            throw new IllegalArgumentException("Invalid Idempotency-Key header");
        }

        Optional<IdompotencyDto> existing = idompotencyRepo.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            Transaction txn = transactionRepo.findById(existing.get().getTransactionId()).orElseThrow();
            return ResponseEntity.ok(
                    txn.getType() == TransactionType.BUY
                            ? "Bought " + txn.getQuantity() + " of " + txn.getAsset() + " successfully"
                            : "Sold " + txn.getQuantity() + " of " + txn.getAsset() + " successfully");
        }

        UserProfileDto userProfile = userService.getUser(request.getUserId());
        if (userProfile.getFirstName() == null ||
                userProfile.getFirstName().trim().isEmpty() ||
                userProfile.getLastName() == null ||
                userProfile.getLastName().trim().isEmpty() ||
                userProfile.getDob() == null || userProfile.getDob().trim().isEmpty()) {
            throw new IllegalArgumentException("Please complete your profile details to perform trades.");
        }

        BigDecimal liveInrPrice = fetchLivePrice(request.getAsset());
        BigDecimal quantity = request.getAmount().divide(liveInrPrice, 10, RoundingMode.HALF_UP);

        request.setPrice(liveInrPrice);
        request.setQuantity(quantity);

        // Call self via lazy proxy to trigger @Transactional and @CacheEvict
        return self.executeTrade(
                idempotencyKey,
                request);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user", key = "#request.userId", cacheManager = "cacheManager"),
            @CacheEvict(value = "portfolio", key = "#request.userId", cacheManager = "cacheManager")
    })
    public ResponseEntity<?> executeTrade(String idempotencyKey, TradeRequestDto request) {
        log.info("Executing trade transaction: User={}, Asset={}, Quantity={}, Price={}", 
                request.getUserId(), request.getAsset(), request.getQuantity(), request.getPrice());
        Wallet wallet = walletRepo.findByUserId(request.getUserId()).orElseThrow();
        BigDecimal amount = request.getAmount();
        Transaction txn = new Transaction();
        if (request.getType() == TransactionType.BUY) {
            if (wallet.getBalance().compareTo(amount) < 0) {
                log.warn("Insufficient balance for user {}: required={}, current={}", 
                        request.getUserId(), amount, wallet.getBalance());
                throw new InsufficientBalanceException("Insufficient balance");
            }
            txn.setDescription(
                    "Buy " + request.getQuantity() + " of " + request.getAsset() + " at " + request.getPrice());
            wallet.setBalance(wallet.getBalance().subtract(amount));
            profileService.updatePortfolioBuy(request);
        } else {
            profileService.validateSell(request);
            txn.setDescription(
                    "Sell " + request.getQuantity() + " of " + request.getAsset() + " at " + request.getPrice());
            wallet.setBalance(wallet.getBalance().add(amount));
            profileService.updatePortfolioSell(request);
        }
        walletRepo.save(wallet);
        txn.setWallet(wallet);
        txn.setType(request.getType());
        txn.setAsset(request.getAsset());
        txn.setQuantity(request.getQuantity());
        txn.setPrice(request.getPrice());
        txn.setAmount(amount);
        txn.setBalanceAfter(wallet.getBalance());
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setReferenceId(UUID.randomUUID().toString());
        Transaction saved = transactionRepo.save(txn);

        Idompotency key = new Idompotency();
        key.setIdempotencyKey(idempotencyKey);
        key.setTransactionId(saved.getId());
        idompotencyRepo.save(key);

        log.info("Trade transaction successfully executed. Transaction ID: {}, User ID: {}", saved.getId(), request.getUserId());

        if (request.getType() == TransactionType.BUY) {
            return ResponseEntity.ok("Bought " + request.getQuantity() + " of " + request.getAsset() + " successfully");
        } else {
            return ResponseEntity.ok("Sold " + request.getQuantity() + " of " + request.getAsset() + " successfully");
        }
    }

    public ResponseEntity<?> tradeFallback(String idempotencyKey, TradeRequestDto request, Throwable t) {
        log.error("tradeFallback triggered for user {}. Reason: {}", request.getUserId(), t.getMessage());
        return ResponseEntity.status(503).body(Map.of(
                "message", "Trading service is temporarily busy. Please try again in a few moments."
        ));
    }
}

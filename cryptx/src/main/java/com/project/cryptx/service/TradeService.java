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

    private BigDecimal fetchLivePrice(String asset) {
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
        return usdPrice.multiply(BigDecimal.valueOf(inrRate));
    }

    public ResponseEntity<?> trade(String idempotencyKey, TradeRequestDto request) {
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
        Wallet wallet = walletRepo.findByUserId(request.getUserId()).orElseThrow();
        BigDecimal amount = request.getAmount();
        Transaction txn = new Transaction();
        if (request.getType() == TransactionType.BUY) {
            if (wallet.getBalance().compareTo(amount) < 0) {
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

        if (request.getType() == TransactionType.BUY) {
            return ResponseEntity.ok("Bought " + request.getQuantity() + " of " + request.getAsset() + " successfully");
        } else {
            return ResponseEntity.ok("Sold " + request.getQuantity() + " of " + request.getAsset() + " successfully");
        }
    }
}

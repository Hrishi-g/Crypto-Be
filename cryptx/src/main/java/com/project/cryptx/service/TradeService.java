package com.project.cryptx.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.project.cryptx.config.SingletonLogger;
import com.project.cryptx.dto.TradeRequestDto;
import com.project.cryptx.dto.UserProfileDto;
import com.project.cryptx.exception.ExternalServiceException;
import com.project.cryptx.exception.InsufficientBalanceException;
import com.project.cryptx.exception.InvalidAmountException;
import com.project.cryptx.exception.ResourceNotFoundException;
import com.project.cryptx.repo.TransactionRepo;
import com.project.cryptx.repo.WalletRepo;
import com.project.cryptx.vo.Transaction;
import com.project.cryptx.vo.Wallet;
import com.project.cryptx.vo.enums.TransactionStatus;
import com.project.cryptx.vo.enums.TransactionType;

@Service
public class TradeService {

    @Value("${crypto-base-url.binance}")
    private String binanceBaseUrl;

    private static final SingletonLogger log = SingletonLogger.log();

    private WalletRepo walletRepo;
    private TransactionRepo transactionRepo;
    private PortFolioService profileService;
    private HomeCumCryptocoinService homeService;
    private UserService userService;

    public TradeService(WalletRepo walletRepo, TransactionRepo transactionRepo, PortFolioService profileService,
            HomeCumCryptocoinService homeService, UserService userService) {
        this.walletRepo = walletRepo;
        this.transactionRepo = transactionRepo;
        this.profileService = profileService;
        this.homeService = homeService;
        this.userService = userService;
    }

    @Transactional
    @CacheEvict(value = "user", key = "#request.userId", cacheManager = "cacheManager")
    public ResponseEntity<?> trade(TradeRequestDto request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Invalid purchase amount");
        }

        UserProfileDto userProfile = userService.getUser(request.getUserId());
        if (userProfile.getFirstName() == null || userProfile.getFirstName().trim().isEmpty() ||
            userProfile.getLastName() == null || userProfile.getLastName().trim().isEmpty() ||
            userProfile.getDob() == null || userProfile.getDob().trim().isEmpty()) {
            throw new IllegalArgumentException("Please complete your profile details to perform trades.");
        }

        // --- BACKEND SECURITY: FETCH LIVE PRICE DYNAMICALLY ---
        try {
            Double inrRate = homeService.getUsdToInrRate().block();
            if (inrRate == null)
                throw new ExternalServiceException("Could not fetch live USD to INR rate.");

            RestTemplate restTemplate = new RestTemplate();
            String symbol = request.getAsset().toUpperCase();
            if (!symbol.endsWith("USDT")) {
                symbol = symbol + "USDT";
            }

            String binanceUrl = binanceBaseUrl + "/api/v3/ticker/price?symbol=" + symbol;
            Map<String, String> bResponse = restTemplate.getForObject(binanceUrl, Map.class);

            if (bResponse != null && bResponse.containsKey("price")) {
                BigDecimal liveUsdPrice = new BigDecimal(bResponse.get("price"));
                BigDecimal liveInrPrice = liveUsdPrice.multiply(BigDecimal.valueOf(inrRate));

                // SECURELY recalculate fraction using live backend price to 10 decimal
                // precision!
                BigDecimal secureQuantity = request.getAmount().divide(liveInrPrice, 10,
                        java.math.RoundingMode.HALF_UP);

                // Override DTO with authentic data
                request.setPrice(liveInrPrice);
                request.setQuantity(secureQuantity);
            } else {
                throw new ExternalServiceException("Could not verify live asset price securely.");
            }
        } catch (Exception e) {
            log.error("Backend Security Error fetching live prices: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to fetch live prices. Please try again later.", e);
        }

        Wallet wallet = walletRepo.findByUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        BigDecimal totalAmount = request.getAmount();
        Transaction txn = new Transaction();
        if (request.getType() == TransactionType.BUY) {
            // 🔴 Check balance
            if (wallet.getBalance().compareTo(totalAmount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance");
            }
            // 🔴 Deduct money
            txn.setDescription(
                    "Buy " + request.getQuantity() + " of " + request.getAsset() + " at " + request.getPrice());
            wallet.setBalance(wallet.getBalance().subtract(totalAmount));
            // 🟢 Update portfolio
            profileService.updatePortfolioBuy(request);
        } else { // SELL
            // 🔴 Check portfolio
            profileService.validateSell(request);
            // 🟢 Add money
            txn.setDescription(
                    "Sell " + request.getQuantity() + " of " + request.getAsset() + " at " + request.getPrice());
            wallet.setBalance(wallet.getBalance().add(totalAmount));
            // 🔴 Update portfolio
            profileService.updatePortfolioSell(request);
        }
        walletRepo.save(wallet);
        // ✅ Save transaction

        txn.setWallet(wallet);
        txn.setType(request.getType());
        txn.setAsset(request.getAsset());
        txn.setQuantity(request.getQuantity());
        txn.setPrice(request.getPrice());
        txn.setAmount(totalAmount);
        txn.setBalanceAfter(wallet.getBalance());
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setReferenceId(UUID.randomUUID().toString());
        transactionRepo.save(txn);

        if (request.getType() == TransactionType.BUY) {
            return ResponseEntity.ok("Bought " + request.getQuantity() + " of " + request.getAsset() + " successfully");
        } else {
            return ResponseEntity.ok("Sold " + request.getQuantity() + " of " + request.getAsset() + " successfully");
        }
    }
}

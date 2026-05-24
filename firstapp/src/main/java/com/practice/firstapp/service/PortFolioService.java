package com.practice.firstapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.practice.firstapp.config.SingletonLogger;
import com.practice.firstapp.exception.ResourceNotFoundException;
import com.practice.firstapp.exception.InsufficientBalanceException;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.practice.firstapp.dto.PortfolioDto;
import com.practice.firstapp.dto.TradeRequestDto;
import com.practice.firstapp.repo.PortfolioRepo;
import com.practice.firstapp.repo.UserRepo;
import com.practice.firstapp.vo.Portfolio;

@Service
public class PortFolioService {

    private static final SingletonLogger log = SingletonLogger.log();

    private PortfolioRepo portfolioRepo;
    private UserRepo userRepo;

    public PortFolioService(PortfolioRepo portfolioRepo, UserRepo userRepo) {
        this.portfolioRepo = portfolioRepo;
        this.userRepo = userRepo;
    }

    @Cacheable(key = "#userId", cacheNames = "portfolio", cacheManager = "cacheManager")
    public List<PortfolioDto> getPortfolio(Long userId) {
        log.debug("PortFolioService.getPortfolio called for ID: {} (Cache MISS)", userId);
        return portfolioRepo.findByUserId(userId);
    }

    @CachePut(key = "#request.userId", cacheNames = "portfolio", cacheManager = "cacheManager")
    @Transactional
    public List<PortfolioDto> updatePortfolioBuy(TradeRequestDto request) {
        Portfolio portfolio = portfolioRepo
                .findByUserIdAndAsset(request.getUserId(), request.getAsset())
                .orElse(null);
        BigDecimal qty = request.getQuantity();
        BigDecimal price = request.getPrice();
        if (portfolio == null) {
            // 🟢 First time buying this asset
            portfolio = new Portfolio();
            portfolio.setUser(userRepo.findById(request.getUserId()).get());
            portfolio.setAsset(request.getAsset());
            portfolio.setQuantity(qty);
            portfolio.setAvgBuyPrice(price);
        } else {
            // 🔥 Update avg price
            BigDecimal oldQty = portfolio.getQuantity();
            BigDecimal oldAvg = portfolio.getAvgBuyPrice();
            BigDecimal totalCost = oldQty.multiply(oldAvg)
                    .add(qty.multiply(price));
            BigDecimal newQty = oldQty.add(qty);
            BigDecimal newAvg = totalCost.divide(newQty, 2, RoundingMode.HALF_UP);
            portfolio.setQuantity(newQty);
            portfolio.setAvgBuyPrice(newAvg);
        }

        portfolioRepo.save(portfolio);
        return portfolioRepo.findByUserId(request.getUserId());
    }

    @CachePut(key = "#request.userId", cacheNames = "portfolio", cacheManager = "cacheManager")
    @Transactional
    public List<PortfolioDto> updatePortfolioSell(TradeRequestDto request) {
        Portfolio portfolio = portfolioRepo
                .findByUserIdAndAsset(request.getUserId(), request.getAsset())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        BigDecimal newQty = portfolio.getQuantity().subtract(request.getQuantity());
        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            // optional: delete row if fully sold
            portfolioRepo.delete(portfolio);
        } else {
            portfolio.setQuantity(newQty);
            // ❗ avgBuyPrice stays SAME
            portfolioRepo.save(portfolio);
        }
        return portfolioRepo.findByUserId(request.getUserId());
    }

    public void validateSell(TradeRequestDto request) {
        Portfolio portfolio = portfolioRepo
                .findByUserIdAndAsset(request.getUserId(), request.getAsset())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        if (portfolio.getQuantity().compareTo(request.getQuantity()) < 0) {
            throw new InsufficientBalanceException("Not enough asset to sell");
        }
    }

}

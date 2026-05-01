package com.practice.firstapp.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.practice.firstapp.dto.TransactionHistory;
import com.practice.firstapp.dto.WalletRequestDto;
import com.practice.firstapp.repo.TransactionRepo;
import com.practice.firstapp.repo.WalletRepo;
import com.practice.firstapp.vo.Transaction;
import com.practice.firstapp.vo.Wallet;
import com.practice.firstapp.vo.enums.TransactionStatus;
import com.practice.firstapp.vo.enums.TransactionType;
import jakarta.transaction.Transactional;

@Service
public class WalletService {

    private WalletRepo walletRepo;
    private TransactionRepo transactionRepo;

    public WalletService(WalletRepo walletRepo, TransactionRepo transactionRepo) {
        this.walletRepo = walletRepo;
        this.transactionRepo = transactionRepo;
    }

    @Transactional
    @CacheEvict(value = "user", key = "#userId", cacheManager = "cacheManager")
    public void updateWallet(Long userId, WalletRequestDto request) {

        BigDecimal amount = request.getAmount();
        TransactionType type = request.getType();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        Wallet wallet = walletRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BigDecimal newBalance;

        // 🔴 DEBIT
        if (type == TransactionType.DEBIT) {

            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            newBalance = wallet.getBalance().subtract(amount);

        } else { // 🟢 CREDIT

            newBalance = wallet.getBalance().add(amount);
        }

        // ✅ Update wallet
        wallet.setBalance(newBalance);
        walletRepo.save(wallet); // @Version handles concurrency

        // ✅ Save transaction
        Transaction txn = new Transaction();
        txn.setWallet(wallet);
        txn.setAmount(amount);
        txn.setType(type);
        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setBalanceAfter(newBalance);
        txn.setReferenceId(UUID.randomUUID().toString());
        txn.setDescription(type + " of amount " + amount);

        transactionRepo.save(txn);
    }

    public Slice<TransactionHistory> walletHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepo.findByWalletUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
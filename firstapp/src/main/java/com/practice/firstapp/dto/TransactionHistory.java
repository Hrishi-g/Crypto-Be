package com.practice.firstapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.practice.firstapp.vo.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.practice.firstapp.vo.enums.TransactionStatus;

public class TransactionHistory {
    private TransactionType type;
    private String asset;
    private LocalDateTime createdAt;

    private BigDecimal quantity;
    private BigDecimal price;

    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private TransactionStatus status;

    public TransactionHistory(TransactionType type, String asset, LocalDateTime createdAt, BigDecimal quantity,
            BigDecimal price,
            BigDecimal amount, BigDecimal balanceAfter, TransactionStatus status) {
        this.type = type;
        this.asset = asset;
        this.createdAt = createdAt;
        this.quantity = quantity;
        this.price = price;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.status = status;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @JsonIgnore
    public BigDecimal getQuantity() {
        return quantity;
    }

    @JsonProperty("quantity")
    public String getQuantityPlain() {
        return quantity != null ? quantity.toPlainString() : null;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @JsonIgnore
    public BigDecimal getPrice() {
        return price;
    }

    @JsonProperty("price")
    public String getPricePlain() {
        return price != null ? price.toPlainString() : null;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @JsonIgnore
    public BigDecimal getAmount() {
        return amount;
    }

    @JsonProperty("amount")
    public String getAmountPlain() {
        return amount != null ? amount.toPlainString() : null;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @JsonIgnore
    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    @JsonProperty("balanceAfter")
    public String getBalanceAfterPlain() {
        return balanceAfter != null ? balanceAfter.toPlainString() : null;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}

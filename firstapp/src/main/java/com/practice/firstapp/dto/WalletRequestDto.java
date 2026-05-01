package com.practice.firstapp.dto;

import java.math.BigDecimal;

import com.practice.firstapp.vo.enums.TransactionType;

public class WalletRequestDto {
    private BigDecimal amount;
    private TransactionType type; // CREDIT or DEBIT

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}
package com.project.cryptx.dto;

public class IdompotencyDto {
    String idempotencyKey;
    Long transactionId;

    public IdompotencyDto(String idempotencyKey, Long transactionId) {
        this.idempotencyKey = idempotencyKey;
        this.transactionId = transactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

}

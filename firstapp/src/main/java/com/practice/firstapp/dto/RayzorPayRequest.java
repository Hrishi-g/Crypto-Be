package com.practice.firstapp.dto;

public class RayzorPayRequest {
    private double amount;
    private String receiptId;

    public RayzorPayRequest() {
    }

    public RayzorPayRequest(double amount, String receiptId) {
        this.amount = amount;
        this.receiptId = receiptId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

}

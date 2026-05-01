package com.practice.firstapp.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PortfolioDto {

    private String asset;

    private BigDecimal quantity;

    private BigDecimal avgBuyPrice;

    public PortfolioDto(String asset, BigDecimal quantity, BigDecimal avgBuyPrice) {
        this.asset = asset;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
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
    public BigDecimal getAvgBuyPrice() {
        return avgBuyPrice;
    }

    @JsonProperty("avgBuyPrice")
    public String getAvgBuyPricePlain() {
        return avgBuyPrice != null ? avgBuyPrice.toPlainString() : null;
    }

    public void setAvgBuyPrice(BigDecimal avgBuyPrice) {
        this.avgBuyPrice = avgBuyPrice;
    }

    @Override
    public String toString() {
        return "PortfolioDto [asset=" + asset + ", quantity=" + quantity + ", avgBuyPrice="
                + avgBuyPrice + "]";
    }

}

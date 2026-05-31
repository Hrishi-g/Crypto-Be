package com.project.cryptx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SearchResponseDto {
    private List<SearchCoinDto> coins;

    public List<SearchCoinDto> getCoins() {
        return coins;
    }

    public void setCoins(List<SearchCoinDto> coins) {
        this.coins = coins;
    }

    public static class SearchCoinDto {
        private String id;
        private String name;
        private String symbol;

        @JsonProperty("market_cap_rank")
        private Integer marketCapRank;

        @JsonProperty("large")
        private String image;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public Integer getMarketCapRank() {
            return marketCapRank;
        }

        public void setMarketCapRank(Integer marketCapRank) {
            this.marketCapRank = marketCapRank;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }
}

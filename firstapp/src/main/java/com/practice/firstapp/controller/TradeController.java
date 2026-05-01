package com.practice.firstapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.firstapp.dto.AuthDto;
import com.practice.firstapp.dto.TradeRequestDto;
import com.practice.firstapp.service.TradeService;

@RestController
@RequestMapping("/trade")
public class TradeController {

    private TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping("/buy-sell")
    public ResponseEntity<?> trade(@AuthenticationPrincipal AuthDto user, @RequestBody TradeRequestDto request) {
        request.setUserId(user.getId());
        return tradeService.trade(request);

    }

}

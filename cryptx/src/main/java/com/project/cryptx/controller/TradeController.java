package com.project.cryptx.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.cryptx.dto.AuthDto;
import com.project.cryptx.dto.TradeRequestDto;
import com.project.cryptx.service.TradeService;

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

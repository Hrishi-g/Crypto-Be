package com.project.cryptx.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.cryptx.dto.AuthDto;
import com.project.cryptx.dto.WalletRequestDto;
import com.project.cryptx.service.WalletService;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateWallet(@AuthenticationPrincipal AuthDto user,
            @Validated @RequestBody WalletRequestDto request) {
        Long userId = user.getId();
        walletService.updateWallet(userId, request);

        return ResponseEntity.ok("Wallet updated successfully");
    }

    @GetMapping("/history")
    public ResponseEntity<?> walletHistory(@AuthenticationPrincipal AuthDto user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = user.getId();
        return ResponseEntity.ok(walletService.walletHistory(userId, page, size));
    }

}

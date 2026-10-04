package com.project.cryptx.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.cryptx.dto.AuthDto;
import com.project.cryptx.service.PortFolioService;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private PortFolioService portFolioService;

    public PortfolioController(PortFolioService portFolioService) {
        this.portFolioService = portFolioService;
    }

    @GetMapping("/get")
    public ResponseEntity<?> getPortfolio(@AuthenticationPrincipal AuthDto user) {
        Long userId = user.getId();
        return ResponseEntity.ok(portFolioService.getPortfolio(userId));
    }
}

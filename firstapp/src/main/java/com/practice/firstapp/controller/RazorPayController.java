package com.practice.firstapp.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.firstapp.dto.AuthDto;
import com.practice.firstapp.service.RazorPayService;
import com.practice.firstapp.service.WalletService;
import com.razorpay.RazorpayException;

@RestController
@RequestMapping("/payment")
public class RazorPayController {

    private final RazorPayService razorPayService;
    private final WalletService walletService;

    public RazorPayController(RazorPayService razorPayService, WalletService walletService) {
        this.razorPayService = razorPayService;
        this.walletService = walletService;
    }

    @PostMapping("/razorpay/create-order")
    public ResponseEntity<?> createOrder(@AuthenticationPrincipal AuthDto user, @RequestBody Double amount) {
        try {
            Map<String, Object> result = razorPayService.createOrder(amount);
            String orderId = (String) result.get("orderId");
            
            // Register a PENDING transaction in DB
            walletService.createPendingTransaction(user.getId(), BigDecimal.valueOf(amount), orderId, "RAZORPAY");
            
            return ResponseEntity.ok(result);
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<?> verifyPayment(@AuthenticationPrincipal AuthDto user, @RequestBody Map<String, Object> payload) {
        String orderId = (String) payload.get("razorpay_order_id");
        try {
            String paymentId = (String) payload.get("razorpay_payment_id");
            String signature = (String) payload.get("razorpay_signature");

            boolean isValid = razorPayService.verifyPayment(orderId, paymentId, signature);
            if (isValid) {
                // Securely credit wallet and change transaction status to SUCCESS
                walletService.approveTransaction(user.getId(), orderId);
                return ResponseEntity.ok(Map.of("message", "Payment verified and wallet updated"));
            } else {
                // Change transaction status to FAILED in DB
                walletService.rejectTransaction(orderId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid payment signature"));
            }
        } catch (Exception e) {
            // Ensure status is marked FAILED on exception
            walletService.rejectTransaction(orderId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/razorpay/cancel")
    public ResponseEntity<?> cancelPayment(@RequestBody Map<String, Object> payload) {
        try {
            String orderId = (String) payload.get("orderId");
            walletService.rejectTransaction(orderId);
            return ResponseEntity.ok(Map.of("message", "Transaction marked as FAILED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }
}



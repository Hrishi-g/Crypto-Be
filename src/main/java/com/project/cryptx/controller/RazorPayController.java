package com.project.cryptx.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.cryptx.dto.AuthDto;
import com.project.cryptx.exception.BadRequestException;
import com.project.cryptx.service.RazorPayService;
import com.project.cryptx.service.WalletService;
import com.razorpay.RazorpayException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    public ResponseEntity<?> createOrder(@AuthenticationPrincipal AuthDto user, @RequestBody Double amount)
            throws RazorpayException {
        log.info("Creating Razorpay order for user: {} of amount: {}", user.getId(), amount);
        Map<String, Object> result = razorPayService.createOrder(amount);
        String orderId = (String) result.get("orderId");

        // Register a PENDING transaction in DB
        walletService.createPendingTransaction(user.getId(), BigDecimal.valueOf(amount), orderId, "RAZORPAY");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<?> verifyPayment(@AuthenticationPrincipal AuthDto user,
            @RequestBody Map<String, Object> payload) {
        String orderId = (String) payload.get("razorpay_order_id");
        log.info("Verifying Razorpay payment for order: {}", orderId);
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
                walletService.rejectTransaction(user.getId(), orderId);
                throw new BadRequestException("Invalid payment signature");
            }
        } catch (Exception e) {
            log.error("Payment verification failed for order: {}", orderId, e);
            // Ensure status is marked FAILED on exception
            walletService.rejectTransaction(user.getId(), orderId);
            throw new BadRequestException("Payment verification failed: " + e.getMessage());
        }
    }

    @PostMapping("/razorpay/cancel")
    public ResponseEntity<?> cancelPayment(@AuthenticationPrincipal AuthDto user,
            @RequestBody Map<String, Object> payload) {
        String orderId = (String) payload.get("orderId");
        log.info("Cancelling Razorpay payment for order: {}", orderId);
        try {
            walletService.rejectTransaction(user.getId(), orderId);
            return ResponseEntity.ok(Map.of("message", "Transaction marked as FAILED"));
        } catch (Exception e) {
            log.error("Error cancelling payment for order: {}", orderId, e);
            throw new BadRequestException("Error cancelling payment: " + e.getMessage());
        }
    }
}

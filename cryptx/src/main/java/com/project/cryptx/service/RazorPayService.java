package com.project.cryptx.service;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RazorPayService {

    private final String apiKey;
    private final String keySecret;
    private final RazorpayClient razorpayClient;

    public RazorPayService(
            @Value("${rayzorpay.api-key}") String apiKey,
            @Value("${rayzorpay.key-secret}") String keySecret
    ) throws RazorpayException {
        this.apiKey = apiKey;
        this.keySecret = keySecret;
        this.razorpayClient = new RazorpayClient(apiKey, keySecret);
    }

    @CircuitBreaker(name = "razorPayCreateOrder", fallbackMethod = "createOrderFallback")
    @Retry(name = "razorPayCreateOrder")
    public Map<String, Object> createOrder(double amount) throws RazorpayException {
        JSONObject options = new JSONObject();
        options.put("amount", amount * 100);
        options.put("currency", "INR");
        options.put("receipt", "txn_" + System.currentTimeMillis());

        Order order = razorpayClient.orders.create(options);

        Map<String, Object> response = new HashMap<>();
        response.put("key", apiKey);
        response.put("orderId", order.get("id"));
        response.put("amount", order.get("amount"));
        response.put("currency", order.get("currency"));
        return response;
    }

    public Map<String, Object> createOrderFallback(double amount, Throwable t) throws RazorpayException {
        log.error("Failed to create RazorPay order: {}", t.getMessage());
        throw new RazorpayException("Razorpay payment gateway is currently unavailable. Please try again later.", t);
    }

    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            return com.razorpay.Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            log.error("RazorPay signature verification error: {}", e.getMessage(), e);
            return false;
        }
    }

}

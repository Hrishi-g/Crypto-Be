package com.practice.firstapp.service;

import java.util.HashMap;
import java.util.Map;

import com.practice.firstapp.config.SingletonLogger;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Service
public class RazorPayService {

    private static final SingletonLogger log = SingletonLogger.log();

    @Value("${rayzorpay.api-key}")
    private String apiKey;

    @Value("${rayzorpay.key-secret}")
    private String keySecret;

    public Map<String, Object> createOrder(double amount) throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(apiKey, keySecret);
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


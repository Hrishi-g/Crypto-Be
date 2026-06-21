package com.project.cryptx.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.from}")
    private String mailFrom;

    private JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @RateLimiter(name = "emailResetLimiter", fallbackMethod = "sendResetMailFallback")
    public void sendResetMail(String to, String token) {
        log.info("Sending password reset email to: {}", to);
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailFrom);
        mailMessage.setTo(to);
        mailMessage.setSubject("Reset Password");
        mailMessage.setText("Click on the link to reset your password:\n" + resetLink);
        javaMailSender.send(mailMessage);
        log.info("Successfully sent password reset email to: {}", to);
    }

    public void sendResetMailFallback(String to, String token, Throwable t) {
        log.warn("sendResetMail rate-limited for email: {}. Reason: {}", to, t.getMessage());
        throw new IllegalStateException("You are requesting password reset emails too frequently. Please wait before trying again.");
    }
}

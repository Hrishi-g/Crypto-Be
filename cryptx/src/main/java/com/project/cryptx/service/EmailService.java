package com.project.cryptx.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

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

    public void sendResetMail(String to, String token) {

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailFrom);
        mailMessage.setTo(to);
        mailMessage.setSubject("Reset Password");
        mailMessage.setText("Click on the link to reset your password:\n" + resetLink);
        javaMailSender.send(mailMessage);
    }
}

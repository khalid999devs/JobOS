package com.jobos.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    public EmailService(JavaMailSender mailSender, EmailTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email to: " + to, e);
        }
    }

    public void sendOtpEmail(String to, String otp) {
        String htmlContent = templateService.processTemplate(
            "email/otp/password-reset-otp",
            Map.of("otp", otp, "recipientEmail", to, "emailTitle", "Password Reset Request")
        );
        
        sendHtmlEmail(to, "JobOS - Password Reset OTP", htmlContent);
    }

    public void sendWelcomeEmail(String to, String userName) {
        String htmlContent = templateService.processTemplate(
            "email/welcome/welcome-email",
            Map.of(
                "userName", userName,
                "recipientEmail", to,
                "emailTitle", "Welcome to JobOS"
            )
        );
        
        sendHtmlEmail(to, "Welcome to JobOS", htmlContent);
    }
}

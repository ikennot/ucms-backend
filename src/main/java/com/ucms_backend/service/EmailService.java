package com.ucms_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String FROM = "ucms.ucc@gmail.com";
    private static final String FROM_NAME = "UCMS - University Concern Management System";

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM, FROM_NAME);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email - UCMS");

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
                        <h2 style="color: #333;">Verify Your Email Address</h2>
                        <p>Thank you for registering with UCMS. Please click the button below to verify your email address.</p>
                        <a href="%s"
                           style="display: inline-block; padding: 12px 24px; background-color: #F5A623;
                                  color: white; text-decoration: none; border-radius: 8px;
                                  font-weight: bold; margin: 16px 0;">
                            Verify Email
                        </a>
                        <p style="color: #888; font-size: 12px;">If you did not request this, you can safely ignore this email.</p>
                    </div>
                    """.formatted(verificationLink);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
}

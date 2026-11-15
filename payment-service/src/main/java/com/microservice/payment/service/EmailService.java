package com.microservice.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    public void sendEmail(String toEmail, String name, String course, double amount) {
        if (javaMailSender == null) {
            log.warn("⚠️ JavaMailSender not configured. Skipping email to {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(toEmail);
            mailMessage.setSubject("Payment Successful - " + course);
            mailMessage.setText("Hi " + name + ", \n\n" +
                    "Thank you for your payment of Rs " + amount + ".\n" +
                    "Your order for " + course + " has been confirmed.\n\n" +
                    "We are looking forward to serving you!"
            );
            javaMailSender.send(mailMessage);
            log.info("✅ Confirmation email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendPaymentConfirmationFromKafka(String toEmail, String name, double amount, Long ecommerceOrderId) {
        if (javaMailSender == null) {
            log.warn("⚠️ JavaMailSender not configured. Skipping Kafka-triggered email to {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(toEmail);
            mailMessage.setSubject("Payment Processed - Order #" + ecommerceOrderId);
            mailMessage.setText("Hi " + name + ", \n\n" +
                    "Your payment of Rs " + amount + " for Order #" + ecommerceOrderId + " has been processed successfully.\n\n" +
                    "Thank you for shopping with us!"
            );
            javaMailSender.send(mailMessage);
            log.info("✅ Kafka-triggered payment email sent to {} for order #{}", toEmail, ecommerceOrderId);
        } catch (Exception e) {
            log.error("❌ Failed to send Kafka email to {}: {}", toEmail, e.getMessage());
        }
    }
}

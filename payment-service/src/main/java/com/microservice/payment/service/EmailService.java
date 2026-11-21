package com.microservice.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    /** Sends a payment confirmation. Email is optional: if mail is not configured it is skipped. */
    public void sendPaymentConfirmation(String toEmail, String name, double amount, Long ecommerceOrderId) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender not configured, skipping email to {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Payment Successful - Order #" + ecommerceOrderId);
            message.setText("Hi " + name + ",\n\n"
                    + "Your payment of Rs " + amount + " for Order #" + ecommerceOrderId
                    + " has been received successfully.\n\n"
                    + "Thank you for shopping with us!");
            mailSender.send(message);
            log.info("Payment confirmation email sent for order #{}", ecommerceOrderId);
        } catch (Exception e) {
            log.error("Failed to send payment email for order #{}: {}", ecommerceOrderId, e.getMessage());
        }
    }
}

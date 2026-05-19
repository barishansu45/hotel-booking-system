package com.hotelbooking.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.info("[Email] No recipient configured. Subject: {}\n{}", subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.warn("Email send failed (configure spring.mail.username/password if needed): {}", e.getMessage());
        }
    }

    public void sendReservationConfirmation(String email, String bookingDetails) {
        sendEmail(email, "Hotel Booking Confirmation",
            "Your booking has been confirmed.\n\n" + bookingDetails);
    }

    public void sendCapacityAlert(String email, String hotelName, String details) {
        sendEmail(email, "Low capacity alert — " + hotelName, details);
    }
}

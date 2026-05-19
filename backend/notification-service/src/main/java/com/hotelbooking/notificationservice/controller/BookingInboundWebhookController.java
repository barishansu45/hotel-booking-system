package com.hotelbooking.notificationservice.controller;

import com.hotelbooking.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Local/demo stand-in for "pull new reservations from the queue" (PDF). In Azure, replace with Service Bus consumer.
 */
@RestController
@RequestMapping("/notifications/inbound")
@RequiredArgsConstructor
@Slf4j
public class BookingInboundWebhookController {

    private final EmailService emailService;

    @Value("${app.alerts.demo-confirmation-email:}")
    private String demoConfirmationEmail;

    @PostMapping("/booking")
    public ResponseEntity<Void> receiveBookingEvent(@RequestBody Map<String, Object> body) {
        log.info("[Notification] Inbound booking event (queue stand-in): {}", body);
        emailService.sendEmail(
            demoConfirmationEmail,
            "Hotel reservation created",
            "Reservation event payload:\n" + body.toString());
        return ResponseEntity.accepted().build();
    }
}

package com.hotelbooking.notificationservice.listener;

import com.hotelbooking.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes booking events from RabbitMQ (CloudAMQP in production).
 * Queue name must match what booking-service publishes to.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventListener {

    private final EmailService emailService;

    @Value("${app.alerts.demo-confirmation-email:}")
    private String confirmationEmail;

    @RabbitListener(queues = "${app.queue.new-reservations:new-reservations}")
    public void handleBookingEvent(Map<String, Object> payload) {
        log.info("[RabbitMQ] Received booking event: {}", payload);

        String bookingId  = String.valueOf(payload.getOrDefault("bookingId",  "N/A"));
        String hotelId    = String.valueOf(payload.getOrDefault("hotelId",    "N/A"));
        String checkIn    = String.valueOf(payload.getOrDefault("checkInDate","N/A"));
        String checkOut   = String.valueOf(payload.getOrDefault("checkOutDate","N/A"));
        String finalPrice = String.valueOf(payload.getOrDefault("finalPrice", "N/A"));

        String body = String.format(
            "Your booking has been confirmed!\n\n" +
            "Booking ID  : %s\n" +
            "Hotel ID    : %s\n" +
            "Check-in    : %s\n" +
            "Check-out   : %s\n" +
            "Final Price : %s\n",
            bookingId, hotelId, checkIn, checkOut, finalPrice);

        emailService.sendReservationConfirmation(confirmationEmail, body);
    }
}

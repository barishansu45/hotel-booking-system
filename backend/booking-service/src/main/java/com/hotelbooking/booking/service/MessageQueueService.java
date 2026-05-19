package com.hotelbooking.booking.service;

import com.hotelbooking.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageQueueService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.queue.new-reservations:new-reservations}")
    private String queueName;

    public void sendBookingNotification(Booking booking) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "BOOKING_CREATED");
            payload.put("bookingId",    booking.getId()          != null ? booking.getId().toString()          : null);
            payload.put("hotelId",      booking.getHotelId()     != null ? booking.getHotelId().toString()     : null);
            payload.put("roomId",       booking.getRoomId()      != null ? booking.getRoomId().toString()      : null);
            payload.put("userId",       booking.getUserId()      != null ? booking.getUserId().toString()      : null);
            payload.put("checkInDate",  booking.getCheckInDate() != null ? booking.getCheckInDate().toString() : null);
            payload.put("checkOutDate", booking.getCheckOutDate()!= null ? booking.getCheckOutDate().toString(): null);
            payload.put("finalPrice",   booking.getFinalPrice()  != null ? booking.getFinalPrice().toPlainString() : null);
            payload.put("status",       booking.getStatus()      != null ? booking.getStatus().name()          : null);

            rabbitTemplate.convertAndSend(queueName, payload);
            log.info("[RabbitMQ] Booking event published to queue '{}': bookingId={}", queueName, payload.get("bookingId"));
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish booking notification", e);
        }
    }
}

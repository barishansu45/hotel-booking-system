package com.hotelbooking.notificationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * PDF: nightly capacity checks; reservation queue is implemented via Azure Service Bus in production,
 * with {@link com.hotelbooking.notificationservice.controller.BookingInboundWebhookController} as a local stand-in.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledNotificationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @Value("${app.hotel-service.url:http://localhost:8081/api/v1}")
    private String hotelServiceUrl;

    @Value("${app.capacity.threshold-percentage:20}")
    private int capacityThresholdPercent;

    @Value("${app.capacity.lookahead-days:30}")
    private int lookaheadDays;

    @Value("${app.alerts.admin-email:}")
    private String adminAlertEmail;
    
    @EventListener(ApplicationReadyEvent.class)
    public void logStartupHints() {
        log.info("Notification service ready. Booking-service can POST reservation payloads to /api/v1/notifications/inbound/booking when app.notifications.webhook-url is set.");
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void checkCapacityAndNotify() {
        try {
            String url = String.format("%s/internal/capacity-alerts?daysAhead=%d&thresholdPercent=%d",
                hotelServiceUrl, lookaheadDays, capacityThresholdPercent);
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) {
                log.warn("Capacity alert response was empty");
                return;
            }
            JsonNode root = objectMapper.readTree(json);
            if (!root.path("success").asBoolean(false)) {
                log.warn("Capacity alert API returned success=false: {}", json);
                return;
            }
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return;
            }
            log.info("Nightly capacity check: {} slot(s) under {}% available (ratio available/total).",
                data.size(), capacityThresholdPercent);
            for (JsonNode row : data) {
                String line = String.format(
                    "hotel=%s city=%s date=%s available=%d total=%d ratio=%.4f roomId=%s",
                    row.path("hotelName").asText(),
                    row.path("city").asText(),
                    row.path("date").asText(),
                    row.path("availableCapacity").asInt(),
                    row.path("totalCapacity").asInt(),
                    row.path("availableToTotalRatio").asDouble(),
                    row.path("roomId").asText());
                log.warn("[CapacityAlert] {}", line);
                if (adminAlertEmail != null && !adminAlertEmail.isBlank()) {
                    emailService.sendCapacityAlert(adminAlertEmail,
                        row.path("hotelName").asText(),
                        line);
                }
            }
        } catch (Exception e) {
            log.error("Nightly capacity check failed (is hotel-service running?)", e);
        }
    }
}

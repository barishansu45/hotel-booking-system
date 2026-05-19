package com.hotelbooking.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelAvailabilityClient {

    private final RestTemplate restTemplate;

    @Value("${app.hotel-service.url:http://localhost:8081/api/v1}")
    private String hotelServiceUrl;

    /** Decreases one unit of capacity per night from check-in through the night before check-out. */
    public void decreaseForStay(UUID roomId, LocalDate checkIn, LocalDate checkOut) {
        if (roomId == null || checkIn == null || checkOut == null) {
            return;
        }
        if (!checkOut.isAfter(checkIn)) {
            log.warn("Stay has no nights; skip availability decrease (checkIn={}, checkOut={})", checkIn, checkOut);
            return;
        }
        LocalDate lastNight = checkOut.minusDays(1);
        String url = UriComponentsBuilder.fromUriString(hotelServiceUrl)
            .path("/availability/room/{roomId}/decrease")
            .queryParam("startDate", checkIn)
            .queryParam("endDate", lastNight)
            .buildAndExpand(roomId)
            .toUriString();

        restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity.EMPTY, Void.class);
        log.info("Decreased availability for room {} from {} to {}", roomId, checkIn, lastNight);
    }
}

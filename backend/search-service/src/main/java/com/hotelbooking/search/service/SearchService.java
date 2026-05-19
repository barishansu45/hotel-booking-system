package com.hotelbooking.search.service;

import com.hotelbooking.search.dto.HotelSearchResult;
import com.hotelbooking.search.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final WebClient.Builder webClientBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${app.hotel-service.url:http://localhost:8081/api/v1}")
    private String hotelServiceUrl;
    
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");
    private static final String HOTEL_CACHE_PREFIX = "hotel:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    @Value("${app.search.hotel-fetch-size:300}")
    private int hotelFetchSize;

    @Cacheable(
        value = "hotelSearches",
        key = "#request.destination + '-' + #request.checkInDate + '-' + #request.checkOutDate + '-' + #request.guests + '-' + #isLoggedIn",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<HotelSearchResult> searchHotels(SearchRequest request, boolean isLoggedIn) {
        log.info("Searching hotels for destination: {} (logged in: {})", request.getDestination(), isLoggedIn);

        try {
            WebClient webClient = webClientBuilder.baseUrl(hotelServiceUrl).build();

            List<Map<String, Object>> hotels = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/hotels")
                    .queryParam("page", 0)
                    .queryParam("size", hotelFetchSize)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (List<Map<String, Object>>) ((Map<String, Object>) response.get("data")).get("content"))
                .block();

            if (hotels == null) {
                return List.of();
            }

            int guestCount = request.getGuests() != null && request.getGuests() > 0 ? request.getGuests() : 1;
            boolean filterByStay =
                request.getCheckInDate() != null
                    && request.getCheckOutDate() != null
                    && request.getCheckOutDate().isAfter(request.getCheckInDate());

            return hotels.stream()
                .filter(hotel -> matchesDestination(hotel, request.getDestination()))
                .filter(hotel -> !filterByStay || hotelHasBookableRoom(webClient, hotel, request.getCheckInDate(),
                    request.getCheckOutDate(), guestCount))
                .map(hotel -> {
                    HotelSearchResult result = convertToSearchResult(webClient, hotel, isLoggedIn, filterByStay,
                        request.getCheckInDate(), request.getCheckOutDate(), guestCount);
                    cacheHotelDetails(result);
                    return result;
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching hotels: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public HotelSearchResult getHotelDetails(String hotelId, boolean isLoggedIn) {
        log.info("Getting hotel details: {} (logged in: {})", hotelId, isLoggedIn);
        
        // Try to get from cache first
        String cacheKey = HOTEL_CACHE_PREFIX + hotelId;
        HotelSearchResult cached = (HotelSearchResult) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            log.info("Hotel found in cache: {}", hotelId);
            // Recalculate price based on login status
            if (isLoggedIn && cached.getBasePrice() != null) {
                cached.setDiscountedPrice(applyDiscount(cached.getBasePrice(), true));
            }
            return cached;
        }

        // Fetch from hotel-service if not in cache
        try {
            WebClient webClient = webClientBuilder.baseUrl(hotelServiceUrl).build();
            
            Map<String, Object> response = webClient.get()
                .uri("/hotels/" + hotelId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.get("data") != null) {
                Map<String, Object> hotel = (Map<String, Object>) response.get("data");
                HotelSearchResult result = convertToSearchResult(webClient, hotel, isLoggedIn, false,
                    null, null, 1);
                cacheHotelDetails(result);
                return result;
            }
        } catch (Exception e) {
            log.error("Error fetching hotel details: {}", e.getMessage());
        }

        return null;
    }

    private void cacheHotelDetails(HotelSearchResult hotel) {
        if (hotel != null && hotel.getHotelId() != null) {
            String cacheKey = HOTEL_CACHE_PREFIX + hotel.getHotelId();
            redisTemplate.opsForValue().set(cacheKey, hotel, CACHE_TTL);
            log.debug("Cached hotel: {}", hotel.getHotelId());
        }
    }

    private HotelSearchResult convertToSearchResult(
        WebClient webClient,
        Map<String, Object> hotel,
        boolean isLoggedIn,
        boolean filterByStay,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount
    ) {
        BigDecimal basePrice = getLowestRoomPrice(webClient, hotel, filterByStay, checkIn, checkOut, guestCount);

        List<String> images = hotel.get("images") != null ? (List<String>) hotel.get("images") : List.of();
        String mainImage = !images.isEmpty() ? images.get(0) : null;

        boolean hasAvailability =
            !filterByStay || basePrice.compareTo(BigDecimal.ZERO) > 0;

        return HotelSearchResult.builder()
            .hotelId(hotel.get("id") != null ? UUID.fromString(hotel.get("id").toString()) : null)
            .name((String) hotel.get("name"))
            .city((String) hotel.get("city"))
            .address((String) hotel.get("address"))
            .starRating(hotel.get("starRating") != null ? new BigDecimal(hotel.get("starRating").toString()) : null)
            .basePrice(basePrice)
            .discountedPrice(applyDiscount(basePrice, isLoggedIn))
            .amenities(hotel.get("amenities") != null ? (List<String>) hotel.get("amenities") : List.of())
            .mainImage(mainImage)
            .latitude(hotel.get("latitude") != null ? new BigDecimal(hotel.get("latitude").toString()) : null)
            .longitude(hotel.get("longitude") != null ? new BigDecimal(hotel.get("longitude").toString()) : null)
            .hasAvailability(hasAvailability)
            .build();
    }

    /**
     * PDF: only hotels with at least one room that is vacant for every night of the stay
     * [checkIn, checkOut) and can accommodate guestCount.
     */
    private boolean hotelHasBookableRoom(
        WebClient webClient,
        Map<String, Object> hotel,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount
    ) {
        try {
            String hotelId = hotel.get("id").toString();
            List<Map<String, Object>> rooms = fetchRooms(webClient, hotelId);
            if (rooms.isEmpty()) {
                return false;
            }
            LocalDate lastNight = checkOut.minusDays(1);
            for (Map<String, Object> room : rooms) {
                if (!roomAccommodatesGuests(room, guestCount)) {
                    continue;
                }
                UUID roomId = UUID.fromString(room.get("id").toString());
                if (roomHasInventoryForStay(webClient, roomId, checkIn, lastNight)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Availability check failed for hotel {}: {}", hotel.get("id"), e.getMessage());
            return false;
        }
    }

    private List<Map<String, Object>> fetchRooms(WebClient webClient, String hotelId) {
        Map<String, Object> response = webClient.get()
            .uri("/rooms/hotel/" + hotelId)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
        if (response == null || response.get("data") == null) {
            return List.of();
        }
        Object data = response.get("data");
        if (data instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rooms = (List<Map<String, Object>>) list;
            return rooms;
        }
        return List.of();
    }

    private boolean roomAccommodatesGuests(Map<String, Object> room, int guestCount) {
        Object mg = room.get("maxGuests");
        if (mg == null) {
            return false;
        }
        int max = mg instanceof Number ? ((Number) mg).intValue() : Integer.parseInt(mg.toString());
        return max >= guestCount;
    }

    private boolean roomHasInventoryForStay(WebClient webClient, UUID roomId, LocalDate checkIn, LocalDate lastNight) {
        long expectedNights = ChronoUnit.DAYS.between(checkIn, lastNight) + 1;
        if (expectedNights <= 0) {
            return false;
        }

        Map<String, Object> response = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/availability/room/{roomId}")
                .queryParam("startDate", checkIn)
                .queryParam("endDate", lastNight)
                .build(roomId))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            return false;
        }
        Object data = response.get("data");
        if (!(data instanceof List<?> list)) {
            return false;
        }
        Set<LocalDate> covered = new HashSet<>();
        for (Object item : list) {
            if (!(item instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) item;
            Object availObj = m.get("availableCapacity");
            int avail = availObj instanceof Number ? ((Number) availObj).intValue() : 0;
            if (avail <= 0) {
                continue;
            }
            LocalDate d = parseJsonDate(m.get("date"));
            if (d != null) {
                covered.add(d);
            }
        }
        LocalDate d = checkIn;
        while (!d.isAfter(lastNight)) {
            if (!covered.contains(d)) {
                return false;
            }
            d = d.plusDays(1);
        }
        return true;
    }

    private LocalDate parseJsonDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate ld) {
            return ld;
        }
        if (value instanceof String s) {
            return LocalDate.parse(s);
        }
        if (value instanceof List<?> parts && parts.size() >= 3) {
            int y = ((Number) parts.get(0)).intValue();
            int mo = ((Number) parts.get(1)).intValue();
            int day = ((Number) parts.get(2)).intValue();
            return LocalDate.of(y, mo, day);
        }
        return null;
    }

    private BigDecimal getLowestRoomPrice(
        WebClient webClient,
        Map<String, Object> hotel,
        boolean filterByStay,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount
    ) {
        try {
            String hotelId = hotel.get("id").toString();
            List<Map<String, Object>> rooms = fetchRooms(webClient, hotelId);
            if (rooms.isEmpty()) {
                return BigDecimal.ZERO;
            }
            LocalDate lastNight = filterByStay && checkOut != null ? checkOut.minusDays(1) : null;

            return rooms.stream()
                .filter(r -> r.get("basePrice") != null)
                .filter(r -> !filterByStay || roomAccommodatesGuests(r, guestCount))
                .filter(r -> {
                    if (!filterByStay || checkIn == null || lastNight == null) {
                        return true;
                    }
                    UUID roomId = UUID.fromString(r.get("id").toString());
                    return roomHasInventoryForStay(webClient, roomId, checkIn, lastNight);
                })
                .map(r -> new BigDecimal(r.get("basePrice").toString()))
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            log.warn("Could not fetch rooms for hotel {}: {}", hotel.get("id"), e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private boolean matchesDestination(Map<String, Object> hotel, String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            return true;
        }
        
        String city = (String) hotel.get("city");
        String name = (String) hotel.get("name");
        String address = (String) hotel.get("address");
        String country = (String) hotel.get("country");
        
        String searchTerm = destination.toLowerCase();
        return (city != null && city.toLowerCase().contains(searchTerm)) ||
               (name != null && name.toLowerCase().contains(searchTerm)) ||
               (address != null && address.toLowerCase().contains(searchTerm)) ||
               (country != null && country.toLowerCase().contains(searchTerm));
    }

    private BigDecimal applyDiscount(BigDecimal price, boolean isLoggedIn) {
        if (isLoggedIn && price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = price.multiply(BigDecimal.ONE.subtract(DISCOUNT_RATE));
            log.debug("Applied 15% discount: {} -> {}", price, discount);
            return discount;
        }
        return price;
    }
}

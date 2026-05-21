package com.hotelbooking.search.service;

import com.hotelbooking.search.dto.HotelSearchResult;
import com.hotelbooking.search.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    // Timeout for each individual HTTP call to hotel-service
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);
    // Max parallel room-fetch calls at once
    private static final int ROOM_FETCH_CONCURRENCY = 10;

    @Value("${app.search.hotel-fetch-size:300}")
    private int hotelFetchSize;

    @PostConstruct
    public void clearStaleCache() {
        try {
            // Clear all search caches on startup to avoid stale data
            Set<String> keys = redisTemplate.keys("hotelSearches*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} stale search cache entries on startup", keys.size());
            }
        } catch (Exception e) {
            log.warn("Could not clear cache on startup: {}", e.getMessage());
        }
    }

    public List<HotelSearchResult> searchHotels(SearchRequest request, boolean isLoggedIn) {
        String destination = request != null ? request.getDestination() : null;
        log.info("=== searchHotels START === destination={}, isLoggedIn={}", destination, isLoggedIn);

        try {
            WebClient webClient = webClientBuilder.baseUrl(hotelServiceUrl).build();
            log.info("Calling hotel-service at: {}/hotels?page=0&size={}", hotelServiceUrl, hotelFetchSize);

            List<Map<String, Object>> allHotels = fetchHotelsFromService(webClient, destination);

            if (allHotels == null || allHotels.isEmpty()) {
                log.warn("No hotels returned from hotel-service");
                return List.of();
            }

            // Filter by destination
            List<Map<String, Object>> matchingHotels = allHotels.stream()
                .filter(hotel -> matchesDestination(hotel, request.getDestination()))
                .collect(Collectors.toList());

            log.info("Destination filter '{}': {} / {} hotels match", destination, matchingHotels.size(), allHotels.size());

            // Build results — NO availability filtering at search time.
            // Availability is checked at booking time only.
            List<HotelSearchResult> results = matchingHotels.stream()
                .map(hotel -> {
                    HotelSearchResult result = buildResultWithoutRooms(hotel, isLoggedIn);
                    cacheHotelDetails(result);
                    return result;
                })
                .collect(Collectors.toList());

            log.info("=== searchHotels END === returning {} results", results.size());
            return results;

        } catch (Exception e) {
            log.error("Error in searchHotels: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Fetches hotels from hotel-service. Tries city-filtered query first (faster),
     * then falls back to unfiltered paginated fetch.
     */
    private List<Map<String, Object>> fetchHotelsFromService(WebClient webClient, String destination) {
        // Strategy 1: city path endpoint — /hotels/city/{city}?page=0&size=N (fastest, DB-level filter)
        if (destination != null && !destination.trim().isEmpty()) {
            String encoded = destination.trim();
            List<Map<String, Object>> cityResults = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/hotels/city/{city}")
                    .queryParam("page", 0)
                    .queryParam("size", hotelFetchSize)
                    .build(encoded))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    resp -> Mono.empty())
                .bodyToMono(Map.class)
                .timeout(HTTP_TIMEOUT)
                .map(r -> r != null ? extractContent(r) : List.<Map<String,Object>>of())
                .onErrorResume(e -> {
                    log.warn("City endpoint failed ({}), will try keyword search", e.getMessage());
                    return Mono.just(List.of());
                })
                .block();

            if (cityResults != null && !cityResults.isEmpty()) {
                log.info("City endpoint returned {} hotels for '{}'", cityResults.size(), destination);
                return cityResults;
            }

            // Strategy 2: keyword search — /hotels/search?keyword={destination}
            log.info("City endpoint returned 0 for '{}', trying keyword search", destination);
            List<Map<String, Object>> keywordResults = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/hotels/search")
                    .queryParam("keyword", destination)
                    .queryParam("page", 0)
                    .queryParam("size", hotelFetchSize)
                    .build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    resp -> Mono.empty())
                .bodyToMono(Map.class)
                .timeout(HTTP_TIMEOUT)
                .map(r -> r != null ? extractContent(r) : List.<Map<String,Object>>of())
                .onErrorResume(e -> {
                    log.warn("Keyword search failed ({}), will try full fetch", e.getMessage());
                    return Mono.just(List.of());
                })
                .block();

            if (keywordResults != null && !keywordResults.isEmpty()) {
                log.info("Keyword search returned {} hotels for '{}'", keywordResults.size(), destination);
                return keywordResults;
            }
            log.info("Keyword search also returned 0 for '{}', falling back to full fetch", destination);
        }

        // Strategy 3 (fallback): fetch first page unfiltered, filter in memory
        List<Map<String, Object>> allHotels = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/hotels")
                .queryParam("page", 0)
                .queryParam("size", hotelFetchSize)
                .build())
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(HTTP_TIMEOUT)
            .map(r -> r != null ? extractContent(r) : List.<Map<String,Object>>of())
            .onErrorResume(e -> {
                log.error("All hotel fetch strategies failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                return Mono.just(List.of());
            })
            .block();

        int count = allHotels != null ? allHotels.size() : 0;
        log.info("Unfiltered fetch returned {} hotels", count);
        return allHotels != null ? allHotels : List.of();
    }

    private List<Map<String, Object>> extractContent(Map<?, ?> response) {
        Object data = response.get("data");
        if (data instanceof Map) {
            Object content = ((Map<?, ?>) data).get("content");
            if (content instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) content;
                log.info("Hotel-service responded: {} hotels", list.size());
                return list;
            }
        }
        // Maybe data is directly a list (non-paginated response)
        if (data instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data;
            log.info("Hotel-service responded (list): {} hotels", list.size());
            return list;
        }
        log.warn("Unexpected hotel-service response structure, keys={}", response.keySet());
        return List.of();
    }

    /**
     * Builds a single HotelSearchResult reactively (non-blocking).
     * When filterByStay=false, skips room/availability fetches to return fast.
     */
    private Mono<HotelSearchResult> buildSearchResult(
        WebClient webClient,
        Map<String, Object> hotel,
        boolean isLoggedIn,
        boolean filterByStay,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount
    ) {
        if (!filterByStay) {
            // No date filter — return hotel info without fetching rooms (fast path)
            HotelSearchResult result = buildResultWithoutRooms(hotel, isLoggedIn);
            cacheHotelDetails(result);
            return Mono.just(result);
        }

        // Date filter — need rooms and availability
        String hotelId = hotel.get("id") != null ? hotel.get("id").toString() : null;
        if (hotelId == null) {
            return Mono.empty();
        }

        return fetchRoomsMono(webClient, hotelId)
            .flatMap(rooms -> {
                if (rooms.isEmpty()) {
                    return Mono.empty();
                }

                LocalDate lastNight = checkOut.minusDays(1);

                // Check each room for availability in parallel
                return Flux.fromIterable(rooms)
                    .filter(room -> roomAccommodatesGuests(room, guestCount))
                    .flatMap(room -> {
                        UUID roomId = UUID.fromString(room.get("id").toString());
                        return roomHasInventoryMono(webClient, roomId, checkIn, lastNight)
                            .map(available -> available ? room : null);
                    }, ROOM_FETCH_CONCURRENCY)
                    .filter(room -> room != null)
                    .collectList()
                    .flatMap(availableRooms -> {
                        if (availableRooms.isEmpty()) {
                            return Mono.empty();
                        }
                        BigDecimal lowestPrice = availableRooms.stream()
                            .filter(r -> r.get("basePrice") != null)
                            .map(r -> new BigDecimal(r.get("basePrice").toString()))
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

                        HotelSearchResult result = buildResult(hotel, isLoggedIn, lowestPrice, true);
                        cacheHotelDetails(result);
                        return Mono.just(result);
                    });
            })
            .onErrorResume(e -> {
                log.debug("Error processing hotel {}: {}", hotel.get("id"), e.getMessage());
                return Mono.empty();
            });
    }

    private HotelSearchResult buildResultWithoutRooms(Map<String, Object> hotel, boolean isLoggedIn) {
        // Try to use a price from the hotel object itself if available, else zero
        BigDecimal basePrice = BigDecimal.ZERO;
        if (hotel.get("basePrice") != null) {
            basePrice = new BigDecimal(hotel.get("basePrice").toString());
        } else if (hotel.get("minPrice") != null) {
            basePrice = new BigDecimal(hotel.get("minPrice").toString());
        }
        return buildResult(hotel, isLoggedIn, basePrice, true);
    }

    private HotelSearchResult buildResult(
        Map<String, Object> hotel,
        boolean isLoggedIn,
        BigDecimal basePrice,
        boolean hasAvailability
    ) {
        List<String> images = hotel.get("images") != null ? (List<String>) hotel.get("images") : List.of();
        String mainImage = !images.isEmpty() ? images.get(0) : null;

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

    public HotelSearchResult getHotelDetails(String hotelId, boolean isLoggedIn) {
        log.info("Getting hotel details: {} (logged in: {})", hotelId, isLoggedIn);

        // Try to get from cache first
        String cacheKey = HOTEL_CACHE_PREFIX + hotelId;
        HotelSearchResult cached = null;
        try {
            cached = (HotelSearchResult) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping cache read for hotel {}: {}", hotelId, e.getMessage());
        }

        if (cached != null) {
            log.info("Hotel found in cache: {}", hotelId);
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
                .timeout(HTTP_TIMEOUT)
                .block();

            if (response != null && response.get("data") != null) {
                Map<String, Object> hotel = (Map<String, Object>) response.get("data");
                HotelSearchResult result = buildResultWithoutRooms(hotel, isLoggedIn);
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
            try {
                redisTemplate.opsForValue().set(cacheKey, hotel, CACHE_TTL);
            } catch (Exception e) {
                log.debug("Could not cache hotel {}: {}", hotel.getHotelId(), e.getMessage());
            }
        }
    }

    private Mono<List<Map<String, Object>>> fetchRoomsMono(WebClient webClient, String hotelId) {
        return webClient.get()
            .uri("/rooms/hotel/" + hotelId)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(HTTP_TIMEOUT)
            .map(response -> {
                if (response == null || response.get("data") == null) return List.<Map<String, Object>>of();
                Object data = response.get("data");
                if (data instanceof List<?> list) {
                    return (List<Map<String, Object>>) list;
                }
                return List.<Map<String, Object>>of();
            })
            .onErrorResume(e -> {
                log.debug("Failed to fetch rooms for hotel {}: {}", hotelId, e.getMessage());
                return Mono.just(List.of());
            });
    }

    private Mono<Boolean> roomHasInventoryMono(WebClient webClient, UUID roomId, LocalDate checkIn, LocalDate lastNight) {
        long expectedNights = ChronoUnit.DAYS.between(checkIn, lastNight) + 1;
        if (expectedNights <= 0) {
            return Mono.just(false);
        }

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/availability/room/{roomId}")
                .queryParam("startDate", checkIn)
                .queryParam("endDate", lastNight)
                .build(roomId))
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(HTTP_TIMEOUT)
            .map(response -> {
                if (response == null || !Boolean.TRUE.equals(response.get("success"))) return false;
                Object data = response.get("data");
                if (!(data instanceof List<?> list)) return false;
                Set<LocalDate> covered = new HashSet<>();
                for (Object item : list) {
                    if (!(item instanceof Map)) continue;
                    Map<String, Object> m = (Map<String, Object>) item;
                    Object availObj = m.get("availableCapacity");
                    int avail = availObj instanceof Number ? ((Number) availObj).intValue() : 0;
                    if (avail <= 0) continue;
                    LocalDate d = parseJsonDate(m.get("date"));
                    if (d != null) covered.add(d);
                }
                LocalDate d = checkIn;
                while (!d.isAfter(lastNight)) {
                    if (!covered.contains(d)) return false;
                    d = d.plusDays(1);
                }
                return true;
            })
            .onErrorResume(e -> {
                log.debug("Availability check failed for room {}: {}", roomId, e.getMessage());
                return Mono.just(false);
            });
    }

    private LocalDate parseJsonDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof String s) {
            try { return LocalDate.parse(s); } catch (Exception e) { return null; }
        }
        if (value instanceof List<?> parts && parts.size() >= 3) {
            int y = ((Number) parts.get(0)).intValue();
            int mo = ((Number) parts.get(1)).intValue();
            int day = ((Number) parts.get(2)).intValue();
            return LocalDate.of(y, mo, day);
        }
        return null;
    }

    private boolean roomAccommodatesGuests(Map<String, Object> room, int guestCount) {
        Object mg = room.get("maxGuests");
        if (mg == null) return false;
        int max = mg instanceof Number ? ((Number) mg).intValue() : Integer.parseInt(mg.toString());
        return max >= guestCount;
    }

    private boolean matchesDestination(Map<String, Object> hotel, String destination) {
        if (destination == null || destination.trim().isEmpty()) return true;

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
            return price.multiply(BigDecimal.ONE.subtract(DISCOUNT_RATE));
        }
        return price;
    }
}

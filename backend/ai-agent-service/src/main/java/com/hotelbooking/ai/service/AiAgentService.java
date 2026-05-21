package com.hotelbooking.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.ai.dto.ChatRequest;
import com.hotelbooking.ai.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAgentService {

    private static final Pattern ISO_DATE = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");
    private static final Pattern GUESTS_WORD = Pattern.compile(
            "(\\d+)\\s*(guests?|people|misafir|kişi|kisi|kis[iı])\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> CITY_HINTS = new LinkedHashMap<>();

    static {
        CITY_HINTS.put("istanbul", "Istanbul");
        CITY_HINTS.put("antalya", "Antalya");
        CITY_HINTS.put("cappadocia", "Nevşehir");
        CITY_HINTS.put("kapadokya", "Nevşehir");
        CITY_HINTS.put("nevşehir", "Nevşehir");
        CITY_HINTS.put("nevsehir", "Nevşehir");
        CITY_HINTS.put("izmir", "Izmir");
        CITY_HINTS.put("ankara", "Ankara");
        CITY_HINTS.put("bodrum", "Bodrum");
    }

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.openai.api-key}")
    private String openaiApiKey;

    @Value("${app.openai.model}")
    private String model;

    @Value("${app.openai.base-url}")
    private String openaiBaseUrl;

    @Value("${app.services.search-service-url}")
    private String searchServiceUrl;

    @Value("${app.services.booking-service-url}")
    private String bookingServiceUrl;

    @Value("${app.services.hotel-service-url}")
    private String hotelServiceUrl;

    public ChatResponse processMessage(ChatRequest request, String authorizationHeader) {
        log.info("Processing AI chat message: {}", request.getMessage());

        String userMessage = request.getMessage() == null ? "" : request.getMessage();
        ApiIntent intent = resolveIntent(userMessage);

        String apiContext = "";
        JsonNode searchResults = null;
        if (intent.destination != null && !intent.destination.isBlank()) {
            searchResults = invokeSearchApi(buildSearchBody(intent), authorizationHeader);
            apiContext = formatSearchBlock(searchResults);
        }

        String bookingNote = "";
        if (intent.wantsBooking
                && request.getUserId() != null
                && !request.getUserId().isBlank()
                && intent.checkIn != null
                && intent.checkOut != null
                && intent.checkOut.isAfter(intent.checkIn)
                && searchResults != null
                && searchResults.isArray()) {
            bookingNote = tryBookingFromSearch(intent, searchResults, request.getUserId());
        }

        if (!bookingNote.isBlank()) {
            apiContext = apiContext.isBlank() ? bookingNote : apiContext + "\n" + bookingNote;
        }

        String systemPrompt = buildSystemPrompt(apiContext);
        String aiResponse;
        try {
            aiResponse = callOpenAI(systemPrompt, userMessage);

            if (aiResponse == null || aiResponse.contains("having trouble connecting")) {
                aiResponse = contextAwareFallback(userMessage, bookingNote, searchResults);
            }
        } catch (Exception e) {
            log.error("Error processing AI message, using fallback", e);
            aiResponse = contextAwareFallback(userMessage, bookingNote, searchResults);
        }

        return ChatResponse.builder()
                .message(aiResponse)
                .conversationId(request.getConversationId())
                .build();
    }

    private ApiIntent resolveIntent(String userMessage) {
        String lower = userMessage.toLowerCase(Locale.ROOT);
        boolean wantsBooking = lower.matches(".*\\b(book|booking|reserve|reservation|rezervasyon|rezerve)\\b.*");
        ApiIntent fromAi = parseIntentWithOpenAI(userMessage);
        if (fromAi != null) {
            if (fromAi.wantsBooking == null) {
                fromAi = new ApiIntent(
                        fromAi.destination,
                        fromAi.checkIn,
                        fromAi.checkOut,
                        fromAi.guests,
                        wantsBooking,
                        fromAi.hotelNameHint);
            }
            return normalizeIntent(fromAi, lower, wantsBooking);
        }
        return ruleBasedIntent(lower, wantsBooking);
    }

    private ApiIntent normalizeIntent(ApiIntent in, String lower, boolean wantsBookingFallback) {
        String dest = in.destination != null ? in.destination.trim() : null;
        if (dest != null && dest.isBlank()) {
            dest = null;
        }
        if (dest == null) {
            dest = guessDestinationFromKeywords(lower);
        }
        Integer guests = in.guests != null ? in.guests : parseGuests(lower);
        boolean wants = in.wantsBooking != null ? in.wantsBooking : wantsBookingFallback;
        return new ApiIntent(dest, in.checkIn, in.checkOut, guests, wants, in.hotelNameHint);
    }

    private ApiIntent ruleBasedIntent(String lower, boolean wantsBooking) {
        String dest = guessDestinationFromKeywords(lower);
        LocalDate[] range = parseIsoDates(lower);
        Integer guests = parseGuests(lower);
        return new ApiIntent(dest, range[0], range[1], guests, wantsBooking, null);
    }

    private String guessDestinationFromKeywords(String lower) {
        for (Map.Entry<String, String> e : CITY_HINTS.entrySet()) {
            if (lower.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private LocalDate[] parseIsoDates(String text) {
        Matcher m = ISO_DATE.matcher(text);
        LocalDate first = null;
        LocalDate second = null;
        while (m.find()) {
            try {
                LocalDate d = LocalDate.parse(m.group(1));
                if (first == null) {
                    first = d;
                } else if (second == null) {
                    second = d;
                }
            } catch (DateTimeParseException ignored) {
                // next match
            }
        }
        if (first != null && second != null && second.isBefore(first)) {
            LocalDate t = first;
            first = second;
            second = t;
        }
        return new LocalDate[] {first, second};
    }

    private Integer parseGuests(String lower) {
        Matcher m = GUESTS_WORD.matcher(lower);
        if (m.find()) {
            try {
                return Math.max(1, Integer.parseInt(m.group(1)));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private ApiIntent parseIntentWithOpenAI(String userMessage) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            return null;
        }
        try {
            String system = """
                Extract fields for a hotel search/booking assistant from the user message.

                Reply with ONLY a JSON object, no markdown, keys:
                destination (string or null, city name in English e.g. Istanbul),
                checkIn (YYYY-MM-DD or null),
                checkOut (YYYY-MM-DD or null),
                guests (integer or null),
                wantsBooking (boolean),
                hotelNameHint (string or null, hotel name fragment if user wants to book a specific hotel)
                """;
            String raw = callOpenAIRaw(system, userMessage);
            if (raw == null) {
                return null;
            }
            raw = stripJsonFence(raw);
            JsonNode n = objectMapper.readTree(raw);
            String dest = textOrNull(n.path("destination"));
            LocalDate in = parseDateNode(n.path("checkIn"));
            LocalDate out = parseDateNode(n.path("checkOut"));
            Integer guests = n.path("guests").isNumber() ? n.path("guests").asInt() : null;
            Boolean wants = n.has("wantsBooking") && !n.path("wantsBooking").isNull()
                    ? n.path("wantsBooking").asBoolean()
                    : null;
            String hint = textOrNull(n.path("hotelNameHint"));
            return new ApiIntent(dest, in, out, guests, wants, hint);
        } catch (Exception e) {
            log.debug("OpenAI intent parse failed, using rules: {}", e.getMessage());
            return null;
        }
    }

    private static String stripJsonFence(String raw) {
        String t = raw.trim();
        if (t.startsWith("```")) {
            int start = t.indexOf('{');
            int end = t.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return t.substring(start, end + 1);
            }
        }
        return t;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().trim();
    }

    private static LocalDate parseDateNode(JsonNode node) {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(node.asText().trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Map<String, Object> buildSearchBody(ApiIntent intent) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("destination", intent.destination);
        body.put("checkInDate", intent.checkIn);
        body.put("checkOutDate", intent.checkOut);
        body.put("guests", intent.guests != null ? intent.guests : 2);
        return body;
    }

    private JsonNode invokeSearchApi(Map<String, Object> body, String authorizationHeader) {
        try {
            WebClient client = webClientBuilder.baseUrl(searchServiceUrl.replaceAll("/$", "")).build();
            WebClient.RequestBodySpec spec =
                    client.post().uri("/search").contentType(MediaType.APPLICATION_JSON);
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
            String json = spec.bodyValue(body).retrieve().bodyToMono(String.class).block();
            return objectMapper.readTree(json);
        } catch (WebClientResponseException e) {
            log.warn("Search API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return objectMapper.createArrayNode();
        } catch (Exception e) {
            log.warn("Search API call failed: {}", e.getMessage());
            return objectMapper.createArrayNode();
        }
    }

    private String formatSearchBlock(JsonNode results) {
        if (results == null || !results.isArray() || results.isEmpty()) {
            return "Search API returned no hotels for this query.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Current search results from POST /search (use these names/prices; do not invent hotels):\n");
        int limit = Math.min(8, results.size());
        for (int i = 0; i < limit; i++) {
            JsonNode h = results.get(i);
            sb.append("- ")
                    .append(h.path("name").asText())
                    .append(" | ")
                    .append(h.path("city").asText())
                    .append(" | price ")
                    .append(displayPrice(h))
                    .append(" | hotelId ")
                    .append(h.path("hotelId").asText())
                    .append(" | hasAvailability ")
                    .append(h.path("hasAvailability").asBoolean(false))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private static String displayPrice(JsonNode h) {
        if (h.hasNonNull("discountedPrice")) {
            return h.path("discountedPrice").asText();
        }
        return h.path("basePrice").asText("n/a");
    }

    private String tryBookingFromSearch(ApiIntent intent, JsonNode hotels, String userIdStr) {
        JsonNode hotel = pickHotel(hotels, intent.hotelNameHint);
        if (hotel == null || hotel.path("hotelId").asText().isBlank()) {
            return "Booking API was not called: specify which hotel (name) from the search list.";
        }
        UUID hotelId = UUID.fromString(hotel.path("hotelId").asText());
        int guests = intent.guests != null ? intent.guests : 2;

        JsonNode room = pickRoom(hotelId, guests);
        if (room == null || !room.hasNonNull("id")) {
            return "Booking API was not called: no suitable room (capacity) found via GET /rooms/hotel/"
                    + hotelId
                    + ".";
        }
        UUID roomId = UUID.fromString(room.path("id").asText());
        long nights = ChronoUnit.DAYS.between(intent.checkIn, intent.checkOut);
        if (nights <= 0) {
            return "";
        }
        BigDecimal nightly = new BigDecimal(room.path("basePrice").asText("0"));
        BigDecimal total = nightly.multiply(BigDecimal.valueOf(nights));

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr.trim());
        } catch (Exception e) {
            return "Booking API was not called: invalid userId.";
        }

        Map<String, Object> bookingBody = new LinkedHashMap<>();
        bookingBody.put("userId", userId.toString());
        bookingBody.put("hotelId", hotelId.toString());
        bookingBody.put("roomId", roomId.toString());
        bookingBody.put("checkInDate", intent.checkIn.toString());
        bookingBody.put("checkOutDate", intent.checkOut.toString());
        bookingBody.put("numGuests", guests);
        bookingBody.put("totalPrice", total);
        bookingBody.put("specialRequests", "Created via AI assistant");

        try {
            WebClient client = webClientBuilder.baseUrl(bookingServiceUrl.replaceAll("/$", "")).build();
            String resp = client.post()
                    .uri("/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bookingBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode b = objectMapper.readTree(resp);
            String id = b.path("id").asText();
            return "Booking API POST /bookings succeeded: bookingId=" + id + ", totalPrice=" + total + ".";
        } catch (WebClientResponseException e) {
            log.warn("Booking API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "Booking API error: "
                    + e.getStatusCode().value()
                    + " — "
                    + e.getResponseBodyAsString().replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            log.warn("Booking failed: {}", e.getMessage());
            return "Booking API call failed: " + e.getMessage();
        }
    }

    private JsonNode pickHotel(JsonNode hotels, String nameHint) {
        if (hotels.isEmpty()) {
            return null;
        }
        // If user mentioned a specific hotel name, prefer it
        if (nameHint != null && !nameHint.isBlank()) {
            String hlow = nameHint.toLowerCase(Locale.ROOT);
            for (JsonNode h : hotels) {
                String n = h.path("name").asText("").toLowerCase(Locale.ROOT);
                if (n.contains(hlow)) {
                    return h;
                }
            }
        }
        // Pick first hotel that has availability; fall back to first in list
        for (JsonNode h : hotels) {
            if (h.path("hasAvailability").asBoolean(true)) {
                return h;
            }
        }
        return hotels.get(0);
    }

    private JsonNode pickRoom(UUID hotelId, int guests) {
        try {
            WebClient client = webClientBuilder.baseUrl(hotelServiceUrl.replaceAll("/$", "")).build();
            String json = client.get()
                    .uri("/rooms/hotel/" + hotelId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return null;
            }
            JsonNode best = null;
            BigDecimal bestPrice = null;
            for (JsonNode r : data) {
                int max = r.path("maxGuests").asInt(0);
                if (max < guests) {
                    continue;
                }
                BigDecimal price = new BigDecimal(r.path("basePrice").asText("999999999"));
                if (best == null || price.compareTo(bestPrice) < 0) {
                    best = r;
                    bestPrice = price;
                }
            }
            return best;
        } catch (Exception e) {
            log.warn("Room fetch failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildSystemPrompt(String apiContext) {
        String base = """
            You are a helpful hotel booking assistant. Your role is to:
            - Help users search for hotels
            - Provide information about hotel amenities and prices
            - Complete bookings on behalf of the user when they request it
            - Answer questions about hotels and accommodations

            Key information:
            - Logged-in users get 15% discount on all bookings
            - To make a booking, the user must provide: destination, check-in date, check-out date
            - If the user says "book", "reserve", or similar AND provides dates, a booking will be attempted automatically
            - If "Booking API POST /bookings succeeded" appears in context, confirm the booking enthusiastically with the bookingId and price
            - If "Booking API was not called" or "Booking API error" appears, explain what info is needed

            When "Current search results" appears below, use those hotel names and prices — do not invent hotels.
            When booking context appears, summarize it clearly for the user.

            Be friendly, concise, and helpful. Keep responses under 150 words.
            """;
        if (apiContext == null || apiContext.isBlank()) {
            return base;
        }
        return base + "\n\n" + apiContext;
    }

    private String callOpenAIRaw(String systemContent, String userMessage) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(openaiBaseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put(
                    "messages",
                    List.of(
                            Map.of("role", "system", "content", systemContent),
                            Map.of("role", "user", "content", userMessage)));
            requestBody.put("max_tokens", 200);
            requestBody.put("temperature", 0.3);

            String response = webClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.path("choices").get(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.error("OpenAI API error: {}", e.getMessage(), e);
            return null;
        }
    }

    private String contextAwareFallback(String userMessage, String bookingNote, JsonNode searchResults) {
        // If booking was attempted, report its result directly
        if (bookingNote != null && !bookingNote.isBlank()) {
            if (bookingNote.contains("succeeded")) {
                // Extract bookingId and price from the note
                String bookingId = "";
                String price = "";
                java.util.regex.Matcher idM = java.util.regex.Pattern.compile("bookingId=([\\w-]+)").matcher(bookingNote);
                java.util.regex.Matcher priceM = java.util.regex.Pattern.compile("totalPrice=([\\d.]+)").matcher(bookingNote);
                if (idM.find()) bookingId = idM.group(1);
                if (priceM.find()) price = priceM.group(1);
                return String.format(
                    "✅ Your booking is confirmed!\n\nBooking ID: %s\nTotal price: $%s\n\nYou can view your reservation in My Bookings. Logged-in members receive a 15%% discount.",
                    bookingId.isEmpty() ? "N/A" : bookingId,
                    price.isEmpty() ? "see My Bookings" : price);
            }
            if (bookingNote.contains("specify which hotel")) {
                // Multiple hotels found, list them
                StringBuilder sb = new StringBuilder("I found several hotels. Please specify the hotel name to complete the booking:\n\n");
                if (searchResults != null && searchResults.isArray()) {
                    int i = 1;
                    for (JsonNode h : searchResults) {
                        if (i > 5) break;
                        sb.append(String.format("%d. **%s** — $%s/night\n", i++,
                            h.path("name").asText("Hotel"),
                            displayPrice(h)));
                    }
                }
                sb.append("\nReply with the hotel name and your dates to book.");
                return sb.toString();
            }
            if (bookingNote.contains("error") || bookingNote.contains("failed")) {
                return "I found hotels for your search but was unable to complete the booking automatically. Please use the 'View Details' button on the hotel card and click 'Book Now' to complete your reservation.";
            }
        }

        // If search returned results, list them
        if (searchResults != null && searchResults.isArray() && !searchResults.isEmpty()) {
            StringBuilder sb = new StringBuilder("Here are the available hotels:\n\n");
            int limit = Math.min(5, searchResults.size());
            for (int i = 0; i < limit; i++) {
                JsonNode h = searchResults.get(i);
                sb.append(String.format("• **%s** — %s — $%s/night\n",
                    h.path("name").asText("Hotel"),
                    h.path("city").asText(""),
                    displayPrice(h)));
            }
            sb.append("\nTo book, say: \"Book [hotel name] from [check-in] to [check-out] for [N] guests\"");
            return sb.toString();
        }

        // Generic fallback
        return fallbackResponse(userMessage);
    }

    private String fallbackResponse(String userMessage) {
        String msg = userMessage == null ? "" : userMessage.toLowerCase();

        boolean isTurkish = containsTurkish(msg);

        if (msg.contains("hello") || msg.contains("hi ") || msg.equals("hi") || msg.contains("merhaba")
                || msg.contains("selam")) {
            return isTurkish
                    ? "Merhaba! Ben otel rezervasyon asistanınızım. Otel arama, fiyat bilgisi ve harika destinasyonlar konusunda size yardımcı olabilirim. Bugün ne arıyorsunuz?"
                    : "Hello! I'm your hotel booking assistant. I can help you find hotels, learn about prices, and discover great destinations. What are you looking for today?";
        }
        if (msg.contains("istanbul")) {
            return isTurkish
                    ? "Harika seçim! Sultanahmet'te muhteşem Boğaz manzaralı 5 yıldızlı Grand Istanbul Hotel'imiz var. Fiyatlar $250/gece'den başlıyor. Üyeler %15 indirim kazanır! Tüm tarihleri görmek için arama yapın."
                    : "Great choice! We have the Grand Istanbul Hotel — a 5-star luxury hotel in Sultanahmet with stunning Bosphorus views. Prices start at $250/night. Logged-in members get 15% off!";
        }
        if (msg.contains("antalya")) {
            return isTurkish
                    ? "Antalya muhteşem bir sahil destinasyonu! Doğrudan plaj erişimli Antalya Beach Resort'u (4.5 yıldız, $180/gece) deneyin. Üyeler %15 indirim kazanır."
                    : "Antalya is a beautiful coastal destination! Try Antalya Beach Resort (4.5 stars, $180/night) with direct beach access. Logged-in members save 15%.";
        }
        if (msg.contains("cappadocia") || msg.contains("kapadokya")) {
            return isTurkish
                    ? "Kapadokya büyüleyici! Cappadocia Cave Hotel'imiz (4 yıldız, $150/gece) otantik mağara odaları ve sabahları muhteşem sıcak hava balonu manzaraları sunuyor."
                    : "Cappadocia is magical! Our Cappadocia Cave Hotel (4 stars, $150/night) offers authentic cave rooms and amazing hot air balloon views.";
        }
        if (msg.contains("izmir")) {
            return isTurkish
                    ? "Izmir Seaside Hotel deniz manzaralı modern bir seçim, $120/gece'den başlıyor. Hem tatil hem iş seyahatleri için ideal."
                    : "Izmir Seaside Hotel is a great modern choice with sea views, starting at $120/night. Perfect for both leisure and business travelers.";
        }
        if (msg.contains("ankara")) {
            return isTurkish
                    ? "Ankara Business Hotel başkentteki iş seyahatleri için ideal, $100/gece'den başlıyor, tam donanımlı iş merkezi mevcut."
                    : "Ankara Business Hotel is perfect for business travelers in the capital, starting at $100/night with a full business center.";
        }
        if (msg.contains("discount") || msg.contains("indirim") || msg.contains("price") || msg.contains("fiyat")) {
            return isTurkish
                    ? "Üye olarak giriş yapan kullanıcılar tüm rezervasyonlarda %15 indirim kazanır! Kayıt olun veya giriş yapın, ardından destinasyonunuzu arayarak indirimli fiyatları görün."
                    : "Logged-in members enjoy 15% off all bookings! Sign up or log in, then search for your destination to see the discounted prices.";
        }
        if (msg.contains("book") || msg.contains("reservation") || msg.contains("rezervasyon") || msg.contains("rezerve")) {
            return isTurkish
                    ? "Otel rezervasyonu için: 1) Destinasyon ve tarihleri arayın, 2) Beğendiğiniz otelde 'View Details' tıklayın, 3) Tarihlerinizi seçip 'Book Now' tıklayın. %15 indirim için giriş yapmayı unutmayın!"
                    : "To book a hotel: 1) Search by destination and dates, 2) Click 'View Details' on a hotel you like, 3) Select your dates and click 'Book Now'. Don't forget to log in for 15% off!";
        }
        if (msg.contains("search") || msg.contains("find") || msg.contains("hotel") || msg.contains("otel")
                || msg.contains("ara") || msg.contains("bul")) {
            return isTurkish
                    ? "Ana sayfadaki arama çubuğunu kullanarak destinasyona göre otel arayabilirsiniz. Istanbul, Antalya, Kapadokya, Izmir ve Ankara'da otellerimiz var. Öneri ister misiniz?"
                    : "You can search for hotels by destination using the search bar on the homepage. We have hotels in Istanbul, Antalya, Cappadocia, Izmir, and Ankara. Need a recommendation?";
        }
        return isTurkish
                ? "Size Istanbul, Antalya, Kapadokya, Izmir veya Ankara'da otel bulmada yardımcı olabilirim. Üyeler %15 indirim kazanır! Hangi destinasyonla ilgileniyorsunuz?"
                : "I can help you find hotels in Istanbul, Antalya, Cappadocia, Izmir, or Ankara. Members get 15% off! What destination are you interested in?";
    }

    private boolean containsTurkish(String msg) {
        if (msg == null) {
            return false;
        }
        return msg.matches(".*[çğıöşüÇĞİÖŞÜ].*")
                || msg.contains("merhaba") || msg.contains("selam")
                || msg.contains("nasıl") || msg.contains("nerede")
                || msg.contains("otel") || msg.contains("indirim")
                || msg.contains("fiyat") || msg.contains("rezerve")
                || msg.contains("rezervasyon") || msg.contains("ara")
                || msg.contains("bana") || msg.contains("kapadokya");
    }

    private String callOpenAI(String systemPrompt, String userMessage) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(openaiBaseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put(
                    "messages",
                    List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)));
            requestBody.put("max_tokens", 200);
            requestBody.put("temperature", 0.7);

            String response = webClient
                    .post()
                    .uri("/chat/completions")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("OpenAI API error: {}", e.getMessage(), e);
            return "I\'m having trouble connecting to my AI brain right now. Please try again in a moment!";
        }
    }

    private record ApiIntent(
            String destination,
            LocalDate checkIn,
            LocalDate checkOut,
            Integer guests,
            Boolean wantsBooking,
            String hotelNameHint) {}
}

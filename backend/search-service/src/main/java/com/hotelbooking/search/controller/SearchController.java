package com.hotelbooking.search.controller;

import com.hotelbooking.search.dto.HotelSearchResult;
import com.hotelbooking.search.dto.SearchRequest;
import com.hotelbooking.search.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<List<HotelSearchResult>> searchHotels(
            @RequestBody SearchRequest request,
            HttpServletRequest httpRequest) {
        
        // Check if user is authenticated (JWT filter sets userId attribute)
        boolean isLoggedIn = httpRequest.getAttribute("userId") != null;
        log.info("Search request from {} user, destination={}", isLoggedIn ? "authenticated" : "guest", request.getDestination());

        try {
            List<HotelSearchResult> results = searchService.searchHotels(request, isLoggedIn);
            log.info("Search complete: {} results returned", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Search controller error: {}", e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelSearchResult> getHotelDetails(
            @PathVariable String hotelId,
            HttpServletRequest httpRequest) {
        
        boolean isLoggedIn = httpRequest.getAttribute("userId") != null;
        HotelSearchResult result = searchService.getHotelDetails(hotelId, isLoggedIn);
        
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(result);
    }
}

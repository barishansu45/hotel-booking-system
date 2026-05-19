package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.ApiResponse;
import com.hotelbooking.hotel.dto.CapacityAlertDTO;
import com.hotelbooking.hotel.service.RoomAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Inter-service API for notification jobs (PDF: nightly capacity check).
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalCapacityController {

    private final RoomAvailabilityService roomAvailabilityService;

    @GetMapping("/capacity-alerts")
    public ResponseEntity<ApiResponse<List<CapacityAlertDTO>>> capacityAlerts(
            @RequestParam(defaultValue = "30") int daysAhead,
            @RequestParam(defaultValue = "20") int thresholdPercent) {
        LocalDate start = LocalDate.now();
        int ahead = Math.max(1, daysAhead);
        LocalDate end = start.plusDays(ahead - 1L);
        List<CapacityAlertDTO> alerts =
            roomAvailabilityService.findLowCapacityAlerts(start, end, thresholdPercent);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /** Explicit date range (optional tooling / tests). */
    @GetMapping("/capacity-alerts/range")
    public ResponseEntity<ApiResponse<List<CapacityAlertDTO>>> capacityAlertsRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "20") int thresholdPercent) {
        List<CapacityAlertDTO> alerts =
            roomAvailabilityService.findLowCapacityAlerts(startDate, endDate, thresholdPercent);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }
}

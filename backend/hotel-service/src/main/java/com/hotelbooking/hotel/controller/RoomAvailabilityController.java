package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.ApiResponse;
import com.hotelbooking.hotel.dto.RoomAvailabilityDTO;
import com.hotelbooking.hotel.service.RoomAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class RoomAvailabilityController {

    private final RoomAvailabilityService availabilityService;

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<RoomAvailabilityDTO>>> getAvailabilityByDateRange(
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<RoomAvailabilityDTO> availabilities = 
                availabilityService.getAvailabilityByDateRange(roomId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(availabilities));
    }

    @GetMapping("/room/{roomId}/available")
    public ResponseEntity<ApiResponse<List<RoomAvailabilityDTO>>> getAvailableRoomsByDateRange(
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<RoomAvailabilityDTO> availabilities = 
                availabilityService.getAvailableRoomsByDateRange(roomId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(availabilities));
    }

    @GetMapping("/room/{roomId}/date/{date}")
    public ResponseEntity<ApiResponse<RoomAvailabilityDTO>> getAvailabilityByDate(
            @PathVariable UUID roomId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        RoomAvailabilityDTO availability = availabilityService.getAvailabilityByDate(roomId, date);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    @PatchMapping("/room/{roomId}/decrease")
    public ResponseEntity<ApiResponse<Void>> decreaseAvailability(
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        availabilityService.decreaseAvailability(roomId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Availability decreased successfully", null));
    }

    @PatchMapping("/room/{roomId}/increase")
    public ResponseEntity<ApiResponse<Void>> increaseAvailability(
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        availabilityService.increaseAvailability(roomId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Availability increased successfully", null));
    }
}

package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.AvailabilityRequest;
import com.hotelbooking.hotel.dto.HotelDTO;
import com.hotelbooking.hotel.dto.RoomDTO;
import com.hotelbooking.hotel.dto.RoomAvailabilityDTO;
import com.hotelbooking.hotel.dto.ApiResponse;
import com.hotelbooking.hotel.service.HotelService;
import com.hotelbooking.hotel.service.RoomService;
import com.hotelbooking.hotel.service.RoomAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final HotelService hotelService;
    private final RoomService roomService;
    private final RoomAvailabilityService roomAvailabilityService;

    @PostMapping("/hotels")
    public ResponseEntity<ApiResponse<HotelDTO>> createHotel(@Valid @RequestBody HotelDTO hotelDTO) {
        log.info("Admin creating hotel: {}", hotelDTO.getName());
        HotelDTO created = hotelService.createHotel(hotelDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Hotel created successfully", created));
    }

    @PutMapping("/hotels/{id}")
    public ResponseEntity<ApiResponse<HotelDTO>> updateHotel(
            @PathVariable UUID id,
            @Valid @RequestBody HotelDTO hotelDTO) {
        log.info("Admin updating hotel: {}", id);
        HotelDTO updated = hotelService.updateHotel(id, hotelDTO);
        return ResponseEntity.ok(ApiResponse.success("Hotel updated successfully", updated));
    }

    @DeleteMapping("/hotels/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHotel(@PathVariable UUID id) {
        log.info("Admin deleting hotel: {}", id);
        hotelService.deleteHotel(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel deleted successfully", null));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<RoomDTO>> createRoom(@Valid @RequestBody RoomDTO roomDTO) {
        log.info("Admin creating room for hotel: {}", roomDTO.getHotelId());
        RoomDTO created = roomService.createRoom(roomDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Room created successfully", created));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<RoomDTO>> updateRoom(
            @PathVariable UUID id,
            @Valid @RequestBody RoomDTO roomDTO) {
        log.info("Admin updating room: {}", id);
        RoomDTO updated = roomService.updateRoom(id, roomDTO);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", updated));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable UUID id) {
        log.info("Admin deleting room: {}", id);
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }

    @PostMapping("/availability")
    public ResponseEntity<ApiResponse<List<RoomAvailabilityDTO>>> setRoomAvailability(
            @Valid @RequestBody AvailabilityRequest request) {
        log.info("Admin setting availability for room {} {}..{}", request.getRoomId(), request.getStartDate(), request.getEndDate());
        List<RoomAvailabilityDTO> rows = roomAvailabilityService.setRoomAvailability(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Availability set successfully", rows));
    }
}


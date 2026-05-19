package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.ApiResponse;
import com.hotelbooking.hotel.dto.HotelDTO;
import com.hotelbooking.hotel.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class HotelController {

    private final HotelService hotelService;

    @PostMapping
    public ResponseEntity<ApiResponse<HotelDTO>> createHotel(@Valid @RequestBody HotelDTO hotelDTO) {
        HotelDTO createdHotel = hotelService.createHotel(hotelDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hotel created successfully", createdHotel));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelDTO>> updateHotel(
            @PathVariable UUID id,
            @Valid @RequestBody HotelDTO hotelDTO) {
        HotelDTO updatedHotel = hotelService.updateHotel(id, hotelDTO);
        return ResponseEntity.ok(ApiResponse.success("Hotel updated successfully", updatedHotel));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelDTO>> getHotelById(@PathVariable UUID id) {
        HotelDTO hotel = hotelService.getHotelById(id);
        return ResponseEntity.ok(ApiResponse.success(hotel));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HotelDTO>>> getAllHotels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("DESC") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<HotelDTO> hotels = hotelService.getAllHotels(pageable);
        return ResponseEntity.ok(ApiResponse.success(hotels));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<Page<HotelDTO>>> getHotelsByCity(
            @PathVariable String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<HotelDTO> hotels = hotelService.getHotelsByCity(city, pageable);
        return ResponseEntity.ok(ApiResponse.success(hotels));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<HotelDTO>>> searchHotels(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<HotelDTO> hotels = hotelService.searchHotels(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(hotels));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHotel(@PathVariable UUID id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel deleted successfully", null));
    }
}

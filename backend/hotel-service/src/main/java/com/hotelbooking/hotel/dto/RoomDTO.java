package com.hotelbooking.hotel.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDTO {
    private UUID id;
    
    private UUID hotelId;
    private String hotelName;

    @NotBlank(message = "Room type is required")
    private String roomType;

    private String roomNumber;
    private String description;

    @NotNull(message = "Max guests is required")
    @Min(value = 1)
    private Integer maxGuests;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0")
    private BigDecimal sizeSqm;

    private List<String> amenities;
    private List<String> images;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

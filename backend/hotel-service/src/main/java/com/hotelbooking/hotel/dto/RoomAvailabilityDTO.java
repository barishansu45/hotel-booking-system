package com.hotelbooking.hotel.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAvailabilityDTO {
    private UUID id;
    
    private UUID roomId;
    private String roomType;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Total capacity is required")
    @Min(value = 1)
    private Integer totalCapacity;

    @NotNull(message = "Available capacity is required")
    @Min(value = 0)
    private Integer availableCapacity;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal priceOverride;

    private BigDecimal effectivePrice;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

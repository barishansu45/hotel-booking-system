package com.hotelbooking.hotel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityRequest {
    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Total capacity is required")
    private Integer totalCapacity;

    @NotNull(message = "Available capacity is required")
    private Integer availableCapacity;
}

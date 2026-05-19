package com.hotelbooking.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityAlertDTO {
    private UUID roomId;
    private UUID hotelId;
    private String hotelName;
    private String city;
    private LocalDate date;
    private int totalCapacity;
    private int availableCapacity;
    /** availableCapacity / totalCapacity */
    private double availableToTotalRatio;
}

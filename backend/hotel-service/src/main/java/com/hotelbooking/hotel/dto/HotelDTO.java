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
public class HotelDTO {
    private UUID id;

    @NotBlank(message = "Hotel name is required")
    @Size(max = 255)
    private String name;

    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Country is required")
    private String country;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    private BigDecimal starRating;

    private String phone;

    @Email
    private String email;

    private List<String> amenities;
    private List<String> images;
    private UUID adminUserId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer totalRooms;
    private BigDecimal averageRating;
    private Integer totalReviews;
}

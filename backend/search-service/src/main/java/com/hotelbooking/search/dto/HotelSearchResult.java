package com.hotelbooking.search.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSearchResult {
    private UUID hotelId;
    private String name;
    private String city;
    private String address;
    private BigDecimal starRating;
    private BigDecimal basePrice;
    private BigDecimal discountedPrice;
    private List<String> amenities;
    private String mainImage;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean hasAvailability;
}

package com.hotelbooking.search.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {
    private String destination;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guests;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minStarRating;
    private Boolean isLoggedIn;
}

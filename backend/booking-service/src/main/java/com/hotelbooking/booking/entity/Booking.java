package com.hotelbooking.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID userId;
    private UUID hotelId;
    private UUID roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numGuests;
    private BigDecimal totalPrice;
    private BigDecimal discountApplied;
    private BigDecimal finalPrice;
    
    @Enumerated(EnumType.STRING)
    private BookingStatus status;
    
    private String specialRequests;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED
    }
}

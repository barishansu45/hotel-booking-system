package com.hotelbooking.hotel.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms", indexes = {
    @Index(name = "idx_rooms_hotel", columnList = "hotel_id"),
    @Index(name = "idx_rooms_type", columnList = "room_type")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @NotBlank(message = "Room type is required")
    @Size(max = 50)
    @Column(name = "room_type", nullable = false, length = 50)
    private String roomType;

    @Size(max = 20)
    @Column(name = "room_number", length = 20)
    private String roomNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Max guests is required")
    @Min(value = 1, message = "Max guests must be at least 1")
    @Column(name = "max_guests", nullable = false)
    @Builder.Default
    private Integer maxGuests = 2;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", message = "Size must be positive")
    @Column(name = "size_sqm", precision = 6, scale = 2)
    private BigDecimal sizeSqm;

    @Column(name = "amenities", columnDefinition = "text[]")
    @Builder.Default
    private String[] amenities = new String[0];

    @Column(name = "images", columnDefinition = "text[]")
    @Builder.Default
    private String[] images = new String[0];

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoomAvailability> availabilities = new ArrayList<>();

    public void addAvailability(RoomAvailability availability) {
        availabilities.add(availability);
        availability.setRoom(this);
    }

    public void removeAvailability(RoomAvailability availability) {
        availabilities.remove(availability);
        availability.setRoom(null);
    }
}

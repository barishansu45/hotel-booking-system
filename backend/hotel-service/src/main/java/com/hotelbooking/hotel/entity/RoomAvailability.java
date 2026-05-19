package com.hotelbooking.hotel.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_availability", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_date", columnNames = {"room_id", "date"})
    },
    indexes = {
        @Index(name = "idx_room_availability_room_date", columnList = "room_id, date"),
        @Index(name = "idx_room_availability_date", columnList = "date")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;

    @NotNull(message = "Total capacity is required")
    @Min(value = 1, message = "Total capacity must be at least 1")
    @Column(name = "total_capacity", nullable = false)
    @Builder.Default
    private Integer totalCapacity = 1;

    @NotNull(message = "Available capacity is required")
    @Min(value = 0, message = "Available capacity cannot be negative")
    @Column(name = "available_capacity", nullable = false)
    @Builder.Default
    private Integer availableCapacity = 1;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price override must be greater than 0")
    @Column(name = "price_override", precision = 10, scale = 2)
    private BigDecimal priceOverride;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isAvailable() {
        return availableCapacity > 0;
    }

    public BigDecimal getEffectivePrice() {
        return priceOverride != null ? priceOverride : room.getBasePrice();
    }

    public void decreaseCapacity() {
        if (availableCapacity > 0) {
            availableCapacity--;
        }
    }

    public void increaseCapacity() {
        if (availableCapacity < totalCapacity) {
            availableCapacity++;
        }
    }
}

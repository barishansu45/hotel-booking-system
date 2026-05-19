package com.hotelbooking.booking.repository;

import com.hotelbooking.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Booking> findByUserId(UUID userId, Pageable pageable);

    List<Booking> findByHotelId(UUID hotelId);
}

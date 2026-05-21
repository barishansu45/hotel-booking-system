package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByIdAndIsActiveTrue(UUID id);

    List<Room> findByHotelIdAndIsActiveTrue(UUID hotelId);

    Page<Room> findByHotelIdAndIsActiveTrue(UUID hotelId, Pageable pageable);

    List<Room> findByHotelIdAndRoomTypeAndIsActiveTrue(UUID hotelId, String roomType);

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.isActive = true AND r.basePrice BETWEEN :minPrice AND :maxPrice")
    List<Room> findByHotelIdAndPriceRange(@Param("hotelId") UUID hotelId, 
                                          @Param("minPrice") Double minPrice, 
                                          @Param("maxPrice") Double maxPrice);

    long countByHotelIdAndIsActiveTrue(UUID hotelId);

    @Query("SELECT MIN(r.basePrice) FROM Room r WHERE r.hotel.id = :hotelId AND r.isActive = true")
    java.math.BigDecimal findMinBasePriceByHotelId(@Param("hotelId") UUID hotelId);
}

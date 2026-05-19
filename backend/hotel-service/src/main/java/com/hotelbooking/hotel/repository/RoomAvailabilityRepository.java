package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.RoomAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, UUID> {

    Optional<RoomAvailability> findByRoomIdAndDate(UUID roomId, LocalDate date);

    List<RoomAvailability> findByRoomIdAndDateBetween(UUID roomId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT ra FROM RoomAvailability ra WHERE ra.room.id = :roomId AND ra.date BETWEEN :startDate AND :endDate AND ra.availableCapacity > 0")
    List<RoomAvailability> findAvailableByRoomIdAndDateRange(@Param("roomId") UUID roomId, 
                                                              @Param("startDate") LocalDate startDate, 
                                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT ra FROM RoomAvailability ra WHERE ra.room.hotel.id = :hotelId AND ra.date BETWEEN :startDate AND :endDate AND ra.availableCapacity > 0")
    List<RoomAvailability> findAvailableByHotelIdAndDateRange(@Param("hotelId") UUID hotelId, 
                                                               @Param("startDate") LocalDate startDate, 
                                                               @Param("endDate") LocalDate endDate);

    boolean existsByRoomIdAndDate(UUID roomId, LocalDate date);

    @Query("SELECT COUNT(ra) FROM RoomAvailability ra WHERE ra.room.hotel.id = :hotelId AND ra.date = :date AND ra.availableCapacity > 0")
    long countAvailableRoomsByHotelAndDate(@Param("hotelId") UUID hotelId, @Param("date") LocalDate date);

    @Query("""
        SELECT ra FROM RoomAvailability ra
        JOIN FETCH ra.room r
        JOIN FETCH r.hotel h
        WHERE ra.date BETWEEN :startDate AND :endDate
          AND ra.totalCapacity > 0
          AND (1.0 * ra.availableCapacity / ra.totalCapacity) < :maxAvailableRatio
        ORDER BY ra.date, h.name, r.id
        """)
    List<RoomAvailability> findWhereAvailableRatioBelow(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("maxAvailableRatio") double maxAvailableRatio);
}

package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.Hotel;
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
public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    Optional<Hotel> findByIdAndIsActiveTrue(UUID id);

    Page<Hotel> findByIsActiveTrue(Pageable pageable);

    Page<Hotel> findByCityAndIsActiveTrue(String city, Pageable pageable);

    Page<Hotel> findByCountryAndIsActiveTrue(String country, Pageable pageable);

    List<Hotel> findByAdminUserId(UUID adminUserId);

    @Query("SELECT h FROM Hotel h WHERE h.isActive = true AND " +
           "LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.country) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Hotel> searchHotels(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT h FROM Hotel h WHERE h.isActive = true AND h.starRating >= :minRating")
    Page<Hotel> findByMinStarRating(@Param("minRating") Double minRating, Pageable pageable);

    boolean existsByIdAndAdminUserId(UUID hotelId, UUID adminUserId);
}

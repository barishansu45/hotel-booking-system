package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.BookingRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final MessageQueueService messageQueueService;
    private final HotelAvailabilityClient hotelAvailabilityClient;

    @Transactional
    public Booking createBooking(BookingRequest request) {
        log.info("Creating booking for hotel: {}", request.getHotelId());
        
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal finalPrice = request.getTotalPrice().subtract(discount);
        
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .hotelId(request.getHotelId())
                .roomId(request.getRoomId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .numGuests(request.getNumGuests())
                .totalPrice(request.getTotalPrice())
                .discountApplied(discount)
                .finalPrice(finalPrice)
                .status(Booking.BookingStatus.CONFIRMED)
                .specialRequests(request.getSpecialRequests())
                .createdAt(LocalDateTime.now())
                .build();
        
        Booking savedBooking = bookingRepository.save(booking);

        try {
            hotelAvailabilityClient.decreaseForStay(
                request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());
        } catch (Exception e) {
            log.error("Could not decrease room availability in hotel-service for booking {}", savedBooking.getId(), e);
        }

        messageQueueService.sendBookingNotification(savedBooking);

        return savedBooking;
    }

    @Transactional(readOnly = true)
    public List<Booking> listBookingsForUser(UUID userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Booking> listBookingsForUser(UUID userId,
            org.springframework.data.domain.Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable);
    }
}

package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.AvailabilityRequest;
import com.hotelbooking.hotel.dto.CapacityAlertDTO;
import com.hotelbooking.hotel.dto.RoomAvailabilityDTO;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.RoomAvailability;
import com.hotelbooking.hotel.exception.BadRequestException;
import com.hotelbooking.hotel.exception.ResourceNotFoundException;
import com.hotelbooking.hotel.repository.RoomAvailabilityRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomAvailabilityService {

    private final RoomAvailabilityRepository availabilityRepository;
    private final RoomRepository roomRepository;

    public List<RoomAvailabilityDTO> setRoomAvailability(AvailabilityRequest request) {
        log.info("Setting availability for room: {} from {} to {}", 
                request.getRoomId(), request.getStartDate(), request.getEndDate());
        
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start date must be before end date");
        }

        Room room = roomRepository.findByIdAndIsActiveTrue(request.getRoomId())
                .orElseThrow(() -> ResourceNotFoundException.room(request.getRoomId()));

        List<RoomAvailability> availabilities = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();

        while (!currentDate.isAfter(request.getEndDate())) {
            final LocalDate dateToCheck = currentDate;
            
            RoomAvailability availability = availabilityRepository
                    .findByRoomIdAndDate(request.getRoomId(), dateToCheck)
                    .orElse(RoomAvailability.builder()
                            .room(room)
                            .date(dateToCheck)
                            .build());

            availability.setTotalCapacity(request.getTotalCapacity());
            availability.setAvailableCapacity(request.getAvailableCapacity());

            availabilities.add(availabilityRepository.save(availability));
            currentDate = currentDate.plusDays(1);
        }

        log.info("Availability set for {} days", availabilities.size());
        return availabilities.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityDTO> getAvailabilityByDateRange(UUID roomId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching availability for room: {} from {} to {}", roomId, startDate, endDate);
        
        if (!roomRepository.existsById(roomId)) {
            throw ResourceNotFoundException.room(roomId);
        }

        return availabilityRepository.findByRoomIdAndDateBetween(roomId, startDate, endDate)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityDTO> getAvailableRoomsByDateRange(UUID roomId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching available rooms for room: {} from {} to {}", roomId, startDate, endDate);
        
        return availabilityRepository.findAvailableByRoomIdAndDateRange(roomId, startDate, endDate)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomAvailabilityDTO getAvailabilityByDate(UUID roomId, LocalDate date) {
        log.info("Fetching availability for room: {} on date: {}", roomId, date);
        
        RoomAvailability availability = availabilityRepository
                .findByRoomIdAndDate(roomId, date)
                .orElseThrow(() -> ResourceNotFoundException.availability(roomId, date.toString()));
        
        return mapToDTO(availability);
    }

    public void decreaseAvailability(UUID roomId, LocalDate startDate, LocalDate endDate) {
        log.info("Decreasing availability for room: {} from {} to {}", roomId, startDate, endDate);
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate dateToUpdate = currentDate;
            
            RoomAvailability availability = availabilityRepository
                    .findByRoomIdAndDate(roomId, dateToUpdate)
                    .orElseThrow(() -> ResourceNotFoundException.availability(roomId, dateToUpdate.toString()));
            
            if (availability.getAvailableCapacity() <= 0) {
                throw new BadRequestException("No available capacity for date: " + dateToUpdate);
            }
            
            availability.decreaseCapacity();
            availabilityRepository.save(availability);
            
            currentDate = currentDate.plusDays(1);
        }
        
        log.info("Availability decreased successfully");
    }

    public void increaseAvailability(UUID roomId, LocalDate startDate, LocalDate endDate) {
        log.info("Increasing availability for room: {} from {} to {}", roomId, startDate, endDate);
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate dateToUpdate = currentDate;
            
            RoomAvailability availability = availabilityRepository
                    .findByRoomIdAndDate(roomId, dateToUpdate)
                    .orElseThrow(() -> ResourceNotFoundException.availability(roomId, dateToUpdate.toString()));
            
            availability.increaseCapacity();
            availabilityRepository.save(availability);
            
            currentDate = currentDate.plusDays(1);
        }
        
        log.info("Availability increased successfully");
    }

    @Transactional(readOnly = true)
    public List<CapacityAlertDTO> findLowCapacityAlerts(LocalDate startDate, LocalDate endDate, int thresholdPercent) {
        double maxRatio = thresholdPercent / 100.0;
        return availabilityRepository.findWhereAvailableRatioBelow(startDate, endDate, maxRatio).stream()
            .map(this::toCapacityAlert)
            .collect(Collectors.toList());
    }

    private CapacityAlertDTO toCapacityAlert(RoomAvailability ra) {
        double ratio = ra.getTotalCapacity() > 0
            ? (double) ra.getAvailableCapacity() / ra.getTotalCapacity()
            : 0.0;
        return CapacityAlertDTO.builder()
            .roomId(ra.getRoom().getId())
            .hotelId(ra.getRoom().getHotel().getId())
            .hotelName(ra.getRoom().getHotel().getName())
            .city(ra.getRoom().getHotel().getCity())
            .date(ra.getDate())
            .totalCapacity(ra.getTotalCapacity())
            .availableCapacity(ra.getAvailableCapacity())
            .availableToTotalRatio(ratio)
            .build();
    }

    private RoomAvailabilityDTO mapToDTO(RoomAvailability availability) {
        return RoomAvailabilityDTO.builder()
                .id(availability.getId())
                .roomId(availability.getRoom().getId())
                .roomType(availability.getRoom().getRoomType())
                .date(availability.getDate())
                .totalCapacity(availability.getTotalCapacity())
                .availableCapacity(availability.getAvailableCapacity())
                .priceOverride(availability.getPriceOverride())
                .effectivePrice(availability.getEffectivePrice())
                .isAvailable(availability.isAvailable())
                .createdAt(availability.getCreatedAt())
                .updatedAt(availability.getUpdatedAt())
                .build();
    }
}

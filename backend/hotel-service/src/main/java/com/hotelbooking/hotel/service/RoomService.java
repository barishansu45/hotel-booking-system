package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.RoomDTO;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.exception.BadRequestException;
import com.hotelbooking.hotel.exception.ResourceNotFoundException;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    public RoomDTO createRoom(RoomDTO roomDTO) {
        log.info("Creating new room for hotel: {}", roomDTO.getHotelId());
        
        Hotel hotel = hotelRepository.findByIdAndIsActiveTrue(roomDTO.getHotelId())
                .orElseThrow(() -> ResourceNotFoundException.hotel(roomDTO.getHotelId()));

        Room room = Room.builder()
                .hotel(hotel)
                .roomType(roomDTO.getRoomType())
                .roomNumber(roomDTO.getRoomNumber())
                .description(roomDTO.getDescription())
                .maxGuests(roomDTO.getMaxGuests())
                .basePrice(roomDTO.getBasePrice())
                .sizeSqm(roomDTO.getSizeSqm())
                .amenities(roomDTO.getAmenities() != null ? 
                    roomDTO.getAmenities().toArray(new String[0]) : new String[0])
                .images(roomDTO.getImages() != null ? 
                    roomDTO.getImages().toArray(new String[0]) : new String[0])
                .isActive(true)
                .build();

        Room savedRoom = roomRepository.save(room);
        log.info("Room created successfully with id: {}", savedRoom.getId());
        
        return mapToDTO(savedRoom);
    }

    public RoomDTO updateRoom(UUID id, RoomDTO roomDTO) {
        log.info("Updating room with id: {}", id);
        
        Room room = roomRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.room(id));

        room.setRoomType(roomDTO.getRoomType());
        room.setRoomNumber(roomDTO.getRoomNumber());
        room.setDescription(roomDTO.getDescription());
        room.setMaxGuests(roomDTO.getMaxGuests());
        room.setBasePrice(roomDTO.getBasePrice());
        room.setSizeSqm(roomDTO.getSizeSqm());
        
        if (roomDTO.getAmenities() != null) {
            room.setAmenities(roomDTO.getAmenities().toArray(new String[0]));
        }
        
        if (roomDTO.getImages() != null) {
            room.setImages(roomDTO.getImages().toArray(new String[0]));
        }

        Room updatedRoom = roomRepository.save(room);
        log.info("Room updated successfully: {}", id);
        
        return mapToDTO(updatedRoom);
    }

    @Transactional(readOnly = true)
    public RoomDTO getRoomById(UUID id) {
        log.info("Fetching room with id: {}", id);
        
        Room room = roomRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.room(id));
        
        return mapToDTO(room);
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getRoomsByHotelId(UUID hotelId) {
        log.info("Fetching rooms for hotel: {}", hotelId);
        
        if (!hotelRepository.existsById(hotelId)) {
            throw ResourceNotFoundException.hotel(hotelId);
        }
        
        return roomRepository.findByHotelIdAndIsActiveTrue(hotelId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<RoomDTO> getRoomsByHotelIdPaginated(UUID hotelId, Pageable pageable) {
        log.info("Fetching paginated rooms for hotel: {}", hotelId);
        
        if (!hotelRepository.existsById(hotelId)) {
            throw ResourceNotFoundException.hotel(hotelId);
        }
        
        return roomRepository.findByHotelIdAndIsActiveTrue(hotelId, pageable)
                .map(this::mapToDTO);
    }

    public void deleteRoom(UUID id) {
        log.info("Deleting room with id: {}", id);
        
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.room(id));
        
        room.setIsActive(false);
        roomRepository.save(room);
        
        log.info("Room soft deleted: {}", id);
    }

    private RoomDTO mapToDTO(Room room) {
        return RoomDTO.builder()
                .id(room.getId())
                .hotelId(room.getHotel().getId())
                .hotelName(room.getHotel().getName())
                .roomType(room.getRoomType())
                .roomNumber(room.getRoomNumber())
                .description(room.getDescription())
                .maxGuests(room.getMaxGuests())
                .basePrice(room.getBasePrice())
                .sizeSqm(room.getSizeSqm())
                .amenities(room.getAmenities() != null ? Arrays.asList(room.getAmenities()) : null)
                .images(room.getImages() != null ? Arrays.asList(room.getImages()) : null)
                .isActive(room.getIsActive())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}

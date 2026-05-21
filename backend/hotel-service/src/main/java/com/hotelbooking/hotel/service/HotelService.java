package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.HotelDTO;
import com.hotelbooking.hotel.entity.Hotel;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public HotelDTO createHotel(HotelDTO hotelDTO) {
        log.info("Creating new hotel: {}", hotelDTO.getName());
        
        Hotel hotel = Hotel.builder()
                .name(hotelDTO.getName())
                .description(hotelDTO.getDescription())
                .address(hotelDTO.getAddress())
                .city(hotelDTO.getCity())
                .country(hotelDTO.getCountry())
                .latitude(hotelDTO.getLatitude())
                .longitude(hotelDTO.getLongitude())
                .starRating(hotelDTO.getStarRating())
                .phone(hotelDTO.getPhone())
                .email(hotelDTO.getEmail())
                .amenities(hotelDTO.getAmenities() != null ? 
                    hotelDTO.getAmenities().toArray(new String[0]) : new String[0])
                .images(hotelDTO.getImages() != null ? 
                    hotelDTO.getImages().toArray(new String[0]) : new String[0])
                .adminUserId(hotelDTO.getAdminUserId())
                .isActive(true)
                .build();

        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel created successfully with id: {}", savedHotel.getId());
        
        return mapToDTO(savedHotel);
    }

    public HotelDTO updateHotel(UUID id, HotelDTO hotelDTO) {
        log.info("Updating hotel with id: {}", id);
        
        Hotel hotel = hotelRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.hotel(id));

        hotel.setName(hotelDTO.getName());
        hotel.setDescription(hotelDTO.getDescription());
        hotel.setAddress(hotelDTO.getAddress());
        hotel.setCity(hotelDTO.getCity());
        hotel.setCountry(hotelDTO.getCountry());
        hotel.setLatitude(hotelDTO.getLatitude());
        hotel.setLongitude(hotelDTO.getLongitude());
        hotel.setStarRating(hotelDTO.getStarRating());
        hotel.setPhone(hotelDTO.getPhone());
        hotel.setEmail(hotelDTO.getEmail());
        
        if (hotelDTO.getAmenities() != null) {
            hotel.setAmenities(hotelDTO.getAmenities().toArray(new String[0]));
        }
        
        if (hotelDTO.getImages() != null) {
            hotel.setImages(hotelDTO.getImages().toArray(new String[0]));
        }

        Hotel updatedHotel = hotelRepository.save(hotel);
        log.info("Hotel updated successfully: {}", id);
        
        return mapToDTO(updatedHotel);
    }

    @Transactional(readOnly = true)
    public HotelDTO getHotelById(UUID id) {
        log.info("Fetching hotel with id: {}", id);
        
        Hotel hotel = hotelRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.hotel(id));
        
        return mapToDTO(hotel);
    }

    @Transactional(readOnly = true)
    public Page<HotelDTO> getAllHotels(Pageable pageable) {
        log.info("Fetching all hotels, page: {}", pageable.getPageNumber());
        return hotelRepository.findByIsActiveTrue(pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<HotelDTO> getHotelsByCity(String city, Pageable pageable) {
        log.info("Fetching hotels in city: {}", city);
        return hotelRepository.findByCityAndIsActiveTrue(city, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<HotelDTO> searchHotels(String keyword, Pageable pageable) {
        log.info("Searching hotels with keyword: {}", keyword);
        return hotelRepository.searchHotels(keyword, pageable)
                .map(this::mapToDTO);
    }

    public void deleteHotel(UUID id) {
        log.info("Deleting hotel with id: {}", id);
        
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.hotel(id));
        
        hotel.setIsActive(false);
        hotelRepository.save(hotel);
        
        log.info("Hotel soft deleted: {}", id);
    }

    private HotelDTO mapToDTO(Hotel hotel) {
        long totalRooms = roomRepository.countByHotelIdAndIsActiveTrue(hotel.getId());
        java.math.BigDecimal minPrice = roomRepository.findMinBasePriceByHotelId(hotel.getId());

        return HotelDTO.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .description(hotel.getDescription())
                .address(hotel.getAddress())
                .city(hotel.getCity())
                .country(hotel.getCountry())
                .latitude(hotel.getLatitude())
                .longitude(hotel.getLongitude())
                .starRating(hotel.getStarRating())
                .phone(hotel.getPhone())
                .email(hotel.getEmail())
                .amenities(hotel.getAmenities() != null ? Arrays.asList(hotel.getAmenities()) : null)
                .images(hotel.getImages() != null ? Arrays.asList(hotel.getImages()) : null)
                .adminUserId(hotel.getAdminUserId())
                .isActive(hotel.getIsActive())
                .createdAt(hotel.getCreatedAt())
                .updatedAt(hotel.getUpdatedAt())
                .totalRooms((int) totalRooms)
                .minPrice(minPrice)
                .build();
    }
}

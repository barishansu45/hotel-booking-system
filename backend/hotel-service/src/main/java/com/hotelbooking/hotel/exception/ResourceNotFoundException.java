package com.hotelbooking.hotel.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException hotel(UUID id) {
        return new ResourceNotFoundException("Hotel not found with id: " + id);
    }

    public static ResourceNotFoundException room(UUID id) {
        return new ResourceNotFoundException("Room not found with id: " + id);
    }

    public static ResourceNotFoundException availability(UUID roomId, String date) {
        return new ResourceNotFoundException("Availability not found for room: " + roomId + " on date: " + date);
    }
}

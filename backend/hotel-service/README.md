# Hotel Service

Microservice for managing hotels and rooms in the Hotel Booking System.

## Features

- Hotel CRUD operations
- Room management
- Room availability management
- Admin authentication support
- RESTful APIs with versioning
- PostgreSQL database
- Flyway migrations

## API Endpoints

### Hotels
- `POST /api/v1/hotels` - Create hotel (**JWT ADMIN**)
- `GET /api/v1/hotels` - Get all hotels (paginated)
- `GET /api/v1/hotels/{id}` - Get hotel by ID
- `PUT /api/v1/hotels/{id}` - Update hotel (**JWT ADMIN**)
- `DELETE /api/v1/hotels/{id}` - Delete hotel (**JWT ADMIN**)
- `GET /api/v1/hotels/city/{city}` - Get hotels by city
- `GET /api/v1/hotels/search?keyword={keyword}` - Search hotels

**Admin namespace** (`/api/v1/admin/**`): same hotel/room mutations plus `POST /admin/availability` — all require **JWT ADMIN**.

### Rooms
- `POST /api/v1/rooms` - Create room (**JWT ADMIN**)
- `GET /api/v1/rooms/{id}` - Get room by ID
- `GET /api/v1/rooms/hotel/{hotelId}` - Get rooms by hotel
- `PUT /api/v1/rooms/{id}` - Update room (**JWT ADMIN**)
- `DELETE /api/v1/rooms/{id}` - Delete room (**JWT ADMIN**)

### Availability
- `POST /api/v1/admin/availability` - Set room availability for a date range (**JWT ADMIN**)
- `GET /api/v1/availability/room/{roomId}?startDate=&endDate=` - Get availability (public)
- `PATCH /api/v1/availability/room/{roomId}/decrease` - Decrease capacity (used by booking-service)
- `PATCH /api/v1/availability/room/{roomId}/increase` - Increase capacity

## Running Locally

```bash
# Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/hoteldb
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres

# Run the application
mvn spring-boot:run
```

## Environment Variables

- `DATABASE_URL` - PostgreSQL connection URL
- `DATABASE_USERNAME` - Database username
- `DATABASE_PASSWORD` - Database password
- `SERVER_PORT` - Server port (default: 8081)
- `ALLOWED_ORIGINS` - CORS allowed origins (default: http://localhost:3000)

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Spring Data JPA
- PostgreSQL
- Flyway
- Lombok
- MapStruct

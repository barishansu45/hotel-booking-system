-- Booking Service Schema

CREATE TABLE IF NOT EXISTS bookings (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL,
    hotel_id       UUID         NOT NULL,
    room_id        UUID         NOT NULL,
    check_in_date  DATE         NOT NULL,
    check_out_date DATE         NOT NULL,
    num_guests     INTEGER      NOT NULL DEFAULT 1,
    total_price    NUMERIC(12, 2) NOT NULL,
    discount_applied NUMERIC(12, 2) NOT NULL DEFAULT 0,
    final_price    NUMERIC(12, 2) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED',
    special_requests TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bookings_user_id  ON bookings (user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_hotel_id ON bookings (hotel_id);
CREATE INDEX IF NOT EXISTS idx_bookings_room_id  ON bookings (room_id);
CREATE INDEX IF NOT EXISTS idx_bookings_dates    ON bookings (check_in_date, check_out_date);

-- Sample Hotels Data
INSERT INTO hotels (name, description, city, country, address, star_rating, latitude, longitude, amenities, images) VALUES
('Grand Istanbul Hotel', 'Luxury hotel in the heart of Istanbul with stunning Bosphorus views', 'Istanbul', 'Turkey', 'Sultanahmet Square, 34122', 5.0, 41.0082, 28.9784, ARRAY['WiFi', 'Pool', 'Spa', 'Breakfast', 'Parking'], ARRAY['https://images.unsplash.com/photo-1566073771259-6a8506099945']),

('Antalya Beach Resort', 'Beautiful beachfront resort with all-inclusive package', 'Antalya', 'Turkey', 'Lara Beach, 07100', 4.5, 36.8969, 30.7133, ARRAY['WiFi', 'Pool', 'Beach', 'Breakfast', 'Restaurant'], ARRAY['https://images.unsplash.com/photo-1520250497591-112f2f40a3f4']),

('Cappadocia Cave Hotel', 'Unique cave hotel with hot air balloon views', 'Cappadocia', 'Turkey', 'Göreme, 50180', 4.0, 38.6431, 34.8286, ARRAY['WiFi', 'Breakfast', 'Terrace', 'Cave Rooms'], ARRAY['https://images.unsplash.com/photo-1542314831-068cd1dbfeeb']),

('Izmir Seaside Hotel', 'Modern hotel with sea views and city center location', 'Izmir', 'Turkey', 'Alsancak, 35220', 4.0, 38.4192, 27.1287, ARRAY['WiFi', 'Breakfast', 'Sea View', 'Restaurant'], ARRAY['https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9']),

('Ankara Business Hotel', 'Perfect for business travelers in capital city', 'Ankara', 'Turkey', 'Çankaya, 06680', 3.5, 39.9334, 32.8597, ARRAY['WiFi', 'Business Center', 'Breakfast', 'Meeting Rooms'], ARRAY['https://images.unsplash.com/photo-1445019980597-93fa8acb246c']);

-- Sample Rooms for Grand Istanbul Hotel
INSERT INTO rooms (hotel_id, room_type, max_guests, base_price, description, amenities) 
SELECT id, 'Deluxe Room', 2, 250.00, 'Spacious room with Bosphorus view', ARRAY['King Bed', 'Mini Bar', 'City View', 'WiFi']
FROM hotels WHERE name = 'Grand Istanbul Hotel';

INSERT INTO rooms (hotel_id, room_type, max_guests, base_price, description, amenities)
SELECT id, 'Suite', 4, 400.00, 'Luxury suite with private balcony', ARRAY['King Bed', 'Living Room', 'Balcony', 'Mini Bar']
FROM hotels WHERE name = 'Grand Istanbul Hotel';

-- Sample Rooms for Antalya Beach Resort
INSERT INTO rooms (hotel_id, room_type, max_guests, base_price, description, amenities)
SELECT id, 'Beach View Room', 2, 180.00, 'Room with direct beach access', ARRAY['Queen Bed', 'Beach View', 'Mini Bar']
FROM hotels WHERE name = 'Antalya Beach Resort';

-- Sample Rooms for Cappadocia Cave Hotel
INSERT INTO rooms (hotel_id, room_type, max_guests, base_price, description, amenities)
SELECT id, 'Cave Room', 2, 150.00, 'Authentic cave room experience', ARRAY['Cave Architecture', 'Breakfast', 'Terrace']
FROM hotels WHERE name = 'Cappadocia Cave Hotel';

-- Sample Rooms for Izmir Seaside Hotel
INSERT INTO rooms (hotel_id, room_type, max_guests, base_price, description, amenities)
SELECT id, 'Sea View Room', 2, 120.00, 'Modern room with sea view', ARRAY['Double Bed', 'Sea View', 'WiFi']
FROM hotels WHERE name = 'Izmir Seaside Hotel';

-- Sample Rooms for Ankara Business Hotel
INSERT INTO rooms (hotel_id, room_type, max_guests, base_price, description, amenities)
SELECT id, 'Standard Room', 2, 100.00, 'Comfortable business room', ARRAY['Double Bed', 'Work Desk', 'WiFi']
FROM hotels WHERE name = 'Ankara Business Hotel';

-- Sample Room Availability (Next 30 days, 5 rooms available per room type per day)
INSERT INTO room_availability (room_id, date, total_capacity, available_capacity)
SELECT r.id, CURRENT_DATE + (n || ' days')::interval, 5, 5
FROM rooms r
CROSS JOIN generate_series(0, 30) n;

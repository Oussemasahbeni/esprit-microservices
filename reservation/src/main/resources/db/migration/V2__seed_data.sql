-- Seed Rooms
INSERT INTO restaurant_room (name, description, floor_number, active) VALUES
('Main Room', 'The main dining area of the restaurant', 0, true),
('Terrace', 'Outdoor rooftop terrace dining', 1, true),
('VIP Area', 'Private lounge for premium guests', 0, true);

-- Seed Tables for Main Room (room_id = 1)
INSERT INTO restaurant_table (room_id, table_number, capacity, x_position, y_position, shape, status, active) VALUES
(1, 'T01', 2, 10, 10, 'ROUND', 'AVAILABLE', true),
(1, 'T02', 2, 20, 10, 'ROUND', 'AVAILABLE', true),
(1, 'T03', 4, 10, 20, 'SQUARE', 'AVAILABLE', true),
(1, 'T04', 4, 20, 20, 'SQUARE', 'AVAILABLE', true),
(1, 'T05', 6, 15, 30, 'RECTANGLE', 'AVAILABLE', true);

-- Seed Tables for Terrace (room_id = 2)
INSERT INTO restaurant_table (room_id, table_number, capacity, x_position, y_position, shape, status, active) VALUES
(2, 'TE01', 2, 5, 5, 'ROUND', 'AVAILABLE', true),
(2, 'TE02', 4, 15, 5, 'SQUARE', 'AVAILABLE', true),
(2, 'TE03', 4, 25, 5, 'SQUARE', 'AVAILABLE', true);

-- Seed Tables for VIP Area (room_id = 3)
INSERT INTO restaurant_table (room_id, table_number, capacity, x_position, y_position, shape, status, active) VALUES
(3, 'VIP01', 8, 50, 50, 'RECTANGLE', 'AVAILABLE', true),
(3, 'VIP02', 8, 100, 50, 'RECTANGLE', 'AVAILABLE', true);

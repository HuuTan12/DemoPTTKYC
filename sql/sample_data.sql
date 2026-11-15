-- =====================================================================
-- FASTSHIP - SAMPLE DATA FOR DEMO (Chapter 7.3 - Table 41 & Table 42)
-- =====================================================================

USE fastship;

-- 1. Service Types (Table 41)
-- Standard: base fee 20,000 VND, 5,000 VND/kg, max 30 kg
-- Express:  base fee 35,000 VND, 8,000 VND/kg, max 20 kg
INSERT INTO ServiceType (service_type_id, name, base_fee, fee_per_kg, max_weight_kg, is_active) VALUES
(1, 'Standard', 20000, 5000, 30.00, TRUE),
(2, 'Express',  35000, 8000, 20.00, TRUE);

-- 2. Customer (Table 41)
INSERT INTO Customer (customer_id, full_name, phone, email) VALUES
(1, 'Nguyen Van An', '0901234567', 'nguyenvanan@email.com');

-- 3. Saved Address for Customer 1 & Receiver Address for pre-loaded sample order
INSERT INTO Address (address_id, customer_id, contact_name, phone, street, ward, district, city, area) VALUES
(1, 1,    'Nguyen Van An', '0901234567', '19 Nguyen Huu Tho',  'Tan Phong Ward', 'District 7', 'Ho Chi Minh City', 'D7'),
(2, NULL, 'Pham Thu Dung', '0912000111', '45 Le Loi',          'Ben Thanh Ward', 'District 7', 'Ho Chi Minh City', 'D7');

-- 4. Drivers (Table 41)
INSERT INTO Driver (driver_id, full_name, phone, license_no, service_area, status) VALUES
(1, 'Le Van Binh',  '0903111222', 'B2-0451', 'D7', 'AVAILABLE'),
(2, 'Pham Thi Cuc', '0903222333', 'B2-0932', 'D7', 'AVAILABLE'),
(3, 'Dang Gia Bao', '0903333444', 'C-1187',  'D1', 'AVAILABLE');

-- 5. Vehicles (Table 41)
INSERT INTO Vehicle (vehicle_id, driver_id, plate_no, vehicle_type, capacity_kg) VALUES
(1, 1, '59X1-123.45', 'Motorbike', 50.00),
(2, 2, '59X2-678.90', 'Motorbike', 50.00),
(3, 3, '51C-456.78',  'Van',       500.00);

-- 6. User Accounts (Table 41 - all passwords are '123456')
INSERT INTO UserAccount (user_id, username, password_hash, full_name, role, customer_id, driver_id, is_active) VALUES
(1, 'customer01', '123456', 'Nguyen Van An',   'CUSTOMER',   1,    NULL, TRUE),
(2, 'staff01',    '123456', 'Tran Thi Lan',    'STAFF',      NULL, NULL, TRUE),
(3, 'dispatch01', '123456', 'Pham Minh Hoang', 'DISPATCHER', NULL, NULL, TRUE),
(4, 'driver01',   '123456', 'Le Van Binh',     'DRIVER',     NULL, 1,    TRUE),
(5, 'driver02',   '123456', 'Pham Thi Cuc',    'DRIVER',     NULL, 2,    TRUE),
(6, 'manager01',  '123456', 'Le Quang',        'MANAGER',    NULL, NULL, TRUE);

-- 7. Sample pre-delivered order (#1) for Demo Step 10 (UC6 - A2: COD Difference test)
INSERT INTO orders (
    order_id, customer_id, pickup_address_id, delivery_address_id, service_type_id,
    total_weight_kg, shipping_fee, cod_amount, status, note, created_at, confirmed_at
) VALUES (
    1, 1, 1, 2, 1,
    2.00, 30000, 450000, 'DELIVERED', 'Sample order for Step 10 COD Difference check',
    NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 110 MINUTE
);

INSERT INTO OrderItem (order_id, description, quantity, weight_kg) VALUES
(1, 'Headphones Box', 1, 2.00);

INSERT INTO Assignment (order_id, driver_id, assigned_by, attempt_no, status, assigned_at, responded_at) VALUES
(1, 1, 3, 1, 'COMPLETED', NOW() - INTERVAL 100 MINUTE, NOW() - INTERVAL 95 MINUTE);

INSERT INTO TrackingEvent (order_id, status, note, recorded_by, event_time) VALUES
(1, 'NEW',        'Order created by customer',          'customer01', NOW() - INTERVAL 120 MINUTE),
(1, 'CONFIRMED',  'Order confirmed by staff',           'staff01',    NOW() - INTERVAL 110 MINUTE),
(1, 'ASSIGNED',   'Assigned to driver Le Van Binh',     'dispatch01', NOW() - INTERVAL 100 MINUTE),
(1, 'PICKED_UP',  'Driver picked up parcel',            'driver01',   NOW() - INTERVAL 80 MINUTE),
(1, 'IN_TRANSIT', 'Parcel is in transit to receiver',   'driver01',   NOW() - INTERVAL 50 MINUTE),
(1, 'DELIVERED',  'Driver reported parcel delivered',   'driver01',   NOW() - INTERVAL 20 MINUTE);

INSERT INTO DeliveryProof (order_id, received_by, delivered_at, note, photo_ref, confirmed_by, confirmed_at) VALUES
(1, 'Pham Thu Dung', NOW() - INTERVAL 20 MINUTE, 'Left with receiver at front desk', 'proof_ORD000001.jpg', NULL, NULL);

INSERT INTO Payment (order_id, driver_id, cod_amount, received_amount, collected_at, status) VALUES
(1, 1, 450000, NULL, NULL, 'PENDING');


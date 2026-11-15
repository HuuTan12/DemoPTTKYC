-- =====================================================================
-- FASTSHIP - DELIVERY ORDER MANAGEMENT SYSTEM
-- Database Schema (MySQL 8) - Chapter 5 (13 Tables: Table 23 to Table 36)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS fastship
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE fastship;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS Notification;
DROP TABLE IF EXISTS Payment;
DROP TABLE IF EXISTS DeliveryProof;
DROP TABLE IF EXISTS TrackingEvent;
DROP TABLE IF EXISTS Assignment;
DROP TABLE IF EXISTS UserAccount;
DROP TABLE IF EXISTS Vehicle;
DROP TABLE IF EXISTS Driver;
DROP TABLE IF EXISTS OrderItem;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS ServiceType;
DROP TABLE IF EXISTS Address;
DROP TABLE IF EXISTS Customer;
SET FOREIGN_KEY_CHECKS = 1;

-- 5.1.1 Customer (Table 24)
CREATE TABLE Customer (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(15)  NOT NULL UNIQUE,
    email       VARCHAR(100) UNIQUE NULL
) ENGINE=InnoDB;

-- 5.1.2 Address (Table 25)
CREATE TABLE Address (
    address_id   INT AUTO_INCREMENT PRIMARY KEY,
    customer_id  INT NULL,
    contact_name VARCHAR(100) NOT NULL,
    phone        VARCHAR(15)  NOT NULL,
    street       VARCHAR(200) NOT NULL,
    ward         VARCHAR(100) NULL,
    district     VARCHAR(100) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    area         VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_address_customer
        FOREIGN KEY (customer_id) REFERENCES Customer(customer_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- 5.1.3 ServiceType (Table 26)
CREATE TABLE ServiceType (
    service_type_id INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(50)   NOT NULL UNIQUE,
    base_fee        DECIMAL(12,0) NOT NULL CHECK (base_fee >= 0),
    fee_per_kg      DECIMAL(12,0) NOT NULL CHECK (fee_per_kg >= 0),
    max_weight_kg   DECIMAL(6,2)  NOT NULL CHECK (max_weight_kg > 0),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- 5.1.4 Order / orders (Table 27)
CREATE TABLE orders (
    order_id            INT AUTO_INCREMENT PRIMARY KEY,
    customer_id         INT           NOT NULL,
    pickup_address_id   INT           NOT NULL,
    delivery_address_id INT           NOT NULL,
    service_type_id     INT           NOT NULL,
    total_weight_kg     DECIMAL(8,2)  NOT NULL CHECK (total_weight_kg > 0),
    shipping_fee        DECIMAL(12,0) NOT NULL CHECK (shipping_fee >= 0),
    cod_amount          DECIMAL(12,0) NOT NULL DEFAULT 0 CHECK (cod_amount >= 0),
    status              VARCHAR(20)   NOT NULL DEFAULT 'NEW'
        CHECK (status IN ('NEW', 'CONFIRMED', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'CANCELLED')),
    note                VARCHAR(500)  NULL,
    cancel_reason       VARCHAR(255)  NULL,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at        DATETIME      NULL,
    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES Customer(customer_id),
    CONSTRAINT fk_orders_pickup_address
        FOREIGN KEY (pickup_address_id) REFERENCES Address(address_id),
    CONSTRAINT fk_orders_delivery_address
        FOREIGN KEY (delivery_address_id) REFERENCES Address(address_id),
    CONSTRAINT fk_orders_service_type
        FOREIGN KEY (service_type_id) REFERENCES ServiceType(service_type_id)
) ENGINE=InnoDB;

-- 5.1.5 OrderItem (Table 28)
CREATE TABLE OrderItem (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id      INT          NOT NULL,
    description   VARCHAR(200) NOT NULL,
    quantity      INT          NOT NULL CHECK (quantity > 0),
    weight_kg     DECIMAL(8,2) NOT NULL CHECK (weight_kg > 0),
    CONSTRAINT fk_orderitem_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5.2.1 Driver (Table 29)
CREATE TABLE Driver (
    driver_id    INT AUTO_INCREMENT PRIMARY KEY,
    full_name    VARCHAR(100) NOT NULL,
    phone        VARCHAR(15)  NOT NULL UNIQUE,
    license_no   VARCHAR(20)  NOT NULL UNIQUE,
    service_area VARCHAR(20)  NOT NULL,
    status       VARCHAR(10)  NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'BUSY', 'OFF'))
) ENGINE=InnoDB;

-- 5.2.2 Vehicle (Table 30)
CREATE TABLE Vehicle (
    vehicle_id   INT AUTO_INCREMENT PRIMARY KEY,
    driver_id    INT          NULL,
    plate_no     VARCHAR(15)  NOT NULL UNIQUE,
    vehicle_type VARCHAR(20)  NOT NULL,
    capacity_kg  DECIMAL(8,2) NOT NULL CHECK (capacity_kg > 0),
    CONSTRAINT fk_vehicle_driver
        FOREIGN KEY (driver_id) REFERENCES Driver(driver_id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- 5.4.1 UserAccount (Table 36)
CREATE TABLE UserAccount (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(12)  NOT NULL
        CHECK (role IN ('CUSTOMER', 'STAFF', 'DISPATCHER', 'DRIVER', 'MANAGER')),
    customer_id   INT UNIQUE NULL,
    driver_id     INT UNIQUE NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_user_customer
        FOREIGN KEY (customer_id) REFERENCES Customer(customer_id),
    CONSTRAINT fk_user_driver
        FOREIGN KEY (driver_id) REFERENCES Driver(driver_id)
) ENGINE=InnoDB;

-- 5.2.3 Assignment (Table 31)
CREATE TABLE Assignment (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id      INT          NOT NULL,
    driver_id     INT          NOT NULL,
    assigned_by   INT          NOT NULL,
    attempt_no    INT          NOT NULL DEFAULT 1 CHECK (attempt_no >= 1),
    status        VARCHAR(10)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED', 'CANCELLED')),
    reject_reason VARCHAR(255) NULL,
    assigned_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at  DATETIME     NULL,
    UNIQUE KEY uk_order_attempt (order_id, attempt_no),
    CONSTRAINT fk_assignment_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_assignment_driver
        FOREIGN KEY (driver_id) REFERENCES Driver(driver_id),
    CONSTRAINT fk_assignment_user
        FOREIGN KEY (assigned_by) REFERENCES UserAccount(user_id)
) ENGINE=InnoDB;

-- 5.3.1 TrackingEvent (Table 32)
CREATE TABLE TrackingEvent (
    event_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    INT          NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    note        VARCHAR(255) NULL,
    recorded_by VARCHAR(50)  NOT NULL,
    event_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tracking_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
) ENGINE=InnoDB;

-- 5.3.2 DeliveryProof (Table 33)
CREATE TABLE DeliveryProof (
    proof_id     INT AUTO_INCREMENT PRIMARY KEY,
    order_id     INT          NOT NULL UNIQUE,
    received_by  VARCHAR(100) NOT NULL,
    delivered_at DATETIME     NOT NULL,
    note         VARCHAR(255) NULL,
    photo_ref    VARCHAR(255) NULL,
    confirmed_by INT          NULL,
    confirmed_at DATETIME     NULL,
    CONSTRAINT fk_proof_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_proof_staff
        FOREIGN KEY (confirmed_by) REFERENCES UserAccount(user_id)
) ENGINE=InnoDB;

-- 5.3.3 Payment (Table 34)
CREATE TABLE Payment (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT           NOT NULL UNIQUE,
    driver_id       INT           NOT NULL,
    cod_amount      DECIMAL(12,0) NOT NULL CHECK (cod_amount > 0),
    received_amount DECIMAL(12,0) NULL CHECK (received_amount >= 0),
    collected_at    DATETIME      NULL,
    status          VARCHAR(12)   NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'COLLECTED', 'RECONCILED', 'DIFFERENCE')),
    reconciled_by   INT           NULL,
    reconciled_at   DATETIME      NULL,
    note            VARCHAR(255)  NULL,
    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_payment_driver
        FOREIGN KEY (driver_id) REFERENCES Driver(driver_id),
    CONSTRAINT fk_payment_staff
        FOREIGN KEY (reconciled_by) REFERENCES UserAccount(user_id)
) ENGINE=InnoDB;

-- 5.3.4 Notification (Table 35)
CREATE TABLE Notification (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT          NOT NULL,
    channel         VARCHAR(10)  NOT NULL DEFAULT 'IN_APP',
    message         VARCHAR(255) NOT NULL,
    sent_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(6)   NOT NULL DEFAULT 'UNREAD'
        CHECK (status IN ('UNREAD', 'READ')),
    CONSTRAINT fk_notification_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
) ENGINE=InnoDB;


CREATE TABLE restaurant_room (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    floor_number INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE restaurant_table (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    table_number VARCHAR(30) NOT NULL,
    capacity INT NOT NULL,
    x_position INT,
    y_position INT,
    shape VARCHAR(30) DEFAULT 'ROUND',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_table_room
        FOREIGN KEY (room_id)
        REFERENCES restaurant_room(id),

    CONSTRAINT uq_table_number
        UNIQUE (table_number),

    CONSTRAINT chk_table_capacity
        CHECK (capacity > 0)
);

CREATE TABLE reservation (
    id BIGSERIAL PRIMARY KEY,
    reservation_code VARCHAR(40) NOT NULL,
    keycloak_user_id VARCHAR(100) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    customer_email VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(30),
    table_id BIGINT,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    guests_count INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    source VARCHAR(30) DEFAULT 'ONLINE',
    special_requests VARCHAR(500),
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_reservation_table
        FOREIGN KEY (table_id)
        REFERENCES restaurant_table(id),

    CONSTRAINT uq_reservation_code
        UNIQUE (reservation_code),

    CONSTRAINT chk_reservation_guests
        CHECK (guests_count > 0),

    CONSTRAINT chk_reservation_time
        CHECK (end_time > start_time)
);

CREATE TABLE waitlist_entry (
    id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(100) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    customer_email VARCHAR(150) NOT NULL,
    customer_phone VARCHAR(30),
    requested_date DATE NOT NULL,
    requested_time TIME NOT NULL,
    guests_count INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    priority INT DEFAULT 0,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservation_status_history (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(100),
    reason VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_history_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservation(id)
);

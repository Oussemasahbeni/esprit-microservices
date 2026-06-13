CREATE TABLE employee (
    id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(30) NOT NULL,
    restaurant_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    position VARCHAR(100),
    contract_type VARCHAR(30),
    hire_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT uq_employee_keycloak_user_id UNIQUE (keycloak_user_id),
    CONSTRAINT uq_employee_email UNIQUE (email)
);

CREATE INDEX idx_employee_restaurant_id ON employee (restaurant_id);
CREATE INDEX idx_employee_role ON employee (role);

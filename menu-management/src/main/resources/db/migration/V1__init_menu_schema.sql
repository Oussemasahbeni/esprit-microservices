CREATE TABLE menu_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_menu_category_name UNIQUE (name)
);

CREATE TABLE dish (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(140) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10, 2) NOT NULL,
    photo_url VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    category_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_dish_category FOREIGN KEY (category_id) REFERENCES menu_category(id),
    INDEX idx_dish_category (category_id),
    INDEX idx_dish_available (available)
);

CREATE TABLE dish_variant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dish_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price_delta DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_dish_variant_dish FOREIGN KEY (dish_id) REFERENCES dish(id) ON DELETE CASCADE,
    INDEX idx_dish_variant_dish (dish_id)
);

CREATE TABLE dish_ingredient (
    dish_id BIGINT NOT NULL,
    ingredient VARCHAR(120) NOT NULL,
    CONSTRAINT fk_dish_ingredient_dish FOREIGN KEY (dish_id) REFERENCES dish(id) ON DELETE CASCADE,
    INDEX idx_dish_ingredient_dish (dish_id)
);

CREATE TABLE dish_allergen (
    dish_id BIGINT NOT NULL,
    allergen VARCHAR(120) NOT NULL,
    CONSTRAINT fk_dish_allergen_dish FOREIGN KEY (dish_id) REFERENCES dish(id) ON DELETE CASCADE,
    INDEX idx_dish_allergen_dish (dish_id)
);

CREATE TABLE promotion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(140) NOT NULL,
    description VARCHAR(700),
    discount_percent DECIMAL(5, 2) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    dish_id BIGINT,
    category_id BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_promotion_dish FOREIGN KEY (dish_id) REFERENCES dish(id),
    CONSTRAINT fk_promotion_category FOREIGN KEY (category_id) REFERENCES menu_category(id),
    CONSTRAINT chk_promotion_target CHECK (dish_id IS NOT NULL OR category_id IS NOT NULL),
    CONSTRAINT chk_promotion_dates CHECK (ends_at > starts_at),
    INDEX idx_promotion_active (active),
    INDEX idx_promotion_window (starts_at, ends_at)
);

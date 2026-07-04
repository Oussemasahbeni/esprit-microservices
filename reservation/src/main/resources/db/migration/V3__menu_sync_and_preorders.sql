CREATE TABLE reservation_pre_order_item (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    menu_dish_id BIGINT NOT NULL,
    dish_name VARCHAR(140) NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    quantity INT NOT NULL,
    still_available BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_preorder_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservation(id),

    CONSTRAINT chk_preorder_quantity
        CHECK (quantity > 0)
);

CREATE INDEX idx_preorder_dish ON reservation_pre_order_item(menu_dish_id);

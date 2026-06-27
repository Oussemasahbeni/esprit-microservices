INSERT INTO menu_category (name, description, display_order, active)
VALUES
    ('Starters', 'Small plates designed for quick service and sharing.', 1, TRUE),
    ('Mains', 'Core dishes served all day.', 2, TRUE),
    ('Desserts', 'Sweet plates and cafe classics.', 3, TRUE),
    ('Drinks', 'Cold and hot beverages.', 4, TRUE);

INSERT INTO dish (name, description, price, photo_url, available, category_id)
VALUES
    ('Burrata Tomato Plate', 'Creamy burrata with basil, tomato confit and toasted bread.', 18.00, 'https://images.unsplash.com/photo-1625944525533-473f1a3d54e7?auto=format&fit=crop&w=900&q=80', TRUE, 1),
    ('Harissa Chicken Bowl', 'Grilled chicken, herbed rice, pickled vegetables and yogurt sauce.', 24.50, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80', TRUE, 2),
    ('Truffle Mushroom Pasta', 'Fresh tagliatelle, wild mushrooms, parmesan and truffle oil.', 28.00, 'https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?auto=format&fit=crop&w=900&q=80', TRUE, 2),
    ('Pistachio Tiramisu', 'Coffee-soaked biscuit, mascarpone cream and pistachio crumble.', 13.00, 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=900&q=80', FALSE, 3),
    ('Mint Lemonade', 'Fresh lemon juice, mint, sparkling water and cane sugar.', 8.50, 'https://images.unsplash.com/photo-1621263764928-df1444c5e859?auto=format&fit=crop&w=900&q=80', TRUE, 4);

INSERT INTO dish_ingredient (dish_id, ingredient)
VALUES
    (1, 'Burrata'), (1, 'Tomato'), (1, 'Basil'),
    (2, 'Chicken'), (2, 'Rice'), (2, 'Yogurt'),
    (3, 'Tagliatelle'), (3, 'Mushroom'), (3, 'Parmesan'),
    (4, 'Mascarpone'), (4, 'Coffee'), (4, 'Pistachio'),
    (5, 'Lemon'), (5, 'Mint');

INSERT INTO dish_allergen (dish_id, allergen)
VALUES
    (1, 'Milk'), (1, 'Gluten'),
    (2, 'Milk'),
    (3, 'Gluten'), (3, 'Milk'),
    (4, 'Eggs'), (4, 'Milk'), (4, 'Tree nuts');

INSERT INTO dish_variant (dish_id, name, price_delta, available)
VALUES
    (2, 'Extra chicken', 5.00, TRUE),
    (2, 'Large bowl', 4.00, TRUE),
    (3, 'Add burrata', 6.00, TRUE),
    (5, 'Pitcher', 12.00, TRUE);

INSERT INTO promotion (name, description, discount_percent, starts_at, ends_at, active, category_id)
VALUES
    ('Lunch Menu Boost', 'Manager promotion for weekday lunch menus.', 15.00, CURRENT_TIMESTAMP(6), DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 14 DAY), TRUE, 2);

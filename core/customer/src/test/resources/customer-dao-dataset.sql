-- Fixture of CustomerDaoImplTest only. Wipes the customer and order tables (and the catalog rows an order needs),
-- then loads four customers: Alice (1) and Carol (3) and Dan (4) with every column, Bob (2) with NULL telephone,
-- email and address. One category and one product exist so a test can insert an order by SQL.
TRUNCATE TABLE orders, products, categories, customers RESTART IDENTITY CASCADE;

INSERT INTO customers (id, first_name, last_name, telephone, email, address) VALUES
    (1, 'Alice', 'Martin', '+33 1 23 45', 'alice@example.com', '1 Main Street'),
    (2, 'Bob', 'Stone', NULL, NULL, NULL),
    (3, 'Carol', 'Diaz', '555-0303', 'carol@example.com', '3 Side Road'),
    (4, 'Dan', 'Lee', '555-0404', 'dan@example.com', '4 Long Avenue');

INSERT INTO categories (id, category_name) VALUES (1, 'Books');
INSERT INTO products (id, category_id, product_name, unit_price) VALUES (1, 1, 'Novel', 12.50);

-- Explicit ids do not advance the sequences: move them high so generated ids never collide
ALTER SEQUENCE customers_id_seq RESTART WITH 100;
ALTER SEQUENCE categories_id_seq RESTART WITH 100;
ALTER SEQUENCE products_id_seq RESTART WITH 100;
ALTER SEQUENCE orders_id_seq RESTART WITH 100;

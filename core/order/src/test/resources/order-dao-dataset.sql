-- Fixture of OrderDaoImplTest only. Wipes the order, catalog and customer tables, then loads two customers (1, 2),
-- one category, two products (1 at 10.00, 2 at 19.99) and five orders: 1 (customer 1, product 1), 2 (customer 1,
-- product 2), 3 (customer 2, product 1), 4 (customer 2, product 2), 5 (customer 1, product 1).
TRUNCATE TABLE orders, products, categories, customers RESTART IDENTITY CASCADE;

INSERT INTO customers (id, first_name, last_name) VALUES (1, 'Alice', 'Martin'), (2, 'Bob', 'Stone');

INSERT INTO categories (id, category_name) VALUES (1, 'Books');
INSERT INTO products (id, category_id, product_name, unit_price) VALUES (1, 1, 'Novel', 10.00), (2, 1, 'Atlas', 19.99);

INSERT INTO orders (id, customer_id, product_id, quantity, total) VALUES
    (1, 1, 1, 2, 20.00),
    (2, 1, 2, 3, 59.97),
    (3, 2, 1, 1, 10.00),
    (4, 2, 2, 5, 99.95),
    (5, 1, 1, 4, 40.00);

-- Explicit ids do not advance the sequences: move them high so generated ids never collide
ALTER SEQUENCE customers_id_seq RESTART WITH 100;
ALTER SEQUENCE categories_id_seq RESTART WITH 100;
ALTER SEQUENCE products_id_seq RESTART WITH 100;
ALTER SEQUENCE orders_id_seq RESTART WITH 100;

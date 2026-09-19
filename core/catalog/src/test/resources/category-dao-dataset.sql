-- Fixture of CategoryDaoImplTest only. Wipes the catalog tables (products first, by cascade), then loads three
-- categories: Books (1), Games (2) and Toys (3). Category 3 has one product so the foreign key can be exercised.
TRUNCATE TABLE products, categories RESTART IDENTITY CASCADE;

INSERT INTO categories (id, category_name) VALUES (1, 'Books'), (2, 'Games'), (3, 'Toys');

INSERT INTO products (id, category_id, product_name, unit_price) VALUES (1, 3, 'Robot', 19.90);

-- Explicit ids do not advance the sequences: move them high so generated ids never collide
ALTER SEQUENCE categories_id_seq RESTART WITH 100;
ALTER SEQUENCE products_id_seq RESTART WITH 100;

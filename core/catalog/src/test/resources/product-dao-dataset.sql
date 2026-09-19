-- Fixture of ProductDaoImplTest only. Wipes the catalog tables, then loads three categories (Books 1, Games 2 and
-- Empty 3 which holds no product) and four products: Novel (1) and Atlas (2) in Books, Chess (3) and Cards (4)
-- in Games. Ids are ordered so pagination is predictable.
TRUNCATE TABLE products, categories RESTART IDENTITY CASCADE;

INSERT INTO categories (id, category_name) VALUES (1, 'Books'), (2, 'Games'), (3, 'Empty');

INSERT INTO products (id, category_id, product_name, unit_price) VALUES
    (1, 1, 'Novel', 12.50),
    (2, 1, 'Atlas', 30.00),
    (3, 2, 'Chess', 25.99),
    (4, 2, 'Cards', 0.00);

-- Explicit ids do not advance the sequences: move them high so generated ids never collide
ALTER SEQUENCE categories_id_seq RESTART WITH 100;
ALTER SEQUENCE products_id_seq RESTART WITH 100;

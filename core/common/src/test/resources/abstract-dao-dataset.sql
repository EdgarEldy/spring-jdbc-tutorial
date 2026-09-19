-- Fixture of AbstractDaoTest only: two categories on a clean table.
DELETE FROM products;
DELETE FROM categories;
INSERT INTO categories (id, category_name) VALUES (1, 'Books'), (2, 'Tools');
-- Explicit ids do not advance the sequence: restart it high so generated ids never collide
ALTER SEQUENCE categories_id_seq RESTART WITH 100;

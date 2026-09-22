INSERT INTO produto (nome, preco)
SELECT v.nome, v.preco
FROM (VALUES
    ('Notebook', 3500.00),
    ('Mouse sem fio', 79.90),
    ('Teclado mecanico', 299.90),
    ('Monitor 27 polegadas', 1299.00),
    ('Webcam Full HD', 199.90),
    ('Headset gamer', 249.50),
    ('SSD 1TB', 459.90),
    ('Cadeira de escritorio', 899.00),
    ('Carregador USB-C 65W', 129.90),
    ('Smartphone', 2199.00)
) AS v (nome, preco)
WHERE NOT EXISTS (SELECT 1 FROM produto);
